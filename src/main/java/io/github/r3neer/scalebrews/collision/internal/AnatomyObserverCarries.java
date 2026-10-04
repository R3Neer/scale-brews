package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-only evidence from an actual passive physical carry; no position packet is inferred here. */
public final class AnatomyObserverCarries {
    public static final int HISTORY_TICKS=40,MAX_PER_BODY=64,MAX_BODIES=512;
    public record Carry(UUID epoch,long revision,Identifier dimension,UUID body,int bodyId,
            UUID support,int supportId,long supportBindingGeneration,String piece,int face,Vec3 localPoint,
            long materialSerial,long beforeSupportFrameSerial,long supportFrameSerial,long transportSequence,long tick,
            Vec3 bodyBefore,Vec3 bodyAfter,Vec3 materialBefore,Vec3 materialAfter,Vec3 appliedDelta) {
        public Carry {
            if(epoch==null || revision<0 || dimension==null || body==null || bodyId<0 || support==null || supportId<0
                    || supportBindingGeneration<1 || piece==null || piece.isBlank() || face<0 || face>5 || localPoint==null
                    || materialSerial<1 || beforeSupportFrameSerial<1 || supportFrameSerial<=beforeSupportFrameSerial
                    || transportSequence<1 || tick<0 || bodyBefore==null || bodyAfter==null
                    || materialBefore==null || materialAfter==null || appliedDelta==null
                    || !finite(localPoint) || !finite(bodyBefore) || !finite(bodyAfter)
                    || !finite(materialBefore) || !finite(materialAfter) || !finite(appliedDelta)
                    || bodyAfter.distanceToSqr(bodyBefore.add(appliedDelta))>1e-12)
                throw new IllegalArgumentException("Invalid observer carry evidence");
        }
    }
    private static final class History {
        final ArrayDeque<Carry> carries=new ArrayDeque<>();
        long saturatedAt=Long.MIN_VALUE;
    }
    /** Entity equality is network-ID based; weak identity keys isolate recycled IDs and worlds. */
    private static final Map<Entity,History> HISTORIES=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().makeMap());
    private AnatomyObserverCarries() {}

    /** Non-Q2 endpoint carries have no material-interval certificate and cannot attest observers. */
    public static void record(Entity body,LivingEntity support,SurfaceContact surface,
            long beforeSerial,long afterSerial,SupportTransport transport,ConvexBox before,ConvexBox after) {
        record(body,support,surface,0,beforeSerial,afterSerial,transport,before,after);
    }

    public static synchronized void record(Entity body,LivingEntity support,SurfaceContact surface,
            long materialSerial,long beforeSerial,long afterSerial,SupportTransport transport,ConvexBox before,ConvexBox after) {
        if(body==null || support==null || surface==null || transport==null || before==null || after==null
                || body.level().isClientSide() || body.level()!=support.level() || materialSerial<1 || beforeSerial<1 || afterSerial<=beforeSerial
                || !surface.support().equals(support.getUUID()))return;
        var server=body.level().getServer();
        var binding=AnatomyBindingState.descriptor(support);
        if(server==null || binding==null || binding.bindingGeneration()<1 || transport.tick()!=body.level().getGameTime())return;
        if(!HISTORIES.containsKey(body) && HISTORIES.size()>=MAX_BODIES) {
            reclaimExpired();
            if(HISTORIES.size()>=MAX_BODIES)return;
        }
        var history=HISTORIES.computeIfAbsent(body,ignored->new History());
        prune(history,transport.tick());
        if(history.saturatedAt!=Long.MIN_VALUE)return;
        if(history.carries.size()>=MAX_PER_BODY) {history.carries.clear();history.saturatedAt=transport.tick();return;}
        if(!history.carries.isEmpty() && transport.sequence()<=history.carries.peekLast().transportSequence()) {
            history.carries.clear();history.saturatedAt=transport.tick();return;
        }
        try {
            var applied=transport.appliedDelta();
            var bodyAfter=body.position();
            history.carries.addLast(new Carry(AnatomyNetworking.epoch(server),surface.revision(),
                body.level().dimension().identifier(),body.getUUID(),body.getId(),support.getUUID(),support.getId(),
                binding.bindingGeneration(),surface.piece(),surface.face(),surface.localPoint(),materialSerial,beforeSerial,afterSerial,
                transport.sequence(),transport.tick(),bodyAfter.subtract(applied),bodyAfter,
                before.point(surface.localPoint()),after.point(surface.localPoint()),applied));
        } catch(RuntimeException invalid) {
            history.carries.clear();history.saturatedAt=transport.tick();
        }
    }
    public static synchronized List<Carry> snapshot(Entity body) {
        if(body==null)return List.of();
        var history=HISTORIES.get(body);
        if(history==null)return List.of();
        prune(history,body.level().getGameTime());
        if(history.carries.isEmpty() && history.saturatedAt==Long.MIN_VALUE) {
            HISTORIES.remove(body);
            return List.of();
        }
        return history.saturatedAt==Long.MIN_VALUE?List.copyOf(history.carries):List.of();
    }
    public static synchronized void invalidate(Entity body) {if(body!=null)HISTORIES.remove(body);}
    private static void prune(History history,long tick) {
        while(!history.carries.isEmpty() && tick-history.carries.peekFirst().tick()>=HISTORY_TICKS)history.carries.removeFirst();
        if(history.saturatedAt!=Long.MIN_VALUE && tick-history.saturatedAt>=HISTORY_TICKS)history.saturatedAt=Long.MIN_VALUE;
    }
    /** Only scan the bounded table when capacity is exhausted, never on an ordinary carry. */
    private static void reclaimExpired() {
        var expired=new ArrayList<Entity>();
        for(var entry:HISTORIES.entrySet()) {
            var tracked=entry.getKey();
            if(tracked==null)continue;
            if(tracked.isRemoved()) {expired.add(tracked);continue;}
            var history=entry.getValue();
            prune(history,tracked.level().getGameTime());
            if(history.carries.isEmpty() && history.saturatedAt==Long.MIN_VALUE)expired.add(tracked);
        }
        for(var tracked:expired)HISTORIES.remove(tracked);
    }
    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
