package io.github.r3neer.scalebrews.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** One current-frame visual attachment shared by saddle, rider and camera. */
public record SeatFrame(String path, Matrix4f saddleTransform, Vector3f cameraPosition,
                        Vector3f up, Matrix4f riderRotationDelta, float width, float depth,
                        float strapLength, float seatHeight, long serial) {
}
