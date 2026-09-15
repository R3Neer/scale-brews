package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** Server tick clock for ordinary locomotion; querying/rendering cannot advance it. */
public final class AuthorityPoseTracker {
    private static final class State {
        final WalkAnimationState walk=new WalkAnimationState();
        Vec3 previous;
        GravityFrame gravity;
        long tick=Long.MIN_VALUE;
        long consumedTransportSequence;
        long transportGeneration;
        PoseEngine.Inputs inputs;
    }
    private final Map<LivingEntity,State> states=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,State>makeMap());
    public PoseEngine.Inputs tick(LivingEntity entity,long tick,boolean supportedPose) {
        if(entity==null || tick<0)throw new IllegalArgumentException("Invalid authority tracker tick");
        State state=states.get(entity);
        if(state!=null && tick<state.tick)throw new IllegalArgumentException("Authority tracker clock cannot rewind");
        if(state==null){state=new State();states.put(entity,state);}
        if(state.tick==tick)return state.inputs;
        Vec3 position=entity.position();
        var gravity=AnatomyMovement.gravity(entity);
        long transportGeneration=TransportLedger.generation(entity);
        var transport=TransportLedger.since(entity,state.consumedTransportSequence);
        if(state.previous==null || tick!=state.tick+1 || position.distanceToSqr(state.previous)>16
                || !gravity.equals(state.gravity) || transportGeneration!=state.transportGeneration
                || !transport.contiguous())state.walk.stop();
        else {
            Vec3 delta=position.subtract(state.previous).subtract(transport.appliedDelta());
            double distance=gravity.tangent(delta).length();
            if(entity.isAlive() && !entity.isPassenger())
                state.walk.update(Math.min((float)distance*4,1),.4f,entity.isBaby()?3f:1f);
            else state.walk.stop();
        }
        state.previous=position;state.tick=tick;state.gravity=gravity;state.transportGeneration=transportGeneration;
        state.consumedTransportSequence=transport.latestSequence();
        Map<String,Float> channels=new HashMap<>();
        channels.put("crouching",entity.isCrouching()?1f:0f);channels.put("sprinting",entity.isSprinting()?1f:0f);
        if(entity instanceof net.minecraft.world.entity.animal.chicken.Chicken chicken) {
            channels.put("flap",chicken.flap);channels.put("flap_speed",chicken.flapSpeed);
        }
        if(entity instanceof net.minecraft.world.entity.animal.bee.Bee bee) {
            boolean rendererGrounded=bee.onGround() && bee.getDeltaMovement().lengthSqr()<1.0E-7;
            channels.put("on_ground",rendererGrounded?1f:0f);channels.put("angry",bee.isAngry()?1f:0f);
            channels.put("roll",bee.getRollAmount(1));
        }
        if(entity instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse) {
            channels.put("eat",horse.getEatAnim(1));channels.put("stand",horse.getStandAnim(1));channels.put("mouth",horse.getMouthAnim(1));
            channels.put("tail",horse.tailCounter>0?1f:0f);channels.put("water",horse.isInWater()?1f:0f);
            channels.put("age_scale",horse.getAgeScale());
        }
        if(entity instanceof net.minecraft.world.entity.animal.feline.Cat cat) {
            channels.put("sitting",cat.isInSittingPose()?1f:0f);
            channels.put("lie",cat.getLieDownAmount(1));channels.put("lie_tail",cat.getLieDownAmountTail(1));
            channels.put("relax",cat.getRelaxStateOneAmount(1));channels.put("age_scale",cat.getAgeScale());
        }
        if(entity instanceof net.minecraft.world.entity.animal.golem.IronGolem golem) {
            int rawAttack=golem.getAttackAnimationTick();
            channels.put("attack",rawAttack>0?(float)(rawAttack-1):0f);channels.put("flower",(float)golem.getOfferFlowerTick());
        }
        if(entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager villager)
            channels.put("unhappy",villager.getUnhappyCounter()>0?1f:0f);
        if(entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep)
            channels.put("grazing",sheep.getHeadEatPositionScale(1));

        Identifier entityType=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean externalChannelsAvailable=sampleExternalChannels(entityType,entity,channels);
        // END_LEVEL_TICK observes the completed entity endpoint. All interpolated fields above use
        // partialTicks=1/current state; vanilla EntityRenderer uses tickCount + partialTicks for age.
        float endpointAge=(float)entity.tickCount+1f;
        state.inputs=new PoseEngine.Inputs(state.walk.position(),state.walk.speed(),endpointAge,
            Mth.wrapDegrees(entity.yHeadRot-entity.yBodyRot),entity.getXRot(),supportedPose&&externalChannelsAvailable,channels);
        return state.inputs;
    }

    /** External channel failures localize to this endpoint instead of crashing the server or publishing partial truth. */
    static boolean sampleExternalChannels(Identifier entityType,LivingEntity entity,Map<String,Float> channels) {
        var adapter=CollisionAdapters.poseChannels(entityType).orElse(null);
        if(adapter==null)return true;
        try {
            adapter.sample(entity,(name,value)->{
                if(name==null || !name.matches("[a-z0-9_.-]{1,64}") || !Float.isFinite(value))
                    throw new IllegalArgumentException("Invalid external pose channel");
                if(channels.containsKey(name))throw new IllegalArgumentException("Duplicate authoritative pose channel "+name);
                if(channels.size()>=64)throw new IllegalArgumentException("Too many authoritative pose channels");
                channels.put(name,value);
            });
            return true;
        } catch(RuntimeException invalid) {
            return false;
        }
    }

    public Optional<PoseEngine.Inputs> current(LivingEntity entity){return Optional.ofNullable(states.get(entity)).map(s->s.inputs);}
}
