package io.github.r3neer.scalebrews.collision.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bounded recipient-local authority for active tracking windows.
 *
 * <p>The ledger deliberately remembers no retired UUIDs. Replay safety comes from one monotonic
 * connection-local sequence: every newly acquired UUID gets a generation greater than every prior
 * tracking window owned by this recipient. Releasing a UUID therefore frees its map entry without
 * making its old generation reusable.</p>
 */
final class TrackingGenerationLedger {
    /** Mirrors the bounded client frame/contact population; excess pairs fail closed. */
    static final int MAX_ACTIVE=4096;
    static final long UNAVAILABLE=0;

    private final Map<UUID,Long> active=new HashMap<>();
    private long nextGeneration=1;

    /** Existing windows are stable; a new window allocates one never-reused generation. */
    long acquire(UUID entity) {
        Objects.requireNonNull(entity,"entity");
        var known=active.get(entity);if(known!=null)return known;
        if(active.size()>=MAX_ACTIVE || nextGeneration<1 || nextGeneration==Long.MAX_VALUE)return UNAVAILABLE;
        long generation=nextGeneration++;
        active.put(entity,generation);
        return generation;
    }

    /** Returns only an already-active window; STOP must never fabricate a generation. */
    long current(UUID entity) {
        Objects.requireNonNull(entity,"entity");
        return active.getOrDefault(entity,UNAVAILABLE);
    }

    /** Retires one window while preserving the scalar anti-replay sequence for the connection. */
    void release(UUID entity) {
        if(entity!=null)active.remove(entity);
    }

    int activeEntries(){return active.size();}
    long nextGeneration(){return nextGeneration;}
}
