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
    /** Builds v4 only from one immutable, server-authoritative published endpoint. */
    public static AnatomyPosePayload posePayload(GeometryProvider.PublishedFrame frame) {
        if(frame==null)throw new IllegalArgumentException("Missing authoritative published frame");
        var identity=frame.identity();var endpoint=frame.endpoint();var sample=endpoint.sample();var root=endpoint.root();
        return new AnatomyPosePayload(identity.epoch(),identity.revision(),identity.dimension().identifier(),identity.entityId(),identity.support(),
            identity.model(),identity.poseProvider(),endpoint.frameSerial(),endpoint.authorityTick(),endpoint.jointSampleTick(),root.sequence(),root.tick(),
            identity.bindingGeneration(),endpoint.availability()==GeometryProvider.Availability.AVAILABLE,sample.inputs(),sample.origin(),sample.yaw(),sample.scale(),sample.gravity().down());
    }
    /** Typed fixture factory; production must pass {@link GeometryProvider.PublishedFrame}. */
    public static AnatomyPosePayload posePayload(UUID epoch,long revision,net.minecraft.resources.Identifier dimension,int entityId,UUID entity,
            net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,long bindingGeneration,GeometryProvider.CausalEndpoint endpoint) {
        if(endpoint==null || bindingGeneration<1)throw new IllegalArgumentException("Missing causal binding frame");
        var sample=endpoint.sample();var root=endpoint.root();
        return new AnatomyPosePayload(epoch,revision,dimension,entityId,entity,model,provider,endpoint.frameSerial(),endpoint.authorityTick(),endpoint.jointSampleTick(),
            root.sequence(),root.tick(),bindingGeneration,endpoint.availability()==GeometryProvider.Availability.AVAILABLE,sample.inputs(),sample.origin(),sample.yaw(),sample.scale(),sample.gravity().down());
    }
    /** Sends exactly one immutable server published endpoint; never reads live TRS after capture. */
    public static void sendPose(ServerPlayer recipient,GeometryProvider.PublishedFrame frame) {
        if(!ServerPlayNetworking.canSend(recipient,AnatomyPosePayload.TYPE))throw new IllegalStateException("Client lacks anatomy pose protocol v4");
        ServerPlayNetworking.send(recipient,posePayload(frame));
    }
    public static void sendCatalog(ServerPlayer player,long revision,Map<String,ModelGeometry> models) {
        sendCatalog(player,revision,models,Map.of());
    }
    public static void sendCatalog(ServerPlayer player,long revision,Map<String,ModelGeometry> models,Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles) {
        if(!ServerPlayNetworking.canSend(player,AnatomyCatalogPayload.TYPE))throw new IllegalStateException("Client lacks anatomy catalog protocol v3");
        UUID epoch=epoch(player.level().getServer());
        for(var packet:AnatomyCatalogTransfer.encode(epoch,revision,models,profiles))ServerPlayNetworking.send(player,packet);
        REVISIONS.merge(player.level().getServer(),revision,Math::max);
    }
}
