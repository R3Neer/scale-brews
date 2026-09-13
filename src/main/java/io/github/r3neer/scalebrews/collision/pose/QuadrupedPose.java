package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;

/** @deprecated Compatibility wrapper. Canonical behavior is owned by {@link QuadrupedPoseEngine}. */
@Deprecated
public final class QuadrupedPose implements PoseProvider, PoseEngine {
    private static final QuadrupedPoseEngine ENGINE = new QuadrupedPoseEngine();

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, PoseProvider.Inputs inputs) {
        return ENGINE.evaluate(geometry, inputs, Map.of());
    }

    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, PoseEngine.Inputs inputs, Map<String, String> parameters) {
        return ENGINE.evaluate(geometry, inputs, parameters);
    }
}
