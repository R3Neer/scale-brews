package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Neutral, server-safe program for Citadel-style procedural animation and ModelAnimator clips.
 *
 * <p>The program contains no Citadel, renderer or entity classes. Model-specific behaviour lives in
 * revision-local data while the evaluator remains reusable across every model that speaks the same
 * small animation language.</p>
 */
public record CitadelPoseProgram(int schema, String source, String version,
                                 List<Clip> clips, List<Operation> operations) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_CLIPS = 128;
    public static final int MAX_KEYFRAMES = 4096;
    public static final int MAX_DELTAS = 8192;
    public static final int MAX_OPERATIONS = 2048;
    public static final int MAX_CONDITION_DEPTH = 16;
    public static final int MAX_CONDITION_NODES = 4096;
    public static final String ANIMATION_CHANNEL = "citadel.animation";
    public static final String ANIMATION_TICK_CHANNEL = "citadel.animation_tick";
    public static final String ANIMATION_PARTIAL_CHANNEL = "citadel.animation_partial";

    public enum ScalarSource { CONSTANT, WALK_PHASE, WALK_AMOUNT, AGE, HEAD_YAW, HEAD_PITCH, CHANNEL }

    /** Value = source * scale + offset; CONSTANT has source value zero and therefore uses offset. */
    public record Scalar(ScalarSource source, String channel, float scale, float offset) {
        public Scalar {
            Objects.requireNonNull(source, "source");
            if (!Float.isFinite(scale) || !Float.isFinite(offset) || Math.abs(scale) > 1_000_000 || Math.abs(offset) > 1_000_000)
                throw new IllegalArgumentException("Invalid Citadel scalar");
            if (source == ScalarSource.CHANNEL) {
                if (!validChannel(channel)) throw new IllegalArgumentException("Invalid Citadel scalar channel");
            } else if (channel != null && !channel.isEmpty()) {
                throw new IllegalArgumentException("Non-channel Citadel scalar cannot name a channel");
            }
        }
        public static Scalar constant(float value) { return new Scalar(ScalarSource.CONSTANT, null, 0, value); }
        public static Scalar builtin(ScalarSource source) { return new Scalar(source, null, 1, 0); }
        public static Scalar channel(String name) { return new Scalar(ScalarSource.CHANNEL, name, 1, 0); }
    }

    public enum ConditionType { ALWAYS, FLAG, NOT_FLAG, GT, GE, LT, LE, EQ, ALL, ANY }

    /** Bounded declarative predicate; scalar leaves read custom authoritative channels only. */
    public record Condition(ConditionType type, String channel, float threshold, List<Condition> terms) {
        public Condition {
            Objects.requireNonNull(type, "type");
            if (!Float.isFinite(threshold)) throw new IllegalArgumentException("Invalid Citadel condition threshold");
            terms = terms == null ? List.of() : List.copyOf(terms);
            switch (type) {
                case ALWAYS -> {
                    if ((channel != null && !channel.isEmpty()) || !terms.isEmpty())
                        throw new IllegalArgumentException("ALWAYS condition cannot carry operands");
                }
                case FLAG, NOT_FLAG, GT, GE, LT, LE, EQ -> {
                    if (!validChannel(channel) || !terms.isEmpty())
                        throw new IllegalArgumentException("Invalid Citadel channel condition");
                }
                case ALL, ANY -> {
                    if ((channel != null && !channel.isEmpty()) || terms.isEmpty() || terms.size() > 32)
                        throw new IllegalArgumentException("Invalid Citadel compound condition");
                }
            }
        }
        public static Condition always() { return new Condition(ConditionType.ALWAYS, null, 0, List.of()); }
    }

    public enum OperationType {
        ADD_ROTATION, ADD_POSITION, SET_SCALE,
        WALK, SWING, FLAP, BOB, FACE_TARGET,
        PROGRESS_ROTATION, PROGRESS_POSITION
    }

    /**
     * One Citadel primitive. Fields are deliberately explicit rather than an expression bytecode:
     * data can express the real helper calls while evaluation stays bounded and inspectable.
     */
    public record Operation(OperationType type, String bone, List<String> bones, Condition when,
                            Scalar x, Scalar y, Scalar z, Scalar clock, Scalar amount, Scalar progress,
                            float speed, float degree, float phase, float weight, float divisor,
                            boolean invert, boolean bounce) {
        public Operation {
            Objects.requireNonNull(type, "type");
            bones = bones == null ? List.of() : List.copyOf(bones);
            when = when == null ? Condition.always() : when;
            if (!finite(speed, degree, phase, weight, divisor)
                    || Math.abs(speed) > 1_000_000 || Math.abs(degree) > 1_000_000
                    || Math.abs(phase) > 1_000_000 || Math.abs(weight) > 1_000_000 || Math.abs(divisor) > 1_000_000)
                throw new IllegalArgumentException("Invalid Citadel operation constants");
            switch (type) {
                case ADD_ROTATION, ADD_POSITION, SET_SCALE -> {
                    requireBone(bone); requireScalars(x, y, z);
                    if (!bones.isEmpty()) throw new IllegalArgumentException("Single-bone operation has bone list");
                }
                case WALK, SWING, FLAP -> {
                    requireBone(bone); requireScalars(clock, amount);
                    if (!bones.isEmpty()) throw new IllegalArgumentException("Wave operation has bone list");
                }
                case BOB -> {
                    requireBone(bone); requireScalars(clock, amount);
                    if (!bones.isEmpty()) throw new IllegalArgumentException("Bob operation has bone list");
                }
                case FACE_TARGET -> {
                    if (bone != null && !bone.isEmpty()) throw new IllegalArgumentException("Face-target uses bone list");
                    if (bones.isEmpty() || bones.size() > 64 || bones.stream().anyMatch(value -> !validBone(value)) || divisor == 0)
                        throw new IllegalArgumentException("Invalid face-target operation");
                }
                case PROGRESS_ROTATION, PROGRESS_POSITION -> {
                    requireBone(bone); requireScalars(x, y, z, progress);
                    if (!bones.isEmpty() || divisor == 0) throw new IllegalArgumentException("Invalid progress operation");
                }
            }
        }
    }

    public record Delta(String bone, float rotX, float rotY, float rotZ, float posX, float posY, float posZ) {
        public Delta {
            requireBone(bone);
            if (!finite(rotX, rotY, rotZ, posX, posY, posZ)
                    || maxAbs(rotX, rotY, rotZ, posX, posY, posZ) > 65536)
                throw new IllegalArgumentException("Invalid Citadel keyframe delta");
        }
    }

    /** stationary mirrors ModelAnimator#setStaticKeyframe; an empty non-stationary frame is resetKeyframe. */
    public record Keyframe(int durationTicks, boolean stationary, List<Delta> deltas) {
        public Keyframe {
            if (durationTicks <= 0 || durationTicks > 1_000_000 || deltas == null || deltas.size() > 512)
                throw new IllegalArgumentException("Invalid Citadel keyframe");
            deltas = List.copyOf(deltas);
            var seen = new LinkedHashSet<String>();
            for (var delta : deltas) {
                Objects.requireNonNull(delta, "delta");
                if (!seen.add(delta.bone())) throw new IllegalArgumentException("Duplicate Citadel keyframe bone " + delta.bone());
            }
        }
    }

    /** animation is a program-local numeric token supplied by the authoritative channel adapter; zero means no clip. */
    public record Clip(int animation, List<Keyframe> keyframes) {
        public Clip {
            if (animation <= 0 || keyframes == null || keyframes.isEmpty() || keyframes.size() > 1024)
                throw new IllegalArgumentException("Invalid Citadel animation clip");
            keyframes = List.copyOf(keyframes);
            long duration = 0;
            for (var frame : keyframes) {
                Objects.requireNonNull(frame, "keyframe");
                duration += frame.durationTicks();
                if (duration > 1_000_000) throw new IllegalArgumentException("Citadel clip duration too large");
            }
        }
    }

    public CitadelPoseProgram {
        if (schema != SCHEMA_VERSION || source == null || source.isBlank() || source.length() > 512
                || version == null || version.isBlank() || version.length() > 128
                || clips == null || clips.size() > MAX_CLIPS || operations == null || operations.size() > MAX_OPERATIONS)
            throw new IllegalArgumentException("Invalid Citadel pose program");
        clips = List.copyOf(clips);
        operations = List.copyOf(operations);
        var animations = new LinkedHashSet<Integer>();
        int frames = 0, deltas = 0;
        for (var clip : clips) {
            Objects.requireNonNull(clip, "clip");
            if (!animations.add(clip.animation())) throw new IllegalArgumentException("Duplicate Citadel animation token " + clip.animation());
            frames = Math.addExact(frames, clip.keyframes().size());
            if (frames > MAX_KEYFRAMES) throw new IllegalArgumentException("Too many Citadel keyframes");
            for (var frame : clip.keyframes()) {
                deltas = Math.addExact(deltas, frame.deltas().size());
                if (deltas > MAX_DELTAS) throw new IllegalArgumentException("Too many Citadel keyframe deltas");
            }
        }
        int conditionNodes = 0;
        for (var operation : operations) {
            Objects.requireNonNull(operation, "operation");
            conditionNodes = Math.addExact(conditionNodes, validateConditionDepth(operation.when(), 0));
            if (conditionNodes > MAX_CONDITION_NODES)
                throw new IllegalArgumentException("Too many Citadel condition nodes");
        }
    }

    /** Channels implied by the program itself; a binding must declare these explicitly. */
    public Set<String> requiredChannels() {
        var result = new LinkedHashSet<String>();
        if (!clips.isEmpty()) {
            result.add(ANIMATION_CHANNEL);
            result.add(ANIMATION_TICK_CHANNEL);
        }
        for (var operation : operations) collect(operation, result);
        if (result.size() > 64) throw new IllegalArgumentException("Citadel program requires too many channels");
        return Set.copyOf(result);
    }

    public static CitadelPoseProgram validatedCopy(CitadelPoseProgram raw) {
        if (raw == null) throw new IllegalArgumentException("Missing Citadel pose program");
        var clips = raw.clips() == null ? null : raw.clips().stream().map(clip -> {
            if (clip == null) throw new IllegalArgumentException("Missing Citadel clip");
            var frames = clip.keyframes() == null ? null : clip.keyframes().stream().map(frame -> {
                if (frame == null) throw new IllegalArgumentException("Missing Citadel keyframe");
                var deltas = frame.deltas() == null ? null : frame.deltas().stream().map(delta -> {
                    if (delta == null) throw new IllegalArgumentException("Missing Citadel delta");
                    return new Delta(delta.bone(), delta.rotX(), delta.rotY(), delta.rotZ(), delta.posX(), delta.posY(), delta.posZ());
                }).toList();
                return new Keyframe(frame.durationTicks(), frame.stationary(), deltas);
            }).toList();
            return new Clip(clip.animation(), frames);
        }).toList();
        var operations = raw.operations() == null ? null : raw.operations().stream().map(CitadelPoseProgram::copyOperation).toList();
        return new CitadelPoseProgram(raw.schema(), raw.source(), raw.version(), clips, operations);
    }

    private static Operation copyOperation(Operation operation) {
        if (operation == null) throw new IllegalArgumentException("Missing Citadel operation");
        return new Operation(operation.type(), operation.bone(), operation.bones(), copyCondition(operation.when()),
            copyScalar(operation.x()), copyScalar(operation.y()), copyScalar(operation.z()), copyScalar(operation.clock()),
            copyScalar(operation.amount()), copyScalar(operation.progress()), operation.speed(), operation.degree(),
            operation.phase(), operation.weight(), operation.divisor(), operation.invert(), operation.bounce());
    }

    private static Scalar copyScalar(Scalar scalar) {
        return scalar == null ? null : new Scalar(scalar.source(), scalar.channel(), scalar.scale(), scalar.offset());
    }

    private static Condition copyCondition(Condition condition) {
        if (condition == null) return null;
        var terms = condition.terms() == null ? null : condition.terms().stream().map(CitadelPoseProgram::copyCondition).toList();
        return new Condition(condition.type(), condition.channel(), condition.threshold(), terms);
    }

    private static void collect(Operation operation, Set<String> result) {
        collect(operation.when(), result);
        collect(operation.x(), result); collect(operation.y(), result); collect(operation.z(), result);
        collect(operation.clock(), result); collect(operation.amount(), result); collect(operation.progress(), result);
    }
    private static void collect(Condition condition, Set<String> result) {
        if (condition == null) return;
        switch (condition.type()) {
            case FLAG, NOT_FLAG, GT, GE, LT, LE, EQ -> result.add(condition.channel());
            case ALL, ANY -> condition.terms().forEach(term -> collect(term, result));
            case ALWAYS -> {}
        }
    }
    private static void collect(Scalar scalar, Set<String> result) {
        if (scalar != null && scalar.source() == ScalarSource.CHANNEL) result.add(scalar.channel());
    }

    private static int validateConditionDepth(Condition condition, int depth) {
        if (condition == null || depth > MAX_CONDITION_DEPTH) throw new IllegalArgumentException("Invalid Citadel condition depth");
        int nodes = 1;
        for (var term : condition.terms()) nodes = Math.addExact(nodes, validateConditionDepth(term, depth + 1));
        return nodes;
    }
    private static void requireBone(String bone) {
        if (!validBone(bone)) throw new IllegalArgumentException("Invalid Citadel bone id");
    }
    private static boolean validBone(String bone) { return bone != null && !bone.isBlank() && bone.length() <= 256; }
    private static boolean validChannel(String channel) { return channel != null && channel.matches("[a-z0-9_.-]{1,64}"); }
    private static void requireScalars(Scalar... values) {
        for (var value : values) Objects.requireNonNull(value, "Citadel scalar");
    }
    private static boolean finite(float... values) { for (float value : values) if (!Float.isFinite(value)) return false; return true; }
    private static float maxAbs(float... values) { float max = 0; for (float value : values) max = Math.max(max, Math.abs(value)); return max; }
}
