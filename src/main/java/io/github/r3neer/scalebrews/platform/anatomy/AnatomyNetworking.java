package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AnatomyNetworking {
    private AnatomyNetworking() {}
    private static final Map<MinecraftServer,UUID> EPOCHS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<MinecraftServer,Long> REVISIONS=Collections.synchronizedMap(new WeakHashMap<>());
    public static long revision(MinecraftServer server){return REVISIONS.getOrDefault(server,0L);}
    public static UUID epoch(MinecraftServer server){return EPOCHS.computeIfAbsent(server,s->UUID.randomUUID());}
    public static void initialize(){
        PayloadTypeRegistry.clientboundPlay().register(AnatomyCatalogPayload.TYPE,AnatomyCatalogPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AnatomyPosePayload.TYPE,AnatomyPosePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AnatomyContactPayload.TYPE,AnatomyContactPayload.CODEC);
    }
    public static void sendPose(ServerPlayer recipient,long revision,net.minecraft.world.entity.LivingEntity entity,net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,PoseProvider.Inputs inputs) {
        if(!ServerPlayNetworking.canSend(recipient,AnatomyPosePayload.TYPE))throw new IllegalStateException("Client lacks anatomy pose protocol v2");
        UUID epoch=epoch(recipient.level().getServer());
        ServerPlayNetworking.send(recipient,new AnatomyPosePayload(epoch,revision,entity.level().dimension().identifier(),entity.getId(),entity.getUUID(),model,provider,
            entity.level().getGameTime(),inputs,entity.position(),entity.yBodyRot,entity.getScale(),AnatomyMovement.gravity(entity).down()));
    }
    public static void sendCatalog(ServerPlayer player,long revision,Map<String,ModelGeometry> models) {
        sendCatalog(player,revision,models,Map.of());
    }
    public static void sendCatalog(ServerPlayer player,long revision,Map<String,ModelGeometry> models,Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles) {
        if(!ServerPlayNetworking.canSend(player,AnatomyCatalogPayload.TYPE))throw new IllegalStateException("Client lacks anatomy catalog protocol v2");
        UUID epoch=epoch(player.level().getServer());
        for(var packet:AnatomyCatalogTransfer.encode(epoch,revision,models,profiles))ServerPlayNetworking.send(player,packet);
        REVISIONS.merge(player.level().getServer(),revision,Math::max);
    }
}
