package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * @deprecated Read-only S18 compatibility adapter. Canonical behavior registry is
 * {@link CollisionEngines#pose}; this class owns no registry and no formulas.
 */
@Deprecated
public final class PoseProviders {
    private PoseProviders() {}

    public static Optional<PoseProvider> find(Identifier id) {
        return CollisionEngines.pose(id).map(engine -> (geometry, inputs) -> engine.evaluate(geometry, inputs, Map.of()));
    }
}
