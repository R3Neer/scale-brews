package io.github.r3neer.scalebrews.client.render;

import io.github.r3neer.scalebrews.mount.TinyMountDefinition;

public interface SaddleState {
    TinyMountDefinition.SaddleVisual scalebrews$saddle();
    void scalebrews$saddle(TinyMountDefinition.SaddleVisual visual);
    boolean scalebrews$occupied();
    void scalebrews$occupied(boolean occupied);
}
