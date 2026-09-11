package io.github.r3neer.scalebrews.collision.api.spi;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Reusable body-category adapter. This extends collision participation for a
 * body technology/category; it does not own contact, carry or solver state.
 */
public interface BodyAdapter {
    String category();
    boolean permits(Entity body);

    /** Special motion implementations may restrict requested carry before collision resolution. */
    default Vec3 transport(Entity body, Vec3 requested) { return requested; }
}
