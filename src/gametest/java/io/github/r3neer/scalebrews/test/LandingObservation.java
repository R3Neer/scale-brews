package io.github.r3neer.scalebrews.test;

/** Scoped test-only observation of native event/particle dispatch. */
public final class LandingObservation implements AutoCloseable {
    public static final ThreadLocal<LandingObservation> ACTIVE = new ThreadLocal<>();
    public final net.minecraft.world.entity.Entity entity;
    public int landings, gusts, terrain;
    public LandingObservation(net.minecraft.world.entity.Entity entity) {
        this.entity = entity;
        ACTIVE.set(this);
    }
    public void close() { ACTIVE.remove(); }
}
