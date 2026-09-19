package io.github.r3neer.scalebrews.collision.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * GameTest-only, action-scoped tracking authority for receipt/kernel fixtures.
 *
 * <p>The test mixin consults this seam only while an explicit action is running. No AnatomyRuntime
 * state, catalog, player list, tracking ledger or lifecycle hook is created or mutated.</p>
 */
public final class S24TrackingAuthorityTestSeam {
    private S24TrackingAuthorityTestSeam() {}

    private static final Map<ServerPlayer,IdentityHashMap<Entity,Long>> ACTIVE=new IdentityHashMap<>();

    public static void run(ServerPlayer recipient,Entity body,Runnable action) {
        run(recipient,body,1L,action);
    }

    public static void run(ServerPlayer recipient,Entity body,long generation,Runnable action) {
        if(recipient==null || body==null || generation<1 || action==null)
            throw new IllegalArgumentException("Missing/invalid tracking fixture participant/action");
        synchronized(ACTIVE) {
            var bodies=ACTIVE.computeIfAbsent(recipient,ignored->new IdentityHashMap<>());
            var previous=bodies.put(body,generation);
            try {
                action.run();
            } finally {
                if(previous==null)bodies.remove(body);else bodies.put(body,previous);
                if(bodies.isEmpty())ACTIVE.remove(recipient);
            }
        }
    }

    /** Called only by the GameTest mixin. Zero means the fixture did not explicitly authorize this pair. */
    public static long generation(ServerPlayer recipient,Entity body) {
        synchronized(ACTIVE) {
            var bodies=ACTIVE.get(recipient);
            return bodies==null?0:bodies.getOrDefault(body,0L);
        }
    }
}
