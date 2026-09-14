package io.github.r3neer.scalebrews.client.render;

import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
public interface SaddleState {
    TinyMountDefinition.SaddleVisual scalebrews$saddle();
    void scalebrews$saddle(TinyMountDefinition.SaddleVisual visual);
    boolean scalebrews$hasSaddle();
    void scalebrews$hasSaddle(boolean value);
    TinyMountVisualProfile scalebrews$visualProfile();
    void scalebrews$visualProfile(TinyMountVisualProfile profile);
    SeatFrame scalebrews$seatFrame();
    void scalebrews$seatFrame(SeatFrame frame);
}
