package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;

/**
 * @deprecated S18 live authority is {@link PoseEngine}. This interface survives only as a
 * source-compatible adapter for old fixtures/integrations while callers migrate.
 */
@Deprecated
public interface PoseProvider {
    /** @deprecated Canonical live DTO is {@link PoseEngine.Inputs}. */
    @Deprecated
    class Inputs extends PoseEngine.Inputs {
        public Inputs(float walkPhase,float walkAmount,float age,float headYaw,float headPitch,boolean ordinary) {
            super(walkPhase,walkAmount,age,headYaw,headPitch,ordinary);
        }
        public Inputs(float walkPhase,float walkAmount,float age,float headYaw,float headPitch,boolean ordinary,Map<String,Float> channels) {
            super(walkPhase,walkAmount,age,headYaw,headPitch,ordinary,channels);
        }
    }

    /** Empty means unsupported, never silently return a frozen anatomy pose. */
    Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs inputs);
}
