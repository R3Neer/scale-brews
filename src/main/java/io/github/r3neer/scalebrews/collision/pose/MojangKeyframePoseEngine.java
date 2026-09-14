package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Common/dedicated evaluator for revision-bound neutral Mojang keyframe programs.
 *
 * <p>Bindings select a revision-local {@code program}, an explicit clock source and amplitude source.
 * {@code clock_scale} converts the selected clock into seconds; {@code walk_phase} additionally mirrors
 * Minecraft's millisecond truncation before sampling. {@code amplitude_scale} and {@code amplitude_max}
 * are optional post-selector transforms so callers can reproduce helpers such as {@code applyWalk}
 * without hiding source-specific formulas inside the generic {@code walk_amount} selector.</p>
 */
public final class MojangKeyframePoseEngine implements PoseEngine {
    private static final Set<String> PARAMETERS = Set.of(
        "program", "clock", "clock_scale", "amplitude", "amplitude_scale", "amplitude_max");

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs inputs, Map<String, String> parameters) {
        // Data-backed execution must be revision-bound. Direct evaluation has no resource owner.
        return Optional.empty();
    }

    @Override
    public Optional<Bound> bind(ModelGeometry geometry, Map<String, String> parameters,
                                Set<String> requiredChannels, Resources resources) {
        if (geometry == null || parameters == null || requiredChannels == null || resources == null
                || !PARAMETERS.containsAll(parameters.keySet())) return Optional.empty();
        String programText = parameters.get("program");
        String clockText = parameters.get("clock");
        String amplitudeText = parameters.get("amplitude");
        if (programText == null || clockText == null || amplitudeText == null) return Optional.empty();
        final Identifier programId;
        try { programId = Identifier.parse(programText); }
        catch (RuntimeException invalid) { return Optional.empty(); }
        var program = resources.program(programId).orElse(null);
        if (program == null) return Optional.empty();
        var evaluator = PoseProgramEvaluator.bind(geometry, program).orElse(null);
        if (evaluator == null) return Optional.empty();

        float clockScale = 1;
        if (!clockText.equals("static")) {
            String scaleText = parameters.get("clock_scale");
            if (scaleText == null) return Optional.empty();
            try { clockScale = Float.parseFloat(scaleText); }
            catch (RuntimeException invalid) { return Optional.empty(); }
            if (!Float.isFinite(clockScale) || clockScale == 0 || Math.abs(clockScale) > 1_000_000) return Optional.empty();
        } else if (parameters.containsKey("clock_scale")) {
            try { clockScale = Float.parseFloat(parameters.get("clock_scale")); }
            catch (RuntimeException invalid) { return Optional.empty(); }
            if (!Float.isFinite(clockScale) || Math.abs(clockScale) > 1_000_000) return Optional.empty();
        }

        float amplitudeScale = 1;
        if (parameters.containsKey("amplitude_scale")) {
            try { amplitudeScale = Float.parseFloat(parameters.get("amplitude_scale")); }
            catch (RuntimeException invalid) { return Optional.empty(); }
            if (!Float.isFinite(amplitudeScale) || Math.abs(amplitudeScale) > 1_000_000) return Optional.empty();
        }
        Float amplitudeMax = null;
        if (parameters.containsKey("amplitude_max")) {
            try { amplitudeMax = Float.parseFloat(parameters.get("amplitude_max")); }
            catch (RuntimeException invalid) { return Optional.empty(); }
            if (!Float.isFinite(amplitudeMax) || Math.abs(amplitudeMax) > 1_000_000) return Optional.empty();
        }

        var required = new LinkedHashSet<>(requiredChannels);
        var clock = clock(clockText, clockScale, required).orElse(null);
        var amplitudeSource = amplitude(amplitudeText, required).orElse(null);
        // Selectors can add channels beyond those declared by the binding. Validate the effective,
        // deduplicated set because PoseEngine.Inputs itself is bounded to 64 channels.
        if (clock == null || amplitudeSource == null || required.size() > 64
                || required.stream().anyMatch(name -> name == null || !name.matches("[a-z0-9_.-]{1,64}")))
            return Optional.empty();
        Set<String> requiredCopy = Set.copyOf(required);
        final float amplitudeFactor = amplitudeScale;
        final Float amplitudeCeiling = amplitudeMax;
        return Optional.of(inputs -> {
            if (inputs == null || !inputs.ordinary() || !inputs.channels().keySet().containsAll(requiredCopy)) return Optional.empty();
            float time = clock.value(inputs);
            float scale = amplitudeSource.value(inputs) * amplitudeFactor;
            if (amplitudeCeiling != null) scale = Math.min(scale, amplitudeCeiling);
            if (!Float.isFinite(time) || !Float.isFinite(scale)) return Optional.empty();
            return evaluator.evaluate(geometry, time, scale);
        });
    }

    @FunctionalInterface private interface Selector { float value(Inputs inputs); }

    private static Optional<Selector> clock(String text, float scale, Set<String> required) {
        return switch (text) {
            case "static" -> Optional.of(inputs -> 0);
            case "age" -> Optional.of(inputs -> inputs.age() * .05f * scale);
            case "walk_phase" -> Optional.of(inputs -> quantizedWalkSeconds(inputs.walkPhase(), scale));
            default -> channelSelector(text, "channel:", scale, required);
        };
    }

    /** Minecraft applyWalk truncates its phase-derived clock to integer milliseconds before sampling. */
    private static float quantizedWalkSeconds(float phase, float secondsPerPhase) {
        float milliseconds = phase * secondsPerPhase * 1000.0f;
        return ((long)milliseconds) / 1000.0f;
    }

    private static Optional<Selector> amplitude(String text, Set<String> required) {
        if (text.equals("one")) return Optional.of(inputs -> 1);
        if (text.equals("walk_amount")) return Optional.of(Inputs::walkAmount);
        if (text.startsWith("constant:")) {
            try {
                float value = Float.parseFloat(text.substring("constant:".length()));
                return Float.isFinite(value) && Math.abs(value) <= 1_000_000 ? Optional.of(inputs -> value) : Optional.empty();
            } catch (RuntimeException invalid) { return Optional.empty(); }
        }
        return channelSelector(text, "channel:", 1, required);
    }

    private static Optional<Selector> channelSelector(String text, String prefix, float scale, Set<String> required) {
        if (!text.startsWith(prefix)) return Optional.empty();
        String channel = text.substring(prefix.length());
        if (!channel.matches("[a-z0-9_.-]{1,64}")) return Optional.empty();
        required.add(channel);
        return Optional.of(inputs -> inputs.channel(channel, Float.NaN) * scale);
    }
}
