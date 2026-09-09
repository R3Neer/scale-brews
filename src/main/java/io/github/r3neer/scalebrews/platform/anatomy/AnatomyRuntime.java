package io.github.r3neer.scalebrews.platform.anatomy;

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
    private AnatomyRuntime() {}
    private static final Map<MinecraftServer,State> STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private record Active(WorldAnatomyCatalog.Binding binding,ModelGeometryProvider provider) {}
    private static final class State {
        final WorldAnatomyCatalog catalog;
        State(MinecraftServer server){catalog=new WorldAnatomyCatalog(AnatomyNetworking.revision(server));}
        final Map<LivingEntity,Active> entities=new WeakHashMap<>();
        final Map<ServerPlayer,Long> sent=new WeakHashMap<>();
        final Map<ServerPlayer,Map<UUID,PublishedContact>> contacts=new WeakHashMap<>();
    }
    private record ContactKey(UUID support,long revision,String piece,int face,Vec3 local,Vec3 normal) {}
    private record PublishedContact(ContactKey key,long sequence) {}
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server->{if(Boolean.getBoolean("scalebrews.anatomyRuntime"))start(server);});
        ServerLifecycleEvents.SERVER_STOPPED.register(AnatomyRuntime::stop);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,resources,success)->{
            if(success && STATES.containsKey(server)) {
                try{reload(server);}catch(RuntimeException invalid){ScaleBrews.LOGGER.error("Anatomical reload rejected; accepted catalog retained",invalid);}
            }
        });
        EntityTrackingEvents.START_TRACKING.register((entity,player)->{
            if(entity instanceof LivingEntity living)send(living,player);
            contact(entity,player,true);
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity,player)->{
            var state=STATES.get(player.level().getServer());
            if(state!=null)state.contacts.computeIfPresent(player,(recipient,known)->{known.remove(entity.getUUID());return known;});
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var state=STATES.get(server);if(state!=null)catalog(state,handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            var state=STATES.get(server);if(state!=null){state.sent.remove(handler.player);state.contacts.remove(handler.player);}
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
        state.entities.clear();state.sent.clear();state.contacts.clear();
        for(var level:server.getAllLevels()){
            Platforms.anatomicalDefinitions(level,state.catalog.snapshot().profiles().values());
            AnatomyMovement.deactivate(level);AnatomyMovement.activate(level);prepare(level);
        }
        for(var player:server.getPlayerList().getPlayers())catalog(state,player);
    }
    public static void stop(MinecraftServer server) {
        if(STATES.remove(server)!=null)for(var level:server.getAllLevels()){
            AnatomyMovement.deactivate(level);Platforms.clearAnatomicalDefinitions(level);
        }
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
            state.entities.put(living,new Active(binding,provider));AnatomyMovement.register(living,provider);
        }
    }
    public static void publish(ServerLevel level) {
        var state=STATES.get(level.getServer());if(state==null)return;
        for(var entity:List.copyOf(state.entities.keySet()))if(entity.level()==level && entity.isAlive()) {
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
        if(!force && Objects.equals(old==null?null:old.key(),key))return;
        long sequence=old==null?1:old.sequence()+1;var epoch=AnatomyNetworking.epoch(recipient.level().getServer());long revision=state.catalog.snapshot().revision();
        AnatomyContactPayload payload;
        if(surface==null)payload=AnatomyContactPayload.clear(epoch,revision,body.getId(),body.getUUID(),sequence,body.level().getGameTime());
        else {
            var support=recipient.level().getEntity(surface.support());
            if(!(support instanceof LivingEntity living))return;
            payload=new AnatomyContactPayload(epoch,revision,body.getId(),body.getUUID(),sequence,body.level().getGameTime(),living.getId(),living.getUUID(),surface.piece(),surface.face(),surface.localPoint(),surface.normal());
        }
        ServerPlayNetworking.send(recipient,payload);map.put(body.getUUID(),new PublishedContact(key,sequence));
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
        var definition=active.binding.policy().anatomy().orElseThrow();
        active.provider.inputs(entity).ifPresent(inputs->AnatomyNetworking.sendPose(player,state.catalog.snapshot().revision(),entity,definition.model(),definition.poses(),inputs));
    }
}
