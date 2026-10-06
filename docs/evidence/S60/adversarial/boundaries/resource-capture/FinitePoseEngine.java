package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Revision-bound finite selection; arithmetic remains owned by NumericPoseProgram. */
public final class FinitePoseEngine implements PoseEngine {
    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs inputs, Map<String, String> parameters) {
        return Optional.empty();
    }

    @Override
    public Optional<Bound> bind(ModelGeometry geometry, Map<String, String> parameters,
                                Set<String> requiredChannels, Resources resources) {
        if (geometry == null || parameters == null || requiredChannels == null || resources == null
            || parameters.size() != 1 || !parameters.containsKey("program") || requiredChannels.size() > 64
            || requiredChannels.stream().anyMatch(k -> k == null || !k.matches("[a-z0-9_.-]{1,64}")))
            return Optional.empty();
        try {
            var tableId = Identifier.parse(FinitePoseProgram.resourceId(parameters.get("program")));
            var table = FinitePoseProgram.validatedCopy(resources.finiteProgram(tableId).orElse(null));
            if (!requiredChannels.containsAll(table.booleans())) return Optional.empty();
            String[] names = table.booleans().toArray(String[]::new);
            Bound[] rows = new Bound[table.rows().size()];
            var unique = new HashMap<String, Bound>();
            for (var row : table.rows()) {
                var bound = unique.get(row.program());
                if (bound == null) {
                    var program = NumericPoseProgram.validatedCopy(
                        resources.numericProgram(Identifier.parse(row.program())).orElse(null));
                    if (program.nodes().stream().anyMatch(n -> n.op() == NumericPoseProgram.Op.INPUT
                        && table.booleans().contains(n.input()))) return Optional.empty();
                    bound = NumericPoseEngine.bindPrepared(program, geometry, requiredChannels).orElse(null);
                    if (bound == null) return Optional.empty();
                    unique.put(row.program(), bound);
                }
                rows[row.mask()] = bound;
            }
            // These private arrays contain only channel names and detached numeric evaluators.
            return Optional.of(inputs -> {
                if (inputs == null || !inputs.ordinary()) return Optional.empty();
                resources.finiteProgram(Identifier.parse(parameters.get("program"))); int mask = 0;
                for (int i = 0; i < names.length; i++) {
                    Float value = inputs.channels().get(names[i]);
                    if (value == null) return Optional.empty();
                    int bits = Float.floatToIntBits(value);
                    if (bits == Float.floatToIntBits(1f)) mask |= 1 << i;
                    else if (bits != Float.floatToIntBits(0f)) return Optional.empty();
                }
                return rows[mask].evaluate(inputs);
            });
        } catch (RuntimeException invalid) { return Optional.empty(); }
    }
}
