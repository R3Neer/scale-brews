package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import org.joml.Vector3fc;

/** Client/tooling-only compiler from Mojang renderer data to Scale's neutral server-safe pose program. */
public final class AnimationDefinitionCompiler {
    private AnimationDefinitionCompiler() {}

    public static PoseProgram compile(String source, String version, AnimationDefinition definition) {
        if (source == null || source.isBlank() || version == null || version.isBlank() || definition == null)
            throw new IllegalArgumentException("Invalid AnimationDefinition compilation request");
        float duration = definition.lengthInSeconds();
        // Mojang ships legitimate zero-length definitions for static/baby transforms. They are not
        // malformed: applyStatic() evaluates their timestamp-zero keyframes directly.
        if (!Float.isFinite(duration) || duration < 0) throw new IllegalArgumentException("Invalid AnimationDefinition duration");

        List<PoseProgram.Track> tracks = new ArrayList<>();
        Map<String, List<AnimationChannel>> bones = new TreeMap<>(Objects.requireNonNull(definition.boneAnimations(), "bone animations"));
        for (var bone : bones.entrySet()) {
            String boneName = bone.getKey();
            if (boneName == null || boneName.isBlank()) throw new IllegalArgumentException("Invalid animation bone");
            var channels = Objects.requireNonNull(bone.getValue(), "animation channels");
            for (var channel : channels) tracks.add(compileTrack(boneName, Objects.requireNonNull(channel, "animation channel")));
        }
        // Deterministic across source map implementations while retaining channel order for equal bone/target pairs.
        // Java's stable sort preserves the original order of equal keys, which matters because channels are additive.
        tracks.sort(Comparator.comparing(PoseProgram.Track::bone).thenComparing(track -> track.target().ordinal()));
        return new PoseProgram(PoseProgram.SCHEMA_VERSION, source, version, duration, definition.looping(), tracks);
    }

    private static PoseProgram.Track compileTrack(String bone, AnimationChannel channel) {
        PoseProgram.Target target = target(channel.target());
        Keyframe[] sourceFrames = Objects.requireNonNull(channel.keyframes(), "keyframes");
        if (sourceFrames.length == 0) throw new IllegalArgumentException("Animation channel has no keyframes");
        List<PoseProgram.Keyframe> frames = new ArrayList<>(sourceFrames.length);
        for (Keyframe frame : sourceFrames) {
            if (frame == null) throw new IllegalArgumentException("Missing animation keyframe");
            frames.add(new PoseProgram.Keyframe(frame.timestamp(), vector(frame.preTarget()), vector(frame.postTarget()), interpolation(frame.interpolation())));
        }
        return new PoseProgram.Track(bone, target, frames);
    }

    private static PoseProgram.Target target(AnimationChannel.Target target) {
        if (target == AnimationChannel.Targets.POSITION) return PoseProgram.Target.TRANSLATION;
        if (target == AnimationChannel.Targets.ROTATION) return PoseProgram.Target.ROTATION;
        if (target == AnimationChannel.Targets.SCALE) return PoseProgram.Target.SCALE;
        throw new IllegalArgumentException("Unsupported AnimationDefinition target: " + target);
    }

    private static PoseProgram.Interpolation interpolation(AnimationChannel.Interpolation interpolation) {
        if (interpolation == AnimationChannel.Interpolations.LINEAR) return PoseProgram.Interpolation.LINEAR;
        if (interpolation == AnimationChannel.Interpolations.CATMULLROM) return PoseProgram.Interpolation.CATMULL_ROM;
        throw new IllegalArgumentException("Unsupported AnimationDefinition interpolation: " + interpolation);
    }

    private static PoseProgram.Vector vector(Vector3fc value) {
        if (value == null) throw new IllegalArgumentException("Missing animation target vector");
        return new PoseProgram.Vector(value.x(), value.y(), value.z());
    }
}
