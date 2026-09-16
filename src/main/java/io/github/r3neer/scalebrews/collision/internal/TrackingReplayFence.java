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
 * while the expired packet itself and every older binding/tracking generation remain rejected.</p>
 *
 * <p>If the constant tombstone budget is exhausted the fence saturates and rejects every later pose
 * until the surrounding connection/level/catalog reset clears it. Evicting a tombstone to make room
 * would make the corresponding old generation authoritative again.</p>
 */
public final class TrackingReplayFence {
    public static final int MAX_RETIRED=4096;

    private record Tombstone(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
            Identifier model,Identifier provider,Identifier rootProvider,long bindingGeneration,long trackingGeneration,
            long frameSerial,long authorityTick,long jointSampleTick) {
        static Tombstone of(AnatomyPosePayload packet) {
            return new Tombstone(packet.epoch(),packet.revision(),packet.dimension(),packet.entityId(),packet.entity(),
                packet.model(),packet.provider(),packet.rootProvider(),packet.bindingGeneration(),packet.trackingGeneration(),
                packet.frameSerial(),packet.authorityTick(),packet.jointSampleTick());
        }
        boolean stableBindingIdentity(AnatomyPosePayload packet) {
            return entityId==packet.entityId() && model.equals(packet.model()) && provider.equals(packet.provider())
                && rootProvider.equals(packet.rootProvider());
        }
    }

    private final Map<UUID,Tombstone> retired=new HashMap<>();
    private boolean saturated;

    /** Retires one accepted causal history. False means the fence saturated conservatively. */
    public boolean retire(AnatomyPosePayload packet) {
        Objects.requireNonNull(packet,"packet");
        if(saturated)return false;
        var next=Tombstone.of(packet);var current=retired.get(packet.entity());
        if(current==null) {
            if(retired.size()>=MAX_RETIRED) {
                saturated=true;
                return false;
            }
            retired.put(packet.entity(),next);
            return true;
        }
        if(packet.trackingGeneration()<current.trackingGeneration || packet.bindingGeneration()<current.bindingGeneration)return true;
        if(packet.trackingGeneration()==current.trackingGeneration && packet.bindingGeneration()==current.bindingGeneration) {
            if(current.stableBindingIdentity(packet) && packet.frameSerial()>current.frameSerial
                    && packet.authorityTick()>=current.authorityTick && packet.jointSampleTick()>=current.jointSampleTick)
                retired.put(packet.entity(),next);
            return true;
        }
        retired.put(packet.entity(),next);
        return true;
    }

    /** True when this packet cannot prove authority beyond the compact retired watermark. */
    public boolean rejects(AnatomyPosePayload packet) {
        Objects.requireNonNull(packet,"packet");
        if(saturated)return true;
        var current=retired.get(packet.entity());if(current==null)return false;
        if(!current.epoch.equals(packet.epoch()) || current.revision!=packet.revision() || !current.dimension.equals(packet.dimension())
                || !current.entity.equals(packet.entity()))return true;
        if(packet.bindingGeneration()<current.bindingGeneration || packet.trackingGeneration()<current.trackingGeneration)return true;

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

    /** The live history now owns the watermark again, so its compact tombstone can be released. */
    public void accepted(AnatomyPosePayload packet) {
        Objects.requireNonNull(packet,"packet");
        if(!saturated)retired.remove(packet.entity());
    }

    public void clear(){retired.clear();saturated=false;}
    public boolean saturated(){return saturated;}
    public int entries(){return retired.size();}
    public long retiredGeneration(UUID entity){var value=retired.get(entity);return value==null?0:value.trackingGeneration;}
}
