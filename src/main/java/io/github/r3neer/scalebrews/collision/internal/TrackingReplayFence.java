package io.github.r3neer.scalebrews.collision.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/**
 * Bounded client-side tombstones for causal frame histories that have left the live history map.
 *
 * <p>The tombstone is intentionally much smaller than {@link AnatomyFrameHistory}: it retains only
 * the identity axes and ordering watermarks required to prove that a later packet is not a replay.
 * A genuinely newer packet from the same tracking window may therefore recover after a quiet TTL,
 * while the expired packet itself and every older binding/tracking generation remain rejected.
 * Explicit lifecycle retirement instead requires a newer tracking window.</p>
 *
 * <p>Dimension is part of the retired scope. A same-connection A -> B -> A transition must retain
 * the watermark from the first visit to A while allowing an independent fresh tracking window in B.
 * If the constant tombstone budget is exhausted the fence saturates and rejects every later pose
 * until the surrounding accepted catalog identity changes and clears it. Evicting a tombstone to make room would
 * make the corresponding old generation authoritative again.</p>
 */
public final class TrackingReplayFence {
    public static final int MAX_RETIRED=4096;

    private record Tombstone(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
            Identifier model,Identifier provider,Identifier rootProvider,long bindingGeneration,long trackingGeneration,
            long frameSerial,long authorityTick,long jointSampleTick,boolean closedWindow) {
        static Tombstone of(AnatomyPosePayload packet,boolean closedWindow) {
            return new Tombstone(packet.epoch(),packet.revision(),packet.dimension(),packet.entityId(),packet.entity(),
                packet.model(),packet.provider(),packet.rootProvider(),packet.bindingGeneration(),packet.trackingGeneration(),
                packet.frameSerial(),packet.authorityTick(),packet.jointSampleTick(),closedWindow);
        }
        boolean stableBindingIdentity(AnatomyPosePayload packet) {
            return entityId==packet.entityId() && model.equals(packet.model()) && provider.equals(packet.provider())
                && rootProvider.equals(packet.rootProvider());
        }
    }

    /** Canonical scopes grouped by support: certificate lookups never visit unrelated UUIDs. */
    private final Map<UUID,Map<Identifier,Tombstone>> retired=new HashMap<>();
    private int retiredScopes;
    private boolean saturated;

    /** Retires one accepted causal history. False means the fence saturated conservatively. */
    public boolean retire(AnatomyPosePayload packet) {
        return retire(packet,false);
    }

    /** Explicit lifecycle barriers require a fresh tracking window, even for newer old-window frames. */
    public boolean retireWindow(AnatomyPosePayload packet) {
        return retire(packet,true);
    }

    private boolean retire(AnatomyPosePayload packet,boolean closedWindow) {
        Objects.requireNonNull(packet,"packet");
        if(saturated)return false;
        var scopes=retired.get(packet.entity());
        var current=scopes==null?null:scopes.get(packet.dimension());
        var next=Tombstone.of(packet,closedWindow || (current!=null && current.closedWindow
            && packet.trackingGeneration()==current.trackingGeneration));
        if(current==null) {
            if(retiredScopes>=MAX_RETIRED) {
                saturated=true;
                return false;
            }
            if(scopes==null) {scopes=new HashMap<>();retired.put(packet.entity(),scopes);}
            scopes.put(packet.dimension(),next);retiredScopes++;
            return true;
        }
        if(packet.trackingGeneration()<current.trackingGeneration || packet.bindingGeneration()<current.bindingGeneration)return true;
        if(packet.trackingGeneration()==current.trackingGeneration && packet.bindingGeneration()==current.bindingGeneration) {
            if(current.stableBindingIdentity(packet) && packet.frameSerial()>=current.frameSerial
                    && packet.authorityTick()>=current.authorityTick && packet.jointSampleTick()>=current.jointSampleTick)
                scopes.put(packet.dimension(),next);
            return true;
        }
        scopes.put(packet.dimension(),next);
        return true;
    }

    /** True when this packet cannot prove authority beyond the compact retired watermark for its dimension. */
    public boolean rejects(AnatomyPosePayload packet) {
        Objects.requireNonNull(packet,"packet");
        if(saturated)return true;
        var scopes=retired.get(packet.entity());
        var current=scopes==null?null:scopes.get(packet.dimension());if(current==null)return false;
        if(!current.epoch.equals(packet.epoch()) || current.revision!=packet.revision()
                || !current.entity.equals(packet.entity()))return true;
        if(packet.bindingGeneration()<current.bindingGeneration || packet.trackingGeneration()<current.trackingGeneration)return true;
        if(current.closedWindow && packet.trackingGeneration()<=current.trackingGeneration)return true;

        boolean bindingChanged=packet.bindingGeneration()!=current.bindingGeneration;
        boolean trackingChanged=packet.trackingGeneration()!=current.trackingGeneration;
        if(!bindingChanged && !trackingChanged) {
            if(!current.stableBindingIdentity(packet))return true;
            return packet.frameSerial()<=current.frameSerial || packet.authorityTick()<current.authorityTick
                || packet.jointSampleTick()<current.jointSampleTick;
        }
        // A pure retrack is still the same physical binding. If its network/model/provider/root identity
        // changed as well, the server must advance bindingGeneration rather than laundering a rebind as tracking.
        return !bindingChanged && !current.stableBindingIdentity(packet);
    }

    /** The live history now owns this dimension's watermark again; other dimension barriers remain intact. */
    public void accepted(AnatomyPosePayload packet) {
        Objects.requireNonNull(packet,"packet");
        if(!saturated) {
            var scopes=retired.get(packet.entity());
            if(scopes!=null && scopes.remove(packet.dimension())!=null) {
                retiredScopes--;
                if(scopes.isEmpty())retired.remove(packet.entity());
            }
        }
    }

    public void clear(){retired.clear();retiredScopes=0;saturated=false;}
    /** Lifecycle-only pass: quiet histories evicted before disconnect must close their windows too. */
    public void closeWindows(){
        for(var scopes:retired.values())scopes.replaceAll((dimension,current)->current.closedWindow?current:
            new Tombstone(current.epoch,current.revision,current.dimension,current.entityId,current.entity,
                current.model,current.provider,current.rootProvider,current.bindingGeneration,current.trackingGeneration,
                current.frameSerial,current.authorityTick,current.jointSampleTick,true));
    }
    public boolean saturated(){return saturated;}
    public int entries(){return retiredScopes;}
    public long retiredGeneration(UUID entity){
        long result=0;
        var scopes=retired.get(entity);if(scopes==null)return 0;
        for(var entry:scopes.values())result=Math.max(result,entry.trackingGeneration());
        return result;
    }
}
