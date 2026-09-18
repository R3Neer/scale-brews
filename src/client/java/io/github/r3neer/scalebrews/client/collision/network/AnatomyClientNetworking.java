package io.github.r3neer.scalebrews.client.collision.network;

import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.pose.*;
import io.github.r3neer.scalebrews.collision.physics.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Catalog is supplied exclusively by the connected server, independent of render/resource packs. */
public final class AnatomyClientNetworking {
    private static final AnatomyClientSession session=new AnatomyClientSession();
    private static final java.util.Map<java.util.UUID,AnatomyPoseHistory> poses=new java.util.HashMap<>();
    /** Root/joint endpoint order is independent of joint-history interpolation. */
    private static final java.util.Map<java.util.UUID,AnatomyFrameHistory> frames=new java.util.HashMap<>();
    /** Current unavailable histories plus UUIDs represented by compact retired replay fences. */
    private static final java.util.Set<java.util.UUID> staleFrames=new java.util.HashSet<>();
    /** Bounded replay watermarks survive frame-history eviction without retaining full histories. */
    private static final TrackingReplayFence frameReplayFence=new TrackingReplayFence();
    private static final java.util.Map<java.util.UUID,Long> receivedAt=new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID,ModelGeometryProvider> evaluators=new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID,ClientGeometryProvider> providers=new java.util.HashMap<>();
    /** One original-model frame per accepted full endpoint, never per observer/render call. */
    private static final java.util.Map<java.util.UUID,CachedPresentationFrame> presentationFrames=new java.util.HashMap<>();
    private static final AnatomyContactInbox contacts=new AnatomyContactInbox();
    private static final java.util.Map<java.util.UUID,AnatomyContactPayload> presentationContacts=new java.util.HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel poseLevel;
    private static long clientTick;
    private record SentMovementReference(long tick,long transportSequence,long rootFrameSequence) {}
    private static final java.util.Map<java.util.UUID,SentMovementReference> sentMovementReferences=new java.util.HashMap<>();
    private AnatomyClientNetworking() {}
    public static AnatomyCatalogTransfer catalog(){return session.catalog();}
    public static AnatomyPoseHistory pose(java.util.UUID entity){return poses.get(entity);}
    private static WorldAnatomyCatalog.Binding binding(net.minecraft.world.entity.Entity entity) {
        if(entity==null)return null;
        return session.catalog().snapshot().bindings().get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }
    /** Wire identity is the complete canonical selection identity, including root authority. */
    private static boolean bindingMatches(net.minecraft.world.entity.LivingEntity entity,AnatomyPosePayload packet) {
        var binding=binding(entity);if(binding==null)return false;
        var selection=binding.selection();
        return selection.geometry().model().equals(packet.model()) && selection.pose().engine().equals(packet.provider())
            && selection.rootTransform().equals(packet.rootProvider());
    }
    /** Client-only, server-confirmed material data for residual camera presentation. */
    public record PresentationContact(net.minecraft.world.entity.Entity body,net.minecraft.world.entity.LivingEntity support,
            SurfaceContact surface,GeometryProvider.Snapshot geometry,long serverTick) {}
    /** Server identity attached to a drawable original-model frame; no local adapter generation is protocol state. */
    public record PresentationIdentity(java.util.UUID epoch,net.minecraft.resources.Identifier dimension,java.util.UUID support,int entityId,
            long revision,net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,
            net.minecraft.resources.Identifier rootProvider,long bindingGeneration) {
        public PresentationIdentity {
            if(epoch==null || dimension==null || support==null || entityId<0 || revision<0 || model==null || provider==null || rootProvider==null || bindingGeneration<1)
                throw new IllegalArgumentException("Invalid presentation identity");
        }
    }
    /** Reserved for Q2's explicit VerifiedInterval; Q1 creates only CURRENT_ENDPOINT. */
    public enum PresentationKind { CURRENT_ENDPOINT, CERTIFIED_INTERVAL }
    public static final class PresentationFrame {
        private final PresentationIdentity identity;
        private final GeometryProvider.CausalEndpoint before,after;
        private final double authorityTime,fraction;
        private final PresentationKind kind;
        private final HierarchyMotion.EvaluatedFrame evaluated;
        private PresentationFrame(PresentationIdentity identity,GeometryProvider.CausalEndpoint before,GeometryProvider.CausalEndpoint after,
                double authorityTime,double fraction,PresentationKind kind,HierarchyMotion.EvaluatedFrame evaluated) {
            if(identity==null || before==null || after==null || !Double.isFinite(authorityTime) || !Double.isFinite(fraction)
                    || fraction<0 || fraction>1 || kind==null || evaluated==null)throw new IllegalArgumentException("Invalid presentation frame");
            if(kind!=PresentationKind.CURRENT_ENDPOINT || before!=after || fraction!=1 || authorityTime!=before.authorityTick())
                throw new IllegalArgumentException("Q1 exposes only the exact current causal endpoint");
            this.identity=identity;this.before=before;this.after=after;this.authorityTime=authorityTime;this.fraction=fraction;this.kind=kind;this.evaluated=evaluated;
        }
        private static PresentationFrame current(PresentationIdentity identity,GeometryProvider.CausalEndpoint endpoint,HierarchyMotion.EvaluatedFrame evaluated) {
            return new PresentationFrame(identity,endpoint,endpoint,endpoint.authorityTick(),1,PresentationKind.CURRENT_ENDPOINT,evaluated);
        }
        public PresentationIdentity identity(){return identity;}
        public GeometryProvider.CausalEndpoint before(){return before;}
        public GeometryProvider.CausalEndpoint after(){return after;}
        public double authorityTime(){return authorityTime;}
        public double fraction(){return fraction;}
        public PresentationKind kind(){return kind;}
        public HierarchyMotion.EvaluatedFrame evaluated(){return evaluated;}
    }
    private record PresentationCacheKey(PresentationIdentity identity,long frameSerial) {}
    private record CachedPresentationFrame(PresentationCacheKey key,PresentationFrame frame) {}
    public static AnatomyMode mode(net.minecraft.world.entity.Entity entity) {
        return entity==null?AnatomyMode.DISABLED:session.mode(entity.level());
    }
    public static boolean ready(net.minecraft.world.entity.Entity entity) {
        return mode(entity)==AnatomyMode.READY;
    }
    /** Level-owned material/caches only. Connection/revision replay watermarks deliberately survive. */
    private static void clearLevelMaterial(){
        if(poseLevel!=null)AnatomyMovement.deactivate(poseLevel);
        poses.clear();frames.clear();staleFrames.clear();receivedAt.clear();evaluators.clear();providers.clear();presentationFrames.clear();contacts.clearPending();presentationContacts.clear();
        sentMovementReferences.clear();
    }
    /** Disconnect/host replacement or accepted catalog revision owns a new causal session. */
    private static void clearConnectionTemporal(){
        clearLevelMaterial();frameReplayFence.clear();contacts.clear();
    }
    private static void discardSupportMaterial(net.minecraft.client.multiplayer.ClientLevel level,java.util.UUID supportId,int entityId) {
        poses.remove(supportId);evaluators.remove(supportId);providers.remove(supportId);presentationFrames.remove(supportId);
        var support=level==null?null:level.getEntity(entityId);
        if(support instanceof net.minecraft.world.entity.LivingEntity living && living.getUUID().equals(supportId))
            AnatomyMovement.invalidateSupport(living);
        contacts.discardPendingSupport(supportId);
        presentationContacts.entrySet().removeIf(entry->supportId.equals(entry.getValue().support()));
    }
    /** Compresses one dead full history into a bounded ordering fence before releasing its material state. */
    private static void retireFrameHistory(net.minecraft.client.multiplayer.ClientLevel level,java.util.UUID supportId,AnatomyFrameHistory history) {
        if(history==null)return;var packet=history.current();if(packet==null)return;
        history.retireCurrentTrackingGeneration();
        boolean wasSaturated=frameReplayFence.saturated();boolean fenced=frameReplayFence.retire(packet);
        if(fenced)staleFrames.add(supportId);else staleFrames.remove(supportId);
        discardSupportMaterial(level,supportId,packet.entityId());receivedAt.remove(supportId);
        if(!wasSaturated && frameReplayFence.saturated())
            io.github.r3neer.scalebrews.ScaleBrews.LOGGER.warn("Client anatomy frame replay fence saturated; rejecting poses until lifecycle reset");
    }
    /** Same-connection level barrier: retire histories, then drop all level-owned material but keep causal watermarks. */
    private static void crossLevelBarrier(){
        if(poseLevel!=null)for(var entry:new java.util.ArrayList<>(frames.entrySet()))
            retireFrameHistory(poseLevel,entry.getKey(),entry.getValue());
        clearLevelMaterial();
    }
    private static void bindPhysics() {
        var transfer=session.catalog();
        if(poseLevel==null || !transfer.ready() || transfer.snapshot().bindings().isEmpty()) {
            if(poseLevel!=null && transfer.binding())AnatomyMovement.deactivate(poseLevel);
            return;
        }
        AnatomyMovement.activate(poseLevel);
        for(var history:frames.values()) {
            var packet=history.current();if(packet==null)continue;var entity=poseLevel.getEntity(packet.entityId());
            if(!(entity instanceof net.minecraft.world.entity.LivingEntity living) || !entity.getUUID().equals(packet.entity()))continue;
            if(!packet.available() || staleFrames.contains(packet.entity()) || !bindingMatches(living,packet))continue;
            if(providers.containsKey(packet.entity()))continue;
            var provider=new ClientGeometryProvider(packet.entity());providers.put(packet.entity(),provider);
            AnatomyMovement.register(living,provider,new GeometryProvider.GeometryIdentityDescriptor(packet.epoch(),packet.revision(),
                packet.model(),packet.provider(),packet.rootProvider(),packet.bindingGeneration()),binding(living).selection());
        }
        for(var packet:contacts.pending().values()) {
            var body=poseLevel.getEntity(packet.bodyId());
            if(body==null)continue;
            if(!body.getUUID().equals(packet.body())) {presentationContacts.remove(packet.body());contacts.consume(packet);continue;}
            if(!packet.present()) {AnatomyMovement.clear(body);presentationContacts.remove(packet.body());contacts.consume(packet);continue;}
            var support=poseLevel.getEntity(packet.supportId());
            if(!(support instanceof net.minecraft.world.entity.LivingEntity living) || !support.getUUID().equals(packet.support()))continue;
            var supportHistory=frames.get(packet.support());var supportPacket=supportHistory==null?null:supportHistory.current();
            if(supportPacket==null)continue;
            if(supportPacket.bindingGeneration()!=packet.supportBindingGeneration()) {
                presentationContacts.remove(packet.body());contacts.consume(packet);continue;
            }
            var surface=new SurfaceContact(packet.support(),packet.revision(),packet.piece(),packet.face(),packet.localPoint(),packet.normal(),packet.tick());
            if(AnatomyMovement.confirm(body,living,surface)) {presentationContacts.put(packet.body(),packet);contacts.consume(packet);}
        }
    }
    public static java.util.Optional<GeometryProvider.Snapshot> geometry(net.minecraft.world.entity.LivingEntity entity,double serverTick) {
        var history=frames.get(entity.getUUID());var packet=history==null?null:history.current();
        if(packet==null || staleFrames.contains(entity.getUUID()) || entity.level()!=poseLevel || !packet.available())return java.util.Optional.empty();
        return currentSnapshot(entity,entity.getUUID());
    }
    private static java.util.Optional<GeometryProvider.Snapshot> currentSnapshot(net.minecraft.world.entity.LivingEntity entity,java.util.UUID support) {
        var frame=frames.get(support);var packet=frame==null?null:frame.current();
        if(packet==null || staleFrames.contains(support) || !packet.available() || !support.equals(entity.getUUID()) || packet.entityId()!=entity.getId() || entity.level()!=poseLevel)return java.util.Optional.empty();
        var endpoint=frame.endpoint().orElse(null);
        if(endpoint==null || endpoint.availability()!=GeometryProvider.Availability.AVAILABLE)return java.util.Optional.empty();
        return evaluator(entity,packet).flatMap(evaluator->evaluator.sampleAt(entity,endpoint.sample(),endpoint.rootTransform()));
    }
    private static java.util.Optional<GeometryProvider.CausalEndpoint> currentEndpoint(net.minecraft.world.entity.LivingEntity entity,java.util.UUID support) {
        if(staleFrames.contains(support))return java.util.Optional.empty();
        var frame=frames.get(support);
        return frame==null?java.util.Optional.empty():frame.endpoint();
    }
    private static java.util.Optional<ModelGeometryProvider> evaluator(net.minecraft.world.entity.LivingEntity entity,AnatomyPosePayload packet) {
        var transfer=session.catalog();var binding=binding(entity);
        if(!transfer.ready() || binding==null || !bindingMatches(entity,packet))return java.util.Optional.empty();
        var evaluator=evaluators.get(entity.getUUID());
        if(evaluator==null) {
            evaluator=new ModelGeometryProvider(binding.model(),binding.poses(),binding.selection().geometry().filter(),packet.revision());
            evaluators.put(entity.getUUID(),evaluator);
        }
        return java.util.Optional.of(evaluator);
    }
    private static final class ClientGeometryProvider implements GeometryProvider {
        private final java.util.UUID support;
        ClientGeometryProvider(java.util.UUID support){this.support=support;}
        public java.util.Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity entity){return currentSnapshot(entity,support);}
        public java.util.Optional<CausalEndpoint> causalEndpoint(net.minecraft.world.entity.LivingEntity entity){return currentEndpoint(entity,support);}
    }
    public static java.util.Optional<PresentationFrame> presentationFrame(net.minecraft.world.entity.LivingEntity support) {
        if(support==null || support.level()!=poseLevel || !ready(support) || staleFrames.contains(support.getUUID()))return java.util.Optional.empty();
        var history=frames.get(support.getUUID());var packet=history==null?null:history.current();
        if(packet==null || !packet.available() || !presentationIdentityMatches(support,packet))return java.util.Optional.empty();
        var endpoint=history.endpoint().orElse(null);
        if(endpoint==null || endpoint.availability()!=GeometryProvider.Availability.AVAILABLE)return java.util.Optional.empty();
        var identity=new PresentationIdentity(packet.epoch(),packet.dimension(),packet.entity(),packet.entityId(),packet.revision(),packet.model(),packet.provider(),packet.rootProvider(),packet.bindingGeneration());
        var key=new PresentationCacheKey(identity,endpoint.frameSerial());
        var cached=presentationFrames.get(support.getUUID());
        if(cached!=null && cached.key().equals(key))return java.util.Optional.of(cached.frame());
        return evaluator(support,packet).flatMap(evaluator->evaluator.evaluatePresentation(support,endpoint)).map(evaluated->{
            var frame=PresentationFrame.current(identity,endpoint,evaluated);
            presentationFrames.put(support.getUUID(),new CachedPresentationFrame(key,frame));
            return frame;
        });
    }
    private static boolean presentationIdentityMatches(net.minecraft.world.entity.LivingEntity support,AnatomyPosePayload packet) {
        var transfer=session.catalog();
        if(!transfer.ready() || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision()
                || !packet.dimension().equals(support.level().dimension().identifier()) || packet.entityId()!=support.getId()
                || !packet.entity().equals(support.getUUID()) || !transfer.snapshot().models().containsKey(packet.model().toString()))return false;
        return bindingMatches(support,packet);
    }
    public static java.util.Optional<PresentationContact> presentationContact(net.minecraft.world.entity.Entity body,double serverTick) {
        if(!ready(body))return java.util.Optional.empty();
        var packet=presentationContacts.get(body.getUUID());
        if(packet==null || !packet.present() || packet.bodyId()!=body.getId() || !packet.body().equals(body.getUUID()))return java.util.Optional.empty();
        var supportEntity=poseLevel.getEntity(packet.supportId());
        if(!(supportEntity instanceof net.minecraft.world.entity.LivingEntity support) || !support.getUUID().equals(packet.support()))return java.util.Optional.empty();
        var surface=new SurfaceContact(packet.support(),packet.revision(),packet.piece(),packet.face(),packet.localPoint(),packet.normal(),packet.tick());
        return presentationFrame(support).filter(frame->frame.identity().revision()==surface.revision()
                && frame.identity().bindingGeneration()==packet.supportBindingGeneration() && frame.evaluated().pieces().containsKey(surface.piece()))
            .map(frame->new PresentationContact(body,support,surface,new GeometryProvider.Snapshot(frame.identity().revision(),frame.evaluated().pieces()),(long)frame.authorityTime()));
    }
    /**
     * Emits at most one metadata cursor for each locally applied transport contribution.
     * The server derives and authorizes the body; no entity id or physical geometry is uploaded.
     */
    public static void sendMovementReference(net.minecraft.world.entity.Entity body) {
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(body==null || player==null || !ready(body) || !body.isLocalInstanceAuthoritative()
                || !ClientPlayNetworking.canSend(AnatomyMoveReferencePayload.TYPE))return;
        boolean vehicle=body!=player;
        if(!vehicle && player.getRootVehicle()!=player)return;
        if(vehicle && (player.getRootVehicle()!=body || body.getControllingPassenger()!=player))return;
        var transport=AnatomyMovement.transport(body);if(transport==null)return;
        var cursor=new SentMovementReference(transport.tick(),transport.sequence(),transport.rootFrameSequence());
        if(cursor.equals(sentMovementReferences.get(body.getUUID())))return;
        ClientPlayNetworking.send(new AnatomyMoveReferencePayload(vehicle,cursor.tick(),cursor.transportSequence(),cursor.rootFrameSequence()));
        sentMovementReferences.put(body.getUUID(),cursor);
    }

    private static void reset(){session.resetConnection();clearConnectionTemporal();poseLevel=null;clientTick=0;}
    private static void useLevel(net.minecraft.client.multiplayer.ClientLevel level){
        if(poseLevel!=level){crossLevelBarrier();poseLevel=level;session.useLevel(level);}
    }
    public static void initialize() {
        AnatomySession.installClientMode(AnatomyClientNetworking::mode);
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
            if(entity instanceof net.minecraft.world.entity.LivingEntity living) {
                var history=frames.remove(living.getUUID());
                if(history!=null)retireFrameHistory(level,living.getUUID(),history);
            }
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            useLevel(client.level);clientTick++;
            if(poseLevel!=null)contacts.prune(poseLevel.getGameTime(),100);
            frames.entrySet().removeIf(entry->{
                var history=entry.getValue();var packet=history.current();var entity=poseLevel==null?null:poseLevel.getEntity(packet.entityId());
                boolean expired=clientTick-receivedAt.getOrDefault(entry.getKey(),0L)>100;
                boolean wrongIdentity=entity!=null && !entity.getUUID().equals(entry.getKey());
                if(expired || wrongIdentity){retireFrameHistory(poseLevel,entry.getKey(),history);return true;}
                return false;
            });
            presentationContacts.entrySet().removeIf(entry->{
                var packet=entry.getValue();var body=poseLevel==null?null:poseLevel.getEntity(packet.bodyId());var support=poseLevel==null?null:poseLevel.getEntity(packet.supportId());
                return poseLevel==null || packet.tick()+100<poseLevel.getGameTime() || body==null || !body.getUUID().equals(packet.body())
                    || !(support instanceof net.minecraft.world.entity.LivingEntity) || !support.getUUID().equals(packet.support());
            });
            bindPhysics();
            if(poseLevel!=null) {
                AnatomyMovement.tickGeometry(poseLevel);
                for(var entity:poseLevel.entitiesForRendering())if(entity.isLocalInstanceAuthoritative() && AnatomyMovement.contact(entity)!=null)AnatomyMovement.carry(entity);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyCatalogPayload.TYPE,(packet,context)->{
            useLevel(context.client().level);var transfer=session.catalog();
            try{if(transfer.accept(packet))clearConnectionTemporal();}
            catch(RuntimeException invalid){transfer.rejectPending();io.github.r3neer.scalebrews.ScaleBrews.LOGGER.error("Rejected anatomical catalog; previous revision retained",invalid);}
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyPosePayload.TYPE,(packet,context)->{
            var level=context.client().level;useLevel(level);var transfer=session.catalog();
            if(level==null || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision() || !packet.dimension().equals(level.dimension().identifier())
                || !transfer.snapshot().models().containsKey(packet.model().toString()) || frameReplayFence.rejects(packet))return;
            var entity=level.getEntity(packet.entityId());
            if(entity!=null) {
                if(!entity.getUUID().equals(packet.entity()) || !(entity instanceof net.minecraft.world.entity.LivingEntity living) || !bindingMatches(living,packet))return;
            }
            if(frames.size()>=4096 && !frames.containsKey(packet.entity()))return;
            var framesForSupport=frames.computeIfAbsent(packet.entity(),id->new AnatomyFrameHistory());
            try {
                var lifecycle=framesForSupport.transition(packet);
                if(lifecycle==AnatomyFrameHistory.LifecycleTransition.REJECT)return;
                if(lifecycle==AnatomyFrameHistory.LifecycleTransition.RESTART) {
                    var previous=framesForSupport.current();
                    discardSupportMaterial(level,packet.entity(),previous.entityId());
                    framesForSupport=new AnatomyFrameHistory();frames.put(packet.entity(),framesForSupport);
                }
                if(!framesForSupport.accept(packet))return;
                frameReplayFence.accepted(packet);receivedAt.put(packet.entity(),clientTick);
                if(!packet.available()) {staleFrames.add(packet.entity());discardSupportMaterial(level,packet.entity(),packet.entityId());return;}
                staleFrames.remove(packet.entity());
                var history=poses.computeIfAbsent(packet.entity(),id->new AnatomyPoseHistory());
                if(history.current()==null || packet.jointSampleTick()>history.current().jointSampleTick())history.accept(packet);
            } catch(IllegalArgumentException invalid) {
                io.github.r3neer.scalebrews.ScaleBrews.LOGGER.warn("Rejected causal pose frame for {}",packet.entity(),invalid);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyContactPayload.TYPE,(packet,context)->{
            var level=context.client().level;useLevel(level);var transfer=session.catalog();
            if(level==null || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision() || !packet.dimension().equals(level.dimension().identifier())
                    || packet.tick()+100<level.getGameTime())return;
            if(contacts.accept(level.dimension().identifier(),packet))presentationContacts.remove(packet.body());
        });
    }
}
