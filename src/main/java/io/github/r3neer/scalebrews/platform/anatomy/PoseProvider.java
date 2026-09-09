package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;

/** Deterministic pose evaluation. Inputs must eventually be sampled/synchronized by the server. */
public interface PoseProvider {
    record Inputs(float walkPhase,float walkAmount,float age,float headYaw,float headPitch,boolean ordinary) {
        public Inputs {
            if(!Float.isFinite(walkPhase+walkAmount+age+headYaw+headPitch) || walkAmount<0)
                throw new IllegalArgumentException("Invalid pose inputs");
        }
    }
    /** Empty means unsupported, never silently return a frozen anatomy pose. */
    Optional<Map<String,Matrix4f>> evaluate(ModelGeometry geometry,Inputs inputs);
}
