package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side staging for one metadata reference immediately followed by one
 * vanilla movement packet. No physical state is created or applied here.
 */
public final class AnatomyMovementReference {
    public static final int MAX_REFERENCES_PER_TICK=16;
    public static final int PENDING_TICKS=2;

    private static final Map<ServerPlayer,State> STATES=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<ServerPlayer,State>makeMap());

    private record Pending(boolean vehicle,UUID epoch,long revision,Identifier dimension,
            int bodyNetworkId,UUID body,long trackingGeneration,long transportGeneration,
            long transportSequence,long acceptedTick) {}

    private static final class State {
        long budgetTick=Long.MIN_VALUE;
        int attempts;
        Pending pending;
    }

    private AnatomyMovementReference() {}

    /** Validates and consumes one exact server-issued receipt, then stages only its scalar cursor. */
    public static synchronized void accept(ServerPlayer player,AnatomyMoveReferencePayload reference) {
        if(player==null || reference==null || player.isRemoved())return;
        var server=player.level().getServer();if(server==null || !server.isSameThread())return;
        long now=player.level().getGameTime();
        var state=STATES.computeIfAbsent(player,ignored->new State());
        if(state.budgetTick!=now){state.budgetTick=now;state.attempts=0;}
        if(state.attempts>=MAX_REFERENCES_PER_TICK)return;
        state.attempts++;

        if(state.pending!=null) {
            if(state.pending.acceptedTick()+PENDING_TICKS<now)state.pending=null;
            else return; // One movement packet can own at most one baseline cursor.
        }

        Entity body=authorizedBody(player,reference.vehicle());
        if(body==null || !AnatomyRuntime.owns(body))return;
        var receipt=AnatomyTransportReceipts.claim(player,body,reference.transportTick(),
            reference.transportSequence(),reference.rootFrameSequence());
        if(receipt==null)return;

        state.pending=new Pending(reference.vehicle(),receipt.epoch(),receipt.catalogRevision(),receipt.dimension(),
            receipt.bodyNetworkId(),receipt.body(),receipt.trackingGeneration(),AnatomyMovement.transportGeneration(body),
            receipt.transportSequence(),now);
    }

    /**
     * Rebases the following vanilla absolute coordinate only by server transport
     * applied after the consumed receipt. The receipt's own delta was already
     * applied and is deliberately never added again.
     */
    public static synchronized Vec3 resolve(ServerPlayer player,Entity body,Vec3 absolute) {
        if(player==null || body==null || absolute==null)return absolute;
        var state=STATES.get(player);if(state==null || state.pending==null)return absolute;
        var pending=state.pending;state.pending=null; // Exactly one following vanilla packet.

        long now=player.level().getGameTime();
        if(pending.acceptedTick()>now || pending.acceptedTick()+PENDING_TICKS<now)return absolute;
        Entity authorized=authorizedBody(player,pending.vehicle());
        if(authorized!=body || body.isRemoved())return absolute;
        var server=body.level().getServer();
        if(server==null || player.level()!=body.level()
                || body.getId()!=pending.bodyNetworkId() || !body.getUUID().equals(pending.body())
                || !pending.epoch().equals(AnatomyNetworking.epoch(server))
                || pending.revision()!=AnatomyNetworking.revision(server)
                || !pending.dimension().equals(body.level().dimension().identifier())
                || pending.trackingGeneration()!=AnatomyRuntime.trackingGeneration(player,body)
                || pending.transportGeneration()!=AnatomyMovement.transportGeneration(body))
            return absolute;

        var window=TransportLedger.since(body,pending.transportSequence());
        if(!window.contiguous())return absolute;
        Vec3 rebased=absolute.add(window.appliedDelta());
        return Double.isFinite(rebased.lengthSqr())?rebased:absolute;
    }

    private static Entity authorizedBody(ServerPlayer player,boolean vehicle) {
        if(player==null || player.isRemoved())return null;
        Entity root=player.getRootVehicle();
        if(!vehicle)return root==player?player:null;
        if(root==player || root.isRemoved() || root.level()!=player.level() || root.getControllingPassenger()!=player)return null;
        return root;
    }

    public static synchronized void disconnect(ServerPlayer player) {
        if(player!=null)STATES.remove(player);
    }

    public static synchronized void clear(MinecraftServer server) {
        if(server==null)return;
        STATES.keySet().removeIf(player->player.level().getServer()==server);
    }
}
