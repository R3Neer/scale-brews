package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Common original-model evaluator. Pose channels and root authority are supplied by server-safe sources, never by a renderer. */
public final class ModelGeometryProvider implements GeometryProvider {
    private static <V> Map<LivingEntity,V> entityMap(){return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,V>makeMap());}
    private final ModelGeometry geometry;
    /** Immutable evaluator already bound to one accepted catalog revision. */
    private final PoseEngine.Bound poses;
    /** Immutable root authority already resolved from the same accepted catalog revision. */
    private final RootTransformProvider roots;
    private final AnatomyFilter filter;
    private final long revision;
    private final Map<LivingEntity,PoseEngine.Inputs> channels=entityMap();
    private final Map<LivingEntity,Cached> cache=entityMap();
    private record JointEndpoint(PoseEngine.Inputs inputs,Optional<Map<String,Matrix4f>> transforms) {}
    private static final class JointEndpoints {JointEndpoint previous,current;}
    private final Map<LivingEntity,JointEndpoints> jointCache=entityMap();
    private final Map<LivingEntity,Trajectory> trajectories=entityMap();
    private record TickFrame(long tick,AnatomyPoseHistory.Sample sample,RootTransformProvider.RootTransform root) {}
    public record AuthoritativeFrame(long tick,AnatomyPoseHistory.Sample sample,RootTransformProvider.RootTransform root) {
        /** Source-compatible pre-S21 view; callers that need root authority must use {@link #root()}. */
        public AuthoritativeFrame(long tick,AnatomyPoseHistory.Sample sample) {
            this(tick,sample,legacyRoot(sample));
        }
        public AuthoritativeFrame {
            if(tick<0 || sample==null || root==null || !root.origin().equals(sample.origin()) || Float.compare(root.scale(),sample.scale())!=0)
                throw new IllegalArgumentException("Invalid authoritative frame");
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
    /** Root and joint cache keys are deliberately separate: joints() is keyed only by PoseEngine.Inputs. */
    private record Key(PoseEngine.Inputs inputs,RootTransformProvider.RootTransform root) {}
    private record Cached(Key key,Optional<Snapshot> snapshot) {}

    /** Procedural/source compatibility constructor; data-backed engines must use the bound constructor. */
    public ModelGeometryProvider(ModelGeometry geometry,PoseEngine poses,AnatomyFilter filter,long revision) {
        this(geometry,bind(geometry,poses,Map.of()),BuiltInRootTransformProviders.entityRoot(),filter,revision);
    }
    /** Pure/test compatibility seam. The evaluator is bound immediately and carries no catalog/resource lookup. */
    public ModelGeometryProvider(ModelGeometry geometry,
            java.util.function.BiFunction<ModelGeometry,PoseEngine.Inputs,Optional<Map<String,Matrix4f>>> poses,
            AnatomyFilter filter,long revision) {
        this(geometry,(PoseEngine.Bound)(inputs->poses.apply(geometry,inputs)),BuiltInRootTransformProviders.entityRoot(),filter,revision);
    }
    /** Procedural/source compatibility constructor with immutable engine parameters. */
    public ModelGeometryProvider(ModelGeometry geometry,PoseEngine poses,Map<String,String> poseParameters,AnatomyFilter filter,long revision) {
        this(geometry,bind(geometry,poses,poseParameters),BuiltInRootTransformProviders.entityRoot(),filter,revision);
    }
    /** Source-compatible S18 constructor; canonical S21 runtime supplies the accepted root provider explicitly. */
    public ModelGeometryProvider(ModelGeometry geometry,PoseEngine.Bound poses,AnatomyFilter filter,long revision) {
        this(geometry,poses,BuiltInRootTransformProviders.entityRoot(),filter,revision);
    }
    /** Canonical S21 runtime constructor: pose and root behavior are both already resolved for one accepted revision. */
    public ModelGeometryProvider(ModelGeometry geometry,PoseEngine.Bound poses,RootTransformProvider roots,AnatomyFilter filter,long revision) {
        this.geometry=Objects.requireNonNull(geometry);this.poses=Objects.requireNonNull(poses);this.roots=Objects.requireNonNull(roots);this.filter=Objects.requireNonNull(filter);
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
    /** Root authority sampling boundary. Callers that only consume an accepted endpoint must not invoke this again. */
    public Optional<RootTransformProvider.RootTransform> root(LivingEntity entity) {
        if(entity==null)return Optional.empty();
        try {
            var sampled=roots.sample(entity);
            return sampled==null?Optional.empty():sampled;
        } catch(RuntimeException rejectedRoot) {
            return Optional.empty();
        }
    }
    /**
     * Refresh only root authority while preserving the last accepted joint sample/tick.
     * This is the S21 same-tick mutation seam: it never evaluates local joints.
     */
    public Optional<RootTransformProvider.RootTransform> refreshRoot(LivingEntity entity) {
        if(entity==null)return Optional.empty();
        var previous=tickFrames.get(entity);if(previous==null)return Optional.empty();
        var root=root(entity).orElse(null);
        if(root==null){tickFrames.remove(entity);tickMotions.remove(entity);return Optional.empty();}
        var sample=new AnatomyPoseHistory.Sample(previous.sample.inputs(),root.origin(),entity.yBodyRot,root.scale(),AnatomyMovement.gravity(entity));
        tickFrames.put(entity,new TickFrame(previous.tick,sample,root));
        tickMotions.put(entity,Optional.empty());
        return Optional.of(root);
    }
    public void tick(LivingEntity entity,long tick) {
        if(entity==null || tick<0)throw new IllegalArgumentException("Invalid authority tick");
        var previous=tickFrames.get(entity);
        if(previous!=null && tick<previous.tick)throw new IllegalArgumentException("Authority clock cannot rewind");
        if(previous!=null && previous.tick==tick)return;
        if(poseEligibility!=null && !entity.level().isClientSide())pose(entity,authority.tick(entity,tick,poseEligibility.test(entity)));
        var inputs=channels.get(entity);if(inputs==null){tickFrames.remove(entity);tickMotions.remove(entity);return;}
        var root=root(entity).orElse(null);if(root==null){tickFrames.remove(entity);tickMotions.remove(entity);return;}
        var frame=new AnatomyPoseHistory.Sample(inputs,root.origin(),entity.yBodyRot,root.scale(),AnatomyMovement.gravity(entity));
        joints(entity,inputs);
        if(previous!=null && previous.tick+1==tick && previous.sample.gravity().equals(frame.gravity()))
            tickMotions.put(entity,motionBetween(entity,previous.sample,previous.root,frame,root)
                .map(m->new MotionSnapshot(revision,previous.tick,tick,previous.root.origin(),root.origin(),m.pieces())));
        else tickMotions.put(entity,Optional.empty());
        tickFrames.put(entity,new TickFrame(tick,frame,root));
    }
    public Optional<MotionSnapshot> motion(LivingEntity entity) {
        var frame=tickFrames.get(entity);if(frame!=null && frame.tick==entity.level().getGameTime())return tickMotions.getOrDefault(entity,Optional.empty());return Optional.empty();
    }
    @Override public Optional<MotionSnapshot> interval(LivingEntity entity,MotionIntervalHandle handle) {
        if(entity==null || handle==null || !handle.identity().matches(entity) || handle.identity().revision()!=revision
                || handle.before().snapshot().revision()!=revision || handle.after().snapshot().revision()!=revision)return Optional.empty();
        return motionBetween(entity,handle.before().sample(),handle.before().rootTransform(),handle.after().sample(),handle.after().rootTransform())
            .map(m->new MotionSnapshot(revision,handle.before().authorityTick(),handle.after().authorityTick(),
                handle.before().rootTransform().origin(),handle.after().rootTransform().origin(),m.pieces()));
    }
    /**
     * The legacy entity_root is defined directly by live entity origin/yaw/scale/gravity. Those fields can
     * change later in the same tick without an explicit mutation hook, so preserve the pre-S21 observation
     * semantics only for that singleton. Generic providers remain endpoint-driven and are never re-sampled
     * by an ordinary query.
     */
    public Optional<AuthoritativeFrame> authoritativeFrame(LivingEntity entity) {
        var frame=tickFrames.get(entity);if(frame==null)return Optional.empty();
        if(roots==BuiltInRootTransformProviders.entityRoot() && entityRootChanged(entity,frame)) {
            if(refreshRoot(entity).isEmpty())return Optional.empty();
            frame=tickFrames.get(entity);if(frame==null)return Optional.empty();
        }
        return Optional.of(new AuthoritativeFrame(frame.tick,frame.sample,frame.root));
    }
    private static boolean entityRootChanged(LivingEntity entity,TickFrame frame) {
        var sample=frame.sample;
        return !frame.root.origin().equals(entity.position())
            || Float.compare(frame.root.scale(),entity.getScale())!=0
            || Float.compare(sample.yaw(),entity.yBodyRot)!=0
            || !sample.gravity().equals(AnatomyMovement.gravity(entity));
    }
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
        var rootAuthority=endpoint.rootTransform();Matrix4f root=rootMatrix(rootAuthority);evaluations++;
        try{return Optional.of(HierarchyMotion.withRootTrs(geometry,transforms.get(),transforms.get(),root,root,
            rootAuthority.origin(),rootAuthority.origin(),filter).evaluate(1));}catch(RuntimeException rejectedGeometry){return Optional.empty();}
    }
    public Optional<HierarchyMotion> motionBetween(AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after) {
        return motionBetween(before,legacyRoot(before),after,legacyRoot(after),evaluateJoints(before.inputs()),evaluateJoints(after.inputs()));
    }
    public Optional<HierarchyMotion> motionBetween(LivingEntity entity,AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after) {
        return motionBetween(before,legacyRoot(before),after,legacyRoot(after),joints(entity,before.inputs()),joints(entity,after.inputs()));
    }
    private Optional<HierarchyMotion> motionBetween(LivingEntity entity,AnatomyPoseHistory.Sample before,RootTransformProvider.RootTransform rootA,
                                                     AnatomyPoseHistory.Sample after,RootTransformProvider.RootTransform rootB) {
        return motionBetween(before,rootA,after,rootB,joints(entity,before.inputs()),joints(entity,after.inputs()));
    }
    private Optional<HierarchyMotion> motionBetween(AnatomyPoseHistory.Sample before,RootTransformProvider.RootTransform rootA,
                                                     AnatomyPoseHistory.Sample after,RootTransformProvider.RootTransform rootB,
                                                     Optional<Map<String,Matrix4f>> a,Optional<Map<String,Matrix4f>> b) {
        if(rootA.origin().distanceToSqr(rootB.origin())>16 || !before.gravity().equals(after.gravity()) || a.isEmpty() || b.isEmpty())return Optional.empty();
        Matrix4f matrixA=rootMatrix(rootA),matrixB=rootMatrix(rootB);
        try{return Optional.of(HierarchyMotion.withRootTrs(geometry,a.get(),b.get(),matrixA,matrixB,rootA.origin(),rootB.origin(),filter));}catch(RuntimeException unsupportedInterval){return Optional.empty();}
    }
    public Optional<Snapshot> sample(LivingEntity entity) {
        var inputs=channels.get(entity);if(inputs==null || !entity.isAlive())return Optional.empty();
        var root=root(entity).orElse(null);if(root==null)return Optional.empty();
        var frame=new AnatomyPoseHistory.Sample(inputs,root.origin(),entity.yBodyRot,root.scale(),AnatomyMovement.gravity(entity));
        return sampleAt(entity,frame,root);
    }
    /** Source-compatible sampler using the pre-S21 gravity+yaw root convention encoded in the sample. */
    public Optional<Snapshot> sampleAt(LivingEntity entity,AnatomyPoseHistory.Sample frame) {
        return sampleAt(entity,frame,legacyRoot(frame));
    }
    /** S21 sampler: root orientation comes from the accepted root DTO, never reconstructed from renderer/client state. */
    public Optional<Snapshot> sampleAt(LivingEntity entity,AnatomyPoseHistory.Sample frame,RootTransformProvider.RootTransform rootAuthority) {
        if(!entity.isAlive() || frame==null || rootAuthority==null)return Optional.empty();var inputs=frame.inputs();
        if(!rootAuthority.origin().equals(frame.origin()) || Float.compare(rootAuthority.scale(),frame.scale())!=0)return Optional.empty();
        var key=new Key(inputs,rootAuthority);var previous=cache.get(entity);if(previous!=null&&previous.key.equals(key))return previous.snapshot;
        var transforms=joints(entity,inputs);Optional<Snapshot> snapshot=Optional.empty();
        if(transforms.isPresent()){Matrix4f root=rootMatrix(rootAuthority).mul(ModelGeometry.matrix(geometry.modelTransform()));Map<String,ConvexBox> pieces=new LinkedHashMap<>();
            try{geometry.evaluate(root,transforms.get(),filter).forEach((id,box)->pieces.put(id,box.move(rootAuthority.origin())));snapshot=Optional.of(new Snapshot(revision,pieces));evaluations++;}catch(RuntimeException rejectedGeometry){snapshot=Optional.empty();}}
        cache.put(entity,new Cached(key,snapshot));return snapshot;
    }
    private static RootTransformProvider.RootTransform legacyRoot(AnatomyPoseHistory.Sample frame) {
        Objects.requireNonNull(frame,"frame");
        Quaternionf rotation=new Quaternionf().setFromNormalized(new Matrix3f(frame.gravity().matrix()))
            .rotateY((float)Math.toRadians(180.0-frame.yaw()));
        return new RootTransformProvider.RootTransform(frame.origin(),rotation,frame.scale());
    }
    private static Matrix4f rootMatrix(RootTransformProvider.RootTransform root) {
        return new Matrix4f().rotation(root.quaternion()).scale(root.scale());
    }
    private Optional<Map<String,Matrix4f>> joints(LivingEntity entity,PoseEngine.Inputs inputs) {synchronized(jointCache){var endpoints=jointCache.computeIfAbsent(entity,ignored->new JointEndpoints());if(endpoints.current!=null&&endpoints.current.inputs().equals(inputs))return endpoints.current.transforms();if(endpoints.previous!=null&&endpoints.previous.inputs().equals(inputs))return endpoints.previous.transforms();var endpoint=new JointEndpoint(inputs,evaluateJoints(inputs));endpoints.previous=endpoints.current;endpoints.current=endpoint;return endpoint.transforms();}}
    private Optional<Map<String,Matrix4f>> evaluateJoints(PoseEngine.Inputs inputs) {
        jointEvaluations++;
        try{var evaluated=poses.evaluate(inputs);if(evaluated==null||evaluated.isEmpty())return Optional.empty();Map<String,Matrix4f> copy=new LinkedHashMap<>();evaluated.get().forEach((id,matrix)->copy.put(id,new Matrix4f(matrix)));geometry.transforms(copy);return Optional.of(Collections.unmodifiableMap(copy));}
        catch(RuntimeException rejectedPose){return Optional.empty();}
    }
}
