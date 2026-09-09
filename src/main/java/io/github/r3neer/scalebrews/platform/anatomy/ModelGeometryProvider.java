package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Common original-model evaluator. Pose channels are supplied by authority, never by a renderer. */
public final class ModelGeometryProvider implements GeometryProvider {
    private final ModelGeometry geometry;
    private final PoseProvider poses;
    private final AnatomyFilter filter;
    private final long revision;
    private final Map<LivingEntity,PoseProvider.Inputs> channels=new WeakHashMap<>();
    private final Map<LivingEntity,Cached> cache=new WeakHashMap<>();
    private final Map<LivingEntity,Trajectory> trajectories=new WeakHashMap<>();
    private record TickFrame(long tick,AnatomyPoseHistory.Sample sample) {}
    private final Map<LivingEntity,TickFrame> tickFrames=new WeakHashMap<>();
    private final Map<LivingEntity,Optional<MotionSnapshot>> tickMotions=new WeakHashMap<>();
    private static final class Trajectory {
        final AnatomyPoseHistory.Sample before,after;
        final Optional<HierarchyMotion> motion;
        double fraction=Double.NaN;
        Optional<Snapshot> sample=Optional.empty();
        Trajectory(AnatomyPoseHistory.Segment segment,Optional<HierarchyMotion> motion){before=segment.before();after=segment.after();this.motion=motion;}
    }
    private long evaluations;
    private final AuthorityPoseTracker authority=new AuthorityPoseTracker();
    private java.util.function.Predicate<LivingEntity> poseEligibility;
    private record Key(PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale,GravityFrame gravity) {}
    private record Cached(Key key,Optional<Snapshot> snapshot) {}
    public ModelGeometryProvider(ModelGeometry geometry,PoseProvider poses,AnatomyFilter filter,long revision) {
        this.geometry=Objects.requireNonNull(geometry);this.poses=Objects.requireNonNull(poses);
        this.filter=Objects.requireNonNull(filter);this.revision=revision;
    }
    public void pose(LivingEntity entity,PoseProvider.Inputs input){channels.put(entity,input);}
    public Optional<PoseProvider.Inputs> inputs(LivingEntity entity){return Optional.ofNullable(channels.get(entity));}
    public ModelGeometryProvider serverDriven(java.util.function.Predicate<LivingEntity> eligibility) {
        poseEligibility=Objects.requireNonNull(eligibility);return this;
    }
    public void tick(LivingEntity entity,long tick) {
        var previous=tickFrames.get(entity);
        if(previous!=null && previous.tick()==tick)return;
        if(poseEligibility!=null && !entity.level().isClientSide())
            pose(entity,authority.tick(entity,tick,poseEligibility.test(entity)));
        var inputs=channels.get(entity);
        if(inputs==null){tickFrames.remove(entity);tickMotions.remove(entity);return;}
        var frame=new AnatomyPoseHistory.Sample(inputs,entity.position(),entity.yBodyRot,entity.getScale(),AnatomyMovement.gravity(entity));
        var start=previous!=null && previous.tick()+1==tick && previous.sample().gravity().equals(frame.gravity())?previous.sample():frame;
        tickMotions.put(entity,motionBetween(start,frame).map(m->new MotionSnapshot(revision,tick,m.pieces())));
        tickFrames.put(entity,new TickFrame(tick,frame));
    }
    public Optional<MotionSnapshot> motion(LivingEntity entity) {
        var frame=tickFrames.get(entity);
        if(frame!=null && frame.tick()==entity.level().getGameTime())return tickMotions.getOrDefault(entity,Optional.empty());
        return GeometryProvider.super.motion(entity);
    }
    public long evaluations(){return evaluations;}
    /** Same joint-space trajectory as continuous collision; cached per support and sample time. */
    public Optional<Snapshot> sampleInterpolated(LivingEntity entity,AnatomyPoseHistory history,double tick) {
        if(!entity.isAlive() || history.current()==null || history.current().revision()!=revision)return Optional.empty();
        var segment=history.segment(tick);var trajectory=trajectories.get(entity);
        if(trajectory==null || !trajectory.before.equals(segment.before()) || !trajectory.after.equals(segment.after())) {
            trajectory=new Trajectory(segment,motionBetween(segment.before(),segment.after()));trajectories.put(entity,trajectory);
        }
        if(trajectory.fraction!=segment.fraction()) {
            trajectory.fraction=segment.fraction();
            trajectory.sample=trajectory.motion.map(motion->{
                Map<String,ConvexBox> pieces=new LinkedHashMap<>();
                motion.pieces().forEach((id,m)->pieces.put(id,m.at().apply(segment.fraction())));
                evaluations++;return new Snapshot(revision,pieces);
            });
        }
        return trajectory.sample;
    }
    /** The physical interpolation is joint TRS between authoritative endpoint poses, not matrix lerp. */
    public Optional<HierarchyMotion> motionBetween(AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after) {
        if(before.origin().distanceToSqr(after.origin())>16 || !before.gravity().equals(after.gravity()))return Optional.empty();
        var a=poses.evaluate(geometry,before.inputs());var b=poses.evaluate(geometry,after.inputs());
        if(a.isEmpty() || b.isEmpty())return Optional.empty();
        Matrix4f rootA=before.gravity().matrix().rotateY((float)Math.toRadians(180-before.yaw())).scale(before.scale()).mul(ModelGeometry.matrix(geometry.modelTransform()));
        Matrix4f rootB=after.gravity().matrix().rotateY((float)Math.toRadians(180-after.yaw())).scale(after.scale()).mul(ModelGeometry.matrix(geometry.modelTransform()));
        return Optional.of(new HierarchyMotion(geometry,a.get(),b.get(),rootA,rootB,before.origin(),after.origin(),filter));
    }
    public Optional<Snapshot> sample(LivingEntity entity) {
        var inputs=channels.get(entity);
        if(inputs==null || !entity.isAlive())return Optional.empty();
        return sampleAt(entity,new AnatomyPoseHistory.Sample(inputs,entity.position(),entity.yBodyRot,entity.getScale(),AnatomyMovement.gravity(entity)));
    }
    /** Evaluates an authoritative/interpolated frame without consulting client animation or position. */
    public Optional<Snapshot> sampleAt(LivingEntity entity,AnatomyPoseHistory.Sample frame) {
        if(!entity.isAlive())return Optional.empty();
        var inputs=frame.inputs();
        float scale=frame.scale();
        if(!Float.isFinite(scale) || scale<=0 || !Float.isFinite(frame.yaw())
            || !Double.isFinite(frame.origin().x) || !Double.isFinite(frame.origin().y) || !Double.isFinite(frame.origin().z))return Optional.empty();
        var key=new Key(inputs,frame.origin(),frame.yaw(),scale,frame.gravity());
        var previous=cache.get(entity);
        if(previous!=null && previous.key.equals(key))return previous.snapshot;
        var transforms=poses.evaluate(geometry,inputs);
        Optional<Snapshot> snapshot=Optional.empty();
        if(transforms.isPresent()) {
            // Renderer-specific scale/offset is exported data, not guessed from entity dimensions.
            Matrix4f root=frame.gravity().matrix().rotateY((float)Math.toRadians(180-frame.yaw()))
                .scale(scale).mul(ModelGeometry.matrix(geometry.modelTransform()));
            Map<String,ConvexBox> pieces=new LinkedHashMap<>();
            geometry.evaluate(root,transforms.get(),filter).forEach((id,box)->pieces.put(id,box.move(frame.origin())));
            snapshot=Optional.of(new Snapshot(revision,pieces));evaluations++;
        }
        cache.put(entity,new Cached(key,snapshot));
        return snapshot;
    }
}
