package io.github.r3neer.scalebrews.collision.pose;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Neutral, server-safe animation program compiled during preparation from a model technology.
 * Programs are immutable revision-local catalog data; they never consult client/render classes.
 */
public record PoseProgram(int schema, String source, String version, float durationSeconds, boolean loop,
                          List<Track> tracks) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_TRACKS = 512;
    public static final int MAX_KEYFRAMES = 8192;

    public enum Target { TRANSLATION, ROTATION, SCALE }
    public enum Interpolation { LINEAR, CATMULL_ROM }

    public record Vector(float x, float y, float z) {
        public Vector {
            if (!Float.isFinite(x + y + z) || Math.abs(x) > 65536 || Math.abs(y) > 65536 || Math.abs(z) > 65536)
                throw new IllegalArgumentException("Invalid pose-program vector");
        }
    }

    /** pre/post preserve Mojang's discontinuity-capable keyframe representation exactly. */
    public record Keyframe(float timestamp, Vector preTarget, Vector postTarget, Interpolation interpolation) {
        public Keyframe {
            if (!Float.isFinite(timestamp) || timestamp < 0 || preTarget == null || postTarget == null || interpolation == null)
                throw new IllegalArgumentException("Invalid pose-program keyframe");
        }
    }

    public record Track(String bone, Target target, List<Keyframe> keyframes) {
        public Track {
            if (bone == null || bone.isBlank() || bone.length() > 256 || target == null || keyframes == null || keyframes.isEmpty())
                throw new IllegalArgumentException("Invalid pose-program track");
            keyframes = List.copyOf(keyframes);
            float previous = -1;
            for (var frame : keyframes) {
                Objects.requireNonNull(frame, "keyframe");
                if (frame.timestamp() < previous) throw new IllegalArgumentException("Pose-program keyframes must be ordered");
                previous = frame.timestamp();
            }
        }
    }

    public PoseProgram {
        if (schema != SCHEMA_VERSION || source == null || source.isBlank() || source.length() > 512
                || version == null || version.isBlank() || version.length() > 128
                || !Float.isFinite(durationSeconds) || durationSeconds <= 0 || durationSeconds > 3600
                || tracks == null || tracks.isEmpty() || tracks.size() > MAX_TRACKS)
            throw new IllegalArgumentException("Invalid pose program");
        tracks = List.copyOf(tracks);
        int total = 0;
        Set<String> owners = new HashSet<>();
        for (var track : tracks) {
            Objects.requireNonNull(track, "track");
            total = Math.addExact(total, track.keyframes().size());
            if (total > MAX_KEYFRAMES) throw new IllegalArgumentException("Pose program has too many keyframes");
            if (!owners.add(track.bone() + "\u0000" + track.target()))
                throw new IllegalArgumentException("Duplicate pose-program bone target: " + track.bone() + "/" + track.target());
            for (var frame : track.keyframes()) if (frame.timestamp() > durationSeconds + 1e-5f)
                throw new IllegalArgumentException("Pose-program keyframe exceeds duration");
        }
    }

    /** Re-run every nested constructor after JSON/wire materialization; deserialization never bypasses validation. */
    public static PoseProgram validatedCopy(PoseProgram raw) {
        if (raw == null) throw new IllegalArgumentException("Missing pose program");
        var tracks = raw.tracks() == null ? null : raw.tracks().stream().map(track -> {
            if (track == null) throw new IllegalArgumentException("Missing pose-program track");
            var frames = track.keyframes() == null ? null : track.keyframes().stream().map(frame -> {
                if (frame == null) throw new IllegalArgumentException("Missing pose-program keyframe");
                var pre = frame.preTarget();
                var post = frame.postTarget();
                return new Keyframe(frame.timestamp(),
                    pre == null ? null : new Vector(pre.x(), pre.y(), pre.z()),
                    post == null ? null : new Vector(post.x(), post.y(), post.z()), frame.interpolation());
            }).toList();
            return new Track(track.bone(), track.target(), frames);
        }).toList();
        return new PoseProgram(raw.schema(), raw.source(), raw.version(), raw.durationSeconds(), raw.loop(), tracks);
    }
}
