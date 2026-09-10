package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyTransportReceipts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import io.github.r3neer.scalebrews.test.mixin.TestVehicleMoveListenerAccess;

/**
 * Bounded test-only audit of the exact vanilla vehicle acceptance state.
 *
 * <p>It is deliberately independent of the production carry/rebase code:
 * the server-thread pre-acceptance snapshot contains the raw client packet and
 * vanilla first/last-good values, while RETURN records post-handler state. A
 * test marks setup and measurement boundaries explicitly, so delayed setup
 * traffic cannot be misreported as a phase correction.</p>
 */
public final class VehicleMoveAudit {
    private static final int MAX_EVENTS=768;
    private static final Map<UUID,Recorder> ACTIVE=new ConcurrentHashMap<>();

    public record Event(long ordinal,long tick,String boundary,Vec3 packetTarget,Vec3 bodyPosition,
            UUID bodyUuid,int bodyId,UUID lastVehicleUuid,int lastVehicleId,boolean lastVehicleMatchesBody,
            Vec3 firstGood,Vec3 lastGood,String surface,long contactSequence,long transportSequence) {}
    public record Marker(long ordinal,long tick,String name) {}
    public record Snapshot(Marker from,Marker to,long dropped,List<Event> events) {}

    private VehicleMoveAudit() {}

    public static void begin(ServerPlayer player) {
        ACTIVE.put(player.getUUID(),new Recorder());
    }

    public static Marker mark(ServerPlayer player,String name) {
        var recorder=ACTIVE.get(player.getUUID());
        return recorder==null ? new Marker(0,player.level().getGameTime(),name) : recorder.mark(player.level().getGameTime(),name);
    }

    public static Snapshot snapshot(ServerPlayer player,Marker from) {
        var recorder=ACTIVE.get(player.getUUID());
        return recorder==null ? new Snapshot(from,new Marker(0,player.level().getGameTime(),"missing"),0,List.of()) : recorder.snapshot(from);
    }

    public static void end(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static void before(ServerGamePacketListenerImpl listener,ServerboundMoveVehiclePacket packet) {
        record(listener,packet,"before");
    }

    public static void after(ServerGamePacketListenerImpl listener,ServerboundMoveVehiclePacket packet) {
        record(listener,packet,"after");
    }

    private static void record(ServerGamePacketListenerImpl listener,ServerboundMoveVehiclePacket packet,String boundary) {
        ServerPlayer player=listener.player;var recorder=ACTIVE.get(player.getUUID());if(recorder==null)return;
        var access=(TestVehicleMoveListenerAccess)listener;Entity body=player.getRootVehicle();Entity last=access.test$lastVehicle();
        var receipt=AnatomyTransportReceipts.history(player,body.getUUID());var latest=receipt.isEmpty()?null:receipt.getLast();
        var surface=AnatomyMovement.surface(body);
        recorder.record(new Event(0,player.level().getGameTime(),boundary,packet.position(),body.position(),body.getUUID(),body.getId(),
            last==null?null:last.getUUID(),last==null?-1:last.getId(),last==body,
            new Vec3(access.test$vehicleFirstGoodX(),access.test$vehicleFirstGoodY(),access.test$vehicleFirstGoodZ()),
            new Vec3(access.test$vehicleLastGoodX(),access.test$vehicleLastGoodY(),access.test$vehicleLastGoodZ()),
            surface==null?"none":surface.piece()+"/"+surface.face(),AnatomyMovement.contactSequence(body),latest==null?0:latest.transportSequence()));
    }

    private static final class Recorder {
        private final List<Event> events=new ArrayList<>();
        private long ordinal;
        private long dropped;

        synchronized void record(Event event) {
            if(events.size()>=MAX_EVENTS) {dropped++;return;}
            events.add(new Event(++ordinal,event.tick(),event.boundary(),event.packetTarget(),event.bodyPosition(),event.bodyUuid(),event.bodyId(),
                event.lastVehicleUuid(),event.lastVehicleId(),event.lastVehicleMatchesBody(),event.firstGood(),event.lastGood(),event.surface(),event.contactSequence(),event.transportSequence()));
        }

        synchronized Marker mark(long tick,String name) {return new Marker(ordinal,tick,name);}

        synchronized Snapshot snapshot(Marker from) {
            var selected=new ArrayList<Event>();for(var event:events)if(event.ordinal()>from.ordinal())selected.add(event);
            return new Snapshot(from,new Marker(ordinal,events.isEmpty()?from.tick():events.getLast().tick(),"snapshot"),dropped,List.copyOf(selected));
        }
    }
}
