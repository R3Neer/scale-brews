package io.github.r3neer.scalebrews.platform;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.HashSet;

public final class PlatformNetworking {
    private PlatformNetworking() {}
    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(PlatformPayload.TYPE,PlatformPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PlatformMovePayload.TYPE,PlatformMovePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PlatformMovePayload.TYPE,(reference,context)->{
            var player=context.player();
            Entity body=reference.body()==player.getId()?player:player.getRootVehicle();
            if((body==player || body.getControllingPassenger()==player)
                && body.getId()==reference.body() && reference.finite() && Platforms.supported(body)
                && Platforms.state(body).support.getId()==reference.support())
                Platforms.state(body).pendingReference=reference;
        });
        EntityTrackingEvents.START_TRACKING.register((entity,player)->{
            if (Platforms.supported(entity)) send(entity,player);
        });
    }
    public static PlatformPayload snapshot(Entity e) {
        var s=Platforms.state(e);
        return new PlatformPayload(e.getId(),s.support==null?-1:s.support.getId(),
            s.surface==null?"":s.surface.id(),s.contact,e.level().getGameTime());
    }
    public static void send(Entity e,ServerPlayer p) {
        if (ServerPlayNetworking.canSend(p,PlatformPayload.TYPE)) ServerPlayNetworking.send(p,snapshot(e));
    }
    public static void broadcast(Entity e) {
        var players=new HashSet<>(PlayerLookup.tracking(e));
        if (e instanceof ServerPlayer p) players.add(p);
        for (var passenger:e.getIndirectPassengers()) if (passenger instanceof ServerPlayer p) players.add(p);
        for (var player:players) send(e,player);
    }
}
