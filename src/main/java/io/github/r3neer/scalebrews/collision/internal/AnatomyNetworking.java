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
    /** Fixture/diagnostic factory. Live sends must provide the recipient tracking generation explicitly. */
    public static AnatomyPosePayload posePayload(GeometryProvider.PublishedFrame frame) {
        return posePayload(frame,1);
    }
    /** Builds v6 from one immutable server-authoritative published endpoint and recipient tracking generation. */
    public static AnatomyPosePayload posePayload(GeometryProvider.PublishedFrame frame,long trackingGeneration) {
        if(frame==null)throw new IllegalArgumentException("Missing authoritative published frame");
        if(trackingGeneration<1)throw new IllegalArgumentException("Invalid tracking generation");
        var identity=frame.identity();var endpoint=frame.endpoint();var sample=endpoint.sample();var root=endpoint.root();
        return new AnatomyPosePayload(identity.epoch(),identity.revision(),identity.dimension().identifier(),identity.entityId(),identity.support(),
            identity.model(),identity.poseProvider(),identity.rootProvider(),endpoint.frameSerial(),endpoint.authorityTick(),endpoint.jointSampleTick(),root.sequence(),root.tick(),
            identity.bindingGeneration(),trackingGeneration,endpoint.availability()==GeometryProvider.Availability.AVAILABLE,sample.inputs(),sample.origin(),sample.yaw(),sample.scale(),sample.gravity().down(),
            endpoint.rootTransform());
    }
    /** Typed fixture factory; production must pass {@link GeometryProvider.PublishedFrame}. */
    public static AnatomyPosePayload posePayload(UUID epoch,long revision,net.minecraft.resources.Identifier dimension,int entityId,UUID entity,
            net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,long bindingGeneration,GeometryProvider.CausalEndpoint endpoint) {
        return posePayload(epoch,revision,dimension,entityId,entity,model,provider,GeometryProvider.DEFAULT_ROOT_PROVIDER,bindingGeneration,1,endpoint);
    }
    /** S24 fixture seam with explicit recipient tracking generation. */
    public static AnatomyPosePayload posePayload(UUID epoch,long revision,net.minecraft.resources.Identifier dimension,int entityId,UUID entity,
            net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,long bindingGeneration,long trackingGeneration,
            GeometryProvider.CausalEndpoint endpoint) {
        return posePayload(epoch,revision,dimension,entityId,entity,model,provider,GeometryProvider.DEFAULT_ROOT_PROVIDER,bindingGeneration,trackingGeneration,endpoint);
    }
    /** S21 fixture seam with explicit root-provider identity and legacy tracking generation 1. */
    public static AnatomyPosePayload posePayload(UUID epoch,long revision,net.minecraft.resources.Identifier dimension,int entityId,UUID entity,
            net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,net.minecraft.resources.Identifier rootProvider,
            long bindingGeneration,GeometryProvider.CausalEndpoint endpoint) {
        return posePayload(epoch,revision,dimension,entityId,entity,model,provider,rootProvider,bindingGeneration,1,endpoint);
    }
    /** S24 fixture seam with complete binding/root/tracking identity. */
    public static AnatomyPosePayload posePayload(UUID epoch,long revision,net.minecraft.resources.Identifier dimension,int entityId,UUID entity,
            net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,net.minecraft.resources.Identifier rootProvider,
            long bindingGeneration,long trackingGeneration,GeometryProvider.CausalEndpoint endpoint) {
        if(endpoint==null || bindingGeneration<1 || trackingGeneration<1 || rootProvider==null)throw new IllegalArgumentException("Missing causal binding/tracking frame");
        var sample=endpoint.sample();var root=endpoint.root();
        return new AnatomyPosePayload(epoch,revision,dimension,entityId,entity,model,provider,rootProvider,endpoint.frameSerial(),endpoint.authorityTick(),endpoint.jointSampleTick(),
            root.sequence(),root.tick(),bindingGeneration,trackingGeneration,endpoint.availability()==GeometryProvider.Availability.AVAILABLE,sample.inputs(),sample.origin(),sample.yaw(),sample.scale(),sample.gravity().down(),
            endpoint.rootTransform());
    }
    /** Resolves the recipient-specific tracking generation before sending one live published endpoint. */
    public static void sendPose(ServerPlayer recipient,GeometryProvider.PublishedFrame frame) {
        if(recipient==null || frame==null)return;
        var identity=frame.identity();var entity=recipient.level().getEntity(identity.entityId());
        if(entity==null || !entity.getUUID().equals(identity.support()))return;
        sendPose(recipient,frame,AnatomyRuntime.trackingGeneration(recipient,entity));
    }
    /** Sends exactly one immutable server published endpoint in the recipient's current tracking generation. */
    public static void sendPose(ServerPlayer recipient,GeometryProvider.PublishedFrame frame,long trackingGeneration) {
        if(!ServerPlayNetworking.canSend(recipient,AnatomyPosePayload.TYPE))throw new IllegalStateException("Client lacks anatomy pose/root protocol v6");
        ServerPlayNetworking.send(recipient,posePayload(frame,trackingGeneration));
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
