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
    private static final class Watermark {
        AnatomyContactPayload last;
        long retiredGeneration;
        Watermark(AnatomyContactPayload last){this.last=last;}
    }
    private final Map<UUID,Watermark> watermarks=new HashMap<>();
    private final Map<UUID,AnatomyContactPayload> pending=new HashMap<>();

    public boolean accept(AnatomyContactPayload candidate) {
        var watermark=watermarks.get(candidate.body());
        if(watermark==null && watermarks.size()>=MAX_BODIES)return false;
        if(watermark!=null) {
            if(candidate.trackingGeneration()<=watermark.retiredGeneration)return false;
            var previous=watermark.last;
            if(candidate.bodyId()!=previous.bodyId()) {
                if(candidate.trackingGeneration()<=previous.trackingGeneration())return false;
            } else if(!AnatomyContactOrdering.newer(candidate,previous))return false;
        }
        if(watermark==null)watermarks.put(candidate.body(),new Watermark(candidate));
        else watermark.last=candidate;
        pending.put(candidate.body(),candidate);return true;
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
    /**
     * A level or socket barrier discards material work and closes every observed body tracking
     * window. A -> B -> A and same-catalog reconnect cannot authorize an unseen later sequence
     * from an old window before fresh tracking arrives.
     */
    public void retireWindows() {
        for(var watermark:watermarks.values())
            watermark.retiredGeneration=Math.max(watermark.retiredGeneration,watermark.last.trackingGeneration());
        pending.clear();
    }
    /** World/server ticks, never wall-clock or render frames, bound retained stale identities. */
    public void prune(long serverTick,long maxAge) {
        if(serverTick<0 || maxAge<0)throw new IllegalArgumentException("Invalid contact retention window");
        for(var iterator=watermarks.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();var last=entry.getValue().last;
            if(last.tick()<serverTick && serverTick-last.tick()>maxAge) {pending.remove(entry.getKey());iterator.remove();}
        }
    }
    /** A different accepted catalog identity replaces the owning causal session. */
    public void clear(){watermarks.clear();pending.clear();}
}
