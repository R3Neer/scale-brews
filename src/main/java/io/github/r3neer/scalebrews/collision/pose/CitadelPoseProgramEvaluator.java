package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Common/dedicated evaluator for revision-bound Citadel pose programs. */
public final class CitadelPoseProgramEvaluator {
    private CitadelPoseProgramEvaluator() {}

    @FunctionalInterface
    public interface Bound {
        Optional<Map<String, Matrix4f>> evaluate(PoseEngine.Inputs inputs);
    }

    public static Optional<Bound> bind(ModelGeometry geometry, CitadelPoseProgram program) {
        if (geometry == null || program == null
                || !geometry.source().equals(program.source()) || !geometry.version().equals(program.version()))
            return Optional.empty();

        var rest = new LinkedHashMap<String, ModelGeometry.SourcePose>();
        for (var part : geometry.parts()) {
            if (part.sourcePose() != null) rest.put(part.id(), part.sourcePose());
        }
        if (!validateBones(program, rest)) return Optional.empty();

        var clips = new LinkedHashMap<Integer, CitadelPoseProgram.Clip>();
        for (var clip : program.clips()) clips.put(clip.animation(), clip);
        Map<String, ModelGeometry.SourcePose> immutableRest = Map.copyOf(rest);
        Map<Integer, CitadelPoseProgram.Clip> immutableClips = Map.copyOf(clips);
        return Optional.of(inputs -> evaluate(immutableRest, immutableClips, program, inputs));
    }

    private static boolean validateBones(CitadelPoseProgram program, Map<String, ModelGeometry.SourcePose> rest) {
        for (var clip : program.clips()) {
            for (var frame : clip.keyframes()) {
                // ModelAnimator's stationary branch ignores transformMap. Supporting transforms authored
                // inside a stationary frame would require reproducing its intentionally odd carry-over state.
                // No accepted 2.1.9 family proof uses that form, so reject it rather than approximate it.
                if (frame.stationary() && !frame.deltas().isEmpty()) return false;
                for (var delta : frame.deltas()) if (!rest.containsKey(delta.bone())) return false;
            }
        }
        for (var operation : program.operations()) {
            if (operation.type() == CitadelPoseProgram.OperationType.FACE_TARGET) {
                for (var bone : operation.bones()) if (!rest.containsKey(bone)) return false;
            } else if (!rest.containsKey(operation.bone())) {
                return false;
            }
        }
        return true;
    }

    private static Optional<Map<String, Matrix4f>> evaluate(Map<String, ModelGeometry.SourcePose> rest,
                                                             Map<Integer, CitadelPoseProgram.Clip> clips,
                                                             CitadelPoseProgram program,
                                                             PoseEngine.Inputs inputs) {
        if (inputs == null) return Optional.empty();
        var poses = new LinkedHashMap<String, MutablePose>();
        if (!applyClip(rest, clips, inputs, poses)) return Optional.empty();
        for (var operation : program.operations()) {
            if (!condition(operation.when(), inputs)) continue;
            if (!applyOperation(rest, operation, inputs, poses)) return Optional.empty();
        }

        var result = new LinkedHashMap<String, Matrix4f>();
        for (var entry : poses.entrySet()) {
            var pose = entry.getValue();
            if (!pose.valid()) return Optional.empty();
            result.put(entry.getKey(), pose.matrix());
        }
        return Optional.of(Map.copyOf(result));
    }

