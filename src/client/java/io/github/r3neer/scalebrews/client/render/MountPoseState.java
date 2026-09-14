package io.github.r3neer.scalebrews.client.render;

/** Entity identity and requested attachment captured with a living render-state snapshot. */
public interface MountPoseState {
    int scalebrews$entityId();
    void scalebrews$entityId(int id);
    boolean scalebrews$hasPassengers();
    void scalebrews$hasPassengers(boolean value);
    String scalebrews$mountAnchor();
    void scalebrews$mountAnchor(String anchor);
}
