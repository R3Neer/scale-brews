package io.github.r3neer.scalebrews.collision.api.spi;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Deterministic reusable pose-family engine. Empty means unsupported, never frozen fallback geometry. */
@FunctionalInterface
public interface PoseEngine {
    Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs inputs, Map<String, String> parameters);

    /**
     * Revision-binding seam. The default preserves legacy/external SAM engines while allowing
     * data-backed engines to resolve immutable revision-local resources exactly once.
     */
    default Optional<Bound> bind(ModelGeometry geometry, Map<String, String> parameters,
                                 Set<String> requiredChannels, Resources resources) {
        Objects.requireNonNull(geometry, "geometry");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(requiredChannels, "requiredChannels");
        Objects.requireNonNull(resources, "resources");
        var params = Collections.unmodifiableMap(new TreeMap<>(parameters));
        var required = Collections.unmodifiableSet(new LinkedHashSet<>(requiredChannels));
        if (required.stream().anyMatch(name -> name == null || !name.matches("[a-z0-9_.-]{1,64}")))
            return Optional.empty();
        return Optional.of(inputs -> {
            if (inputs == null || !inputs.channels().keySet().containsAll(required)) return Optional.empty();
            return evaluate(geometry, inputs, params);
        });
    }

    @FunctionalInterface
    interface Bound {
        Optional<Map<String, Matrix4f>> evaluate(Inputs inputs);
    }

    /** Read-only view of the accepted catalog revision. It is never a process-global program registry. */
    @FunctionalInterface
    interface Resources {
        Optional<PoseProgram> program(Identifier id);
        /** Additive resource family; default keeps existing external Resources lambdas source-compatible. */
        default Optional<CitadelPoseProgram> citadelProgram(Identifier id) { return Optional.empty(); }
        static Resources empty() { return ignored -> Optional.empty(); }
    }

    /**
     * Canonical authoritative pose-input DTO. It is a class rather than a record so the
     * pre-S18 PoseProvider compatibility DTO can subclass it without leaking that legacy
     * type back into live runtime signatures.
     */
    class Inputs {
        private final float walkPhase, walkAmount, age, headYaw, headPitch;
        private final boolean ordinary;
        private final Map<String, Float> channels;

        public Inputs(float walkPhase, float walkAmount, float age, float headYaw, float headPitch, boolean ordinary) {
            this(walkPhase, walkAmount, age, headYaw, headPitch, ordinary, Map.of());
        }

        public Inputs(float walkPhase, float walkAmount, float age, float headYaw, float headPitch,
                      boolean ordinary, Map<String, Float> channels) {
            if (!Float.isFinite(walkPhase) || !Float.isFinite(walkAmount) || !Float.isFinite(age)
                    || !Float.isFinite(headYaw) || !Float.isFinite(headPitch) || walkAmount < 0)
                throw new IllegalArgumentException("Invalid pose inputs");
            if (channels == null || channels.size() > 64) throw new IllegalArgumentException("Invalid pose channels");
            var copy = new TreeMap<String, Float>();
            channels.forEach((name, value) -> {
                if (name == null || !name.matches("[a-z0-9_.-]{1,64}") || value == null || !Float.isFinite(value))
                    throw new IllegalArgumentException("Invalid pose channel");
                copy.put(name, value);
            });
            this.walkPhase = walkPhase;
            this.walkAmount = walkAmount;
            this.age = age;
            this.headYaw = headYaw;
            this.headPitch = headPitch;
            this.ordinary = ordinary;
            this.channels = Collections.unmodifiableMap(copy);
        }

        public float walkPhase() { return walkPhase; }
        public float walkAmount() { return walkAmount; }
        public float age() { return age; }
        public float headYaw() { return headYaw; }
        public float headPitch() { return headPitch; }
        public boolean ordinary() { return ordinary; }
        public Map<String, Float> channels() { return channels; }
        public float channel(String name, float fallback) { return channels.getOrDefault(name, fallback); }
        public boolean flag(String name) { return channel(name, 0) > .5f; }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Inputs value)) return false;
            return Float.floatToIntBits(walkPhase) == Float.floatToIntBits(value.walkPhase)
                && Float.floatToIntBits(walkAmount) == Float.floatToIntBits(value.walkAmount)
                && Float.floatToIntBits(age) == Float.floatToIntBits(value.age)
                && Float.floatToIntBits(headYaw) == Float.floatToIntBits(value.headYaw)
                && Float.floatToIntBits(headPitch) == Float.floatToIntBits(value.headPitch)
                && ordinary == value.ordinary && channels.equals(value.channels);
        }

        @Override public int hashCode() {
            return Objects.hash(walkPhase, walkAmount, age, headYaw, headPitch, ordinary, channels);
        }

        @Override public String toString() {
            return "Inputs[walkPhase=" + walkPhase + ", walkAmount=" + walkAmount + ", age=" + age
                + ", headYaw=" + headYaw + ", headPitch=" + headPitch + ", ordinary=" + ordinary
                + ", channels=" + channels + "]";
        }
    }
}
