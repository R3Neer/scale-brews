package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyAnatomyCatalogMigration;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-owned binding/pose lifecycle. Opt-in preparation runtime until the acceptance gate passes. */
public final class AnatomyRuntime {
    /** A quiet valid contact must refresh before the client stale-window, without changing physics. */
    public static final long CONTACT_HEARTBEAT_TICKS=20;
    private AnatomyRuntime() {}
    private static final Map<MinecraftServer,State> STATES=Collections.synchronizedMap(new WeakHashMap<>());
    /** Entity/connection equality is network-ID based; lifecycle state needs object identity. */
    private static <K,V> Map<K,V> weakIdentityMap() {
        return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<K,V>makeMap());
    }
    /** A server allocation, never an entity-instance/local adapter counter. */
    private record Active(WorldAnatomyCatalog.Binding binding,ModelGeometryProvider provider,long bindingGeneration) {}
    /**
     * Server-authoritative pose already ticked for an active runtime binding.
     * This is deliberately a read-only observation seam for acceptance tests
     * and diagnostics: it never advances {@link AuthorityPoseTracker}, renders,
     * or evaluates model geometry.
     */
    public record AuthoritativePose(UUID entity,long serverTick,PoseEngine.Inputs inputs) {}
    private static final class State {
        final WorldAnatomyCatalog catalog;
        State(MinecraftServer server){catalog=new WorldAnatomyCatalog(AnatomyNetworking.revision(server));}
        final Map<LivingEntity,Active> entities=weakIdentityMap();
        final Map<ServerPlayer,Long> sent=weakIdentityMap();
        final Map<ServerPlayer,Map<UUID,PublishedContact>> contacts=weakIdentityMap();
        /** One bounded active-window ledger per live recipient; retired UUIDs are never retained. */
        final Map<ServerPlayer,TrackingGenerationLedger> trackingGenerations=weakIdentityMap();
        /** Monotonic for this server epoch; survives reload/reset and object reuse. */
        long nextBindingGeneration=1;
        long allocateBindingGeneration() {
            if(nextBindingGeneration<1)throw new IllegalStateException("Anatomical binding generation overflow");
            return nextBindingGeneration++;
        }
    }
    private record ContactKey(UUID support,long revision,String piece,int face,Vec3 local,Vec3 normal) {}
    private record PublishedContact(ContactKey key,long generation,long sequence,long sentTick) {}
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server->{if(Boolean.getBoolean("scalebrews.anatomyRuntime"))start(server);});
        ServerLifecycleEvents.SERVER_STOPPED.register(AnatomyRuntime::stop);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,resources,success)->{
            if(success && STATES.containsKey(server)) {
                try{reload(server);}catch(RuntimeException invalid){ScaleBrews.LOGGER.error("Anatomical reload rejected; accepted catalog retained",invalid);}
            }
        });
        // New entities are the only steady-state source of new canonical bindings. Existing entities
        // are enumerated once at start/reload by reset(), never on every tick.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var state=STATES.get(level.getServer());
            if(state!=null && entity instanceof LivingEntity living)bindIfEligible(state,living);
        });
        EntityTrackingEvents.START_TRACKING.register((entity,player)->{
            var state=STATES.get(player.level().getServer());
            if(state!=null && generation(state,player,entity.getUUID())<1)return;
            if(entity instanceof LivingEntity living)send(living,player);
            contact(entity,player,true);
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity,player)->{
            var state=STATES.get(player.level().getServer());
            if(state!=null) {
                clearContact(state,entity,player);
                state.contacts.computeIfPresent(player,(recipient,known)->{known.remove(entity.getUUID());return known;});
                var generations=state.trackingGenerations.get(player);
                if(generations!=null)generations.release(entity.getUUID());
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var state=STATES.get(server);
            if(state!=null) {
                generation(state,handler.player,handler.player.getUUID()); // explicit self-tracking window for this connection
                catalog(state,handler.player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            var state=STATES.get(server);if(state!=null){state.sent.remove(handler.player);state.contacts.remove(handler.player);state.trackingGenerations.remove(handler.player);}
            AnatomyTransportReceipts.disconnect(handler.player);
            AnatomyMovementReference.disconnect(handler.player);
        });
    }
    public static void start(MinecraftServer server) {
        if(STATES.containsKey(server))return;
        var state=new State(server);state.catalog.reload(server.getResourceManager());
        STATES.put(server,state);reset(server,state);
    }
    /** Preparation harness supplies released data only at this explicit migration seam, never as live authority. */
    public static void startPrepared(MinecraftServer server,Map<String,ModelGeometry> models,Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles) {
        if(STATES.containsKey(server))throw new IllegalStateException("Anatomical runtime already running");
        var state=new State(server);state.catalog.replace(models,LegacyAnatomyCatalogMigration.bindings(profiles));
        STATES.put(server,state);reset(server,state);
    }
    public static void reload(MinecraftServer server) {
        reload(server,server.getResourceManager());
    }
    /**
     * Single transactional runtime reload path. Package visibility permits failure injection without
     * changing the server's installed packs; production always calls the public overload above.
     */
    static void reload(MinecraftServer server,net.minecraft.server.packs.resources.ResourceManager resources) {
        Objects.requireNonNull(server,"server");Objects.requireNonNull(resources,"resources");
        var state=STATES.get(server);if(state==null)return;
        state.catalog.reload(resources); // Candidate fully validates and swaps before any live runtime state is reset.
        reset(server,state);
    }
    private static void reset(MinecraftServer server,State state) {
        MaterialIntervalRuntime.clear(server);MaterialPhysicsRuntime.clear(server);
        state.entities.clear();state.sent.clear();state.contacts.clear();state.trackingGenerations.clear();
        AnatomyTransportReceipts.clear(server);AnatomyMovementReference.clear(server);
        var snapshot=state.catalog.snapshot();
        AnatomyNetworking.acceptCatalogRevision(server,snapshot.revision());
        // Materialize the immutable packet list now so the first player does no catalog preparation work.
        state.catalog.preparedPackets(AnatomyNetworking.epoch(server));
        for(var level:server.getAllLevels()){
            AnatomyMovement.deactivate(level);AnatomyMovement.activate(level);prepareExisting(level,state);
        }
        // catalog(...) may disconnect an incompatible recipient, mutating the live player list.
        for(var player:new ArrayList<>(server.getPlayerList().getPlayers())) {
            generation(state,player,player.getUUID()); // reset/reload starts a fresh explicit self window
            catalog(state,player);
        }
    }
    /** One-time start/reload enumeration; explicitly not part of steady tick orchestration. */
    private static void prepareExisting(ServerLevel level,State state) {
        for(var entity:level.getAllEntities()) {
            // START_TRACKING may have happened before AnatomyRuntime started/reloaded. Reconstruct that
            // already-authorized vanilla window exactly once here instead of letting a later consumer mint it.
            for(var player:PlayerLookup.tracking(entity))generation(state,player,entity.getUUID());
            if(entity instanceof LivingEntity living)bindIfEligible(state,living);
        }
    }
    private static void bindIfEligible(State state,LivingEntity living) {
        if(state==null || living==null || living.isRemoved() || state.entities.containsKey(living))return;
        var snapshot=state.catalog.snapshot();
        var binding=snapshot.bindings().get(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()));
        if(binding==null || binding.selection().policy().enabled().orElse(true)==false)return;
        var selection=binding.selection();
        var provider=new ModelGeometryProvider(binding.model(),binding.poses(),binding.root(),selection.geometry().filter(),snapshot.revision())
            .serverDriven(binding::supportsAuthorityPose);
        long bindingGeneration=state.allocateBindingGeneration();
        state.entities.put(living,new Active(binding,provider,bindingGeneration));
        AnatomyMovement.register(living,provider,new GeometryProvider.GeometryIdentityDescriptor(
            AnatomyNetworking.epoch(living.level().getServer()),snapshot.revision(),selection.geometry().model(),selection.pose().engine(),selection.rootTransform(),bindingGeneration),selection);
    }
    public static void stop(MinecraftServer server) {
        MaterialIntervalRuntime.clear(server);MaterialPhysicsRuntime.clear(server);
        AnatomyTransportReceipts.clear(server);AnatomyMovementReference.clear(server);
        if(STATES.remove(server)!=null)for(var level:server.getAllLevels())AnatomyMovement.deactivate(level);
    }
    /** Server half of {@link AnatomySession}: running catalog session, not an own-support test. */
    public static boolean owns(Entity entity) {
        var server=entity.level().getServer();
        boolean active=AnatomyMovement.active(entity);
        boolean state=server!=null && STATES.containsKey(server);
        if(active && !state)
            ScaleBrews.LOGGER.info("S25 owns diagnostic: entity={} removed={} serverNull={} runtimeState={} level={} server={}",
                entity.getUUID(),entity.isRemoved(),server==null,state,entity.level(),server);
        return server!=null && state && active;
    }
    /**
     * Internal support-binding ownership, intentionally narrower than {@link #owns(Entity)}.
     * A prepared/running session must not suppress the fixture/local fallback for a manually registered support;
     * only the exact live provider/descriptor installed by this runtime has certified material intervals to own its carry.
     */
    static boolean owns(LivingEntity support) {
        var server=support.level().getServer();
        var state=server==null?null:STATES.get(server);
        if(state==null)return false;
        var active=state.entities.get(support);
        var descriptor=AnatomyBindingState.descriptor(support);
        return active!=null && AnatomyBindingState.provider(support)==active.provider()
            && descriptor!=null && descriptor.bindingGeneration()==active.bindingGeneration();
    }
    /**
     * Canonical default selection for this entity type in the accepted server catalog.
     * This is intentionally independent of provider executability: a manual/core fixture may
     * borrow policy authority without becoming an AnatomyRuntime causal binding.
     */
    public static Optional<CollisionBinding> catalogBinding(LivingEntity support) {
        if(support==null)return Optional.empty();
        var server=support.level().getServer();
        var state=server==null?null:STATES.get(server);
        if(state==null)return Optional.empty();
        return state.catalog.snapshot().catalog().resolve(BuiltInRegistries.ENTITY_TYPE.getKey(support.getType()),Map.of());
    }

    public static boolean ready(Entity entity) {
        var server=entity.level().getServer();
        if(server==null)return false;
        var state=STATES.get(server);
        return owns(entity) && state!=null && state.catalog.snapshot().revision()>=0;
    }
    /**
     * Exact immutable authority endpoint for this live binding. It never
     * samples a provider or reads current entity TRS; callers must not replace
     * it with client/render animation.
     */
    public static Optional<ModelGeometryProvider.AuthoritativeFrame> authoritativeFrame(LivingEntity entity) {
        if(entity==null)return Optional.empty();
        var server=entity.level().getServer();
        if(server==null)return Optional.empty();
        var state=STATES.get(server);
        if(state==null)return Optional.empty();
        var active=state.entities.get(entity);
        if(active==null)return Optional.empty();
        return active.provider.authoritativeFrame(entity);
    }
    /** Compatibility observation of the inputs within {@link #authoritativeFrame}. */
    public static Optional<AuthoritativePose> authoritativePose(LivingEntity entity) {
        return authoritativeFrame(entity).map(frame->new AuthoritativePose(entity.getUUID(),frame.tick(),frame.sample().inputs()));
    }
    /**
     * Identity-only gate for queued work. False means the handle belongs to a previous binding/lifecycle and
     * may be discarded without invalidating the currently active support.
     */
    public static boolean acceptsIntervalIdentity(LivingEntity entity,GeometryProvider.MotionIntervalHandle handle) {
        if(entity==null || handle==null || entity.isRemoved())return false;
        var server=entity.level().getServer();if(server==null)return false;
        var state=STATES.get(server);if(state==null)return false;
        var active=state.entities.get(entity);if(active==null)return false;
        var identity=handle.identity();var selection=active.binding().selection();
        return identity.matches(entity)
            && identity.bindingGeneration()==active.bindingGeneration()
            && identity.localRegistrationGeneration()==AnatomyMovement.registrationGeneration(entity)
            && identity.epoch().equals(AnatomyNetworking.epoch(server))
            && identity.revision()==state.catalog.snapshot().revision()
            && identity.model().equals(selection.geometry().model())
            && identity.poseProvider().equals(selection.pose().engine())
            && identity.rootProvider().equals(selection.rootTransform());
    }
    /** Provider certification seam for a replay-fenced S06 handle; S07 consumes this without rediscovering identity. */
    public static Optional<GeometryProvider.MotionSnapshot> interval(LivingEntity entity,GeometryProvider.MotionIntervalHandle handle) {
        if(!acceptsIntervalIdentity(entity,handle))return Optional.empty();
        var state=STATES.get(entity.level().getServer());
        var active=state==null?null:state.entities.get(entity);
        return active==null?Optional.empty():active.provider.interval(entity,handle);
    }
    /** Canonical one-shot S06 queue for the later S07 dispatcher. */
    public static List<MaterialIntervalRuntime.Pending> pollIntervals(ServerLevel level) {
        return MaterialIntervalRuntime.poll(level);
    }
    /** Called before the shared core's once-per-level pose/carry tick, never from render queries. */
    public static void prepare(ServerLevel level) {
        var state=STATES.get(level.getServer());if(state==null)return;
        // New bindings arrive through ENTITY_LOAD. Steady tick only retires dead participants.
        state.entities.entrySet().removeIf(e->!e.getKey().isAlive() || e.getKey().isRemoved());
    }
    public static void publish(ServerLevel level) {
        var state=STATES.get(level.getServer());if(state==null)return;
        var activeEntities=new ArrayList<>(state.entities.keySet());
        activeEntities.sort(Comparator.comparing((LivingEntity e)->e.getUUID()).thenComparingInt(LivingEntity::getId));
        for(var entity:activeEntities)if(entity.level()==level && entity.isAlive()) {
            MaterialIntervalRuntime.observe(entity);
            var players=new HashSet<>(PlayerLookup.tracking(entity));
            if(entity instanceof ServerPlayer player)players.add(player);
            for(var player:players)send(entity,player);
        }
        // Contact publication is participant-local: current material contacts plus previously-published
        // non-null contacts that may need one clearing packet. Null tracking watermarks are retained for
        // sequence causality but never heartbeat-scanned.
        var bodies=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        for(var body:AnatomyContactState.contactBodies())if(body.level()==level && !body.isRemoved())bodies.add(body);
        for(var recipientEntry:new ArrayList<>(state.contacts.entrySet())) {
            var recipient=recipientEntry.getKey();
            if(recipient==null || recipient.level()!=level)continue;
            for(var known:recipientEntry.getValue().entrySet())if(known.getValue()!=null && known.getValue().key()!=null) {
                var body=level.getEntity(known.getKey());
                if(body!=null && !body.isRemoved())bodies.add(body);
            }
        }
        for(var body:bodies) {
            var interested=new HashSet<>(PlayerLookup.tracking(body));
            if(body instanceof ServerPlayer player)interested.add(player);
            for(var player:interested)contact(body,player,false);
        }
    }
    private static void contact(Entity body,ServerPlayer recipient,boolean force) {
        var state=STATES.get(recipient.level().getServer());if(state==null || !catalog(state,recipient)
                || !ServerPlayNetworking.canSend(recipient,AnatomyContactPayload.TYPE))return;
        var surface=AnatomyMovement.surface(body);
        ContactKey key=surface==null?null:new ContactKey(surface.support(),surface.revision(),surface.piece(),surface.face(),surface.localPoint(),surface.normal());
        var map=state.contacts.computeIfAbsent(recipient,p->new HashMap<>());var old=map.get(body.getUUID());
        long tick=body.level().getGameTime();
        if(!force && Objects.equals(old==null?null:old.key(),key) && old!=null && tick-old.sentTick()<CONTACT_HEARTBEAT_TICKS)return;
        long generation=currentGeneration(state,recipient,body.getUUID());if(generation<1)return;
        long sequence=old==null || old.generation()!=generation?1:old.sequence()+1;var epoch=AnatomyNetworking.epoch(recipient.level().getServer());long revision=state.catalog.snapshot().revision();
        AnatomyContactPayload payload;
        if(surface==null)payload=AnatomyContactPayload.clear(epoch,revision,body.level().dimension().identifier(),body.getId(),body.getUUID(),generation,sequence,tick);
        else {
            var support=recipient.level().getEntity(surface.support());
            if(!(support instanceof LivingEntity living))return;
            var supportDescriptor=AnatomyBindingState.descriptor(living);
            if(supportDescriptor==null || supportDescriptor.bindingGeneration()<1)return;
            payload=new AnatomyContactPayload(epoch,revision,body.level().dimension().identifier(),body.getId(),body.getUUID(),generation,sequence,tick,
                living.getId(),living.getUUID(),supportDescriptor.bindingGeneration(),surface.piece(),surface.face(),surface.localPoint(),surface.normal());
        }
        ServerPlayNetworking.send(recipient,payload);map.put(body.getUUID(),new PublishedContact(key,generation,sequence,tick));
    }
    /** Opens or reuses an authority window only from an explicit server tracking transition. */
    private static long generation(State state,ServerPlayer recipient,UUID body) {
        return state.trackingGenerations.computeIfAbsent(recipient,ignored->new TrackingGenerationLedger()).acquire(body);
    }
    /** Pure observation: consumers must never create or revive recipient/body authority. */
    private static long currentGeneration(State state,ServerPlayer recipient,UUID body) {
        var ledger=state.trackingGenerations.get(recipient);
        return ledger==null?TrackingGenerationLedger.UNAVAILABLE:ledger.current(body);
    }
    /** Server-owned tracking transition helper retained for fixture/source compatibility. */
    public static long nextTrackingGeneration(long current) {
        if(current<1)throw new IllegalArgumentException("Invalid tracking generation");
        return Math.incrementExact(current);
    }
    /** Current recipient/body tracking generation for a server receipt; never client authority. Zero means saturated/no active authority. */
    public static long trackingGeneration(ServerPlayer recipient,Entity body) {
        if(recipient==null || body==null)return TrackingGenerationLedger.UNAVAILABLE;
        var state=STATES.get(recipient.level().getServer());
        return state==null?TrackingGenerationLedger.UNAVAILABLE:currentGeneration(state,recipient,body.getUUID());
    }
    /** Clear with the active generation before STOP releases the UUID; no retired UUID tombstone is retained server-side. */
    private static void clearContact(State state,Entity body,ServerPlayer recipient) {
        if(!catalog(state,recipient) || !ServerPlayNetworking.canSend(recipient,AnatomyContactPayload.TYPE))return;
        var ledger=state.trackingGenerations.get(recipient);long generation=ledger==null?0:ledger.current(body.getUUID());if(generation<1)return;
        var known=state.contacts.computeIfAbsent(recipient,ignored->new HashMap<>());var old=known.get(body.getUUID());
        long sequence=old==null || old.generation()!=generation?1:old.sequence()+1;
        long tick=body.level().getGameTime();
        var payload=AnatomyContactPayload.clear(AnatomyNetworking.epoch(recipient.level().getServer()),state.catalog.snapshot().revision(),body.level().dimension().identifier(),body.getId(),body.getUUID(),generation,sequence,tick);
        ServerPlayNetworking.send(recipient,payload);known.put(body.getUUID(),new PublishedContact(null,generation,sequence,tick));
    }
    private static boolean catalog(State state,ServerPlayer player) {
        if(!ServerPlayNetworking.canSend(player,AnatomyCatalogPayload.TYPE) || !ServerPlayNetworking.canSend(player,AnatomyPosePayload.TYPE)
                || !ServerPlayNetworking.canSend(player,AnatomyContactPayload.TYPE)) {
            player.connection.disconnect(Component.literal("Scale Brews: incompatible anatomical protocol; update the client mod."));return false;
        }
        var snapshot=state.catalog.snapshot();
        if(!Objects.equals(state.sent.get(player),snapshot.revision())) {
            AnatomyNetworking.sendCatalog(player,state.catalog.preparedPackets(AnatomyNetworking.epoch(player.level().getServer())));
            state.sent.put(player,snapshot.revision());
        }
        return true;
    }
    private static void send(LivingEntity entity,ServerPlayer player) {
        var state=STATES.get(player.level().getServer());if(state==null)return;
        var active=state.entities.get(entity);if(active==null || !catalog(state,player))return;
        long generation=currentGeneration(state,player,entity.getUUID());if(generation<1)return;
        AnatomyMovement.publishedFrame(entity).ifPresent(frame->AnatomyNetworking.sendPose(player,frame,generation));
    }
}
