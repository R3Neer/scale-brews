package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.pose.LivingLocomotionClock;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;

/** Server tick clock for ordinary locomotion; querying/rendering cannot advance it. */
public final class AuthorityPoseTracker {
    private static final class State {
        final LivingLocomotionClock clock=new LivingLocomotionClock();
        LivingLocomotionClock.Frame frame;
        PoseEngine.Inputs inputs;
        boolean externalChannelsAvailable;
        boolean sampling;
    }
    private final Map<LivingEntity,State> states=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,State>makeMap());
    public PoseEngine.Inputs tick(LivingEntity entity,long tick,boolean supportedPose) {
        if(entity==null || tick<0 || entity.isRemoved() || entity.level().isClientSide()
                || entity.level().getServer()==null || !entity.level().getServer().isSameThread())
            throw new IllegalArgumentException("Invalid server authority tracker tick");
        State state=states.get(entity);
        if(state!=null && state.frame!=null && tick<state.frame.tick())
            throw new IllegalArgumentException("Authority tracker clock cannot rewind");
        boolean fresh=state==null;
        if(fresh)state=new State();
        if(state.sampling)throw new IllegalStateException("Authority tracker cannot sample reentrantly");
        state.sampling=true;
        if(fresh)states.put(entity,state);
        try { return sample(entity,tick,supportedPose,state); }
        catch(RuntimeException | Error failure) {
            state.inputs=null;state.frame=null;
            if(fresh)states.remove(entity);
            throw failure;
        } finally {state.sampling=false;}
    }
    private PoseEngine.Inputs sample(LivingEntity entity,long tick,boolean supportedPose,State state) {
        var frame=state.clock.advance(entity,tick,AnatomyMovement.gravity(entity));
        if(state.frame==frame) {
            boolean ordinary=supportedPose&&state.externalChannelsAvailable&&frame.available();
            if(state.inputs.ordinary()!=ordinary)state.inputs=new PoseEngine.Inputs(state.inputs.walkPhase(),state.inputs.walkAmount(),
                state.inputs.age(),state.inputs.headYaw(),state.inputs.headPitch(),ordinary,state.inputs.channels());
            return state.inputs;
        }
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
        var staged=new PoseEngine.Inputs(frame.position(),frame.speed(),endpointAge,
            Mth.wrapDegrees(entity.yHeadRot-entity.yBodyRot),entity.getXRot(),supportedPose&&externalChannelsAvailable&&frame.available(),channels);
        // Adapters and getters can retire authority: commit only after every delegated read.
        boolean frameValid=frame.validFor(entity,tick);
        if(!frameValid && staged.ordinary())staged=new PoseEngine.Inputs(staged.walkPhase(),staged.walkAmount(),
            staged.age(),staged.headYaw(),staged.headPitch(),false,staged.channels());
        state.externalChannelsAvailable=externalChannelsAvailable&&frameValid;
        state.frame=frame;state.inputs=staged;
        return state.inputs;
    }

    /** External channel sampling is transactional: failure cannot publish any prefix of adapter output. */
    static boolean sampleExternalChannels(Identifier entityType,LivingEntity entity,Map<String,Float> channels) {
        var adapter=CollisionAdapters.poseChannels(entityType).orElse(null);
        if(adapter==null)return true;
        Map<String,Float> staged=new HashMap<>();
        try {
            adapter.sample(entity,(name,value)->{
                if(name==null || !name.matches("[a-z0-9_.-]{1,64}") || !Float.isFinite(value))
                    throw new IllegalArgumentException("Invalid external pose channel");
                if(channels.containsKey(name) || staged.containsKey(name))
                    throw new IllegalArgumentException("Duplicate authoritative pose channel "+name);
                if(channels.size()+staged.size()>=64)
                    throw new IllegalArgumentException("Too many authoritative pose channels");
                staged.put(name,value);
            });
            channels.putAll(staged);
            return true;
        } catch(RuntimeException invalid) {
            return false;
        }
    }

    public Optional<PoseEngine.Inputs> current(LivingEntity entity){return Optional.ofNullable(states.get(entity)).filter(s->!s.sampling).map(s->s.inputs);}
    /** Existing participant clock only; reading never creates/advances state. Validate at consumption. */
    public Optional<LivingLocomotionClock.Frame> locomotion(LivingEntity entity) {
        return Optional.ofNullable(states.get(entity)).filter(s->!s.sampling&&s.inputs!=null).flatMap(s->s.clock.current());
    }
}
