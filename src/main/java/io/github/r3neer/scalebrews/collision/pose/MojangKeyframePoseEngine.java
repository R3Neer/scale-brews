package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;

/**
 * Common/dedicated Mojang keyframe family. S18 binds revision-local neutral
 * PosePrograms before this engine becomes executable; direct SAM evaluation
 * therefore fails closed rather than consulting any global/client resource.
 */
public final class MojangKeyframePoseEngine implements PoseEngine {
    @Override
    public Optional<Map<String, Matrix4f>> evaluate(ModelGeometry geometry, Inputs inputs, Map<String, String> parameters) {
        return Optional.empty();
    }
}
