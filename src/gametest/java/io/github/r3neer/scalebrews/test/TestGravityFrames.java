package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

/** Test-only dynamic provider. Production has no mutable frame override API. */
public final class TestGravityFrames {
    public static final String OWNER = "scalebrews-test";
    private static final Map<Entity, Direction> FRAMES = new WeakHashMap<>();
    private TestGravityFrames() {}

    /** Returns false only when a real optional provider already owns the service. */
    public static synchronized boolean ensure() {
        String owner = GravityFrames.owner();
        if (owner == null) GravityFrames.install(OWNER, entity -> {
            synchronized (TestGravityFrames.class) {
                return FRAMES.getOrDefault(entity, Direction.DOWN);
            }
        });
        return OWNER.equals(GravityFrames.owner());
    }

    public static synchronized void set(Entity entity, Direction direction) {
        if (!ensure()) throw new IllegalStateException("Test gravity provider cannot replace " + GravityFrames.owner());
        FRAMES.put(entity, direction);
    }

    public static synchronized void clear(Entity entity) {
        FRAMES.remove(entity);
    }
}