    private static boolean applyClip(Map<String, ModelGeometry.SourcePose> rest,
                                     Map<Integer, CitadelPoseProgram.Clip> clips,
                                     PoseEngine.Inputs inputs,
                                     Map<String, MutablePose> poses) {
        if (clips.isEmpty()) return true;
        float animationValue = inputs.channel(CitadelPoseProgram.ANIMATION_CHANNEL, Float.NaN);
        float tickValue = inputs.channel(CitadelPoseProgram.ANIMATION_TICK_CHANNEL, Float.NaN);
        if (!integral(animationValue) || !integral(tickValue) || animationValue < 0 || tickValue < 0) return false;
        int animation = (int) animationValue;
        int tick = (int) tickValue;
        if (animation == 0) return true;
        var clip = clips.get(animation);
        if (clip == null) return false;

        float partial = inputs.channels().containsKey(CitadelPoseProgram.ANIMATION_PARTIAL_CHANNEL)
            ? inputs.channel(CitadelPoseProgram.ANIMATION_PARTIAL_CHANNEL, Float.NaN) : 0;
        if (!Float.isFinite(partial) || partial < 0 || partial >= 1) return false;

        Map<String, CitadelPoseProgram.Delta> previous = Map.of();
        int start = 0;
        for (var frame : clip.keyframes()) {
            int end;
            try { end = Math.addExact(start, frame.durationTicks()); }
            catch (ArithmeticException invalid) { return false; }
            Map<String, CitadelPoseProgram.Delta> current = deltas(frame);
            if (tick >= start && tick < end) {
                if (frame.stationary()) {
                    applyDeltas(rest, poses, previous, 1);
                } else {
                    float fraction = (tick - start + partial) / frame.durationTicks();
                    float inc = Mth.sin((float) (fraction * Math.PI / 2.0));
                    float dec = 1.0f - inc;
                    applyDeltas(rest, poses, previous, dec);
                    applyDeltas(rest, poses, current, inc);
                }
                return poses.values().stream().allMatch(MutablePose::valid);
            }
            if (!frame.stationary()) previous = current;
            start = end;
        }
        // ModelAnimator contributes no keyframe transform once the animation tick is outside all frames.
        return true;
    }

    private static Map<String, CitadelPoseProgram.Delta> deltas(CitadelPoseProgram.Keyframe frame) {
        var result = new LinkedHashMap<String, CitadelPoseProgram.Delta>();
        for (var delta : frame.deltas()) result.put(delta.bone(), delta);
        return Map.copyOf(result);
    }

    private static void applyDeltas(Map<String, ModelGeometry.SourcePose> rest,
                                    Map<String, MutablePose> poses,
                                    Map<String, CitadelPoseProgram.Delta> deltas,
                                    float factor) {
        if (factor == 0) return;
        for (var delta : deltas.values()) {
            var pose = pose(rest, poses, delta.bone());
            pose.xRot += factor * delta.rotX();
            pose.yRot += factor * delta.rotY();
            pose.zRot += factor * delta.rotZ();
            pose.x += factor * delta.posX();
            pose.y += factor * delta.posY();
            pose.z += factor * delta.posZ();
        }
    }

    private static boolean applyOperation(Map<String, ModelGeometry.SourcePose> rest,
                                          CitadelPoseProgram.Operation operation,
                                          PoseEngine.Inputs inputs,
                                          Map<String, MutablePose> poses) {
        switch (operation.type()) {
            case ADD_ROTATION -> {
                var pose = pose(rest, poses, operation.bone());
                pose.xRot += scalar(operation.x(), inputs);
                pose.yRot += scalar(operation.y(), inputs);
                pose.zRot += scalar(operation.z(), inputs);
            }
            case ADD_POSITION -> {
                var pose = pose(rest, poses, operation.bone());
                pose.x += scalar(operation.x(), inputs);
                pose.y += scalar(operation.y(), inputs);
                pose.z += scalar(operation.z(), inputs);
            }
            case SET_SCALE -> {
                var pose = pose(rest, poses, operation.bone());
                pose.xScale = scalar(operation.x(), inputs);
                pose.yScale = scalar(operation.y(), inputs);
                pose.zScale = scalar(operation.z(), inputs);
            }
            case WALK, SWING, FLAP -> {
                float clock = scalar(operation.clock(), inputs);
                float amount = scalar(operation.amount(), inputs);
                float dir = operation.invert() ? -1.0f : 1.0f;
                float value = dir * (Mth.cos(clock * operation.speed() + operation.phase())
                    * operation.degree() * amount + operation.weight() * amount);
                var pose = pose(rest, poses, operation.bone());
                if (operation.type() == CitadelPoseProgram.OperationType.WALK) pose.xRot += value;
                else if (operation.type() == CitadelPoseProgram.OperationType.SWING) pose.yRot += value;
                else pose.zRot += value;
            }
            case BOB -> {
                float clock = scalar(operation.clock(), inputs);
                float amount = scalar(operation.amount(), inputs);
                float value = Mth.cos(clock * operation.speed()) * operation.degree() * amount;
                if (operation.bounce()) value = -Math.abs(value);
                pose(rest, poses, operation.bone()).y += value;
            }
            case FACE_TARGET -> {
                float yaw = inputs.headYaw() * Mth.DEG_TO_RAD / operation.divisor();
                float pitch = inputs.headPitch() * Mth.DEG_TO_RAD / operation.divisor();
                for (var bone : operation.bones()) {
                    var pose = pose(rest, poses, bone);
                    pose.yRot += yaw;
                    pose.xRot += pitch;
                }
            }
            case PROGRESS_ROTATION -> {
                var base = rest.get(operation.bone());
                var pose = pose(rest, poses, operation.bone());
                float progress = scalar(operation.progress(), inputs) / operation.divisor();
                pose.xRot += progress * (scalar(operation.x(), inputs) - base.xRot());
                pose.yRot += progress * (scalar(operation.y(), inputs) - base.yRot());
                pose.zRot += progress * (scalar(operation.z(), inputs) - base.zRot());
            }
            case PROGRESS_POSITION -> {
                var base = rest.get(operation.bone());
                var pose = pose(rest, poses, operation.bone());
                float progress = scalar(operation.progress(), inputs) / operation.divisor();
                pose.x += progress * (scalar(operation.x(), inputs) - base.x());
                pose.y += progress * (scalar(operation.y(), inputs) - base.y());
                pose.z += progress * (scalar(operation.z(), inputs) - base.z());
            }
        }
        return poses.values().stream().allMatch(MutablePose::valid);
    }

