package io.github.r3neer.scalebrews.client.platform.anatomy;

import io.github.r3neer.scalebrews.platform.anatomy.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Catalog is supplied exclusively by the connected server, independent of render/resource packs. */
public final class AnatomyClientNetworking {
    private static AnatomyCatalogTransfer transfer=new AnatomyCatalogTransfer();
    private static final java.util.Map<java.util.UUID,AnatomyPoseHistory> poses=new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID,Long> receivedAt=new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID,ModelGeometryProvider> evaluators=new java.util.HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel poseLevel;
    private static long clientTick;
    private AnatomyClientNetworking() {}
    public static AnatomyCatalogTransfer catalog(){return transfer;}
    public static AnatomyPoseHistory pose(java.util.UUID entity){return poses.get(entity);}
    private static void clearPoses(){
        if(poseLevel!=null)AnatomyMovement.deactivate(poseLevel);
        poses.clear();receivedAt.clear();evaluators.clear();
    }
    /** Physics reads the latest confirmed pose, never a renderer or an extrapolated animation. */
    private static void bindPhysics() {
        if(poseLevel==null || transfer.snapshot().bindings().isEmpty())return;
        AnatomyMovement.activate(poseLevel);
        for(var history:poses.values()) {
            var packet=history.current();var entity=poseLevel.getEntity(packet.entityId());
            if(!(entity instanceof net.minecraft.world.entity.LivingEntity living) || !entity.getUUID().equals(packet.entity()))continue;
            if(!transfer.snapshot().bindings().containsKey(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))continue;
            AnatomyMovement.register(living,support->{
                var latest=poses.get(support.getUUID());
                return latest==null?java.util.Optional.empty():geometry(support,latest.current().tick());
            });
        }
    }
    /** Shared collision/presentation query; never reads local model animations or resource packs. */
    public static java.util.Optional<GeometryProvider.Snapshot> geometry(net.minecraft.world.entity.LivingEntity entity,double serverTick) {
        var history=poses.get(entity.getUUID());
        if(history==null || entity.level()!=poseLevel)return java.util.Optional.empty();
        var packet=history.current();
        if(packet.entityId()!=entity.getId())return java.util.Optional.empty();
        var evaluator=evaluators.get(entity.getUUID());
        if(evaluator==null) {
            PoseProvider provider=PoseProviders.find(packet.provider()).orElse(null);
            var model=transfer.snapshot().models().get(packet.model().toString());
            if(provider==null || model==null)return java.util.Optional.empty();
            var binding=transfer.snapshot().bindings().get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
            var definition=binding==null?null:binding.policy();
            var anatomy=definition==null?java.util.Optional.<AnatomyDefinition>empty():definition.anatomy();
            if(anatomy.isPresent() && (!anatomy.get().model().equals(packet.model()) || !anatomy.get().poses().equals(packet.provider())))return java.util.Optional.empty();
            var filter=anatomy.map(AnatomyDefinition::filter).orElse(AnatomyFilter.DEFAULT);
            evaluator=new ModelGeometryProvider(model,provider,filter,packet.revision());evaluators.put(entity.getUUID(),evaluator);
        }
        return evaluator.sampleInterpolated(entity,history,serverTick);
    }
    private static void reset(){if(poseLevel!=null)io.github.r3neer.scalebrews.platform.Platforms.clearAnatomicalDefinitions(poseLevel);transfer=new AnatomyCatalogTransfer();clearPoses();poseLevel=null;clientTick=0;}
    private static void useLevel(net.minecraft.client.multiplayer.ClientLevel level){
        if(poseLevel!=level){
            if(poseLevel!=null)io.github.r3neer.scalebrews.platform.Platforms.clearAnatomicalDefinitions(poseLevel);
            clearPoses();poseLevel=level;
            if(level!=null && transfer.revision()>=0)io.github.r3neer.scalebrews.platform.Platforms.anatomicalDefinitions(level,transfer.snapshot().profiles().values());
        }
    }
    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            useLevel(client.level);
            clientTick++;
            poses.entrySet().removeIf(entry->{
                var packet=entry.getValue().current();
                var entity=poseLevel==null?null:poseLevel.getEntity(packet.entityId());
                boolean expired=clientTick-receivedAt.getOrDefault(entry.getKey(),0L)>100;
                boolean wrongIdentity=entity!=null && !entity.getUUID().equals(entry.getKey());
                if(expired || wrongIdentity){receivedAt.remove(entry.getKey());evaluators.remove(entry.getKey());return true;}
                return false;
            });
            bindPhysics();
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyCatalogPayload.TYPE,(packet,context)->{
            try{if(transfer.accept(packet)){
                clearPoses();
                if(context.client().level!=null)io.github.r3neer.scalebrews.platform.Platforms.anatomicalDefinitions(context.client().level,transfer.snapshot().profiles().values());
            }}
            catch(RuntimeException invalid){io.github.r3neer.scalebrews.ScaleBrews.LOGGER.error("Rejected anatomical catalog; previous revision retained",invalid);}
        });
        ClientPlayNetworking.registerGlobalReceiver(AnatomyPosePayload.TYPE,(packet,context)->{
            var level=context.client().level;
            useLevel(level);
            if(level==null || !packet.epoch().equals(transfer.epoch()) || packet.revision()!=transfer.revision() || !packet.dimension().equals(level.dimension().identifier())
                || !transfer.snapshot().models().containsKey(packet.model().toString()))return;
            var entity=level.getEntity(packet.entityId());
            if(entity!=null && !entity.getUUID().equals(packet.entity()))return;
            if(poses.size()>=4096 && !poses.containsKey(packet.entity()))return;
            var history=poses.computeIfAbsent(packet.entity(),id->new AnatomyPoseHistory());
            try{if(history.accept(packet))receivedAt.put(packet.entity(),clientTick);}
            catch(IllegalArgumentException invalid){poses.remove(packet.entity());receivedAt.remove(packet.entity());evaluators.remove(packet.entity());}
        });
    }
}
