package io.github.r3neer.scalebrews.platform.anatomy;

import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Shared movement integration under explicit proof activation until Gate 1 passes. */
public final class AnatomyMovement {
    private AnatomyMovement() {}
    private static final Set<Level> ACTIVE=Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    // Entity equality reuses network IDs across logical worlds in an integrated process.
    // Weak identity keys keep server and client instances strictly separate.
    private static <K,V> Map<K,V> entityMap(){return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<K,V>makeMap());}
    private static final Map<LivingEntity,GeometryProvider> PROVIDERS=entityMap();
    /** Local binding generation; Runtime supplies causal descriptor in the Q1 production overload. */
    private static final Map<LivingEntity,Long> REGISTRATIONS=entityMap();
    private static final Map<LivingEntity,GeometryProvider.GeometryIdentityDescriptor> DESCRIPTORS=entityMap();
    /** Server-owned material endpoint serial; it advances for a root or joint endpoint change. */
    private record EndpointStamp(long jointSampleTick,RootFrame root,AnatomyPoseHistory.Sample sample,long revision,GeometryProvider.Availability availability) {}
    /** Retains the whole endpoint, including its original authority time, for a material serial. */
    private record EndpointSerial(EndpointStamp stamp,GeometryProvider.CausalEndpoint endpoint,GeometryProvider.Snapshot snapshot,boolean invalidated) {}
    private static final Map<LivingEntity,EndpointSerial> FRAME_SERIALS=entityMap();
    private static final Map<Entity,GravityFrame> GRAVITY=entityMap();
    private static final Map<Entity,Contact> CONTACTS=entityMap();
    private static final Map<Entity,Long> CONTACT_SEQUENCES=entityMap();
    private static final Map<Entity,Anchor> ANCHORS=entityMap();
    private static final Map<Entity,SupportTransport> TRANSPORT=entityMap();
    /** Applied segments survive a contact release until the pose sampler consumes them. */
    private static final Map<Entity,TransportHistory> TRANSPORT_HISTORY=entityMap();
    /** Changes only at an explicit physical lifecycle discontinuity, never on a contact release. */
    private static final Map<Entity,Long> TRANSPORT_GENERATIONS=entityMap();
    private static final Map<Entity,SurfaceContact> SURFACES=entityMap();
    private static final Map<Entity,Set<UUID>> SUSPENDED=entityMap();
    /** Rigid root provenance is independent of 20 Hz joint-pose evaluation. */
    public record RootFrame(long sequence,long tick,Vec3 origin,float yaw,float scale,GravityFrame gravity) {
        public RootFrame {
            if(sequence<0 || origin==null || gravity==null || !Double.isFinite(origin.lengthSqr()) || !Float.isFinite(yaw) || !Float.isFinite(scale) || scale<=0)
                throw new IllegalArgumentException("Invalid root frame");
        }
    }
    private static final class RootHistory {final ArrayDeque<RootFrame> frames=new ArrayDeque<>();}
    private static final Map<LivingEntity,RootHistory> ROOTS=entityMap();
    private static final int TRANSPORT_HISTORY_TICKS=40,TRANSPORT_HISTORY_ENTRIES=64;
    private static final class TransportHistory {final ArrayDeque<SupportTransport> entries=new ArrayDeque<>();}
    /** Package-private cursor result for AuthorityPoseTracker; never a wire/API DTO. */
    static record TransportWindow(boolean contiguous,long latestSequence,Vec3 appliedDelta) {
        TransportWindow {
            if(latestSequence<0 || appliedDelta==null || !Double.isFinite(appliedDelta.lengthSqr()))
                throw new IllegalArgumentException("Invalid transport window");
        }
    }
    /** Geometry broadphase is indexed from actual convex bounds, never support AABBs. */
    private record Cell(int x,int y,int z) {}
    /** Production frames seal causal identity plus full material serial; legacy fixtures retain root/revision only. */
    private record FrameStamp(GeometryProvider.GeometryIdentity identity,RootFrame root,long revision,long registration,long frameSerial) {}
    private record SpatialIndex(long tick,Map<Cell,List<LivingEntity>> cells,List<LivingEntity> overflow,Map<LivingEntity,AABB> bounds,Map<LivingEntity,FrameStamp> frames) {}
    private static final double CELL_SIZE=4;
    private static final long MAX_INDEX_CELLS=4096;
    private static final Map<Level,SpatialIndex> SPATIAL=Collections.synchronizedMap(new WeakHashMap<>());
    public static boolean suspended(Entity body,LivingEntity support) {
        var suspended=SUSPENDED.get(body);
        if(suspended==null || !suspended.contains(support.getUUID()))return false;
        var provider=PROVIDERS.get(support);var shape=currentSnapshot(support,provider).orElse(null);
        if(shape!=null && shape.pieces().values().stream().anyMatch(p->p.overlaps(body.getBoundingBox())))return true;
        suspended.remove(support.getUUID());if(suspended.isEmpty())SUSPENDED.remove(body);return false;
    }
    private static void suspend(Entity body,LivingEntity support) {
        SUSPENDED.computeIfAbsent(body,e->ConcurrentHashMapHolder.newSet()).add(support.getUUID());
        var contact=contact(body);if(contact!=null && contact.support()==support)clear(body);
    }
    private static final class ConcurrentHashMapHolder {static Set<UUID> newSet(){return java.util.concurrent.ConcurrentHashMap.newKeySet();}}
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
    /** Exact prior convex, never a later reconstruction, for a material transport contribution. */
    private record Anchor(Vec3 local,Vec3 previous,ConvexBox materialBefore,Vec3 supportOrigin,Vec3 bodyOrigin,GravityFrame bodyGravity,GravityFrame supportGravity) {
        private Anchor {if(materialBefore==null)throw new IllegalArgumentException("Missing material provenance");}
    }
    public record Contact(LivingEntity support,String piece,long revision,Vec3 normal,long sequence) {}
    public static synchronized void activate(Level level){ACTIVE.add(level);}
    public static synchronized boolean active(Level level){return ACTIVE.contains(level);}
    public static synchronized boolean active(Entity e){return ACTIVE.contains(e.level());}
    /** Server simulates every body; a client predicts only entities it owns locally. */
    public static boolean simulates(Entity body){return !body.level().isClientSide() || body.isLocalInstanceAuthoritative();}
    /** Legacy fixture overload. Runtime must use the causal-descriptor overload once T2 publishes it. */
    public static synchronized void register(LivingEntity support,GeometryProvider provider){
        PROVIDERS.put(support,Objects.requireNonNull(provider));REGISTRATIONS.merge(support,1L,(old,ignored)->Math.incrementExact(old));
        DESCRIPTORS.remove(support);
        FRAME_SERIALS.remove(support);ROOTS.remove(support);clearSupportContacts(support);SPATIAL.remove(support.level());
    }
    /** Runtime causal registration; model and pose provider are catalog identifiers, never model source text. */
    public static synchronized void register(LivingEntity support,GeometryProvider provider,GeometryProvider.GeometryIdentityDescriptor descriptor){
        register(support,provider);descriptor=Objects.requireNonNull(descriptor);
        // A server registration is the authority that allocates its binding identity.
        // Network/client adapters must supply the received nonzero generation instead.
        if(descriptor.bindingGeneration()==0)descriptor=new GeometryProvider.GeometryIdentityDescriptor(descriptor.epoch(),descriptor.revision(),descriptor.model(),descriptor.poseProvider(),registrationGeneration(support));
        DESCRIPTORS.put(support,descriptor);
    }
    /** Local binding generation; changes on every provider rebind for this entity instance. */
    public static synchronized long registrationGeneration(LivingEntity support){return REGISTRATIONS.getOrDefault(support,0L);}
    /** Causal publication survives an unavailable pose so networking can clear stale geometry. */
    public static synchronized Optional<GeometryProvider.PublishedFrame> publishedFrame(LivingEntity support){
        var descriptor=DESCRIPTORS.get(support);var provider=PROVIDERS.get(support);
        if(descriptor==null)return Optional.empty();
        var endpoint=causalEndpoint(support,provider).orElse(null);
        if(endpoint==null)return Optional.empty();
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),descriptor.epoch(),descriptor.revision(),
            descriptor.model(),descriptor.poseProvider(),descriptor.bindingGeneration(),registrationGeneration(support));
        return Optional.of(new GeometryProvider.PublishedFrame(identity,endpoint));
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
    private static Optional<GeometryProvider.CausalEndpoint> causalEndpoint(LivingEntity support,GeometryProvider provider) {
        if(provider==null)return Optional.empty();
        var direct=provider.causalEndpoint(support);
        if(direct.isPresent()) {
            var snapshot=direct.get().availability()==GeometryProvider.Availability.AVAILABLE?provider.sample(support).orElse(null):null;
            return acceptEndpoint(support,direct.get(),snapshot);
        }
        if(!(provider instanceof ModelGeometryProvider model))return Optional.empty();
        var joints=model.authoritativeFrame(support).orElse(null);
        if(joints==null)return Optional.empty();
        var root=observeRoot(support);
        var sample=new AnatomyPoseHistory.Sample(joints.sample().inputs(),root.origin(),root.yaw(),root.scale(),root.gravity());
        var snapshot=sample.inputs().ordinary()?model.sampleAt(support,sample).orElse(null):null;
        return Optional.of(serverEndpoint(support,joints.tick(),root,sample,snapshot));
    }
    private static synchronized GeometryProvider.CausalEndpoint serverEndpoint(LivingEntity support,long jointSampleTick,RootFrame root,AnatomyPoseHistory.Sample sample,GeometryProvider.Snapshot snapshot) {
        var availability=snapshot==null?GeometryProvider.Availability.UNAVAILABLE:GeometryProvider.Availability.AVAILABLE;
        var stamp=new EndpointStamp(jointSampleTick,root,sample,snapshot==null?-1:snapshot.revision(),availability);var old=FRAME_SERIALS.get(support);
        if(old!=null && !old.invalidated() && old.stamp()!=null && old.stamp().equals(stamp))return old.endpoint();
        long next=old==null?1:Math.incrementExact(old.endpoint().frameSerial());
        var endpoint=new GeometryProvider.CausalEndpoint(next,support.level().getGameTime(),jointSampleTick,root,sample,availability);
        if(availability==GeometryProvider.Availability.UNAVAILABLE && old!=null && old.endpoint().availability()==GeometryProvider.Availability.AVAILABLE)
            clearSupportContacts(support);
        FRAME_SERIALS.put(support,new EndpointSerial(stamp,endpoint,snapshot,false));return endpoint;
    }
    /**
     * A wire/provider endpoint may be queried repeatedly.  Same material serial
     * returns the immutable cached object; a changed endpoint without a new serial
     * fails closed rather than silently changing its authority time or TRS.
     */
    private static synchronized Optional<GeometryProvider.CausalEndpoint> acceptEndpoint(LivingEntity support,GeometryProvider.CausalEndpoint endpoint,GeometryProvider.Snapshot snapshot) {
        if(endpoint.availability()==GeometryProvider.Availability.AVAILABLE && (snapshot==null || snapshot.revision()!=DESCRIPTORS.get(support).revision()))return Optional.empty();
        if(endpoint.availability()==GeometryProvider.Availability.UNAVAILABLE && snapshot!=null)return Optional.empty();
        var old=FRAME_SERIALS.get(support);
        if(old==null) {
            FRAME_SERIALS.put(support,new EndpointSerial(null,endpoint,snapshot,false));return Optional.of(endpoint);
        }
        long prior=old.endpoint().frameSerial();
        if(old.invalidated()) {
            if(endpoint.frameSerial()<=prior)return Optional.empty();
        } else if(endpoint.frameSerial()<prior)return Optional.empty();
        else if(endpoint.frameSerial()==prior)return Optional.of(old.endpoint());
        if(endpoint.availability()==GeometryProvider.Availability.UNAVAILABLE && old.endpoint().availability()==GeometryProvider.Availability.AVAILABLE)
            clearSupportContacts(support);
        FRAME_SERIALS.put(support,new EndpointSerial(null,endpoint,snapshot,false));return Optional.of(endpoint);
    }
    /** Instantaneous query geometry. A descriptor must have a causal endpoint; fixture-only legacy bindings retain sample(). */
    private static Optional<GeometryProvider.Snapshot> currentSnapshot(LivingEntity support,GeometryProvider provider) {
        if(DESCRIPTORS.containsKey(support))return queryFrame(support).map(GeometryProvider.QueryFrame::snapshot);
        return provider==null?Optional.empty():provider.sample(support);
    }
    public static synchronized void gravity(Entity body,GravityFrame gravity){GRAVITY.put(body,gravity);}
    public static synchronized GravityFrame gravity(Entity body){return GRAVITY.containsKey(body)?GRAVITY.get(body):GravityFrames.get(body);}
    public static synchronized Contact contact(Entity body){return CONTACTS.get(body);}
    public static synchronized long contactSequence(Entity body){return CONTACT_SEQUENCES.getOrDefault(body,0L);}
    /** Release a material contact and any receipt that could otherwise outlive it. */
    public static synchronized void clear(Entity body){
        CONTACTS.remove(body);ANCHORS.remove(body);SURFACES.remove(body);
        AnatomyTransportReceipts.invalidate(body);
    }
    public static synchronized SurfaceContact surface(Entity body){return SURFACES.get(body);}
    public static synchronized SupportTransport transport(Entity body){return TRANSPORT.get(body);}
    /** Internal lifecycle identity paired with the cursor; it is not protocol state. */
    static synchronized long transportGeneration(Entity body){return TRANSPORT_GENERATIONS.getOrDefault(body,0L);}
    /**
     * Returns every real passive contribution strictly after {@code consumedSequence} when the
     * bounded identity history can prove there is no missing serial. A released contact does not
     * erase this history: its already-applied delta may still sit between two pose samples.
     */
    static synchronized TransportWindow transportSince(Entity body,long consumedSequence) {
        if(consumedSequence<0)throw new IllegalArgumentException("Invalid consumed transport sequence");
        var current=TRANSPORT.get(body);
        long latest=current==null?0:current.sequence();
        var history=TRANSPORT_HISTORY.get(body);
        if(history!=null)pruneTransport(history,body.level().getGameTime());
        if(consumedSequence==latest)return new TransportWindow(true,latest,Vec3.ZERO);
        if(consumedSequence>latest)return new TransportWindow(false,latest,Vec3.ZERO);
        if(history==null || history.entries.isEmpty())return new TransportWindow(false,latest,Vec3.ZERO);
        long expected=consumedSequence+1;Vec3 applied=Vec3.ZERO;
        for(var transport:history.entries)if(transport.sequence()>consumedSequence) {
            if(transport.sequence()!=expected)return new TransportWindow(false,latest,Vec3.ZERO);
            applied=applied.add(transport.appliedDelta());expected++;
        }
        return expected==latest+1?new TransportWindow(true,latest,applied):new TransportWindow(false,latest,Vec3.ZERO);
    }
    private static synchronized void rememberTransport(Entity body,SupportTransport transport) {
        var history=TRANSPORT_HISTORY.computeIfAbsent(body,ignored->new TransportHistory());
        var last=history.entries.peekLast();
        if(last!=null && transport.sequence()<=last.sequence())history.entries.clear();
        history.entries.addLast(transport);pruneTransport(history,body.level().getGameTime());
        while(history.entries.size()>TRANSPORT_HISTORY_ENTRIES)history.entries.removeFirst();
    }
    private static void pruneTransport(TransportHistory history,long tick) {
        while(history.entries.peekFirst()!=null && history.entries.peekFirst().tick()<tick-TRANSPORT_HISTORY_TICKS+1)
            history.entries.removeFirst();
    }
    private static void forgetTransport(Entity body) {TRANSPORT.remove(body);TRANSPORT_HISTORY.remove(body);}
    /** A teleport/removal invalidates a body's own anchor but may retain already-applied carry. */
    private static void invalidateBody(Entity body,boolean discardTransport) {
        clear(body);
        TRANSPORT_GENERATIONS.merge(body,1L,Long::sum);
        if(discardTransport)forgetTransport(body);
    }
    public static synchronized boolean confirm(Entity body,LivingEntity support,SurfaceContact surface) {
        if(!active(body) || surface==null || !support.getUUID().equals(surface.support()) || !Platforms.eligible(body,support))return false;
        var provider=PROVIDERS.get(support);var snapshot=currentSnapshot(support,provider).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(surface.piece());
        if(piece==null || snapshot.revision()!=surface.revision() || surface.face()<0 || surface.face()>5)return false;
        Vec3 point=piece.point(surface.localPoint()),normal=piece.faceNormal(surface.face());
        // The authoritative normal belongs to the packet tick. Recompute it from the newest
        // accepted pose instead of rejecting a valid contact while the support is animating.
        if(!gravity(body).supports(normal))return false;
        setContact(body,support,surface.piece(),surface.revision(),normal);
        SURFACES.put(body,new SurfaceContact(surface.support(),surface.revision(),surface.piece(),surface.face(),surface.localPoint(),normal,surface.tick()));
        ANCHORS.put(body,new Anchor(surface.localPoint(),point,piece,support.position(),body.position(),gravity(body),gravity(support)));
        return true;
    }
    public static boolean supported(Entity body) {
        var c=contact(body);if(c==null || suspended(body,c.support()) || !Platforms.eligible(body,c.support()))return false;
        var provider=PROVIDERS.get(c.support());var snapshot=currentSnapshot(c.support(),provider).orElse(null);
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
        var provider=PROVIDERS.get(contact.support());var snapshot=currentSnapshot(contact.support(),provider).orElse(null);
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
        for(var candidate:candidates(body,box))if(candidate.box.overlaps(box))return false;
        return true;
    }
    public static void tick(Level level) {
        if(!ACTIVE.contains(level))return;
        Map<LivingEntity,GeometryProvider> providers;List<Entity> bodies;
        synchronized(PROVIDERS){providers=new IdentityHashMap<>(PROVIDERS);}
        synchronized(CONTACTS){bodies=List.copyOf(CONTACTS.keySet());}
        for(var entry:providers.entrySet())if(entry.getKey().level()==level) {
            observeRoot(entry.getKey());entry.getValue().tick(entry.getKey(),level.getGameTime());
        }
        rebuildSpatial(level,providers);
        for(var body:bodies)if(body.level()==level)carry(body);
    }
    /** Client-side pose cache update without moving server-owned observer entities. */
    public static void tickGeometry(Level level) {
        if(!ACTIVE.contains(level))return;
        Map<LivingEntity,GeometryProvider> providers;
        synchronized(PROVIDERS){providers=new IdentityHashMap<>(PROVIDERS);}
        for(var entry:providers.entrySet())if(entry.getKey().level()==level)
            entry.getValue().tick(entry.getKey(),level.getGameTime());
    }
    public static synchronized void deactivate(Level level){
        ACTIVE.remove(level);PROVIDERS.keySet().removeIf(e->e.level()==level);
        REGISTRATIONS.keySet().removeIf(e->e.level()==level);
        DESCRIPTORS.keySet().removeIf(e->e.level()==level);
        FRAME_SERIALS.keySet().removeIf(e->e.level()==level);
        GRAVITY.keySet().removeIf(e->e.level()==level);CONTACTS.keySet().removeIf(e->e.level()==level);
        CONTACT_SEQUENCES.keySet().removeIf(e->e.level()==level);ROOTS.keySet().removeIf(e->e.level()==level);
        ANCHORS.keySet().removeIf(e->e.level()==level);TRANSPORT.keySet().removeIf(e->e.level()==level);
        TRANSPORT_HISTORY.keySet().removeIf(e->e.level()==level);
        TRANSPORT_GENERATIONS.keySet().removeIf(e->e.level()==level);
        SURFACES.keySet().removeIf(e->e.level()==level);
        SUSPENDED.keySet().removeIf(e->e.level()==level);
        METRICS.remove(level);
        SPATIAL.remove(level);
    }
    private static synchronized Contact setContact(Entity body,LivingEntity support,String piece,long revision,Vec3 normal) {
        var old=CONTACTS.get(body);
        boolean same=old!=null && old.support()==support && old.revision()==revision && old.piece().equals(piece);
        long sequence=same?old.sequence():Math.incrementExact(CONTACT_SEQUENCES.getOrDefault(body,0L));
        if(!same)CONTACT_SEQUENCES.put(body,sequence);
        var contact=new Contact(support,piece,revision,normal,sequence);CONTACTS.put(body,contact);return contact;
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
        clearSupportContacts(support);ROOTS.remove(support);SPATIAL.remove(support.level());
    }
    /**
     * An accepted unavailable pose keeps its endpoint watermark, but every body
     * anchored to that support must stop immediately.  Client lifecycle code uses
     * this without deactivating the whole level or resetting binding serials.
     */
    public static synchronized void invalidateSupport(LivingEntity support) {
        clearSupportContacts(support);SPATIAL.remove(support.level());
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
        for(var entry:new ArrayList<>(CONTACTS.entrySet()))if(entry.getValue().support()==support)
            // A support discontinuity is also a body discontinuity, but the body may need its
            // last applied segment for the next pose sample to remove passive motion exactly once.
            invalidateBody(entry.getKey(),false);
    }
    private record Candidate(LivingEntity entity,String id,long revision,ConvexBox box) {}
    public record SweepContact(LivingEntity support,String piece,long revision,ConservativeSweep.Result result) {}
    /** Dedicated temporal query, deliberately separate from navigation and vanilla noCollision. */
    public static SweepContact sweep(Entity body,Vec3 displacement,int iterations) {
        if(!active(body))return null;
        AABB swept=body.getBoundingBox().expandTowards(displacement);
        var supports=new ArrayList<>(indexedSupports(body,swept.inflate(Platforms.searchMargin(body.level())+4)));
        supports.sort(Comparator.comparingInt(Entity::getId));
        SweepContact best=null;
        int queried=0,evaluations=0,exhausted=0;
        for(var support:supports) {
            if(suspended(body,support))continue;
            var provider=PROVIDERS.get(support);
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
        Selection best=null;
        for(var candidate:candidates(observer,new AABB(start,end).inflate(.001))) {
            var hit=candidate.box.raycast(start,end);
            if(hit==null || best!=null && hit.fraction()>=best.fraction()-1e-9)continue;
            var contact=new SurfaceContact(candidate.entity.getUUID(),candidate.revision,candidate.id,hit.face(),hit.localPoint(),hit.normal(),observer.level().getGameTime());
            best=new Selection(candidate.entity,contact,start.add(end.subtract(start).scale(hit.fraction())),hit.fraction());
        }
        return best;
    }
    private static List<Candidate> candidates(Entity body,AABB swept) {
        List<Candidate> result=new ArrayList<>();
        for(var support:indexedSupports(body,swept.inflate(Platforms.searchMargin(body.level())))) {
            GeometryProvider provider=PROVIDERS.get(support);
            if(provider==null)continue;
            currentSnapshot(support,provider).ifPresent(snapshot->snapshot.pieces().forEach((id,box)->{
                if(box.bounds().inflate(.001).intersects(swept))result.add(new Candidate(support,id,snapshot.revision(),box));
            }));
        }
        result.sort(Comparator.comparingInt((Candidate c)->c.entity.getId()).thenComparing(Candidate::id));
        return result;
    }
    private static List<LivingEntity> indexedSupports(Entity body,AABB query) {
        var index=spatial(body.level());var seen=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());
        int minX=cell(query.minX),maxX=cell(query.maxX),minY=cell(query.minY),maxY=cell(query.maxY),minZ=cell(query.minZ),maxZ=cell(query.maxZ);
        // Movement requests are bounded by vanilla, but an invalid/extreme caller must not
        // turn a spatial lookup into an unbounded cell walk or silently miss a limb.
        long cellCount=cellCount(minX,maxX,minY,maxY,minZ,maxZ);
        if(cellCount>MAX_INDEX_CELLS)seen.addAll(index.bounds().keySet());
        else for(long x=minX;x<=maxX;x++)for(long y=minY;y<=maxY;y++)for(long z=minZ;z<=maxZ;z++)
            seen.addAll(index.cells().getOrDefault(new Cell((int)x,(int)y,(int)z),List.of()));
        seen.addAll(index.overflow());
        var result=new ArrayList<LivingEntity>();
        for(var support:seen)if(support.level()==body.level() && Platforms.eligible(body,support) && !suspended(body,support)
                && index.bounds().getOrDefault(support,new AABB(0,0,0,0,0,0)).intersects(query))result.add(support);
        result.sort(Comparator.comparingInt(Entity::getId));return result;
    }
    private static int cell(double coordinate) {
        if(!Double.isFinite(coordinate))return coordinate<0?Integer.MIN_VALUE:Integer.MAX_VALUE;
        double value=Math.floor(coordinate/CELL_SIZE);
        if(value<=Integer.MIN_VALUE)return Integer.MIN_VALUE;
        if(value>=Integer.MAX_VALUE)return Integer.MAX_VALUE;
        return (int)value;
    }
    private static long cellCount(int minX,int maxX,int minY,int maxY,int minZ,int maxZ) {
        long x=(long)maxX-minX+1,y=(long)maxY-minY+1,z=(long)maxZ-minZ+1;
        if(x<=0 || y<=0 || z<=0 || x>MAX_INDEX_CELLS || y>MAX_INDEX_CELLS || z>MAX_INDEX_CELLS)return Long.MAX_VALUE;
        if(x>MAX_INDEX_CELLS/y || x*y>MAX_INDEX_CELLS/z)return Long.MAX_VALUE;return x*y*z;
    }
    private static SpatialIndex spatial(Level level) {
        var current=SPATIAL.get(level);
        if(current!=null && current.tick()==level.getGameTime() && current.frames().entrySet().stream().allMatch(entry->{
            var support=entry.getKey();var provider=PROVIDERS.get(support);var frame=currentFrameStamp(support,provider);
            return frame!=null && entry.getValue().equals(frame);
        }))return current;
        Map<LivingEntity,GeometryProvider> providers;
        synchronized(PROVIDERS){providers=new IdentityHashMap<>(PROVIDERS);}
        return rebuildSpatial(level,providers);
    }
    private static SpatialIndex rebuildSpatial(Level level,Map<LivingEntity,GeometryProvider> providers) {
        Map<LivingEntity,AABB> bounds=new IdentityHashMap<>();Map<LivingEntity,FrameStamp> frames=new IdentityHashMap<>();Map<Cell,List<LivingEntity>> cells=new HashMap<>();List<LivingEntity> overflow=new ArrayList<>();
        for(var entry:providers.entrySet()) {
            var support=entry.getKey();if(support.level()!=level)continue;
            var sample=currentSnapshot(support,entry.getValue()).orElse(null);var frame=currentFrameStamp(support,entry.getValue());
            if(sample==null || frame==null)continue;
            AABB envelope=snapshotBounds(sample);if(envelope==null)continue;
            bounds.put(support,envelope);frames.put(support,frame);
            int minX=cell(envelope.minX),maxX=cell(envelope.maxX),minY=cell(envelope.minY),maxY=cell(envelope.maxY),minZ=cell(envelope.minZ),maxZ=cell(envelope.maxZ);
            if(cellCount(minX,maxX,minY,maxY,minZ,maxZ)>MAX_INDEX_CELLS){overflow.add(support);continue;}
            for(long x=minX;x<=maxX;x++)for(long y=minY;y<=maxY;y++)for(long z=minZ;z<=maxZ;z++)
                cells.computeIfAbsent(new Cell((int)x,(int)y,(int)z),ignored->new ArrayList<>()).add(support);
        }
        cells.replaceAll((ignored,entries)->List.copyOf(entries));
        var index=new SpatialIndex(level.getGameTime(),Map.copyOf(cells),List.copyOf(overflow),Map.copyOf(bounds),Map.copyOf(frames));SPATIAL.put(level,index);return index;
    }
    private static FrameStamp currentFrameStamp(LivingEntity support,GeometryProvider provider) {
        if(provider==null)return null;
        if(DESCRIPTORS.containsKey(support))return queryFrame(support).map(frame->new FrameStamp(frame.identity(),frame.root(),frame.snapshot().revision(),registrationGeneration(support),frame.endpoint().frameSerial())).orElse(null);
        var sample=provider.sample(support).orElse(null);
        return sample==null?null:new FrameStamp(null,observeRoot(support),sample.revision(),registrationGeneration(support),0);
    }
    /** The Q1 index contains only the instantaneous convex endpoint. Temporal envelopes belong to Q2 intervals. */
    private static AABB snapshotBounds(GeometryProvider.Snapshot current) {
        return current.pieces().values().stream().map(ConvexBox::bounds).reduce((a,b)->new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ))).orElse(null);
    }
    public static boolean replacesPair(Entity body,Entity other) {
        if(!(other instanceof LivingEntity support) || !Platforms.eligible(body,support))return false;
        var provider=PROVIDERS.get(support);
        return currentSnapshot(support,provider).filter(s->!s.pieces().isEmpty()).isPresent();
    }
    /** Resweep after sliding, with block collision validation on every remaining segment. */
    public static Vec3 collide(Entity body,Vec3 requested) {
        if(gravity(body).vertical(requested)>1e-5)clear(body);
        AABB box=body.getBoundingBox();
        var existing=contact(body);
        Map<String,ConservativeSweep.Motion> motions=new TreeMap<>();Map<String,Candidate> identities=new HashMap<>();
        for(var support:indexedSupports(body,box.expandTowards(requested).inflate(Platforms.searchMargin(body.level())+4))) {
            var provider=PROVIDERS.get(support);
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
        // A tangential own move can produce no new hit at all. Preserve the prior
        // material support only after validating it against the post-recovery box;
        // otherwise choose the first stable new supporting event that is also final-valid.
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
            if(!selected && existing!=null)clear(body);
        }
        return moved;
    }
    /** A contact may survive a tangent move without appearing in this response's hit list. */
    private static Vec3 finalSupportNormal(Entity body,LivingEntity support,String pieceId,long revision,AABB finalBox) {
        if(suspended(body,support) || !Platforms.eligible(body,support))return null;
        var provider=PROVIDERS.get(support);var snapshot=currentSnapshot(support,provider).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(pieceId);
        if(piece==null || snapshot.revision()!=revision)return null;
        var separation=piece.separation(finalBox);
        return Math.abs(separation.gap())<=.025 && gravity(body).supports(separation.normal())?separation.normal():null;
    }
    public static void afterMove(Entity body) {
        var c=contact(body);
        if(c==null)return;
        if(!Platforms.eligible(body,c.support)){clear(body);return;}
        var provider=PROVIDERS.get(c.support);
        var snapshot=currentSnapshot(c.support,provider).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(c.piece);
        if(piece==null || snapshot.revision()!=c.revision){clear(body);return;}
        var separation=piece.separation(body.getBoundingBox());
        if(Math.abs(separation.gap())>.025 || !gravity(body).supports(separation.normal())){clear(body);return;}
        int face=piece.closestFace(separation.normal());var normal=piece.faceNormal(face);
        if(!gravity(body).supports(normal)){clear(body);return;}
        Vec3 local=piece.facePoint(face,body.getBoundingBox().getCenter());
        SURFACES.put(body,new SurfaceContact(c.support.getUUID(),c.revision,c.piece,face,local,normal,body.level().getGameTime()));
        ANCHORS.put(body,new Anchor(local,piece.point(local),piece,c.support.position(),body.position(),gravity(body),gravity(c.support)));
        body.setOnGround(true);
        body.verticalCollisionBelow=true;
    }
    public static void carry(Entity body){carry(body,Collections.newSetFromMap(new IdentityHashMap<>()));}
    private static void carry(Entity body,Set<Entity> visiting) {
        var c=contact(body);var anchor=ANCHORS.get(body);
        if(c==null || anchor==null)return;
        if(!visiting.add(body) || !Platforms.eligible(body,c.support)
            || !gravity(body).equals(anchor.bodyGravity) || !gravity(c.support).equals(anchor.supportGravity)){clear(body);return;}
        try {
            carry(c.support,visiting);var root=observeRoot(c.support);
            var provider=PROVIDERS.get(c.support);
            var snapshot=currentSnapshot(c.support,provider).orElse(null);
            var piece=snapshot==null?null:snapshot.pieces().get(c.piece);
            if(piece==null || snapshot.revision()!=c.revision || c.support.position().distanceToSqr(anchor.supportOrigin)>16
                    || body.position().distanceToSqr(anchor.bodyOrigin)>16){clear(body);return;}
            var surface=SURFACES.get(body);
            if(surface==null || !gravity(body).supports(piece.faceNormal(surface.face()))){clear(body);return;}
            Vec3 now=piece.point(anchor.local),delta=now.subtract(anchor.previous);
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
                var before=TRANSPORT.get(body);long tick=body.level().getGameTime();
                var transport=new SupportTransport(tick,before==null?1:before.sequence()+1,root.sequence(),
                    before!=null && before.tick()==tick?before.displacement().add(allowed):allowed,allowed);
                TRANSPORT.put(body,transport);
                rememberTransport(body,transport);
                AnatomyTransportReceipts.record(body,c,surface,root,transport,anchor.materialBefore,piece);
                if(allowed.distanceToSqr(delta)>1e-8){clear(body);return;}
            }
            ANCHORS.put(body,new Anchor(anchor.local,now,piece,c.support.position(),body.position(),anchor.bodyGravity,anchor.supportGravity));
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
