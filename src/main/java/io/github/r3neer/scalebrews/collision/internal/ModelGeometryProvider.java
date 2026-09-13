package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Common original-model evaluator. Pose channels are supplied by authority, never by a renderer. */
public final class ModelGeometryProvider implements GeometryProvider {
    private static <V> Map<LivingEntity,V> entityMap(){return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,V>makeMap());}
    private final ModelGeometry geometry;
    /** Immutable evaluator already bound to one accepted catalog revision. */
    private final PoseEngine.Bound poses;
    private final AnatomyFilter filter;
    private final long revision;
    private final Map<LivingEntity,PoseEngine.Inputs> channels=entityMap();
    private final Map<LivingEntity,Cached> cache=entityMap();
    private record JointEndpoint(PoseEngine.Inputs inputs,Optional<Map<String,Matrix4f>> transforms) {}
    private static final class JointEndpoints {JointEndpoint previous,current;}
    private final Map<LivingEntity,JointEndpoints> jointCache=entityMap();
    private final Map<LivingEntity,Trajectory> trajectories=entityMap();
    private record TickFrame(long tick,AnatomyPoseHistory.Sample sample) {}
    public record AuthoritativeFrame(long tick,AnatomyPoseHistory.Sample sample) {
        public AuthoritativeFrame {
            if(tick<0 || sample==null)throw new IllegalArgumentException("Invalid authoritative frame");
        }
    }
    /** Compatibility view while callers migrate to the complete immutable endpoint. */
    @Deprecated public record AuthoritativeInputs(long tick,PoseEngine.Inputs inputs) {}
    private final Map<LivingEntity,TickFrame> tickFrames=entityMap();
    private final Map<LivingEntity,Optional<MotionSnapshot>> tickMotions=entityMap();
    private static final class Trajectory {
        final AnatomyPoseHistory.Sample before,after;
        final Optional<HierarchyMotion> motion;
        double fraction=Double.NaN;
        Optional<Snapshot> sample=Optional.empty();
        Trajectory(AnatomyPoseHistory.Segment segment,Optional<HierarchyMotion> motion){before=segment.before();after=segment.after();this.motion=motion;}
    }
    private long evaluations,jointEvaluations;
    private final AuthorityPoseTracker authority=new AuthorityPoseTracker();
    private java.util.function.Predicate<LivingEntity> poseEligibility;
    private record Key(PoseEngine.Inputs inputs,Vec3 origin,float yaw,float scale,GravityFrame gravity) {}
    private record Cached(Key key,Optional<Snapshot> snapshot) {}

    /** Procedural/source compatibility constructor; data-backed engines must use the bound constructor. */
    public ModelGeometryProvider(ModelGeometry geometry,PoseEngine poses,AnatomyFilter filter,long revision) {
        this(geometry,bind(geometry,poses,Map.of()),filter,revision);
    }
    /** Procedural/source compatibility constructor with immutable engine parameters. */
    public ModelGeometryProvider(ModelGeometry geometry,PoseEngine poses,Map<String,String> poseParameters,AnatomyFilter filter,long revision) {
        this(geometry,bind(geometry,poses,poseParameters),filter,revision);
    }
    /** Canonical S18 runtime constructor: no resource lookup remains in the hot path. */
    public ModelGeometryProvider(ModelGeometry geometry,PoseEngine.Bound poses,AnatomyFilter filter,long revision) {
        this.geometry=Objects.requireNonNull(geometry);this.poses=Objects.requireNonNull(poses);this.filter=Objects.requireNonNull(filter);
        if(revision<0)throw new IllegalArgumentException("Invalid geometry revision");
        this.revision=revision;
    }
    private static PoseEngine.Bound bind(ModelGeometry geometry,PoseEngine engine,Map<String,String> parameters) {
        Objects.requireNonNull(geometry);Objects.requireNonNull(engine);Objects.requireNonNull(parameters);
        return engine.bind(geometry,parameters,Set.of(),PoseEngine.Resources.empty())
            .orElseThrow(()->new IllegalArgumentException("Pose engine cannot bind geometry/version"));
    }
    public void pose(LivingEntity entity,PoseEngine.Inputs input){channels.put(entity,input);}
    public Optional<PoseEngine.Inputs> inputs(LivingEntity entity){return Optional.ofNullable(channels.get(entity));}
    public ModelGeometryProvider serverDriven(java.util.function.Predicate<LivingEntity> eligibility) {
        poseEligibility=Objects.requireNonNull(eligibility);return this;
    }
    public void tick(LivingEntity entity,long tick) {
        if(entity==null || tick<0)throw new IllegalArgumentException("Invalid authority tick");
        var previous=tickFrames.get(entity);
        if(previous!=null && tick<previous.tick)throw new IllegalArgumentException("Authority clock cannot rewind");
        if(previous!=null && previous.tick==tick)return;
        if(poseEligibility!=null && !entity.level().isClientSide())pose(entity,authority.tick(entity,tick,poseEligibility.test(entity)));
        var inputs=channels.get(entity);if(inputs==null){tickFrames.remove(entity);tickMotions.remove(entity);return;}
        var frame=new AnatomyPoseHistory.Sample(inputs,entity.position(),entity.yBodyRot,entity.getScale(),AnatomyMovement.gravity(entity));
        joints(entity,inputs);
        if(previous!=null && previous.tick+1==tick && previous.sample.gravity().equals(frame.gravity()))
            tickMotions.put(entity,motionBetween(entity,previous.sample,frame).map(m->new MotionSnapshot(revision,previous.tick,tick,previous.sample.origin(),frame.origin(),m.pieces())));
        else tickMotions.put(entity,Optional.empty());
        tickFrames.put(entity,new TickFrame(tick,frame));
    }
    public Optional<MotionSnapshot> motion(LivingEntity entity) {
        var frame=tickFrames.get(entity);if(frame!=null && frame.tick==entity.level().getGameTime())return tickMotions.getOrDefault(entity,Optional.empty());return Optional.empty();
    }
    @Override public Optional<MotionSnapshot> interval(LivingEntity entity,MotionIntervalHandle handle) {
        if(entity==null || handle==null || !handle.identity().matches(entity) || handle.identity().revision()!=revision
                || handle.before().snapshot().revision()!=revision || handle.after().snapshot().revision()!=revision)return Optional.empty();
        return motionBetween(entity,handle.before().sample(),handle.after().sample()).map(m->new MotionSnapshot(revision,handle.before().authorityTick(),handle.after().authorityTick(),handle.before().root().origin(),handle.after().root().origin(),m.pieces()));
    }
    public Optional<AuthoritativeFrame> authoritativeFrame(LivingEntity entity) {var frame=tickFrames.get(entity);return frame==null?Optional.empty():Optional.of(new AuthoritativeFrame(frame.tick,frame.sample));}
    @Deprecated public Optional<AuthoritativeInputs> authoritativeInputs(LivingEntity entity) {return authoritativeFrame(entity).map(frame->new AuthoritativeInputs(frame.tick(),frame.sample().inputs()));}
    public long evaluations(){return evaluations;} public long jointEvaluations(){return jointEvaluations;}
    public int cachedJointEndpoints(LivingEntity entity) {synchronized(jointCache){var endpoints=jointCache.get(entity);return endpoints==null?0:(endpoints.previous==null?0:1)+(endpoints.current==null?0:1);}}
    public long revision(){return revision;}
    public Optional<Snapshot> sampleInterpolated(LivingEntity entity,AnatomyPoseHistory history,double tick) {
        if(!entity.isAlive() || history.current()==null || history.current().revision()!=revision)return Optional.empty();
        var segment=history.segment(tick);var trajectory=trajectories.get(entity);
        if(trajectory==null || !trajectory.before.equals(segment.before()) || !trajectory.after.equals(segment.after())){trajectory=new Trajectory(segment,motionBetween(entity,segment.before(),segment.after()));trajectories.put(entity,trajectory);}
        if(trajectory.fraction!=segment.fraction()) {trajectory.fraction=segment.fraction();try{trajectory.sample=trajectory.motion.map(motion->{evaluations++;return new Snapshot(revision,motion.evaluate(segment.fraction()).pieces());});}catch(RuntimeException rejectedGeometry){trajectory.sample=Optional.empty();}}
        return trajectory.sample;
    }
    public Optional<HierarchyMotion.EvaluatedFrame> evaluatePresentation(LivingEntity cacheKey,GeometryProvider.CausalEndpoint endpoint) {
        if(cacheKey==null || endpoint==null || endpoint.availability()!=GeometryProvider.Availability.AVAILABLE)return Optional.empty();
        var sample=endpoint.sample();var transforms=joints(cacheKey,sample.inputs());if(transforms.isEmpty())return Optional.empty();
        Matrix4f root=sample.gravity().matrix().rotateY((float)Math.toRadians(180-sample.yaw())).scale(sample.scale());evaluations++;
        try{return Optional.of(HierarchyMotion.withRootTrs(geometry,transforms.get(),transforms.get(),root,root,sample.origin(),sample.origin(),filter).evaluate(1));}catch(RuntimeException rejectedGeometry){return Optional.empty();}
    }
    public Optional<HierarchyMotion> motionBetween(AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after) {return motionBetween(before,after,evaluateJoints(before.inputs()),evaluateJoints(after.inputs()));}
    public Optional<HierarchyMotion> motionBetween(LivingEntity entity,AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after) {return motionBetween(before,after,joints(entity,before.inputs()),joints(entity,after.inputs()));}
    private Optional<HierarchyMotion> motionBetween(AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after,Optional<Map<String,Matrix4f>> a,Optional<Map<String,Matrix4f>> b) {
        if(before.origin().distanceToSqr(after.origin())>16 || !before.gravity().equals(after.gravity()) || a.isEmpty() || b.isEmpty())return Optional.empty();
        Matrix4f rootA=before.gravity().matrix().rotateY((float)Math.toRadians(180-before.yaw())).scale(before.scale());
        Matrix4f rootB=after.gravity().matrix().rotateY((float)Math.toRadians(180-after.yaw())).scale(after.scale());
        try{return Optional.of(HierarchyMotion.withRootTrs(geometry,a.get(),b.get(),rootA,rootB,before.origin(),after.origin(),filter));}catch(RuntimeException unsupportedInterval){return Optional.empty();}
    }
    public Optional<Snapshot> sample(LivingEntity entity) {var inputs=channels.get(entity);if(inputs==null || !entity.isAlive())return Optional.empty();return sampleAt(entity,new AnatomyPoseHistory.Sample(inputs,entity.position(),entity.yBodyRot,entity.getScale(),AnatomyMovement.gravity(entity)));}
    public Optional<Snapshot> sampleAt(LivingEntity entity,AnatomyPoseHistory.Sample frame) {
        if(!entity.isAlive())return Optional.empty();var inputs=frame.inputs();float scale=frame.scale();
        if(!Float.isFinite(scale)||scale<=0||!Float.isFinite(frame.yaw())||!Double.isFinite(frame.origin().x)||!Double.isFinite(frame.origin().y)||!Double.isFinite(frame.origin().z))return Optional.empty();
        var key=new Key(inputs,frame.origin(),frame.yaw(),scale,frame.gravity());var previous=cache.get(entity);if(previous!=null&&previous.key.equals(key))return previous.snapshot;
        var transforms=joints(entity,inputs);Optional<Snapshot> snapshot=Optional.empty();
        if(transforms.isPresent()){Matrix4f root=frame.gravity().matrix().rotateY((float)Math.toRadians(180-frame.yaw())).scale(scale).mul(ModelGeometry.matrix(geometry.modelTransform()));Map<String,ConvexBox> pieces=new LinkedHashMap<>();
            try{geometry.evaluate(root,transforms.get(),filter).forEach((id,box)->pieces.put(id,box.move(frame.origin())));snapshot=Optional.of(new Snapshot(revision,pieces));evaluations++;}catch(RuntimeException rejectedGeometry){snapshot=Optional.empty();}}
        cache.put(entity,new Cached(key,snapshot));return snapshot;
    }
    private Optional<Map<String,Matrix4f>> joints(LivingEntity entity,PoseEngine.Inputs inputs) {synchronized(jointCache){var endpoints=jointCache.computeIfAbsent(entity,ignored->new JointEndpoints());if(endpoints.current!=null&&endpoints.current.inputs().equals(inputs))return endpoints.current.transforms();if(endpoints.previous!=null&&endpoints.previous.inputs().equals(inputs))return endpoints.previous.transforms();var endpoint=new JointEndpoint(inputs,evaluateJoints(inputs));endpoints.previous=endpoints.current;endpoints.current=endpoint;return endpoint.transforms();}}
    private Optional<Map<String,Matrix4f>> evaluateJoints(PoseEngine.Inputs inputs) {
        jointEvaluations++;
        try{var evaluated=poses.evaluate(inputs);if(evaluated==null||evaluated.isEmpty())return Optional.empty();Map<String,Matrix4f> copy=new LinkedHashMap<>();evaluated.get().forEach((id,matrix)->copy.put(id,new Matrix4f(matrix)));geometry.transforms(copy);return Optional.of(Collections.unmodifiableMap(copy));}
        catch(RuntimeException rejectedPose){return Optional.empty();}
    }
}
