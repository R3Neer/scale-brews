package io.github.r3neer.scalebrews.collision.integration;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import net.minecraft.world.entity.Entity;

/** Scoped identity of the native movement query; it owns no geometry or contact. */
public final class MovingBodyContext {
    private static final ThreadLocal<Entity> MOVING = new ThreadLocal<>();
    private MovingBodyContext() {}
    /** The caller restores this exact previous identity in finally, including nested clipping. */
    public static Entity enter(Entity body) {
        Entity previous = MOVING.get(); MOVING.set(body); return previous;
    }
    public static void exit(Entity previous) {
        if (previous == null) MOVING.remove(); else MOVING.set(previous);
    }
    public static boolean suppressPair(Entity body, Entity other) {
        return MOVING.get() == body && AnatomyApi.ready(body) && AnatomyMovement.predictsBody(body)
            && AnatomyMovement.replacesPair(body, other);
    }
}
