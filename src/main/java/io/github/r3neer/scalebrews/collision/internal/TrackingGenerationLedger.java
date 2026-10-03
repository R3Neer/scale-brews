package io.github.r3neer.scalebrews.collision.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bounded recipient-local authority for active tracking windows.
 *
 * <p>The ledger deliberately remembers no retired UUIDs. A runtime supplies one monotonic server
 * sequence to every recipient ledger, so reconnecting cannot reuse a retired generation even when
 * the new recipient tracks entities in the same order. A standalone ledger owns its own sequence
 * for pure tests. Releasing a UUID frees its map entry without recycling its generation.</p>
 */
final class TrackingGenerationLedger {
    /** Mirrors the bounded client frame/contact population; excess pairs fail closed. */
    static final int MAX_ACTIVE=4096;
    static final long UNAVAILABLE=0;

    static final class Sequence {
        private long nextGeneration=1;
        synchronized long allocate() {
            if(nextGeneration<1 || nextGeneration==Long.MAX_VALUE)return UNAVAILABLE;
            return nextGeneration++;
        }
        synchronized long next(){return nextGeneration;}
    }

    private final Map<UUID,Long> active=new HashMap<>();
    private final Sequence sequence;
    TrackingGenerationLedger(){this(new Sequence());}
    TrackingGenerationLedger(Sequence sequence){this.sequence=Objects.requireNonNull(sequence,"sequence");}

    /** Existing windows are stable; a new window allocates one never-reused generation. */
    long acquire(UUID entity) {
        Objects.requireNonNull(entity,"entity");
        var known=active.get(entity);if(known!=null)return known;
        if(active.size()>=MAX_ACTIVE)return UNAVAILABLE;
        long generation=sequence.allocate();
        if(generation<1)return UNAVAILABLE;
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
    long nextGeneration(){return sequence.next();}
}
