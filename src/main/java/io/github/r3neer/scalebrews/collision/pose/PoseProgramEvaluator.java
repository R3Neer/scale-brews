package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure server-safe evaluator for an already validated revision-local {@link PoseProgram}. */
public final class PoseProgramEvaluator {
    private PoseProgramEvaluator() {}

    public record Bound(PoseProgram program, Map<String, String> parts) {
        public Bound {
            if (program == null || parts == null) throw new IllegalArgumentException("Invalid bound pose program");
            parts = Collections.unmodifiableMap(new LinkedHashMap<>(parts));
        }

        public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, float timeSeconds, float amplitude) {
            if (geometry == null || !Float.isFinite(timeSeconds) || !Float.isFinite(amplitude)) return Optional.empty();
            float time = normalize(program, timeSeconds);
            Map<String, ModelGeometry.Part> byId = new LinkedHashMap<>();
            geometry.parts().forEach(part -> byId.put(part.id(), part));
            Map<String, RuntimePose> poses = new LinkedHashMap<>();

            for (var track : program.tracks()) {
                String partId = parts.get(track.bone());
                if (partId == null) return Optional.empty();
                var part = byId.get(partId);
                if (part == null || part.sourcePose() == null) return Optional.empty();
                var sampled = scale(sample(track, time), amplitude);
                if (!sampled.finite()) return Optional.empty();

                // AnimationDefinition applies every channel directly to the ModelPart fields in channel order.
                // Preserve that float-addition order instead of summing deltas separately and adding once later.
                var pose = poses.computeIfAbsent(partId, ignored -> new RuntimePose(part.sourcePose()));
                pose.apply(track.target(), sampled);
                if (!pose.finite()) return Optional.empty();
            }

            Map<String, Matrix4f> out = new LinkedHashMap<>();
            for (var entry : poses.entrySet()) {
                var pose = entry.getValue();
                if (Math.abs(pose.xScale) < 1e-12f || Math.abs(pose.yScale) < 1e-12f || Math.abs(pose.zScale) < 1e-12f)
                    return Optional.empty();
                out.put(entry.getKey(), pose.matrix());
            }
            try {
                geometry.transforms(out);
                return Optional.of(Collections.unmodifiableMap(out));
            } catch (RuntimeException invalid) {
                return Optional.empty();
            }
        }
    }

    public static Optional<Bound> bind(ModelGeometry geometry, PoseProgram program) {
        if (geometry == null || program == null || !geometry.version().equals(program.version())) return Optional.empty();
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        Map<String, ModelGeometry.Part> byId = new LinkedHashMap<>();
        for (var part : geometry.parts()) {
            byId.put(part.id(), part);
            aliases.computeIfAbsent(part.id(), ignored -> new ArrayList<>()).add(part.id());
            String simple = part.id().substring(part.id().lastIndexOf('/') + 1);
            if (!simple.equals(part.id())) aliases.computeIfAbsent(simple, ignored -> new ArrayList<>()).add(part.id());
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        for (var track : program.tracks()) {
            var matches = aliases.get(track.bone());
            if (matches == null || matches.size() != 1) return Optional.empty();
            String partId = matches.getFirst();
            var part = byId.get(partId);
            if (part == null || part.sourcePose() == null) return Optional.empty();
            resolved.put(track.bone(), partId);
        }
        return Optional.of(new Bound(program, resolved));
    }

    private static final class RuntimePose {
        float x, y, z, xRot, yRot, zRot, xScale, yScale, zScale;

        RuntimePose(ModelGeometry.SourcePose source) {
            x = source.x();
            y = source.y();
            z = source.z();
            xRot = source.xRot();
            yRot = source.yRot();
            zRot = source.zRot();
            xScale = source.xScale();
            yScale = source.yScale();
            zScale = source.zScale();
        }

        void apply(PoseProgram.Target target, RuntimeVector value) {
            switch (target) {
                case TRANSLATION -> {
                    x += value.x();
                    y += value.y();
                    z += value.z();
                }
                case ROTATION -> {
                    xRot += value.x();
                    yRot += value.y();
                    zRot += value.z();
                }
                case SCALE -> {
                    xScale += value.x();
                    yScale += value.y();
                    zScale += value.z();
                }
            }
        }

        boolean finite() {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)
                && Float.isFinite(xRot) && Float.isFinite(yRot) && Float.isFinite(zRot)
                && Float.isFinite(xScale) && Float.isFinite(yScale) && Float.isFinite(zScale);
        }

        Matrix4f matrix() {
            return new Matrix4f().translationRotateScale(
                new Vector3f(x / 16f, y / 16f, z / 16f),
                new Quaternionf().rotationZYX(zRot, yRot, xRot),
                new Vector3f(xScale, yScale, zScale));
        }
    }

    /** Runtime math value; serialized {@link PoseProgram.Vector} bounds do not apply after interpolation/amplitude. */
    private record RuntimeVector(float x, float y, float z) {
        static RuntimeVector from(PoseProgram.Vector value) {
            return new RuntimeVector(value.x(), value.y(), value.z());
        }

        boolean finite() {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
        }
    }

    private static float normalize(PoseProgram program, float time) {
        if (program.loop()) {
            // KeyframeAnimation uses Java remainder directly. Negative clocks stay negative and
            // subsequently clamp to the first interpolation segment rather than wrapping positive.
            return time % program.durationSeconds();
        }
        return Math.clamp(time, 0, program.durationSeconds());
    }

    private static RuntimeVector sample(PoseProgram.Track track, float time) {
        var frames = track.keyframes();

        // Mirror AnimationChannel selection: choose the interval immediately before the first
        // keyframe whose timestamp is >= time, then clamp both indices at the ends.
        int insertion = 0;
        while (insertion < frames.size() && time > frames.get(insertion).timestamp()) insertion++;
        int previous = Math.max(0, insertion - 1);
        int next = Math.min(frames.size() - 1, previous + 1);
        if (insertion >= frames.size()) {
            previous = frames.size() - 1;
            next = previous;
        }

        var a = frames.get(previous);
        var b = frames.get(next);
        float span = b.timestamp() - a.timestamp();
        float t = previous == next || !(span > 0) ? 0
            : Math.clamp((time - a.timestamp()) / span, 0, 1);

        return switch (b.interpolation()) {
            case LINEAR -> lerp(RuntimeVector.from(a.postTarget()), RuntimeVector.from(b.preTarget()), t);
            case CATMULL_ROM -> {
                // Mojang CATMULL_ROM uses postTarget at all four clamped keyframe indices.
                var p0 = RuntimeVector.from(frames.get(Math.max(0, previous - 1)).postTarget());
                var p1 = RuntimeVector.from(a.postTarget());
                var p2 = RuntimeVector.from(b.postTarget());
                var p3 = RuntimeVector.from(frames.get(Math.min(frames.size() - 1, next + 1)).postTarget());
                yield catmull(p0, p1, p2, p3, t);
            }
        };
    }

    private static RuntimeVector scale(RuntimeVector value, float scale) {
        return new RuntimeVector(value.x() * scale, value.y() * scale, value.z() * scale);
    }

    private static RuntimeVector lerp(RuntimeVector a, RuntimeVector b, float t) {
        return new RuntimeVector(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t, a.z() + (b.z() - a.z()) * t);
    }

    private static RuntimeVector catmull(RuntimeVector p0, RuntimeVector p1, RuntimeVector p2, RuntimeVector p3, float t) {
        return new RuntimeVector(catmull(p0.x(), p1.x(), p2.x(), p3.x(), t),
            catmull(p0.y(), p1.y(), p2.y(), p3.y(), t), catmull(p0.z(), p1.z(), p2.z(), p3.z(), t));
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return .5f * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
            + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }
}
