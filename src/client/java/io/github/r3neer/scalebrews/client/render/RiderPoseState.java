package io.github.r3neer.scalebrews.client.render;

import org.joml.Matrix4fc;

/** Per-frame transform in the passenger renderer's local world axes; null when not on an animated mount. */
public interface RiderPoseState {
    Matrix4fc scalebrews$riderPose();
    void scalebrews$riderPose(Matrix4fc pose);
}
