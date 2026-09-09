package io.github.r3neer.scalebrews.platform.anatomy;

import net.minecraft.world.phys.Vec3;

/** Confirmed passive contribution, kept distinct from the body's own movement. */
public record SupportTransport(long tick,long sequence,Vec3 displacement) {
    public SupportTransport {
        if(!Double.isFinite(displacement.lengthSqr()))throw new IllegalArgumentException("Invalid transport");
    }
}
