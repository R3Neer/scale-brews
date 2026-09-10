package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Bounded, server-authoritative provenance for deltas that have already been
 * applied and baselined. A receipt never computes or reapplies transport; it
 * only lets a future metadata reference correlate a vanilla absolute move with
 * a material frame that the server actually accepted.
 */
public final class AnatomyTransportReceipts {
    public static final int HISTORY_TICKS=40;
    public static final int MAX_RECEIPTS_PER_TICK=16;
    // ServerPlayer may reuse an entity/network ID. Weak identity keys keep a
    // replacement connection distinct until its explicit disconnect cleanup.
    private static final Map<ServerPlayer,Map<UUID,History>> HISTORIES=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<ServerPlayer,Map<UUID,History>>makeMap());
    private static long recorded,duplicates,overflows,invalidations;
    private AnatomyTransportReceipts() {}

    public record Receipt(UUID epoch,long catalogRevision,Identifier dimension,int bodyNetworkId,UUID body,long trackingGeneration,
            UUID support,String piece,int face,Vec3 localPoint,Vec3 normal,long surfaceTick,long contactSequence,
            long rootFrameSequence,long transportSequence,long tick,Vec3 bodyBefore,Vec3 bodyAfter,
            AnatomyMovement.RootFrame rootFrame,ConvexBox materialBefore,ConvexBox materialAfter,Vec3 appliedDelta) {
        public Receipt {
            if(epoch==null || catalogRevision<0 || dimension==null || bodyNetworkId<0 || body==null || trackingGeneration<1 || support==null
                    || piece==null || piece.isBlank() || face<0 || face>5 || localPoint==null || normal==null
                    || surfaceTick<0 || contactSequence<0 || rootFrameSequence<0 || transportSequence<1 || tick<0 || bodyBefore==null || bodyAfter==null
                    || rootFrame==null || materialBefore==null || materialAfter==null || appliedDelta==null || !finite(localPoint) || !finite(normal) || !finite(bodyBefore)
                    || !finite(bodyAfter) || !finite(appliedDelta))
                throw new IllegalArgumentException("Invalid confirmed anatomical transport receipt");
        }
        private static boolean finite(Vec3 value){return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);}
    }
    public record Metrics(long recorded,long duplicates,long overflows,long invalidations,long entries) {}
    private static final class History {
        final ArrayDeque<Receipt> entries=new ArrayDeque<>();
        /** Every saturated tick remains rejectable for the complete receipt TTL. */
        final ArrayDeque<Long> saturatedTicks=new ArrayDeque<>();
        long countTick=Long.MIN_VALUE;
        int count;
    }

    /** Called only after P1 has moved the root and refreshed every vanilla baseline. */
    public static synchronized void record(Entity body,AnatomyMovement.Contact contact,SurfaceContact surface,
            AnatomyMovement.RootFrame root,SupportTransport transport,ConvexBox materialBefore,ConvexBox materialAfter) {
        if(body==null || contact==null || surface==null || root==null || transport==null || materialBefore==null || materialAfter==null || body.level().isClientSide())return;
        if(!contact.support().getUUID().equals(surface.support()) || contact.revision()!=surface.revision()
                || !contact.piece().equals(surface.piece()))return;
        var server=body.level().getServer();if(server==null)return;
        var recipients=new LinkedHashSet<ServerPlayer>();
        if(body instanceof ServerPlayer player)recipients.add(player);
        for(var passenger:body.getIndirectPassengers())if(passenger instanceof ServerPlayer player)recipients.add(player);
        if(recipients.isEmpty())return;
        long tick=body.level().getGameTime();
        Vec3 appliedDelta=transport.appliedDelta(),after=body.position(),before=after.subtract(appliedDelta);
        for(var recipient:recipients) {
            long generation=AnatomyRuntime.trackingGeneration(recipient,body);
            var receipt=new Receipt(AnatomyNetworking.epoch(server),contact.revision(),body.level().dimension().identifier(),body.getId(),body.getUUID(),generation,
                surface.support(),surface.piece(),surface.face(),surface.localPoint(),surface.normal(),surface.tick(),contact.sequence(),
                root.sequence(),transport.sequence(),tick,before,after,root,materialBefore,materialAfter,appliedDelta);
            append(recipient,receipt);
        }
    }

    private static void append(ServerPlayer recipient,Receipt receipt) {
        var history=HISTORIES.computeIfAbsent(recipient,ignored->new HashMap<>()).computeIfAbsent(receipt.body(),ignored->new History());
        prune(history,receipt.tick());
        if(history.countTick!=receipt.tick()) {history.countTick=receipt.tick();history.count=0;}
        for(var entry:history.entries)if(entry.body().equals(receipt.body())
                && entry.transportSequence()==receipt.transportSequence()) {duplicates++;return;}
        if(history.count>=MAX_RECEIPTS_PER_TICK) {markSaturated(history,receipt.tick());overflows++;return;}
        history.entries.addLast(receipt);history.count++;recorded++;
    }
    private static void prune(History history,long tick) {
        while(!history.entries.isEmpty() && history.entries.peekFirst().tick()<tick-HISTORY_TICKS+1)history.entries.removeFirst();
        while(!history.saturatedTicks.isEmpty() && history.saturatedTicks.peekFirst()<tick-HISTORY_TICKS+1)history.saturatedTicks.removeFirst();
    }
    private static void markSaturated(History history,long tick) {
        if(history.saturatedTicks.isEmpty() || history.saturatedTicks.peekLast()!=tick)history.saturatedTicks.addLast(tick);
    }
    private static boolean empty(History history) {
        return history.entries.isEmpty() && history.saturatedTicks.isEmpty();
    }
    private static History pruneAndFind(ServerPlayer recipient,UUID body,long tick) {
        var roots=HISTORIES.get(recipient);if(roots==null)return null;
        var history=roots.get(body);if(history==null)return null;
        prune(history,tick);
        if(!empty(history))return history;
        roots.remove(body);if(roots.isEmpty())HISTORIES.remove(recipient);
        return null;
    }
    /** Immutable snapshot for an eventual metadata-only C2S reference validator. */
    public static synchronized List<Receipt> history(ServerPlayer recipient,UUID body) {
        var history=recipient==null || body==null?null:pruneAndFind(recipient,body,recipient.level().getGameTime());
        return history==null?List.of():List.copyOf(history.entries);
    }
    /** A future C2S validator must reject a reference from a saturated body/tick. */
    public static synchronized boolean saturated(ServerPlayer recipient,UUID body,long tick) {
        var history=recipient==null || body==null?null:pruneAndFind(recipient,body,recipient.level().getGameTime());
        return history!=null && history.saturatedTicks.contains(tick);
    }
    public static synchronized void invalidate(Entity body) {
        if(body==null)return;
        var emptyRecipients=new ArrayList<ServerPlayer>();
        for(var entry:HISTORIES.entrySet()) {
            var roots=entry.getValue();roots.remove(body.getUUID());
            if(roots.isEmpty())emptyRecipients.add(entry.getKey());
        }
        for(var recipient:emptyRecipients)HISTORIES.remove(recipient);
        invalidations++;
    }
    public static synchronized void disconnect(ServerPlayer player) {
        if(player!=null)HISTORIES.remove(player);
    }
    public static synchronized void clear(MinecraftServer server) {
        if(server==null)return;
        HISTORIES.keySet().removeIf(player->player.level().getServer()==server);
    }
    public static synchronized Metrics metrics() {
        long entries=0;
        var emptyRecipients=new ArrayList<ServerPlayer>();
        for(var recipient:HISTORIES.entrySet()) {
            var roots=recipient.getValue();long tick=recipient.getKey().level().getGameTime();
            var bodies=roots.entrySet().iterator();
            while(bodies.hasNext()) {
                var history=bodies.next().getValue();prune(history,tick);
                if(empty(history))bodies.remove();else entries+=history.entries.size();
            }
            if(roots.isEmpty())emptyRecipients.add(recipient.getKey());
        }
        for(var recipient:emptyRecipients)HISTORIES.remove(recipient);
        return new Metrics(recorded,duplicates,overflows,invalidations,entries);
    }
}
