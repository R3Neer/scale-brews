package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;

/** @deprecated Compatibility wrapper. Canonical family behavior is owned by {@link VanillaFamilyPoseEngine}. */
@Deprecated
public final class VanillaFamilyPose implements PoseProvider, PoseEngine {
    public enum Family { CHICKEN, VILLAGER, IRON_GOLEM, GHAST, FELINE, EQUINE, BEE }
    private final VanillaFamilyPoseEngine engine;

    public VanillaFamilyPose(Family family) {
        engine = new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.valueOf(family.name()));
    }

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, PoseProvider.Inputs inputs) {
        return engine.evaluate(geometry, inputs, Map.of());
    }

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, PoseEngine.Inputs inputs, Map<String, String> parameters) {
        return engine.evaluate(geometry, inputs, parameters);
    }
}
