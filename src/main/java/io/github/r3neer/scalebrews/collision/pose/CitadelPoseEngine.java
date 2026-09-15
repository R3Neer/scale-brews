package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Revision-bound common/dedicated evaluator for neutral Citadel pose programs. */
public final class CitadelPoseEngine implements PoseEngine {
    private static final Set<String> PARAMETERS = Set.of("program");

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs inputs, Map<String, String> parameters) {
        // Data-backed execution must be bound to one accepted catalog revision.
        return Optional.empty();
    }

    @Override
    public Optional<Bound> bind(ModelGeometry geometry, Map<String, String> parameters,
                                Set<String> requiredChannels, Resources resources) {
        if (geometry == null || parameters == null || requiredChannels == null || resources == null
                || !PARAMETERS.containsAll(parameters.keySet()) || parameters.size() != 1)
            return Optional.empty();
        String programText = parameters.get("program");
        if (programText == null) return Optional.empty();
        final Identifier programId;
        try { programId = Identifier.parse(programText); }
        catch (RuntimeException invalid) { return Optional.empty(); }

        var program = resources.citadelProgram(programId).orElse(null);
        if (program == null) return Optional.empty();
        Set<String> programChannels;
        try { programChannels = program.requiredChannels(); }
        catch (RuntimeException invalid) { return Optional.empty(); }
        if (requiredChannels.size() > 64 || requiredChannels.stream().anyMatch(name -> name == null || !name.matches("[a-z0-9_.-]{1,64}"))
                || !requiredChannels.containsAll(programChannels))
            return Optional.empty();

        var evaluator = CitadelPoseProgramEvaluator.bind(geometry, program).orElse(null);
        if (evaluator == null) return Optional.empty();
        Set<String> required = Set.copyOf(requiredChannels);
        return Optional.of(inputs -> {
            if (inputs == null || !inputs.ordinary() || !inputs.channels().keySet().containsAll(required))
                return Optional.empty();
            return evaluator.evaluate(inputs);
        });
    }
}
