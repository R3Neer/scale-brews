package io.github.r3neer.scalebrews.mount;

import io.github.r3neer.scalebrews.integration.gravity.GravityFrame;
import net.minecraft.world.phys.Vec3;

/** Pure gravity-frame rules for movement generated or modified by Tiny Mount mechanics. */
public final class TinyMountGravity {
    private TinyMountGravity() {}

    /**
     * Applies vanilla-style fall damping only to local downward velocity.
     * Tangential components and already-upward local movement are preserved.
     */
    public static Vec3 dampLocalFall(Vec3 worldMovement, GravityFrame frame, double multiplier, boolean suppress) {
        if (suppress) return worldMovement;
        Vec3 local=frame.toLocal(worldMovement);
        if(local.y>=0)return worldMovement;
        return frame.multiplyLocalVertical(worldMovement,multiplier);
    }
}