    private static boolean condition(CitadelPoseProgram.Condition condition, PoseEngine.Inputs inputs) {
        return switch (condition.type()) {
            case ALWAYS -> true;
            case FLAG -> inputs.channel(condition.channel(), Float.NaN) > 0.5f;
            case NOT_FLAG -> inputs.channel(condition.channel(), Float.NaN) <= 0.5f;
            case GT -> inputs.channel(condition.channel(), Float.NaN) > condition.threshold();
            case GE -> inputs.channel(condition.channel(), Float.NaN) >= condition.threshold();
            case LT -> inputs.channel(condition.channel(), Float.NaN) < condition.threshold();
            case LE -> inputs.channel(condition.channel(), Float.NaN) <= condition.threshold();
            case EQ -> Float.compare(inputs.channel(condition.channel(), Float.NaN), condition.threshold()) == 0;
            case ALL -> condition.terms().stream().allMatch(term -> condition(term, inputs));
            case ANY -> condition.terms().stream().anyMatch(term -> condition(term, inputs));
        };
    }

    private static float scalar(CitadelPoseProgram.Scalar scalar, PoseEngine.Inputs inputs) {
        float base = switch (scalar.source()) {
            case CONSTANT -> 0;
            case WALK_PHASE -> inputs.walkPhase();
            case WALK_AMOUNT -> inputs.walkAmount();
            case AGE -> inputs.age();
            case HEAD_YAW -> inputs.headYaw();
            case HEAD_PITCH -> inputs.headPitch();
            case CHANNEL -> inputs.channel(scalar.channel(), Float.NaN);
        };
        return base * scalar.scale() + scalar.offset();
    }

    private static MutablePose pose(Map<String, ModelGeometry.SourcePose> rest,
                                    Map<String, MutablePose> poses, String bone) {
        return poses.computeIfAbsent(bone, ignored -> new MutablePose(rest.get(bone)));
    }

    private static boolean integral(float value) {
        return Float.isFinite(value) && value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE && value == (int) value;
    }

    private static final class MutablePose {
        float x, y, z, xRot, yRot, zRot, xScale, yScale, zScale;

        MutablePose(ModelGeometry.SourcePose source) {
            x = source.x(); y = source.y(); z = source.z();
            xRot = source.xRot(); yRot = source.yRot(); zRot = source.zRot();
            xScale = source.xScale(); yScale = source.yScale(); zScale = source.zScale();
        }

        boolean valid() {
            return finite(x, y, z, xRot, yRot, zRot, xScale, yScale, zScale)
                && Math.abs(x) <= 65536 && Math.abs(y) <= 65536 && Math.abs(z) <= 65536
                && Math.abs(xRot) <= 65536 && Math.abs(yRot) <= 65536 && Math.abs(zRot) <= 65536
                && Math.abs(xScale) <= 65536 && Math.abs(yScale) <= 65536 && Math.abs(zScale) <= 65536
                && Math.abs(xScale) >= 1e-12f && Math.abs(yScale) >= 1e-12f && Math.abs(zScale) >= 1e-12f;
        }

        Matrix4f matrix() {
            return new ModelGeometry.SourcePose(x, y, z, xRot, yRot, zRot, xScale, yScale, zScale).matrix();
        }

        private static boolean finite(float... values) {
            for (float value : values) if (!Float.isFinite(value)) return false;
            return true;
        }
    }
}
