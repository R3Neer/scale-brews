package io.github.r3neer.scalebrews.collision.internal;

import java.util.*;
import net.minecraft.resources.Identifier;

/**
 * Bounded client-side pending queue plus accepted watermark. Consuming a
 * pending packet after physical confirmation never discards its ordering
 * watermark, so a delayed packet cannot restore a cleared/confirmed contact.
 */
public final class AnatomyContactInbox {
    public static final int MAX_BODIES=4096;
    private final Map<UUID,AnatomyContactPayload> watermarks=new HashMap<>();
    private final Map<UUID,AnatomyContactPayload> pending=new HashMap<>();

    public boolean accept(AnatomyContactPayload candidate) {
        var previous=watermarks.get(candidate.body());
        if(previous==null && watermarks.size()>=MAX_BODIES)return false;
        if(previous!=null) {
            if(candidate.bodyId()!=previous.bodyId()) {
                if(candidate.trackingGeneration()<=previous.trackingGeneration())return false;
            } else if(!AnatomyContactOrdering.newer(candidate,previous))return false;
        }
        watermarks.put(candidate.body(),candidate);pending.put(candidate.body(),candidate);return true;
    }
    /** Receiver-facing gate: a contact is authoritative only for its declared level. */
    public boolean accept(Identifier levelDimension,AnatomyContactPayload candidate) {
        return levelDimension!=null && candidate.dimension().equals(levelDimension) && accept(candidate);
    }
    public Map<UUID,AnatomyContactPayload> pending(){return Map.copyOf(pending);}
    public void consume(AnatomyContactPayload packet){pending.remove(packet.body(),packet);}
    /** A support's geometry became unavailable; retain watermarks but do not confirm stale pending contacts. */
    public void discardPendingSupport(UUID support) {
        if(support==null)return;
        pending.entrySet().removeIf(entry->support.equals(entry.getValue().support()));
    }
    /** World/server ticks, never wall-clock or render frames, bound retained stale identities. */
    public void prune(long serverTick,long maxAge) {
        for(var iterator=watermarks.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();if(entry.getValue().tick()+maxAge<serverTick) {pending.remove(entry.getKey());iterator.remove();}
        }
    }
    public void clear(){watermarks.clear();pending.clear();}
}
