package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.AnatomySeparation;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.MaterialBroadphase;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;

import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Shared movement integration for the replacement entity-collision runtime. */
public final class AnatomyMovement {
    private AnatomyMovement() {}
    private static final Set<Level> ACTIVE=Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    // Entity equality reuses network IDs across logical worlds in an integrated process.
    // Weak identity keys keep server and client instances strictly separate.
    private static <K,V> Map<K,V> entityMap(){return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<K,V>makeMap());}
    private record CaptureStamp(GeometryProvider provider,GeometryProvider.GeometryIdentityDescriptor descriptor,long registration,long lifecycle) {}
    /** Server-owned material endpoint serial; it advances for a root or joint endpoint change. */
    private record EndpointStamp(long jointSampleTick,RootFrame root,AnatomyPoseHistory.Sample sample,long revision,GeometryProvider.Availability availability) {}
    /** Retains the whole endpoint, including its original authority time, for a material serial. */
    private record EndpointSerial(EndpointStamp stamp,GeometryProvider.CausalEndpoint endpoint,GeometryProvider.Snapshot snapshot,boolean invalidated) {}
    private static final Map<LivingEntity,EndpointSerial> FRAME_SERIALS=entityMap();
    /** Rigid root provenance is independent of 20 Hz joint-pose evaluation. */
    public record RootFrame(long sequence,long tick,Vec3 origin,float yaw,float scale,GravityFrame gravity) {
        public RootFrame {
            if(sequence<0 || tick<0 || origin==null || gravity==null || !Double.isFinite(origin.lengthSqr()) || !Float.isFinite(yaw) || !Float.isFinite(scale) || scale<=0)
                throw new IllegalArgumentException("Invalid root frame");
        }
    }
    private static final class RootHistory {final ArrayDeque<RootFrame> frames=new ArrayDeque<>();}
    private static final Map<LivingEntity,RootHistory> ROOTS=entityMap();
    /** Per-support live maintenance. Never rebuild or sample unrelated providers from a local mutation hook. */
    private static synchronized void removeSpatialEntry(LivingEntity support) {
        if(support==null)return;
        AnatomySpatialIndex.removeIfCurrent(support);
    }
    private static synchronized void refreshSpatialEntry(LivingEntity support) {
        if(support==null)return;
        var level=support.level();
        if(!AnatomySpatialIndex.current(level,level.getGameTime()))return;
        AnatomySpatialIndex.removeIfCurrent(support);
        var provider=AnatomyBindingState.provider(support);if(provider==null || support.isRemoved())return;
        GeometryProvider.Snapshot snapshot;RootFrame root;
        var descriptor=AnatomyBindingState.descriptor(support);
        if(descriptor!=null) {
            var accepted=FRAME_SERIALS.get(support);
            if(accepted==null || accepted.invalidated() || accepted.snapshot()==null
                    || accepted.endpoint().availability()!=GeometryProvider.Availability.AVAILABLE
                    || accepted.snapshot().revision()!=descriptor.revision())return;
            snapshot=accepted.snapshot();root=accepted.endpoint().root();
        } else {
            snapshot=provider.sample(support).orElse(null);if(snapshot==null)return;
            root=observeRoot(support);
        }
        var envelope=spatialEnvelope(snapshot,root);if(envelope==null)return;
        var rejected=AnatomySpatialIndex.upsertIfCurrent(support,envelope);
        if(rejected!=null) {
            clearSupportContacts(support);
            if(descriptor!=null)quarantineEndpoint(support);
        }
    }
    /** Supported external root/dimension mutation hook; observes only this registered support. */
    public static synchronized void spatialMutation(LivingEntity support) {
        if(support==null || !ACTIVE.contains(support.level()) || !AnatomyBindingState.hasProvider(support))return;
        observeRoot(support);
        if(AnatomyBindingState.causal(support))queryFrame(support);else refreshSpatialEntry(support);
    }
    public static boolean suspended(Entity body,LivingEntity support) {
        var generation=AnatomyContactState.suspensionGeneration(body,support);
        if(generation==null || generation.longValue()!=registrationGeneration(support)) {
            if(generation!=null)AnatomyContactState.clearSuspension(body,support);
            return false;
        }
        var provider=AnatomyBindingState.provider(support);var shape=currentSnapshot(support,provider).orElse(null);
        if(shape!=null && shape.pieces().values().stream().anyMatch(p->p.overlaps(body.getBoundingBox())))return true;
        AnatomyContactState.clearSuspension(body,support);return false;
    }
    static void suspend(Entity body,LivingEntity support) {
        AnatomyContactState.suspend(body,support,registrationGeneration(support));
        var contact=contact(body);if(contact!=null && contact.support()==support)clear(body);
    }
    public record SweepMetrics(long tick,long queries,long pieces,long evaluations,long exhausted) {}
    private static final Map<Level,SweepMetrics> METRICS=Collections.synchronizedMap(new WeakHashMap<>());
    public static SweepMetrics sweepMetrics(Level level){return METRICS.getOrDefault(level,new SweepMetrics(level.getGameTime(),0,0,0,0));}
    private static void recordSweep(Level level,int pieces,int evaluations,int exhausted) {
        synchronized(METRICS) {
            var old=sweepMetrics(level);
            if(old.tick()!=level.getGameTime())old=new SweepMetrics(level.getGameTime(),0,0,0,0);
            METRICS.put(level,new SweepMetrics(old.tick(),old.queries()+1,old.pieces()+pieces,old.evaluations()+evaluations,old.exhausted()+exhausted));
        }
    }
    public record Contact(LivingEntity support,String piece,long revision,Vec3 normal,long sequence) {}
    public static synchronized void activate(Level level){ACTIVE.add(level);}
    public static synchronized boolean active(Level level){return ACTIVE.contains(level);}
    public static synchronized boolean active(Entity e){return ACTIVE.contains(e.level());}
    /** Server simulates every body; a client predicts only entities it owns locally. */
    public static boolean simulates(Entity body){return !body.level().isClientSide() || body.isLocalInstanceAuthoritative();}
    /** Fixture-only overload. Production runtime uses the causal-descriptor overload. */
    public static synchronized void register(LivingEntity support,GeometryProvider provider){
        if(support==null || provider==null)throw new IllegalArgumentException("Missing geometry registration");
        requireServerThread(support.level());
        AnatomyBindingState.rebind(support,provider,null);
        FRAME_SERIALS.remove(support);ROOTS.remove(support);clearSupportContacts(support);removeSpatialEntry(support);
        refreshSpatialEntry(support);
    }
    /** Runtime causal registration; model and pose provider are catalog identifiers, never model source text. */
    public static synchronized void register(LivingEntity support,GeometryProvider provider,GeometryProvider.GeometryIdentityDescriptor descriptor){
        if(support==null || provider==null)throw new IllegalArgumentException("Missing geometry registration");
        if(descriptor==null)throw new IllegalArgumentException("Missing geometry descriptor");
        requireServerThread(support.level());
        AnatomyBindingState.rebind(support,provider,descriptor);
        FRAME_SERIALS.remove(support);ROOTS.remove(support);clearSupportContacts(support);removeSpatialEntry(support);
        queryFrame(support);
    }
    private static void requireServerThread(Level level) {
        var server=level.getServer();
        if(!level.isClientSide() && server!=null && !server.isSameThread())
            throw new IllegalStateException("Entity collision mutation requires the world thread");
    }
    /** Local binding generation; changes on every provider rebind for this entity instance. */
    public static synchronized long registrationGeneration(LivingEntity support){return AnatomyBindingState.generation(support);}
    /** Causal publication survives an unavailable pose so networking can clear stale geometry. */
    public static synchronized Optional<GeometryProvider.PublishedFrame> publishedFrame(LivingEntity support){
        requireServerThread(support.level());
        var binding=AnatomyBindingState.beginCapture(support);
        if(binding==null)return Optional.empty();
        try {
            var descriptor=binding.descriptor();var provider=binding.provider();
            var capture=new CaptureStamp(provider,descriptor,binding.generation(),transportGeneration(support));
            var endpoint=causalEndpoint(support,provider,capture).orElse(null);
            if(endpoint==null || !captureCurrent(support,capture))return Optional.empty();
            var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),descriptor.epoch(),descriptor.revision(),
                descriptor.model(),descriptor.poseProvider(),descriptor.bindingGeneration(),capture.registration());
            return Optional.of(new GeometryProvider.PublishedFrame(identity,endpoint));
        } finally {AnatomyBindingState.endCapture(support);}
    }
    /** Current collision frame; an unavailable causal endpoint deliberately has no convex query. */
    public static synchronized Optional<GeometryProvider.QueryFrame> queryFrame(LivingEntity support){
        var published=publishedFrame(support).orElse(null);
        if(published==null || published.endpoint().availability()!=GeometryProvider.Availability.AVAILABLE)return Optional.empty();
        var cached=FRAME_SERIALS.get(support);var snapshot=cached==null?null:cached.snapshot();
        if(snapshot==null || snapshot.revision()!=published.identity().revision())return Optional.empty();
        return Optional.of(new GeometryProvider.QueryFrame(published.identity(),published.endpoint(),snapshot));
    }
    /**
     * A root move may happen after a 20 Hz joint sample in the same game tick.
     * Reuse only that sample's already-evaluated inputs, capture the root once,
     * and rebuild the convexes from the combined endpoint.  We never combine an
     * old root-bearing Sample with a newer RootFrame.
     */
    private static Optional<GeometryProvider.CausalEndpoint> causalEndpoint(LivingEntity support,GeometryProvider provider,CaptureStamp capture) {
        try {return evaluateEndpoint(support,provider,capture);}
        catch(RuntimeException rejectedProvider) {
            return captureBindingCurrent(support,capture)?quarantineEndpoint(support):Optional.empty();
        }
    }
    private static Optional<GeometryProvider.CausalEndpoint> evaluateEndpoint(LivingEntity support,GeometryProvider provider,CaptureStamp capture) {
        if(provider==null)return Optional.empty();
        var direct=provider.causalEndpoint(support);
        if(!captureCurrent(support,capture))return rejectStaleCapture(support,capture,direct.orElse(null),null);
        if(direct.isPresent()) {
            var snapshot=direct.get().availability()==GeometryProvider.Availability.AVAILABLE?provider.sample(support).orElse(null):null;
            if(!captureCurrent(support,capture))return rejectStaleCapture(support,capture,direct.get(),snapshot);
            return acceptEndpoint(support,direct.get(),snapshot);
        }
        if(!(provider instanceof ModelGeometryProvider model))return Optional.empty();
        var joints=model.authoritativeFrame(support).orElse(null);
        if(!captureCurrent(support,capture) || joints==null)return Optional.empty();
        var root=observeRoot(support);
        if(!captureCurrent(support,capture))return Optional.empty();
        var sample=new AnatomyPoseHistory.Sample(joints.sample().inputs(),root.origin(),root.yaw(),root.scale(),root.gravity());
        var snapshot=sample.inputs().ordinary()?model.sampleAt(support,sample).orElse(null):null;
        if(!captureCurrent(support,capture))return Optional.empty();
        return Optional.of(serverEndpoint(support,joints.tick(),root,sample,snapshot));
    }
    private static boolean captureBindingCurrent(LivingEntity support,CaptureStamp capture) {
        var binding=AnatomyBindingState.snapshot(support);
        return binding!=null && binding.provider()==capture.provider() && Objects.equals(binding.descriptor(),capture.descriptor())
            && binding.generation()==capture.registration();
    }
    private static boolean captureCurrent(LivingEntity support,CaptureStamp capture) {
        return captureBindingCurrent(support,capture) && transportGeneration(support)==capture.lifecycle();
    }
    /**
     * A lifecycle callback may invalidate the very endpoint currently being sampled. Retain that
     * serial only as an invalidated fence; a rebind instead owns a new registration and is untouched.
     */
    private static Optional<GeometryProvider.CausalEndpoint> rejectStaleCapture(LivingEntity support,CaptureStamp capture,
            GeometryProvider.CausalEndpoint endpoint,GeometryProvider.Snapshot snapshot) {
        if(!captureBindingCurrent(support,capture))return Optional.empty();
        var old=FRAME_SERIALS.get(support);
        if(endpoint!=null && (old==null || endpoint.frameSerial()>old.endpoint().frameSerial()))
            FRAME_SERIALS.put(support,new EndpointSerial(null,endpoint,snapshot,true));
        else if(old!=null)FRAME_SERIALS.put(support,new EndpointSerial(old.stamp(),old.endpoint(),old.snapshot(),true));
        clearSupportContacts(support);removeSpatialEntry(support);
        return Optional.empty();
    }
    private static synchronized GeometryProvider.CausalEndpoint serverEndpoint(LivingEntity support,long jointSampleTick,RootFrame root,AnatomyPoseHistory.Sample sample,GeometryProvider.Snapshot snapshot) {
        var availability=snapshot==null?GeometryProvider.Availability.UNAVAILABLE:GeometryProvider.Availability.AVAILABLE;
        var stamp=new EndpointStamp(jointSampleTick,root,sample,snapshot==null?-1:snapshot.revision(),availability);var old=FRAME_SERIALS.get(support);
        if(old!=null && !old.invalidated() && old.stamp()!=null && old.stamp().equals(stamp))return old.endpoint();
        long next=old==null?1:Math.incrementExact(old.endpoint().frameSerial());
        var endpoint=new GeometryProvider.CausalEndpoint(next,support.level().getGameTime(),jointSampleTick,root,sample,availability);
        if(availability==GeometryProvider.Availability.UNAVAILABLE && old!=null && old.endpoint().availability()==GeometryProvider.Availability.AVAILABLE)
            clearSupportContacts(support);
        FRAME_SERIALS.put(support,new EndpointSerial(stamp,endpoint,snapshot,false));refreshSpatialEntry(support);return endpoint;
    }
    /**
     * A wire/provider endpoint may be queried repeatedly.  Same material serial
     * returns the immutable cached object; a changed endpoint without a new serial
     * fails closed rather than silently changing its authority time or TRS.
     */
    private static synchronized Optional<GeometryProvider.CausalEndpoint> acceptEndpoint(LivingEntity support,GeometryProvider.CausalEndpoint endpoint,GeometryProvider.Snapshot snapshot) {
        if(endpoint.availability()==GeometryProvider.Availability.AVAILABLE && (snapshot==null || snapshot.revision()!=AnatomyBindingState.descriptor(support).revision()))return quarantineEndpoint(support);
        if(endpoint.availability()==GeometryProvider.Availability.UNAVAILABLE && snapshot!=null)return quarantineEndpoint(support);
        var old=FRAME_SERIALS.get(support);
        if(old==null) {
            FRAME_SERIALS.put(support,new EndpointSerial(null,endpoint,snapshot,false));refreshSpatialEntry(support);return Optional.of(endpoint);
        }
        long prior=old.endpoint().frameSerial();
        if(old.invalidated()) {
            if(endpoint.frameSerial()<=prior)return Optional.empty();
        } else if(endpoint.frameSerial()<prior)return Optional.empty();
        else if(endpoint.frameSerial()==prior) {
            if(!old.endpoint().equals(endpoint) || !Objects.equals(old.snapshot(),snapshot))return quarantineEndpoint(support);
            return Optional.of(old.endpoint());
        }
        if(endpoint.authorityTick()<old.endpoint().authorityTick() || endpoint.jointSampleTick()<old.endpoint().jointSampleTick())return quarantineEndpoint(support);
        if(endpoint.availability()==GeometryProvider.Availability.UNAVAILABLE && old.endpoint().availability()==GeometryProvider.Availability.AVAILABLE)
            clearSupportContacts(support);
        FRAME_SERIALS.put(support,new EndpointSerial(null,endpoint,snapshot,false));refreshSpatialEntry(support);return Optional.of(endpoint);
    }
    /** Retain the rejected serial's fence; without one, quarantine this exact local registration until rebind. */
    private static Optional<GeometryProvider.CausalEndpoint> quarantineEndpoint(LivingEntity support) {
        var old=FRAME_SERIALS.get(support);
        if(old==null)AnatomyBindingState.quarantineCurrent(support);
        else FRAME_SERIALS.put(support,new EndpointSerial(old.stamp(),old.endpoint(),old.snapshot(),true));
        clearSupportContacts(support);removeSpatialEntry(support);
        return Optional.empty();
    }
    /** Instantaneous query geometry. A descriptor must have a causal endpoint; fixture-only legacy bindings retain sample(). */
    private static Optional<GeometryProvider.Snapshot> currentSnapshot(LivingEntity support,GeometryProvider provider) {
        if(AnatomyBindingState.quarantined(support))return Optional.empty();
        if(AnatomyBindingState.causal(support))return queryFrame(support).map(GeometryProvider.QueryFrame::snapshot);
        return provider==null?Optional.empty():provider.sample(support);
    }
    /** Fixture-only gravity seam; the effective value still lives in the shared Scale authority. */
    public static synchronized void gravity(Entity body,GravityFrame gravity){io.github.r3neer.scalebrews.integration.gravity.GravityFrames.overrideForTests(body,gravity);}
    public static synchronized GravityFrame gravity(Entity body){return io.github.r3neer.scalebrews.integration.gravity.GravityFrames.frame(body);}
    public static synchronized Contact contact(Entity body){
        var contact=AnatomyContactState.contact(body);
        return contact==null?null:new Contact(contact.support(),contact.piece(),contact.revision(),contact.normal(),contact.sequence());
    }
    public static synchronized long contactSequence(Entity body){return AnatomyContactState.contactSequence(body);}
    /** Release a material contact and any receipt that could otherwise outlive it. */
    public static synchronized void clear(Entity body){
        AnatomyContactState.clear(body);
        AnatomyTransportReceipts.invalidate(body);
    }
    public static synchronized SurfaceContact surface(Entity body){return AnatomyContactState.surface(body);}
    /** Transitional façade while callers migrate to the runtime ledger. */
    public static synchronized SupportTransport transport(Entity body){return TransportLedger.current(body);}
    /** Transitional lifecycle façade paired with the transport cursor. */
    static synchronized long transportGeneration(Entity body){return TransportLedger.generation(body);}
    /** A teleport/removal invalidates a body's own anchor but may retain already-applied carry. */
    private static void invalidateBody(Entity body,boolean discardTransport) {
        clear(body);
        TransportLedger.invalidate(body,discardTransport);
    }
    public static synchronized boolean confirm(Entity body,LivingEntity support,SurfaceContact surface) {
        if(!active(body) || surface==null || !support.getUUID().equals(surface.support()) || !Platforms.eligible(body,support))return false;
        var provider=AnatomyBindingState.provider(support);var snapshot=currentSnapshot(support,provider).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(surface.piece());
        if(piece==null || snapshot.revision()!=surface.revision() || surface.face()<0 || surface.face()>5)return false;
        Vec3 point=piece.point(surface.localPoint()),normal=piece.faceNormal(surface.face());
        // The authoritative normal belongs to the packet tick. Recompute it from the newest
        // accepted pose instead of rejecting a valid contact while the support is animating.
        if(!gravity(body).supports(normal))return false;
        setContact(body,support,surface.piece(),surface.revision(),normal);
        AnatomyContactState.surface(body,new SurfaceContact(surface.support(),surface.revision(),surface.piece(),surface.face(),surface.localPoint(),normal,surface.tick()));
        AnatomyContactState.anchor(body,new AnatomyContactState.Anchor(surface.localPoint(),point,piece,support.position(),body.position(),gravity(body),gravity(support)));
        return true;
    }
    public static boolean supported(Entity body) {
        var c=contact(body);if(c==null || suspended(body,c.support()) || !Platforms.eligible(body,c.support()))return false;
        var provider=AnatomyBindingState.provider(c.support());var snapshot=currentSnapshot(c.support(),provider).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(c.piece());
        if(piece==null || snapshot.revision()!=c.revision())return false;
        var separation=piece.separation(body.getBoundingBox());
        return Math.abs(separation.gap())<=.025 && gravity(body).supports(separation.normal());
    }
    /** Crouch edge protection in the body's gravity tangent plane, never legacy world-Y surfaces. */
    public static Vec3 edge(net.minecraft.world.entity.player.Player body,Vec3 requested) {
        if(!active(body) || !simulates(body) || !body.isShiftKeyDown())return requested;
        var contact=contact(body);if(contact==null || !Platforms.eligible(body,contact.support()))return requested;
        var frame=gravity(body);double vertical=frame.vertical(requested);
        if(vertical>1e-5)return requested; // Moving away from support is not an edge walk.
        Vec3 tangent=frame.tangent(requested);double step=Math.max(.005,.05*body.getScale());
        while(tangent.lengthSqr()>1e-10 && !hasEdgeSupport(body,contact,body.getBoundingBox().move(tangent),-vertical))
            tangent=tangent.length()<=step?Vec3.ZERO:tangent.subtract(tangent.scale(step/tangent.length()));
        return tangent.add(frame.up().scale(vertical));
    }
    private static boolean hasEdgeSupport(Entity body,Contact contact,AABB candidate,double extraDrop) {
        var provider=AnatomyBindingState.provider(contact.support());var snapshot=currentSnapshot(contact.support(),provider).orElse(null);
        if(snapshot==null || snapshot.revision()!=contact.revision())return false;
        Vec3 drop=gravity(body).gravity().scale(Math.max(.001,body instanceof LivingEntity living?living.maxUpStep():0)+extraDrop+.001);
        for(var piece:snapshot.pieces().values()) {
            var hit=piece.sweep(candidate,drop);
            if(hit!=null && !hit.penetrating() && gravity(body).supports(hit.normal()))return true;
        }
        return false;
    }
    /** Suppress only the material support/body pair; all unrelated pushing remains vanilla. */
    public static boolean suppressesPush(Entity first,Entity second) {
        var a=contact(first);var b=contact(second);
        return a!=null && a.support()==second || b!=null && b.support()==first;
    }
    public static boolean spaceClear(Entity body,AABB box) {
        var query=candidates(body,box);
        if(!query.complete())return false;
        for(var candidate:query.candidates())if(candidate.box.overlaps(box))return false;
        return true;
    }
    public static void tick(Level level) {
        if(!ACTIVE.contains(level))return;
        Map<LivingEntity,GeometryProvider> providers=AnatomyBindingState.providersSnapshot();List<Entity> bodies;
        bodies=AnatomyContactState.contactBodies();
        for(var entry:providers.entrySet())if(entry.getKey().level()==level) {
            observeRoot(entry.getKey());entry.getValue().tick(entry.getKey(),level.getGameTime());
        }
        rebuildSpatial(level,providers);
        for(var body:bodies)if(body.level()==level)carry(body);
    }
    /** Client-side pose cache update without moving server-owned observer entities. */
    public static void tickGeometry(Level level) {
        if(!ACTIVE.contains(level))return;
        Map<LivingEntity,GeometryProvider> providers=AnatomyBindingState.providersSnapshot();
        for(var entry:providers.entrySet())if(entry.getKey().level()==level)
            entry.getValue().tick(entry.getKey(),level.getGameTime());
    }
    public static synchronized void deactivate(Level level){
        ACTIVE.remove(level);AnatomyBindingState.deactivate(level);
        // Local registration generation is a weak identity watermark and must not rewind on level lifecycle.
        FRAME_SERIALS.keySet().removeIf(e->e.level()==level);
        io.github.r3neer.scalebrews.integration.gravity.GravityFrames.clearTestOverrides(level);
        AnatomyContactState.deactivate(level);
        ROOTS.keySet().removeIf(e->e.level()==level);
        TransportLedger.deactivate(level);
        METRICS.remove(level);
        AnatomySpatialIndex.deactivate(level);
    }
    private static synchronized Contact setContact(Entity body,LivingEntity support,String piece,long revision,Vec3 normal) {
        var contact=AnatomyContactState.setContact(body,support,piece,revision,normal);
        return new Contact(contact.support(),contact.piece(),contact.revision(),contact.normal(),contact.sequence());
    }
    /** Capture before a root move; no pose channels are read or evaluated. */
    public static RootFrame captureRoot(LivingEntity support){return observeRoot(support);}
    /**
     * Record a continuous rigid transform segment after move/setPos/yaw/scale. The
     * caller may provide the before frame captured above; stale callers are safely
     * reduced to the canonical history rather than inventing a second segment.
     */
    public static synchronized RootFrame observeRoot(LivingEntity support,RootFrame before) {
        var history=ROOTS.computeIfAbsent(support,ignored->new RootHistory());
        var last=history.frames.peekLast();
        // A stale callback must not invent a segment across an unrelated move.
        // A current capture is the explicit START/before -> after provenance edge.
        if(before==null || last==null || before.sequence()!=last.sequence() || !sameRoot(before,last))return observeRoot(support);
        return appendRootFrame(support,history,last);
    }
    /** Cheap deduplicated observation for query/tick paths that bypass Entity.move. */
    public static synchronized RootFrame observeRoot(LivingEntity support) {
        var history=ROOTS.computeIfAbsent(support,ignored->new RootHistory());var last=history.frames.peekLast();
        return appendRootFrame(support,history,last);
    }
    private static RootFrame appendRootFrame(LivingEntity support,RootHistory history,RootFrame last) {
        if(last==null) {var initial=rootFrame(support,0);history.frames.add(initial);return initial;}
        var current=rootFrame(support,last.sequence()+1);
        if(sameRoot(last,current))return last;
        if(last.origin().distanceToSqr(current.origin())>16 || !last.gravity().equals(current.gravity())) {
            clearSupportContacts(support);history.frames.clear();history.frames.add(current);return current;
        }
        history.frames.add(current);trimRootHistory(history,current.tick());return current;
    }
    /** Explicit teleport/dimension/removal lifecycle hook; even a small jump is discontinuous. */
    public static synchronized void invalidateRoot(LivingEntity support) {
        // The support can itself be standing on another support.  Clear that anchor too, so a
        // sub-four-block teleport cannot pull it through air on the next carry pass.
        invalidateBody(support,true);
        // Do not reset the binding's serial: a receiver/cursor must distinguish
        // this discontinuity from an old endpoint with the same transform.
        FRAME_SERIALS.computeIfPresent(support,(ignored,old)->new EndpointSerial(old.stamp(),old.endpoint(),old.snapshot(),true));
        clearSupportContacts(support);ROOTS.remove(support);removeSpatialEntry(support);
    }
    /**
     * An accepted unavailable pose keeps its endpoint watermark, but every body
     * anchored to that support must stop immediately.  Client lifecycle code uses
     * this without deactivating the whole level or resetting binding serials.
     */
    public static synchronized void invalidateSupport(LivingEntity support) {
        clearSupportContacts(support);removeSpatialEntry(support);
    }
    private static RootFrame rootFrame(LivingEntity support,long sequence) {
        return new RootFrame(sequence,support.level().getGameTime(),support.position(),support.yBodyRot,support.getScale(),gravity(support));
    }
    private static boolean sameRoot(RootFrame a,RootFrame b) {
        return a.origin().equals(b.origin()) && Float.compare(a.yaw(),b.yaw())==0 && Float.compare(a.scale(),b.scale())==0 && a.gravity().equals(b.gravity());
    }
    private static void trimRootHistory(RootHistory history,long tick) {
        while(history.frames.size()>64 || history.frames.peekFirst()!=null && history.frames.peekFirst().tick()<tick-20)history.frames.removeFirst();
    }
    private static void clearSupportContacts(LivingEntity support) {
        for(var body:AnatomyContactState.bodiesSupportedBy(support))
            // A support discontinuity is also a body discontinuity, but the body may need its
            // last applied segment for the next pose sample to remove passive motion exactly once.
            invalidateBody(body,false);
    }
    static record Candidate(LivingEntity entity,String id,long revision,ConvexBox box) {}
    static record CandidateQuery(MaterialBroadphase.QueryStatus status,List<Candidate> candidates) {
        boolean complete(){return status==MaterialBroadphase.QueryStatus.COMPLETE;}
    }
    public record SweepContact(LivingEntity support,String piece,long revision,ConservativeSweep.Result result) {}
    /** Dedicated temporal query, deliberately separate from navigation and vanilla noCollision. */
    public static SweepContact sweep(Entity body,Vec3 displacement,int iterations) {
        if(!active(body))return null;
        AABB swept=body.getBoundingBox().expandTowards(displacement);
        var supportQuery=indexedSupports(body,swept.inflate(Platforms.searchMargin(body.level())+4));
        if(!supportQuery.complete()){recordSweep(body.level(),0,0,1);return null;}
        var supports=new ArrayList<>(supportQuery.candidates());
        supports.sort(Comparator.comparingInt(Entity::getId));
        SweepContact best=null;
        int queried=0,evaluations=0,exhausted=0;
        for(var support:supports) {
            if(suspended(body,support))continue;
            var provider=AnatomyBindingState.provider(support);
            var snapshot=provider==null?null:provider.motion(support).orElse(null);
            if(snapshot==null || snapshot.toTick()!=body.level().getGameTime())continue;
            for(var id:new TreeSet<>(snapshot.pieces().keySet())) {
                var motion=snapshot.pieces().get(id);
                // Every point stays within speed*dt of its initial point; endpoint envelopes alone are unsafe.
                if(!motion.at().apply(0).bounds().inflate(motion.maxPointSpeed()+.001).intersects(swept))continue;
                var hit=ConservativeSweep.query(body.getBoundingBox(),displacement,motion,iterations);
                queried++;evaluations+=hit.evaluations();
                if(hit.status()==ConservativeSweep.Status.ITERATION_LIMIT)exhausted++;
                if(hit.status()==ConservativeSweep.Status.CLEAR)continue;
                if(best==null || hit.safeFraction()<best.result().safeFraction()-1e-9)
                    best=new SweepContact(support,id,snapshot.revision(),hit);
            }
        }
        recordSweep(body.level(),queried,evaluations,exhausted);
        return best;
    }
    public record Selection(LivingEntity support,SurfaceContact contact,Vec3 position,double fraction) {}
    public static Selection raycast(Entity observer,Vec3 start,Vec3 end) {
        if(!active(observer))return null;
        var query=candidates(observer,new AABB(start,end).inflate(.001));
        if(!query.complete())return null;
        Selection best=null;
        for(var candidate:query.candidates()) {
            var hit=candidate.box.raycast(start,end);
            if(hit==null || best!=null && hit.fraction()>=best.fraction()-1e-9)continue;
            var contact=new SurfaceContact(candidate.entity.getUUID(),candidate.revision,candidate.id,hit.face(),hit.localPoint(),hit.normal(),observer.level().getGameTime());
            best=new Selection(candidate.entity,contact,start.add(end.subtract(start).scale(hit.fraction())),hit.fraction());
        }
        return best;
    }
    static CandidateQuery candidates(Entity body,AABB swept) {
        List<Candidate> result=new ArrayList<>();
        var supportQuery=indexedSupports(body,swept.inflate(Platforms.searchMargin(body.level())));
        if(!supportQuery.complete())return new CandidateQuery(supportQuery.status(),List.of());
        for(var support:supportQuery.candidates()) {
            GeometryProvider provider=AnatomyBindingState.provider(support);
            if(provider==null)continue;
            currentSnapshot(support,provider).ifPresent(snapshot->snapshot.pieces().forEach((id,box)->{
                if(box.bounds().inflate(.001).intersects(swept))result.add(new Candidate(support,id,snapshot.revision(),box));
            }));
        }
        result.sort(Comparator.comparingInt((Candidate c)->c.entity.getId()).thenComparing(c->c.entity.getUUID()).thenComparing(Candidate::id));
        return new CandidateQuery(MaterialBroadphase.QueryStatus.COMPLETE,result);
    }
    private static MaterialBroadphase.QueryResult<LivingEntity> indexedSupports(Entity body,AABB query) {
        var raw=spatialQuery(body.level(),query);
        if(!raw.complete())return raw;
        var result=new ArrayList<LivingEntity>();
        for(var support:raw.candidates())if(support.level()==body.level() && Platforms.eligible(body,support) && !suspended(body,support))result.add(support);
        return new MaterialBroadphase.QueryResult<>(raw.status(),result,raw.requestedCells(),raw.cellsVisited(),raw.candidatesVisited());
    }
    private static MaterialBroadphase.QueryResult<LivingEntity> spatialQuery(Level level,AABB query) {
        long tick=level.getGameTime();
        var current=AnatomySpatialIndex.queryIfCurrent(level,tick,query);
        if(current!=null)return current;
        Map<LivingEntity,GeometryProvider> providers=AnatomyBindingState.providersSnapshot();
        rebuildSpatial(level,providers);
        var rebuilt=AnatomySpatialIndex.queryIfCurrent(level,tick,query);
        if(rebuilt==null)throw new IllegalStateException("Spatial index rebuild lost current tick");
        return rebuilt;
    }
    private static void rebuildSpatial(Level level,Map<LivingEntity,GeometryProvider> providers) {
        List<MaterialBroadphase.Entry<LivingEntity>> entries=new ArrayList<>();
        for(var entry:providers.entrySet()) {
            var support=entry.getKey();if(support.level()!=level)continue;
            GeometryProvider.Snapshot sample;RootFrame root;
            if(AnatomyBindingState.causal(support)) {
                var frame=queryFrame(support).orElse(null);if(frame==null)continue;
                sample=frame.snapshot();root=frame.root();
            } else {
                sample=currentSnapshot(support,entry.getValue()).orElse(null);if(sample==null)continue;
                root=observeRoot(support);
            }
            AABB envelope=spatialEnvelope(sample,root);if(envelope==null)continue;
            entries.add(new MaterialBroadphase.Entry<>(support,envelope));
        }
        for(var rejected:AnatomySpatialIndex.rebuild(level,level.getGameTime(),entries))quarantineEndpoint(rejected.key());
    }
    /** The current S05 caller supplies instantaneous convex bounds; later Q2 stages may supply certified interval envelopes. */
    private static AABB snapshotBounds(GeometryProvider.Snapshot current) {
        return current.pieces().values().stream().map(ConvexBox::bounds).reduce((a,b)->new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ))).orElse(null);
    }
    /**
     * Coarse membership must survive root yaw/gravity rotations that can happen without a setter hook.
     * Every rotated point preserves its Euclidean distance from the captured root, so the root-centered
     * cube enclosing the snapshot's farthest AABB corner is conservative for any subsequent rotation.
     * Exact convexes are still revalidated from the causal frame after the broadphase returns the support.
     */
    private static AABB spatialEnvelope(GeometryProvider.Snapshot current,RootFrame root) {
        var bounds=snapshotBounds(current);if(bounds==null || root==null)return null;
        var origin=root.origin();
        double dx=Math.max(Math.abs(bounds.minX-origin.x),Math.abs(bounds.maxX-origin.x));
        double dy=Math.max(Math.abs(bounds.minY-origin.y),Math.abs(bounds.maxY-origin.y));
        double dz=Math.max(Math.abs(bounds.minZ-origin.z),Math.abs(bounds.maxZ-origin.z));
        double radius=Math.sqrt(dx*dx+dy*dy+dz*dz)+ConservativeSweep.SKIN;
        if(!Double.isFinite(radius))return null;
        return new AABB(origin.x-radius,origin.y-radius,origin.z-radius,
            origin.x+radius,origin.y+radius,origin.z+radius);
    }
    public static boolean replacesPair(Entity body,Entity other) {
        if(!(other instanceof LivingEntity support) || !Platforms.eligible(body,support))return false;
        var provider=AnatomyBindingState.provider(support);
        return currentSnapshot(support,provider).filter(s->!s.pieces().isEmpty()).isPresent();
    }
    /** Resweep after sliding, with block collision validation on every remaining segment. */
    public static Vec3 collide(Entity body,Vec3 requested) {
        if(gravity(body).vertical(requested)>1e-5)clear(body);
        AABB box=body.getBoundingBox();
        var existing=contact(body);
        Map<String,ConservativeSweep.Motion> motions=new TreeMap<>();Map<String,Candidate> identities=new HashMap<>();
        var supportQuery=indexedSupports(body,box.expandTowards(requested).inflate(Platforms.searchMargin(body.level())+4));
        if(!supportQuery.complete()){clear(body);return Vec3.ZERO;}
        for(var support:supportQuery.candidates()) {
            var provider=AnatomyBindingState.provider(support);
            // Own Entity.move is swept against the current causal endpoint only.  A
            // certified support interval is deliberately not consumed here: Q2
            // applies each support material interval once from its own cursor.
            currentSnapshot(support,provider).ifPresent(current->current.pieces().forEach((id,piece)->{
                String key=(existing!=null && existing.support()==support?"0/":"1/")+String.format(java.util.Locale.ROOT,"%010d",support.getId())+"/"+id;
                motions.put(key,new ConservativeSweep.Motion(t->piece,0));identities.put(key,new Candidate(support,id,current.revision(),piece));
            }));
        }
        java.util.function.BiFunction<AABB,Vec3,Vec3> clip=(bounds,delta)->
            Entity.collideBoundingBox(body,delta,bounds,body.level(),body.level().getEntityCollisions(body,bounds.expandTowards(delta)));
        var initial=AnatomySeparation.resolve(box,motions.values().stream().map(m->m.at().apply(0)).toList(),4,128,clip);
        Vec3 correction=initial.displacement();
        if(!initial.separated()) {
            final AABB overlapBox=box;
            Set<LivingEntity> blocked=new HashSet<>();
            motions.forEach((id,m)->{if(m.at().apply(0).overlaps(overlapBox))blocked.add(identities.get(id).entity);});
            blocked.forEach(s->suspend(body,s));motions.keySet().removeIf(id->blocked.contains(identities.get(id).entity));
        }
        var response=TemporalResponse.resolve(box.move(correction),requested,motions,32,256,clip);
        recordSweep(body.level(),motions.size(),response.evaluations(),response.status()==TemporalResponse.Status.ITERATION_LIMIT?1:0);
        Vec3 moved=correction.add(response.displacement());AABB finalBox=box.move(moved);
        var recovery=AnatomySeparation.resolve(finalBox,motions.values().stream().map(m->m.at().apply(1)).toList(),4,128,clip);
        if(recovery.separated()) {moved=moved.add(recovery.displacement());finalBox=box.move(moved);}
        else for(var entry:motions.entrySet())if(entry.getValue().at().apply(1).overlaps(finalBox))
            suspend(body,identities.get(entry.getKey()).entity);
        // A tangential own move or a bounded initial separation can produce no new hit at all.
        // Preserve the prior support first, then prefer actual sweep contacts. Only when the
        // initial separation actually moved the body may a final-valid candidate establish
        // a new anchor; mere proximity to a separated endpoint must never magnetize contact.
        Vec3 retained=existing==null?null:finalSupportNormal(body,existing.support(),existing.piece(),existing.revision(),finalBox);
        if(retained!=null)setContact(body,existing.support(),existing.piece(),existing.revision(),retained);
        else {
            boolean selected=false;
            for(var hit:response.contacts()) {
                var candidate=identities.get(hit.piece());
                if(candidate==null)continue;
                Vec3 normal=finalSupportNormal(body,candidate.entity,candidate.id,candidate.revision,finalBox);
                if(normal==null)continue;
                setContact(body,candidate.entity,candidate.id,candidate.revision,normal);selected=true;break;
            }
            if(!selected && correction.lengthSqr()>1e-20)for(var entry:motions.entrySet()) {
                var candidate=identities.get(entry.getKey());
                if(candidate==null)continue;
                Vec3 normal=finalSupportNormal(body,candidate.entity,candidate.id,candidate.revision,finalBox);
                if(normal==null)continue;
                setContact(body,candidate.entity,candidate.id,candidate.revision,normal);selected=true;break;
            }
            if(!selected && existing!=null)clear(body);
        }
        return moved;
    }
    /** A contact may survive a tangent move without appearing in this response's hit list. */
    private static Vec3 finalSupportNormal(Entity body,LivingEntity support,String pieceId,long revision,AABB finalBox) {
        if(suspended(body,support) || !Platforms.eligible(body,support))return null;
        var provider=AnatomyBindingState.provider(support);var snapshot=currentSnapshot(support,provider).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(pieceId);
        if(piece==null || snapshot.revision()!=revision)return null;
        var separation=piece.separation(finalBox);
        return Math.abs(separation.gap())<=.025 && gravity(body).supports(separation.normal())?separation.normal():null;
    }
    public static void afterMove(Entity body) {
        var c=contact(body);
        if(c==null)return;
        if(!Platforms.eligible(body,c.support)){clear(body);return;}
        var provider=AnatomyBindingState.provider(c.support);
        var snapshot=currentSnapshot(c.support,provider).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(c.piece);
        if(piece==null || snapshot.revision()!=c.revision){clear(body);return;}
        var separation=piece.separation(body.getBoundingBox());
        if(Math.abs(separation.gap())>.025 || !gravity(body).supports(separation.normal())){clear(body);return;}
        int face=piece.closestFace(separation.normal());var normal=piece.faceNormal(face);
        if(!gravity(body).supports(normal)){clear(body);return;}
        Vec3 local=piece.facePoint(face,body.getBoundingBox().getCenter());
        AnatomyContactState.surface(body,new SurfaceContact(c.support.getUUID(),c.revision,c.piece,face,local,normal,body.level().getGameTime()));
        AnatomyContactState.anchor(body,new AnatomyContactState.Anchor(local,piece.point(local),piece,c.support.position(),body.position(),gravity(body),gravity(c.support)));
        body.setOnGround(true);
        body.verticalCollisionBelow=true;
    }
    /** Record one displacement already certified/applied by the S08 material dispatcher. */
    static boolean recordCertifiedTransport(Entity body,LivingEntity support,SurfaceContact surface,RootFrame root,
            Vec3 applied,ConvexBox materialBefore,ConvexBox materialAfter) {
        var contact=contact(body);
        if(body==null || support==null || surface==null || root==null || applied==null || materialBefore==null || materialAfter==null
                || contact==null || contact.support()!=support || !surface.support().equals(support.getUUID())
                || contact.revision()!=surface.revision() || !contact.piece().equals(surface.piece())
                || !Double.isFinite(applied.lengthSqr()))return false;
        if(applied.lengthSqr()<=1e-20)return true;
        positionPassengers(body);
        if(body instanceof net.minecraft.server.level.ServerPlayer player)
            ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,applied);
        for(var passenger:body.getIndirectPassengers())
            if(passenger instanceof net.minecraft.server.level.ServerPlayer player)
                ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,applied);
        var before=TransportLedger.current(body);long tick=body.level().getGameTime();
        var transport=new SupportTransport(tick,before==null?1:before.sequence()+1,root.sequence(),
            before!=null && before.tick()==tick?before.displacement().add(applied):applied,applied);
        TransportLedger.record(body,transport);
        AnatomyTransportReceipts.record(body,contact,surface,root,transport,materialBefore,materialAfter);
        return true;
    }
    public static void carry(Entity body){carry(body,Collections.newSetFromMap(new IdentityHashMap<>()));}
    private static void carry(Entity body,Set<Entity> visiting) {
        if(!simulates(body))return;
        var c=contact(body);var anchor=AnatomyContactState.anchor(body);
        if(c==null || anchor==null)return;
        // Server runtime material intervals own carry. Keeping this endpoint path active in
        // parallel would double-apply ROOT/JOINT work before the causal dispatcher drains it.
        if(!body.level().isClientSide() && AnatomyRuntime.owns(c.support()))return;
        if(!visiting.add(body) || !Platforms.eligible(body,c.support)
            || !gravity(body).equals(anchor.bodyGravity()) || !gravity(c.support).equals(anchor.supportGravity())){clear(body);return;}
        try {
            carry(c.support,visiting);var root=observeRoot(c.support);
            var provider=AnatomyBindingState.provider(c.support);
            var snapshot=currentSnapshot(c.support,provider).orElse(null);
            var piece=snapshot==null?null:snapshot.pieces().get(c.piece);
            if(piece==null || snapshot.revision()!=c.revision || c.support.position().distanceToSqr(anchor.supportOrigin())>16
                    || body.position().distanceToSqr(anchor.bodyOrigin())>16){clear(body);return;}
            var surface=AnatomyContactState.surface(body);
            if(surface==null || !gravity(body).supports(piece.faceNormal(surface.face()))){clear(body);return;}
            Vec3 now=piece.point(anchor.local()),delta=now.subtract(anchor.previous());
            if(delta.lengthSqr()>16){clear(body);return;}
            if(delta.lengthSqr()>1e-16) {
                Entity previous=io.github.r3neer.scalebrews.platform.PlatformPhysics.enter(body);
                Vec3 allowed;
                try {
                    allowed=Entity.collideBoundingBox(body,delta,body.getBoundingBox(),body.level(),body.level().getEntityCollisions(body,body.getBoundingBox().expandTowards(delta)));
                } finally {io.github.r3neer.scalebrews.platform.PlatformPhysics.exit(previous);}
                body.setPos(body.position().add(allowed));
                positionPassengers(body);
                if(body instanceof net.minecraft.server.level.ServerPlayer player)
                    ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,allowed);
                for(var passenger:body.getIndirectPassengers())
                    if(passenger instanceof net.minecraft.server.level.ServerPlayer player)
                        ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,allowed);
                var before=TransportLedger.current(body);long tick=body.level().getGameTime();
                var transport=new SupportTransport(tick,before==null?1:before.sequence()+1,root.sequence(),
                    before!=null && before.tick()==tick?before.displacement().add(allowed):allowed,allowed);
                TransportLedger.record(body,transport);
                AnatomyTransportReceipts.record(body,c,surface,root,transport,anchor.materialBefore(),piece);
                if(allowed.distanceToSqr(delta)>1e-8){clear(body);return;}
            }
            AnatomyContactState.anchor(body,new AnatomyContactState.Anchor(anchor.local(),now,piece,c.support.position(),body.position(),anchor.bodyGravity(),anchor.supportGravity()));
        } finally {visiting.remove(body);}
    }
    /** Root transport can run after passenger ticks; refresh native seats without another physics move. */
    private static void positionPassengers(Entity root) {
        var pending=new ArrayDeque<Entity>();var visited=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        pending.add(root);
        while(!pending.isEmpty()) {
            var vehicle=pending.removeFirst();if(!visited.add(vehicle))continue;
            for(var passenger:vehicle.getPassengers()) {
                vehicle.positionRider(passenger);
                pending.addLast(passenger);
            }
        }
    }
}
