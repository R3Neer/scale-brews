package io.github.r3neer.scalebrews.client.render;

import net.minecraft.resources.Identifier;

/** Entity identity and requested attachment captured with a living render-state snapshot. */
public interface MountPoseState {
    int scalebrews$entityId();
    void scalebrews$entityId(int id);
    Identifier scalebrews$mountType();
    void scalebrews$mountType(Identifier type);
    boolean scalebrews$tinyMount();
    void scalebrews$tinyMount(boolean value);
}
