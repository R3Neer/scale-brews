package io.github.r3neer.scalebrews.platform.anatomy;

import net.minecraft.world.phys.Vec3;

/** Confirmed passive contribution, kept distinct from the body's own movement. */
public record SupportTransport(long tick,long sequence,long rootFrameSequence,Vec3 displacement,Vec3 appliedDelta) {
    public SupportTransport {
        if(sequence<1 || rootFrameSequence<0 || appliedDelta==null
                || !Double.isFinite(displacement.lengthSqr()) || !Double.isFinite(appliedDelta.lengthSqr()))
            throw new IllegalArgumentException("Invalid transport");
    }
}
