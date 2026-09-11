package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;

import java.util.*;
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
        /** Last passive contribution already removed from a locomotion sample. */
        long consumedTransportSequence;
        /** Explicit physical lifecycle marker; a small teleport need not change a sequence. */
        long transportGeneration;
        PoseProvider.Inputs inputs;
    }
    // Entity/network IDs can be reused. This tracker is server-authoritative,
    // so weak object identity is the correct lifetime key rather than equality.
    private final Map<LivingEntity,State> states=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,State>makeMap());
    public PoseProvider.Inputs tick(LivingEntity entity,long tick,boolean supportedPose) {
        if(entity==null || tick<0)throw new IllegalArgumentException("Invalid authority tracker tick");
        State state=states.get(entity);
        if(state!=null && tick<state.tick)throw new IllegalArgumentException("Authority tracker clock cannot rewind");
        if(state==null){state=new State();states.put(entity,state);}
        if(state.tick==tick)return state.inputs;
        Vec3 position=entity.position();
        var gravity=AnatomyMovement.gravity(entity);
        long transportGeneration=AnatomyMovement.transportGeneration(entity);
        var transport=AnatomyMovement.transportSince(entity,state.consumedTransportSequence);
        if(state.previous==null || tick!=state.tick+1 || position.distanceToSqr(state.previous)>16
                || !gravity.equals(state.gravity) || transportGeneration!=state.transportGeneration
                || !transport.contiguous())state.walk.stop();
        else {
            // Carries happen at END, after the usual pose sample. Consume every exact
            // contribution since the prior sample, rather than checking this tick only.
            Vec3 delta=position.subtract(state.previous).subtract(transport.appliedDelta());
            double distance=gravity.tangent(delta).length();
            if(entity.isAlive() && !entity.isPassenger())state.walk.update(Math.min((float)distance*4,1),.4f,1);
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
            channels.put("on_ground",bee.onGround()?1f:0f);channels.put("angry",bee.isAngry()?1f:0f);
            channels.put("roll",bee.getRollAmount(1));
        }
        if(entity instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse) {
            channels.put("eat",horse.getEatAnim(1));channels.put("stand",horse.getStandAnim(1));channels.put("mouth",horse.getMouthAnim(1));
            channels.put("tail",horse.tailCounter>0?1f:0f);channels.put("water",horse.isInWater()?1f:0f);
        }
        if(entity instanceof net.minecraft.world.entity.animal.golem.IronGolem golem) {
            channels.put("attack",(float)golem.getAttackAnimationTick());channels.put("flower",(float)golem.getOfferFlowerTick());
        }
        if(entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager villager)
            channels.put("unhappy",villager.getUnhappyCounter()>0?1f:0f);
        if(entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep)
            channels.put("grazing",sheep.getHeadEatPositionScale(1));
        state.inputs=new PoseProvider.Inputs(state.walk.position(),state.walk.speed(),entity.tickCount,
            Mth.wrapDegrees(entity.yHeadRot-entity.yBodyRot),entity.getXRot(),supportedPose,channels);
        return state.inputs;
    }
    public Optional<PoseProvider.Inputs> current(LivingEntity entity){return Optional.ofNullable(states.get(entity)).map(s->s.inputs);}
}
