package io.github.r3neer.scalebrews.platform.anatomy;

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
        PoseProvider.Inputs inputs;
    }
    private final Map<LivingEntity,State> states=new WeakHashMap<>();
    public PoseProvider.Inputs tick(LivingEntity entity,long tick,boolean supportedPose) {
        State state=states.computeIfAbsent(entity,e->new State());
        if(state.tick==tick)return state.inputs;
        Vec3 position=entity.position();
        var gravity=AnatomyMovement.gravity(entity);
        if(state.previous==null || tick!=state.tick+1 || position.distanceToSqr(state.previous)>16 || !gravity.equals(state.gravity))state.walk.stop();
        else {
            Vec3 delta=position.subtract(state.previous);
            var transport=AnatomyMovement.transport(entity);
            if(transport!=null && transport.tick()==tick)delta=delta.subtract(transport.displacement());
            double distance=gravity.tangent(delta).length();
            if(entity.isAlive() && !entity.isPassenger())state.walk.update(Math.min((float)distance*4,1),.4f,1);
            else state.walk.stop();
        }
        state.previous=position;state.tick=tick;state.gravity=gravity;
        state.inputs=new PoseProvider.Inputs(state.walk.position(),state.walk.speed(),entity.tickCount,
            Mth.wrapDegrees(entity.yHeadRot-entity.yBodyRot),entity.getXRot(),supportedPose);
        return state.inputs;
    }
    public Optional<PoseProvider.Inputs> current(LivingEntity entity){return Optional.ofNullable(states.get(entity)).map(s->s.inputs);}
}
