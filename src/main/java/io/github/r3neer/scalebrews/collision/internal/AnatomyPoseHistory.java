package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.pose.PoseChannels;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Two authoritative samples; interpolation cannot advance or mutate the server animation clock. */
public final class AnatomyPoseHistory {
    public record Sample(PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale,GravityFrame gravity) {
        public Sample(PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale){this(inputs,origin,yaw,scale,GravityFrame.VANILLA);}
        public Sample {
            if(inputs==null || origin==null || gravity==null || !Double.isFinite(origin.lengthSqr())
                    || !Float.isFinite(yaw) || !Float.isFinite(scale) || scale<=0)
                throw new IllegalArgumentException("Invalid authoritative pose sample");
        }
    }
    public record Segment(Sample before,Sample after,double fraction) {}
    private AnatomyPosePayload previous,current;
    public AnatomyPosePayload current(){return current;}
    private static Sample frame(AnatomyPosePayload p){return new Sample(p.inputs(),p.origin(),p.yaw(),p.scale(),new GravityFrame(p.gravity()));}
    /** Geometry consumers interpolate joint transforms, not interpolated animation parameters. */
    public Segment segment(double tick) {
        if(current==null || !Double.isFinite(tick))throw new IllegalStateException("No valid pose sample");
        if(previous==null || !current.inputs().ordinary() || !previous.inputs().ordinary())return new Segment(frame(current),frame(current),1);
        return new Segment(frame(previous),frame(current),Math.clamp((tick-previous.jointSampleTick())/(current.jointSampleTick()-previous.jointSampleTick()),0,1));
    }
    public boolean accept(AnatomyPosePayload next) {
        if(next==null)throw new IllegalArgumentException("Missing pose frame");
        if(current!=null) {
            if(!current.epoch().equals(next.epoch()) || current.revision()!=next.revision() || !current.dimension().equals(next.dimension())
                || !current.entity().equals(next.entity()) || current.entityId()!=next.entityId() || !current.model().equals(next.model()) || !current.provider().equals(next.provider()) || current.bindingGeneration()!=next.bindingGeneration())
                throw new IllegalArgumentException("Pose history identity changed without reset");
            if(next.jointSampleTick()<=current.jointSampleTick() || next.authorityTick()<current.authorityTick())return false;
        }
        previous=current;
        if(previous!=null && (next.origin().distanceToSqr(previous.origin())>16 || next.jointSampleTick()-previous.jointSampleTick()>20 || next.gravity()!=previous.gravity()))previous=null;
        current=next;return true;
    }
    /**
     * Compatibility helper for non-geometry consumers. Geometry must use
     * {@link #segment(double)} and interpolate the evaluated joint trajectory.
     * Declared continuous channels are lerped; flags and unknown future channels
     * use nearest authoritative sample and are never invented as fractional flags.
     */
    public Sample sample(double tick) {
        if(current==null || !Double.isFinite(tick))throw new IllegalStateException("No valid pose sample");
        if(previous==null || !current.inputs().ordinary() || !previous.inputs().ordinary())return frame(current);
        float t=(float)Math.clamp((tick-previous.jointSampleTick())/(current.jointSampleTick()-previous.jointSampleTick()),0,1);
        var a=previous.inputs();var b=current.inputs();
        var channels=PoseChannels.interpolate(a.channels(),b.channels(),t);
        var pose=new PoseProvider.Inputs(Mth.lerp(t,a.walkPhase(),b.walkPhase()),Mth.lerp(t,a.walkAmount(),b.walkAmount()),Mth.lerp(t,a.age(),b.age()),
            Mth.rotLerp(t,a.headYaw(),b.headYaw()),Mth.lerp(t,a.headPitch(),b.headPitch()),true,channels);
        return new Sample(pose,previous.origin().lerp(current.origin(),t),Mth.rotLerp(t,previous.yaw(),current.yaw()),Mth.lerp(t,previous.scale(),current.scale()),new GravityFrame(current.gravity()));
    }
}
