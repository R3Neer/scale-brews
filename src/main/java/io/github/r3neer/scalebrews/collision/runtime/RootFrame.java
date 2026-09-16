package io.github.r3neer.scalebrews.collision.runtime;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import net.minecraft.world.phys.Vec3;

/** Immutable server/client-neutral provenance for one observed rigid support root. */
public record RootFrame(long sequence,long tick,Vec3 origin,float yaw,float scale,GravityFrame gravity) {
    public RootFrame {
        if(sequence<0 || tick<0 || origin==null || gravity==null || !Double.isFinite(origin.lengthSqr())
                || !Float.isFinite(yaw) || !Float.isFinite(scale) || scale<=0)
            throw new IllegalArgumentException("Invalid root frame");
    }
}
