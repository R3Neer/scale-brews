package io.github.r3neer.scalebrews.collision.internal;

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
    /** Server publication fence advances when a revision is accepted, never when the first recipient happens to join. */
    public static void acceptCatalogRevision(MinecraftServer server,long revision) {
        Objects.requireNonNull(server,"server");
        if(revision<0)throw new IllegalArgumentException("Negative catalog revision");
        synchronized(REVISIONS) {
            long current=REVISIONS.getOrDefault(server,0L);
            if(revision<current)throw new IllegalArgumentException("Catalog revision rollback");
            REVISIONS.put(server,revision);
        }
    }
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
    /** Hot recipient path: packets were serialized, hashed and fragmented when the revision was accepted. */
    public static void sendCatalog(ServerPlayer player,List<AnatomyCatalogPayload> packets) {
        Objects.requireNonNull(player,"player");Objects.requireNonNull(packets,"packets");
        if(!ServerPlayNetworking.canSend(player,AnatomyCatalogPayload.TYPE))throw new IllegalStateException("Client lacks anatomy catalog protocol v3");
        if(packets.isEmpty())throw new IllegalArgumentException("Missing prepared catalog packets");
        var server=player.level().getServer();UUID expectedEpoch=epoch(server);long expectedRevision=revision(server);
        for(var packet:packets) {
            if(packet==null || !expectedEpoch.equals(packet.epoch()) || packet.revision()!=expectedRevision)
                throw new IllegalArgumentException("Prepared catalog packets do not match accepted server revision");
            ServerPlayNetworking.send(player,packet);
        }
    }
}
