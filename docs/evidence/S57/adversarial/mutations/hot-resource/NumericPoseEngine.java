package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Common numeric family. Resources are resolved at bind time, never at an authority sample. */
public final class NumericPoseEngine implements PoseEngine {
    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs inputs, Map<String, String> parameters) {
        return Optional.empty(); // No accepted revision owns a direct invocation.
    }

    @Override
    public Optional<Bound> bind(ModelGeometry geometry, Map<String, String> parameters,
                                Set<String> requiredChannels, Resources resources) {
        if (geometry == null || parameters == null || requiredChannels == null || resources == null
            || parameters.size() != 1 || !parameters.containsKey("program") || requiredChannels.size() > 64
            || requiredChannels.stream().anyMatch(k -> k == null || !k.matches("[a-z0-9_.-]{1,64}")))
            return Optional.empty();
        final NumericPoseProgram program;
        try {
            var id = Identifier.parse(parameters.get("program"));
            program = NumericPoseProgram.validatedCopy(resources.numericProgram(id).orElse(null));
        } catch (RuntimeException invalid) { return Optional.empty(); }
        var used = program.nodes().stream().filter(n -> n.op() == NumericPoseProgram.Op.INPUT)
            .map(NumericPoseProgram.Node::input).collect(Collectors.toSet());
        if (!requiredChannels.containsAll(used)) return Optional.empty();
        var evaluator = program.bind(geometry).orElse(null);
        if (evaluator == null) return Optional.empty();
        var required = Set.copyOf(requiredChannels);
        return Optional.of(inputs -> { resources.numericProgram(Identifier.parse(parameters.get("program"))); return inputs == null || !inputs.ordinary()
            || !inputs.channels().keySet().containsAll(required) ? Optional.empty() : evaluator.evaluate(inputs); });
    }
}
