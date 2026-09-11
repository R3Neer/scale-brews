package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
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
    public record AuthoritativePose(UUID entity,long serverTick,PoseProvider.Inputs inputs) {}
    private static final class State {
        final WorldAnatomyCatalog catalog;
        State(MinecraftServer server){catalog=new WorldAnatomyCatalog(AnatomyNetworking.revision(server));}
        final Map<LivingEntity,Active> entities=weakIdentityMap();
        final Map<ServerPlayer,Long> sent=weakIdentityMap();
        final Map<ServerPlayer,Map<UUID,PublishedContact>> contacts=weakIdentityMap();
        final Map<ServerPlayer,Map<UUID,Long>> trackingGenerations=weakIdentityMap();
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
        EntityTrackingEvents.START_TRACKING.register((entity,player)->{
            var state=STATES.get(player.level().getServer());
            if(state!=null)generation(state,player,entity.getUUID());
            if(entity instanceof LivingEntity living)send(living,player);
            contact(entity,player,true);
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity,player)->{
            var state=STATES.get(player.level().getServer());
            if(state!=null) {
                clearContact(state,entity,player);
                state.contacts.computeIfPresent(player,(recipient,known)->{known.remove(entity.getUUID());return known;});
                state.trackingGenerations.computeIfAbsent(player,ignored->new HashMap<>()).compute(entity.getUUID(),
                    (ignored,current)->nextTrackingGeneration(current==null?1:current));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var state=STATES.get(server);if(state!=null)catalog(state,handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            var state=STATES.get(server);if(state!=null){state.sent.remove(handler.player);state.contacts.remove(handler.player);state.trackingGenerations.remove(handler.player);}
            AnatomyTransportReceipts.disconnect(handler.player);
        });
    }
    public static void start(MinecraftServer server) {
        if(STATES.containsKey(server))return;
        var state=new State(server);state.catalog.reload(server.getResourceManager());
        STATES.put(server,state);reset(server,state);
    }
    /** Preparation harness supplies already exported server-side data, never a player upload. */
    public static void startPrepared(MinecraftServer server,Map<String,ModelGeometry> models,Map<String,io.github.r3neer.scalebrews.platform.PlatformDefinition> profiles) {
        if(STATES.containsKey(server))throw new IllegalStateException("Anatomical runtime already running");
        var state=new State(server);state.catalog.replace(models,profiles);
        STATES.put(server,state);reset(server,state);
    }
    public static void reload(MinecraftServer server) {
        var state=STATES.get(server);if(state==null)return;
        state.catalog.reload(server.getResourceManager()); // Throws before any contact/provider is replaced.
        reset(server,state);
    }
    private static void reset(MinecraftServer server,State state) {
        MaterialIntervalRuntime.clear(server);MaterialPhysicsRuntime.clear(server);
        state.entities.clear();state.sent.clear();state.contacts.clear();state.trackingGenerations.clear();AnatomyTransportReceipts.clear(server);
        for(var level:server.getAllLevels()){
            Platforms.anatomicalDefinitions(level,state.catalog.snapshot().profiles().values());
            AnatomyMovement.deactivate(level);AnatomyMovement.activate(level);prepare(level);
        }
        for(var player:server.getPlayerList().getPlayers())catalog(state,player);
    }
    public static void stop(MinecraftServer server) {
        MaterialIntervalRuntime.clear(server);MaterialPhysicsRuntime.clear(server);
        AnatomyTransportReceipts.clear(server);
        if(STATES.remove(server)!=null)for(var level:server.getAllLevels()){
            AnatomyMovement.deactivate(level);Platforms.clearAnatomicalDefinitions(level);
        }
    }
    /** Server half of {@link AnatomySession}: running catalog session, not an own-support test. */
    public static boolean owns(net.minecraft.world.entity.Entity entity) {
        var server=entity.level().getServer();
        return server!=null && STATES.containsKey(server) && AnatomyMovement.active(entity);
    }
    /**
     * Internal support-binding ownership, intentionally narrower than {@link #owns(net.minecraft.world.entity.Entity)}.
     * A prepared/running session must not suppress the fixture/local fallback for a manually registered support;
     * only a support present in the runtime's active binding table has certified material intervals to own its carry.
     */
    static boolean owns(LivingEntity support) {
        var server=support.level().getServer();
        var state=server==null?null:STATES.get(server);
        return state!=null && state.entities.containsKey(support);
    }
    public static boolean ready(net.minecraft.world.entity.Entity entity) {
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
        if(entity==null || handle==null)return false;
        var server=entity.level().getServer();if(server==null)return false;
        var state=STATES.get(server);if(state==null)return false;
        var active=state.entities.get(entity);if(active==null)return false;
        var identity=handle.identity();
        var definition=active.binding().policy().anatomy().orElse(null);
        return definition!=null && identity.matches(entity)
            && identity.bindingGeneration()==active.bindingGeneration()
            && identity.localRegistrationGeneration()==AnatomyMovement.registrationGeneration(entity)
            && identity.epoch().equals(AnatomyNetworking.epoch(server))
            && identity.revision()==state.catalog.snapshot().revision()
            && identity.model().equals(definition.model())
            && identity.poseProvider().equals(definition.poses());
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
        state.entities.entrySet().removeIf(e->!e.getKey().isAlive() || e.getKey().isRemoved());
        var snapshot=state.catalog.snapshot();
        for(var entity:level.getAllEntities())if(entity instanceof LivingEntity living && !state.entities.containsKey(living)) {
            var binding=snapshot.bindings().get(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()));
            if(binding==null || !binding.policy().enabled())continue;
            var definition=binding.policy().anatomy().orElseThrow();
            var provider=new ModelGeometryProvider(binding.model(),binding.poses(),definition.filter(),snapshot.revision())
                .serverDriven(e->AnatomyPoseEligibility.supported(definition.poses(),e));
            long bindingGeneration=state.allocateBindingGeneration();
            state.entities.put(living,new Active(binding,provider,bindingGeneration));
            AnatomyMovement.register(living,provider,new GeometryProvider.GeometryIdentityDescriptor(
                AnatomyNetworking.epoch(level.getServer()),snapshot.revision(),definition.model(),definition.poses(),bindingGeneration));
        }
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
        for(var body:level.getAllEntities()) {
            var interested=new HashSet<>(PlayerLookup.tracking(body));
            if(body instanceof ServerPlayer player)interested.add(player);
            for(var player:interested)contact(body,player,false);
        }
    }
    private static void contact(net.minecraft.world.entity.Entity body,ServerPlayer recipient,boolean force) {
        var state=STATES.get(recipient.level().getServer());if(state==null || !catalog(state,recipient)
                || !ServerPlayNetworking.canSend(recipient,AnatomyContactPayload.TYPE))return;
        var surface=AnatomyMovement.surface(body);
        ContactKey key=surface==null?null:new ContactKey(surface.support(),surface.revision(),surface.piece(),surface.face(),surface.localPoint(),surface.normal());
        var map=state.contacts.computeIfAbsent(recipient,p->new HashMap<>());var old=map.get(body.getUUID());
        long tick=body.level().getGameTime();
        if(!force && Objects.equals(old==null?null:old.key(),key) && old!=null && tick-old.sentTick()<CONTACT_HEARTBEAT_TICKS)return;
        long generation=generation(state,recipient,body.getUUID());
        long sequence=old==null || old.generation()!=generation?1:old.sequence()+1;var epoch=AnatomyNetworking.epoch(recipient.level().getServer());long revision=state.catalog.snapshot().revision();
        AnatomyContactPayload payload;
        if(surface==null)payload=AnatomyContactPayload.clear(epoch,revision,body.level().dimension().identifier(),body.getId(),body.getUUID(),generation,sequence,tick);
        else {
            var support=recipient.level().getEntity(surface.support());
            if(!(support instanceof LivingEntity living))return;
            payload=new AnatomyContactPayload(epoch,revision,body.level().dimension().identifier(),body.getId(),body.getUUID(),generation,sequence,tick,living.getId(),living.getUUID(),surface.piece(),surface.face(),surface.localPoint(),surface.normal());
        }
        ServerPlayNetworking.send(recipient,payload);map.put(body.getUUID(),new PublishedContact(key,generation,sequence,tick));
    }
    private static long generation(State state,ServerPlayer recipient,UUID body) {
        return state.trackingGenerations.computeIfAbsent(recipient,ignored->new HashMap<>()).computeIfAbsent(body,ignored->1L);
    }
    /** Server-owned tracking transition; packet consumers never synthesize this counter. */
    public static long nextTrackingGeneration(long current) {
        if(current<1)throw new IllegalArgumentException("Invalid tracking generation");
        return Math.incrementExact(current);
    }
    /** Current recipient/body tracking generation for a server receipt; never client authority. */
    public static long trackingGeneration(ServerPlayer recipient,net.minecraft.world.entity.Entity body) {
        if(recipient==null || body==null)return 1;
        var state=STATES.get(recipient.level().getServer());
        return state==null?1:generation(state,recipient,body.getUUID());
    }
    /** Clear with the current generation before STOP advances the next START generation. */
    private static void clearContact(State state,net.minecraft.world.entity.Entity body,ServerPlayer recipient) {
        if(!catalog(state,recipient) || !ServerPlayNetworking.canSend(recipient,AnatomyContactPayload.TYPE))return;
        long generation=generation(state,recipient,body.getUUID());
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
            AnatomyNetworking.sendCatalog(player,snapshot.revision(),snapshot.models(),snapshot.profiles());state.sent.put(player,snapshot.revision());
        }
        return true;
    }
    private static void send(LivingEntity entity,ServerPlayer player) {
        var state=STATES.get(player.level().getServer());if(state==null)return;
        var active=state.entities.get(entity);if(active==null || !catalog(state,player))return;
        AnatomyMovement.publishedFrame(entity).ifPresent(frame->AnatomyNetworking.sendPose(player,frame));
    }
}
