package io.github.r3neer.scalebrews.collision.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bounded client-side tombstones for recipient tracking windows whose frame history was retired.
 *
 * <p>Frame histories themselves may expire, but a delayed packet from the retired tracking window
 * must not regain authority merely because its history left memory. A newer server-owned tracking
 * generation releases that entity's tombstone. If the constant tombstone budget is exhausted the
 * fence saturates and rejects every later pose until the surrounding connection/level/catalog
 * reset clears it; eviction would make an old generation authoritative again.</p>
 */
public final class TrackingReplayFence {
    public static final int MAX_RETIRED=4096;

    private final Map<UUID,Long> retired=new HashMap<>();
    private boolean saturated;

    /** Retires one accepted tracking window. False means the fence saturated conservatively. */
    public boolean retire(UUID entity,long trackingGeneration) {
        Objects.requireNonNull(entity,"entity");
        if(trackingGeneration<1)throw new IllegalArgumentException("Invalid retired tracking generation");
        if(saturated)return false;
        var current=retired.get(entity);
        if(current!=null) {
            if(trackingGeneration>current)retired.put(entity,trackingGeneration);
            return true;
        }
        if(retired.size()>=MAX_RETIRED) {
            saturated=true;
            return false;
        }
        retired.put(entity,trackingGeneration);
        return true;
    }

    /** A saturated fence has lost localized capacity and therefore fails closed for every packet. */
    public boolean rejects(UUID entity,long trackingGeneration) {
        Objects.requireNonNull(entity,"entity");
        if(trackingGeneration<1)throw new IllegalArgumentException("Invalid tracking generation");
        if(saturated)return true;
        return trackingGeneration<=retired.getOrDefault(entity,0L);
    }

    /** A strictly newer accepted tracking window makes only that entity's old tombstone obsolete. */
    public void accepted(UUID entity,long trackingGeneration) {
        Objects.requireNonNull(entity,"entity");
        if(trackingGeneration<1)throw new IllegalArgumentException("Invalid tracking generation");
        if(saturated)return;
        var current=retired.get(entity);
        if(current!=null && trackingGeneration>current)retired.remove(entity);
    }

    public void clear(){retired.clear();saturated=false;}
    public boolean saturated(){return saturated;}
    public int entries(){return retired.size();}
    public long retiredGeneration(UUID entity){return retired.getOrDefault(entity,0L);}
}
