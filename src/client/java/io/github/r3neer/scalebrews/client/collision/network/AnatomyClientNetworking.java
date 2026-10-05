package io.github.r3neer.scalebrews.client.collision.network;

import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.pose.*;
import io.github.r3neer.scalebrews.collision.physics.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalBodyResidual;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Catalog is supplied exclusively by the connected server, independent of render/resource packs. */
public final class AnatomyClientNetworking {
    private static final AnatomyClientSession session=new AnatomyClientSession();
    /** Client-thread-owned listener identity; retired callbacks cannot reset a newer session. */
    private static net.minecraft.client.multiplayer.ClientPacketListener connectionOwner;
    private static final java.util.Map<java.util.UUID,AnatomyPoseHistory> poses=new java.util.HashMap<>();
    /** Root/joint endpoint order is independent of joint-history interpolation. */
    private static final java.util.Map<java.util.UUID,AnatomyFrameHistory> frames=new java.util.HashMap<>();
    /** Current unavailable histories plus UUIDs represented by compact retired replay fences. */
    private static final java.util.Set<java.util.UUID> staleFrames=new java.util.HashSet<>();
    /** Bounded replay watermarks survive frame-history eviction without retaining full histories. */
    private static final TrackingReplayFence frameReplayFence=new TrackingReplayFence();
    /** Tombstones belong to server catalog identity, which can survive a socket replacement. */
    private record ReplayCatalog(java.util.UUID epoch,long revision) {}
    private static ReplayCatalog replayCatalog;
    private static final java.util.Map<java.util.UUID,Long> receivedAt=new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID,ModelGeometryProvider> evaluators=new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID,ClientGeometryProvider> providers=new java.util.HashMap<>();
    /** One bounded cached presentation result per support. */
    private static final java.util.Map<java.util.UUID,CachedPresentationFrame> presentationFrames=new java.util.HashMap<>();
    /** Separate slot prevents current endpoint queries from evicting the same interval/fraction. */
    private static final java.util.Map<java.util.UUID,CachedPresentationFrame> presentationIntervals=new java.util.HashMap<>();
    /** Server-issued Q2 interval certificates; exact serial lookup only. */
    private static final AnatomyCertifiedIntervalInbox certifiedIntervals=new AnatomyCertifiedIntervalInbox();
    private static final AnatomyContactInbox contacts=new AnatomyContactInbox();
    // Disconnect/entity-unload can race an arriving contact callback. The whole
    // removeIf must hold the same lock as put/remove/clear, not iterate a plain HashMap.
    private static final java.util.Map<java.util.UUID,AnatomyContactPayload> presentationContacts=
        java.util.Collections.synchronizedMap(new java.util.HashMap<>());
    private static net.minecraft.client.multiplayer.ClientLevel poseLevel;
    private static long clientTick;
    /** Exact server-issued receipt token staged only after a local carry has incorporated its support endpoint. */
    private record PredictedMovementReference(long receiptSequence,long localTransportSequence) {}
    private static final java.util.Map<java.util.UUID,PredictedMovementReference> predictedMovementReferences=new java.util.HashMap<>();
    /** TTL/cap owner for unincorporated server-issued receipt tokens. */
    private static final AnatomyReceiptTokenInbox transportReceiptTokens=new AnatomyReceiptTokenInbox();
    /** Exactly one client body owns full physical prediction at a time. */
    private static java.util.UUID predictionOwner;
    private AnatomyClientNetworking() {}
    public static AnatomyCatalogTransfer catalog(){return session.catalog();}
    public static AnatomyPoseHistory pose(java.util.UUID entity){return poses.get(entity);}
    private static WorldAnatomyCatalog.Binding binding(net.minecraft.world.entity.Entity entity,AnatomyPosePayload packet) {
        if(entity==null)return null;
        return session.catalog().snapshot().resolvePose(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),packet).orElse(null);
    }
    /** Wire identity is the complete canonical selection identity, including root authority. */
    private static boolean bindingMatches(net.minecraft.world.entity.LivingEntity entity,AnatomyPosePayload packet) {
        return binding(entity,packet)!=null;
    }
    /** Client-only, server-confirmed material data for residual camera presentation. */
    public record PresentationContact(net.minecraft.world.entity.Entity body,net.minecraft.world.entity.LivingEntity support,
            SurfaceContact surface,GeometryProvider.Snapshot geometry,long serverTick,long bodyTrackingGeneration) {}
    /** Server identity attached to a drawable original-model frame; no local adapter generation is protocol state. */
    public record PresentationIdentity(java.util.UUID epoch,net.minecraft.resources.Identifier dimension,java.util.UUID support,int entityId,
            long revision,net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,
            net.minecraft.resources.Identifier rootProvider,long bindingGeneration,net.minecraft.resources.Identifier bindingIdentity) {
        /** Local fixture compatibility; live presentation always supplies the accepted token. */
        public PresentationIdentity(java.util.UUID epoch,net.minecraft.resources.Identifier dimension,java.util.UUID support,int entityId,
                long revision,net.minecraft.resources.Identifier model,net.minecraft.resources.Identifier provider,
                net.minecraft.resources.Identifier rootProvider,long bindingGeneration) {
            this(epoch,dimension,support,entityId,revision,model,provider,rootProvider,bindingGeneration,null);
        }
        public PresentationIdentity {
            if(epoch==null || dimension==null || support==null || entityId<0 || revision<0 || model==null || provider==null || rootProvider==null || bindingGeneration<1
                    || bindingIdentity!=null && !io.github.r3neer.scalebrews.collision.internal.CollisionBindingIdentity.validToken(bindingIdentity))
                throw new IllegalArgumentException("Invalid presentation identity");
        }
    }
    /** Reserved for Q2's explicit VerifiedInterval; Q1 creates only CURRENT_ENDPOINT. */
    public enum PresentationKind { CURRENT_ENDPOINT, CERTIFIED_INTERVAL }
    public static final class PresentationFrame {
        private final PresentationIdentity identity;
        private final long trackingGeneration;
        private final GeometryProvider.CausalEndpoint before,after;
        private final double authorityTime,fraction;
        private final PresentationKind kind;
        private final HierarchyMotion.EvaluatedFrame evaluated;
        private PresentationFrame(PresentationIdentity identity,long trackingGeneration,GeometryProvider.CausalEndpoint before,GeometryProvider.CausalEndpoint after,
                double authorityTime,double fraction,PresentationKind kind,HierarchyMotion.EvaluatedFrame evaluated) {
            if(identity==null || trackingGeneration<1 || before==null || after==null || !Double.isFinite(authorityTime) || !Double.isFinite(fraction)
                    || fraction<0 || fraction>1 || kind==null || evaluated==null)throw new IllegalArgumentException("Invalid presentation frame");
            if(kind==PresentationKind.CURRENT_ENDPOINT) {
                if(before!=after || fraction!=1 || authorityTime!=before.authorityTick())
                    throw new IllegalArgumentException("CURRENT_ENDPOINT must name one exact endpoint");
            } else {
                double expected=before.authorityTick()+fraction*(after.authorityTick()-before.authorityTick());
                if(before==after || after.frameSerial()<=before.frameSerial()
                        || before.availability()!=GeometryProvider.Availability.AVAILABLE
                        || after.availability()!=GeometryProvider.Availability.AVAILABLE
                        || !before.root().gravity().equals(after.root().gravity())
                        || Double.compare(authorityTime,expected)!=0)
                    throw new IllegalArgumentException("Invalid certified presentation interval");
            }
            this.identity=identity;this.trackingGeneration=trackingGeneration;this.before=before;this.after=after;this.authorityTime=authorityTime;this.fraction=fraction;this.kind=kind;this.evaluated=evaluated;
        }
        private static PresentationFrame current(PresentationIdentity identity,long trackingGeneration,GeometryProvider.CausalEndpoint endpoint,HierarchyMotion.EvaluatedFrame evaluated) {
            return new PresentationFrame(identity,trackingGeneration,endpoint,endpoint,endpoint.authorityTick(),1,PresentationKind.CURRENT_ENDPOINT,evaluated);
        }
        private static PresentationFrame interval(PresentationIdentity identity,long trackingGeneration,GeometryProvider.CausalEndpoint before,
                GeometryProvider.CausalEndpoint after,double fraction,HierarchyMotion.EvaluatedFrame evaluated) {
            double authorityTime=before.authorityTick()+fraction*(after.authorityTick()-before.authorityTick());
            return new PresentationFrame(identity,trackingGeneration,before,after,authorityTime,fraction,PresentationKind.CERTIFIED_INTERVAL,evaluated);
        }
        public PresentationIdentity identity(){return identity;}
        public long trackingGeneration(){return trackingGeneration;}
        public GeometryProvider.CausalEndpoint before(){return before;}
        public GeometryProvider.CausalEndpoint after(){return after;}
        public double authorityTime(){return authorityTime;}
        public double fraction(){return fraction;}
        public PresentationKind kind(){return kind;}
        public HierarchyMotion.EvaluatedFrame evaluated(){return evaluated;}
    }
    private record PresentationCacheKey(PresentationIdentity identity,long trackingGeneration,PresentationKind kind,long materialSerial,
            long beforeFrameSerial,long afterFrameSerial,long fractionBits) {}
    private record CachedPresentationFrame(PresentationCacheKey key,PresentationFrame frame) {}
    public static AnatomyMode mode(net.minecraft.world.entity.Entity entity) {
        return entity==null?AnatomyMode.DISABLED:session.mode(entity.level());
    }
    public static boolean ready(net.minecraft.world.entity.Entity entity) {
        return mode(entity)==AnatomyMode.READY;
    }
    /** Level-owned material/caches only. Connection/revision replay watermarks deliberately survive. */
    private static void clearLevelMaterial(){
        CausalBodyResidual.reset();
        CausalRemoteResidual.reset();
        if(poseLevel!=null)AnatomyMovement.deactivate(poseLevel);
        poses.clear();frames.clear();staleFrames.clear();receivedAt.clear();evaluators.clear();providers.clear();presentationFrames.clear();presentationIntervals.clear();certifiedIntervals.clear();contacts.retireWindows();presentationContacts.clear();
        predictedMovementReferences.clear();transportReceiptTokens.clear();predictionOwner=null;
    }
    /** A newly accepted server catalog decides whether retired windows still belong to this authority. */
    private static void acceptedCatalog(){
        var transfer=session.catalog();var next=new ReplayCatalog(transfer.epoch(),transfer.revision());
        clearLevelMaterial();
        if(!next.equals(replayCatalog)) {
            contacts.clear();
            frameReplayFence.clear();
        }
        replayCatalog=next;
    }
    private static void discardSupportMaterial(net.minecraft.client.multiplayer.ClientLevel level,java.util.UUID supportId,int entityId) {
        discardSupportMaterial(level,supportId,entityId,true);
    }
    private static void discardSupportMaterial(net.minecraft.client.multiplayer.ClientLevel level,java.util.UUID supportId,int entityId,boolean discardCertificates) {
        CausalBodyResidual.retireSupport(supportId);
        CausalRemoteResidual.retireSupport(supportId);
        poses.remove(supportId);evaluators.remove(supportId);providers.remove(supportId);presentationFrames.remove(supportId);presentationIntervals.remove(supportId);
        if(discardCertificates)certifiedIntervals.discardSupport(supportId);
        var support=level==null?null:level.getEntity(entityId);
        if(support instanceof net.minecraft.world.entity.LivingEntity living && living.getUUID().equals(supportId))
            AnatomyMovement.invalidateSupport(living);
        contacts.discardPendingSupport(supportId);
        presentationContacts.entrySet().removeIf(entry->supportId.equals(entry.getValue().support()));
        transportReceiptTokens.discardSupport(supportId);
    }
    /** Compresses one dead full history into a bounded ordering fence before releasing its material state. */
    private static void retireFrameHistory(net.minecraft.client.multiplayer.ClientLevel level,java.util.UUID supportId,AnatomyFrameHistory history) {
        retireFrameHistory(level,supportId,history,true);
    }
    private static void retireFrameHistory(net.minecraft.client.multiplayer.ClientLevel level,java.util.UUID supportId,AnatomyFrameHistory history,boolean closeWindow) {
        if(history==null)return;var packet=history.current();if(packet==null)return;
        history.retireCurrentTrackingGeneration();
        boolean wasSaturated=frameReplayFence.saturated();
        boolean fenced=closeWindow?frameReplayFence.retireWindow(packet):frameReplayFence.retire(packet);
        if(fenced)staleFrames.add(supportId);else staleFrames.remove(supportId);
        discardSupportMaterial(level,supportId,packet.entityId());receivedAt.remove(supportId);
        if(!wasSaturated && frameReplayFence.saturated())
            io.github.r3neer.scalebrews.ScaleBrews.LOGGER.warn("Client anatomy frame replay fence saturated; rejecting poses until lifecycle reset");
    }
    /** Same-connection level barrier: retire histories, then drop all level-owned material but keep causal watermarks. */
    private static void crossLevelBarrier(){
        if(poseLevel!=null)for(var entry:new java.util.ArrayList<>(frames.entrySet()))
            retireFrameHistory(poseLevel,entry.getKey(),entry.getValue());
        frameReplayFence.closeWindows();
        clearLevelMaterial();
    }
    private static void bindPhysics() {
        var transfer=session.catalog();
        if(poseLevel==null || !transfer.ready() || transfer.snapshot().prepared().size()==0) {
            if(poseLevel!=null && transfer.binding()) {
                AnatomyMovement.deactivate(poseLevel);
                // The core lost these registrations. A rejected candidate must be able to
                // bind the still-accepted endpoints again when its old revision returns READY.
                providers.clear();
            }
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
                packet.model(),packet.provider(),packet.rootProvider(),packet.bindingGeneration(),packet.bindingIdentity()),binding(living,packet).selection());
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
            // A newer confirmed point must not overwrite the physical anchor before an already
            // accepted support frame has offered its pending local carry at the old point.
            if(AnatomyMovement.predictsBody(body) && AnatomyMovement.contact(body)!=null)AnatomyMovement.carry(body);
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
        var transfer=session.catalog();var binding=binding(entity,packet);
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
        var identity=new PresentationIdentity(packet.epoch(),packet.dimension(),packet.entity(),packet.entityId(),packet.revision(),packet.model(),packet.provider(),packet.rootProvider(),packet.bindingGeneration(),packet.bindingIdentity());
        var key=new PresentationCacheKey(identity,packet.trackingGeneration(),PresentationKind.CURRENT_ENDPOINT,0,
            endpoint.frameSerial(),endpoint.frameSerial(),Double.doubleToLongBits(1));
        var cached=presentationFrames.get(support.getUUID());
        if(cached!=null && cached.key().equals(key))return java.util.Optional.of(cached.frame());
        return evaluator(support,packet).flatMap(evaluator->evaluator.evaluatePresentation(support,endpoint)).map(evaluated->{
            var frame=PresentationFrame.current(identity,packet.trackingGeneration(),endpoint,evaluated);
            presentationFrames.put(support.getUUID(),new CachedPresentationFrame(key,frame));
            return frame;
        });
    }
    /**
     * Materializes one exact server-certified Q2 interval. No certificate means no interval:
     * adjacent endpoint serials alone are never sufficient authority.
     */
    public static java.util.Optional<PresentationFrame> presentationInterval(net.minecraft.world.entity.LivingEntity support,
            long materialSerial,double fraction) {
        if(support==null || support.level()!=poseLevel || !ready(support) || staleFrames.contains(support.getUUID())
                || materialSerial<1 || !Double.isFinite(fraction) || fraction<0 || fraction>1)return java.util.Optional.empty();
        var certificate=certifiedIntervals.exact(support.getUUID(),materialSerial,clientTick).orElse(null);
        var history=frames.get(support.getUUID());
        if(certificate==null || history==null || certificate.supportId()!=support.getId()
                || !certificate.support().equals(support.getUUID()))return java.util.Optional.empty();
        var beforePacket=history.packet(certificate.beforeFrameSerial()).orElse(null);
        var afterPacket=history.packet(certificate.afterFrameSerial()).orElse(null);
        if(beforePacket==null || afterPacket==null || !certificateMatches(certificate,beforePacket)
                || !certificateMatches(certificate,afterPacket)
                || beforePacket.frameSerial()!=certificate.beforeFrameSerial()
                || afterPacket.frameSerial()!=certificate.afterFrameSerial()
                || beforePacket.authorityTick()!=certificate.beforeAuthorityTick()
                || afterPacket.authorityTick()!=certificate.afterAuthorityTick()
                || beforePacket.jointSampleTick()!=certificate.beforeJointSampleTick()
                || afterPacket.jointSampleTick()!=certificate.afterJointSampleTick()
                || !beforePacket.available() || !afterPacket.available()
                || beforePacket.gravity()!=afterPacket.gravity()
                || !presentationIdentityMatches(support,beforePacket) || !presentationIdentityMatches(support,afterPacket))
            return java.util.Optional.empty();
        var before=history.endpoint(certificate.beforeFrameSerial()).orElse(null);
        var after=history.endpoint(certificate.afterFrameSerial()).orElse(null);
        if(before==null || after==null)return java.util.Optional.empty();
        var identity=new PresentationIdentity(afterPacket.epoch(),afterPacket.dimension(),afterPacket.entity(),afterPacket.entityId(),
            afterPacket.revision(),afterPacket.model(),afterPacket.provider(),afterPacket.rootProvider(),afterPacket.bindingGeneration(),afterPacket.bindingIdentity());
        var key=new PresentationCacheKey(identity,afterPacket.trackingGeneration(),PresentationKind.CERTIFIED_INTERVAL,materialSerial,
            before.frameSerial(),after.frameSerial(),Double.doubleToLongBits(fraction));
        var cached=presentationIntervals.get(support.getUUID());
        if(cached!=null && cached.key().equals(key))return java.util.Optional.of(cached.frame());
        return evaluator(support,afterPacket).flatMap(evaluator->evaluator.evaluatePresentation(support,before,after,fraction)).map(evaluated->{
            var frame=PresentationFrame.interval(identity,afterPacket.trackingGeneration(),before,after,fraction,evaluated);
            presentationIntervals.put(support.getUUID(),new CachedPresentationFrame(key,frame));
            return frame;
        });
    }

    private static boolean certificateMatches(AnatomyMaterialIntervalPayload certificate,AnatomyPosePayload packet) {
        return certificate.epoch().equals(packet.epoch()) && certificate.revision()==packet.revision()
            && certificate.dimension().equals(packet.dimension()) && certificate.supportId()==packet.entityId()
            && certificate.support().equals(packet.entity()) && certificate.bindingGeneration()==packet.bindingGeneration()
            && certificate.trackingGeneration()==packet.trackingGeneration();
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
            .map(frame->new PresentationContact(body,support,surface,new GeometryProvider.Snapshot(frame.identity().revision(),frame.evaluated().pieces()),
                (long)frame.authorityTime(),packet.trackingGeneration()));
    }
    /** Current unique physical-prediction actor, derived only from live local control. */
    private static net.minecraft.world.entity.Entity currentPredictionBody(net.minecraft.client.Minecraft client) {
        var player=client==null?null:client.player;if(player==null)return null;
        var root=player.getRootVehicle();
        if(root==player)return player;
        return root.getControllingPassenger()==player && root.isLocalInstanceAuthoritative()?root:null;
    }
    public record PredictionObservation(net.minecraft.world.entity.Entity body,long generation) {}
    private static volatile PredictionObservation predictionObservation=new PredictionObservation(null,0);
    /** Same-tick control round trips must not revive an old visual carry. */
    public static PredictionObservation observePredictionBody() {
        var current=currentPredictionBody(net.minecraft.client.Minecraft.getInstance());
        var prior=predictionObservation;
        if(prior.body()==current)return prior;
        synchronized(AnatomyClientNetworking.class) {
            prior=predictionObservation;
            if(prior.body()==current)return prior;
            var next=new PredictionObservation(current,prior.generation()==Long.MAX_VALUE
                ?Long.MAX_VALUE:prior.generation()+1);
            predictionObservation=next;
            return next;
        }
    }
    /** Effective authority check for same-tick visual consumers after mount/control changes. */
    public static boolean isCurrentPredictionBody(net.minecraft.world.entity.Entity body) {
        return body!=null && observePredictionBody().body()==body;
    }

    /**
     * Mount/dismount/control replacement is an authority barrier. Metadata from either side of the
     * handoff is discarded rather than being reinterpreted under the new owner.
     */
    private static net.minecraft.world.entity.Entity refreshPredictionOwner() {
        var current=observePredictionBody().body();
        java.util.UUID currentId=current==null?null:current.getUUID();
        if(!java.util.Objects.equals(predictionOwner,currentId)) {
            CausalBodyResidual.reset();
            if(predictionOwner!=null) {
                predictedMovementReferences.remove(predictionOwner);
                transportReceiptTokens.discardBody(predictionOwner);
            }
            if(currentId!=null) {
                predictedMovementReferences.remove(currentId);
                transportReceiptTokens.discardBody(currentId);
            }
            predictionOwner=currentId;
        }
        if(currentId==null)predictedMovementReferences.clear();
        else predictedMovementReferences.keySet().removeIf(id->!id.equals(currentId));
        return current;
    }

    /** Stages the newest exact server receipt whose support endpoint is already incorporated locally. */
    private static void stageMovementReference(net.minecraft.world.entity.Entity body,AnatomyMovement.ClientTransportCursor cursor) {
        if(body==null || cursor==null || body.level()!=poseLevel)return;
        if(refreshPredictionOwner()!=body)return;
        var chosen=transportReceiptTokens.consumeIncorporated(body.getUUID(),cursor.support(),cursor.supportFrameSerial(),
            body.level().getGameTime()).orElse(null);
        if(chosen==null)return;
        predictedMovementReferences.put(body.getUUID(),
            new PredictedMovementReference(chosen.receiptSequence(),cursor.localTransportSequence()));
    }

    /**
     * Returns one exact server-issued receipt token before the following vanilla movement packet.
     * The local transport sequence is only an internal freshness guard and never crosses the wire.
     */
    public static void sendMovementReference(net.minecraft.world.entity.Entity body) {
        var client=net.minecraft.client.Minecraft.getInstance();var player=client.player;
        if(body==null || player==null || refreshPredictionOwner()!=body || !ready(body) || !body.isLocalInstanceAuthoritative()
                || !ClientPlayNetworking.canSend(AnatomyMoveReferenceV2Payload.TYPE))return;
        boolean vehicle=body!=player;
        if(!vehicle && player.getRootVehicle()!=player)return;
        if(vehicle && (player.getRootVehicle()!=body || body.getControllingPassenger()!=player))return;
        var reference=predictedMovementReferences.get(body.getUUID());if(reference==null)return;
        var transport=AnatomyMovement.transport(body);
        if(transport==null || transport.sequence()!=reference.localTransportSequence()) {
            predictedMovementReferences.remove(body.getUUID(),reference);return;
        }
        ClientPlayNetworking.send(new AnatomyMoveReferenceV2Payload(vehicle,reference.receiptSequence()));
        predictedMovementReferences.remove(body.getUUID(),reference);
    }

    private static void reset(){crossLevelBarrier();session.resetConnection();poseLevel=null;clientTick=0;}
    private static void useLevel(net.minecraft.client.multiplayer.ClientLevel level){
        if(poseLevel!=level){crossLevelBarrier();poseLevel=level;session.useLevel(level);}
    }
    public static void initialize() {
        AnatomySession.installClientMode(AnatomyClientNetworking::mode);
        AnatomyMovement.installClientTransportObserver((body,cursor)->{
            CausalBodyResidual.carried(body,cursor);
            stageMovementReference(body,cursor);
        });
        AnatomyMovement.installClientQuietFrameObserver(CausalBodyResidual::quietFrame);
        AnatomyMovement.installClientTransportLifecycleObserver(body->{
            CausalBodyResidual.retireBody(body);
            predictedMovementReferences.remove(body.getUUID());
            transportReceiptTokens.discardBody(body.getUUID());
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->client.execute(()->{
            if(client.getConnection()!=handler)return;
            reset();connectionOwner=handler;
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(()->{
            if(connectionOwner!=handler)return;
            reset();connectionOwner=null;
        }));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
            CausalBodyResidual.retireBody(entity);
            CausalRemoteResidual.forget(entity);
            predictedMovementReferences.remove(entity.getUUID());
            transportReceiptTokens.discardBody(entity.getUUID());
            if(entity instanceof net.minecraft.world.entity.LivingEntity living) {
                CausalBodyResidual.retireSupport(living.getUUID());
                CausalRemoteResidual.retireSupport(living.getUUID());
                var history=frames.remove(living.getUUID());
                if(history!=null)retireFrameHistory(level,living.getUUID(),history);
            }
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            useLevel(client.level);clientTick++;
            refreshPredictionOwner();
            if(poseLevel!=null) {
                contacts.prune(poseLevel.getGameTime(),100);
                transportReceiptTokens.prune(poseLevel.getGameTime());
                certifiedIntervals.prune(clientTick);
            }
            frames.entrySet().removeIf(entry->{
                var history=entry.getValue();var packet=history.current();var entity=poseLevel==null?null:poseLevel.getEntity(packet.entityId());
                history.pruneRecent(clientTick);
                boolean expired=clientTick-receivedAt.getOrDefault(entry.getKey(),0L)>100;
                boolean wrongIdentity=entity!=null && !entity.getUUID().equals(entry.getKey());
                if(expired || wrongIdentity){retireFrameHistory(poseLevel,entry.getKey(),history,wrongIdentity);return true;}
                return false;
            });
            presentationContacts.entrySet().removeIf(entry->{
                var packet=entry.getValue();var body=poseLevel==null?null:poseLevel.getEntity(packet.bodyId());var support=poseLevel==null?null:poseLevel.getEntity(packet.supportId());
                return poseLevel==null || packet.tick()+100<poseLevel.getGameTime() || body==null || !body.getUUID().equals(packet.body())
                    || !(support instanceof net.minecraft.world.entity.LivingEntity) || !support.getUUID().equals(packet.support());
            });
            bindPhysics();
            CausalRemoteResidual.endTick();
            var predictionBody=currentPredictionBody(client);
            CausalBodyResidual.refresh(predictionBody);
            if(poseLevel!=null) {
                AnatomyMovement.tickGeometry(poseLevel);
                if(predictionBody!=null && AnatomyMovement.contact(predictionBody)!=null)
                    AnatomyMovement.carry(predictionBody);
                for(var entity:poseLevel.entitiesForRendering())
                    if(entity!=predictionBody && entity.isLocalInstanceAuthoritative()
                            && AnatomyMovement.contact(entity)!=null)AnatomyMovement.carry(entity);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyCatalogPayload.TYPE,(packet,context)->{
            useLevel(context.client().level);var transfer=session.catalog();
            try{if(transfer.accept(packet))acceptedCatalog();}
            catch(RuntimeException invalid){transfer.rejectPending();io.github.r3neer.scalebrews.ScaleBrews.LOGGER.error("Rejected anatomical catalog; previous revision retained",invalid);}
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyPosePayload.TYPE,(packet,context)->{
            var level=context.client().level;useLevel(level);var transfer=session.catalog();
            if(level==null || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision() || !packet.dimension().equals(level.dimension().identifier())
                || transfer.snapshot().resolvePose(null,packet).isEmpty() || frameReplayFence.rejects(packet))return;
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
                    certifiedIntervals.retainLifecycle(packet.entity(),packet.entityId(),packet.bindingGeneration(),packet.trackingGeneration());
                    discardSupportMaterial(level,packet.entity(),previous.entityId(),false);
                    framesForSupport=new AnatomyFrameHistory();frames.put(packet.entity(),framesForSupport);
                }
                if(!framesForSupport.accept(packet,clientTick))return;
                frameReplayFence.accepted(packet);receivedAt.put(packet.entity(),clientTick);
                if(!packet.available()) {staleFrames.add(packet.entity());discardSupportMaterial(level,packet.entity(),packet.entityId());return;}
                staleFrames.remove(packet.entity());
                var history=poses.computeIfAbsent(packet.entity(),id->new AnatomyPoseHistory());
                if(history.current()==null || packet.jointSampleTick()>history.current().jointSampleTick())history.accept(packet);
                if(entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    CausalBodyResidual.acceptedEndpoint(living);
                    CausalRemoteResidual.acceptedOwnFrame(living);
                }
            } catch(IllegalArgumentException invalid) {
                io.github.r3neer.scalebrews.ScaleBrews.LOGGER.warn("Rejected causal pose frame for {}",packet.entity(),invalid);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyContactPayload.TYPE,(packet,context)->{
            var level=context.client().level;useLevel(level);var transfer=session.catalog();
            if(level==null || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision() || !packet.dimension().equals(level.dimension().identifier())
                    || packet.tick()+100<level.getGameTime())return;
            if(contacts.accept(level.dimension().identifier(),packet)){
                CausalBodyResidual.contactPacket(packet);
                var body=level.getEntity(packet.bodyId());
                if(body!=null && body.getUUID().equals(packet.body()))CausalRemoteResidual.contactPacket(body,packet);
                presentationContacts.remove(packet.body());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyMaterialIntervalPayload.TYPE,(packet,context)->{
            var level=context.client().level;useLevel(level);var transfer=session.catalog();
            if(level==null || !transfer.ready() || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision()
                    || !packet.dimension().equals(level.dimension().identifier())
                    || packet.trackingGeneration()<=frameReplayFence.retiredGeneration(packet.support()))return;
            var support=level.getEntity(packet.supportId());
            if(!(support instanceof net.minecraft.world.entity.LivingEntity living) || !living.getUUID().equals(packet.support()))return;
            certifiedIntervals.accept(packet,clientTick);
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyTransportReceiptPayload.TYPE,(packet,context)->{
            var level=context.client().level;useLevel(level);var transfer=session.catalog();
            if(level==null || !transfer.ready() || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision()
                    || !packet.dimension().equals(level.dimension().identifier()))return;
            var body=level.getEntity(packet.bodyId());
            if(body==null || !body.getUUID().equals(packet.body()))return;
            transportReceiptTokens.accept(packet,level.getGameTime());
        });
        // The bundle/native-handler scope validates observer attestations synchronously.
        // This receiver advertises protocol support but never treats callback order as provenance.
        ClientPlayNetworking.registerGlobalReceiver(AnatomyObserverCarryPayload.TYPE,(packet,context)->{});
    }
}
