package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Common original-model evaluator. Pose channels are supplied by authority, never by a renderer. */
public final class ModelGeometryProvider implements GeometryProvider {
    // Vanilla Entity equality is network-ID based; all reusable provider caches need identity keys.
    private static <V> Map<LivingEntity,V> entityMap(){return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<LivingEntity,V>makeMap());}
    private final ModelGeometry geometry;
    private final PoseProvider poses;
    private final AnatomyFilter filter;
    private final long revision;
    private final Map<LivingEntity,PoseProvider.Inputs> channels=entityMap();
    private final Map<LivingEntity,Cached> cache=entityMap();
    /** Two immutable authority-joint endpoints per support, independent of root TRS rebuilds. */
    private record JointEndpoint(PoseProvider.Inputs inputs,Optional<Map<String,Matrix4f>> transforms) {}
    private static final class JointEndpoints {JointEndpoint previous,current;}
    private final Map<LivingEntity,JointEndpoints> jointCache=entityMap();
    private final Map<LivingEntity,Trajectory> trajectories=entityMap();
    private record TickFrame(long tick,AnatomyPoseHistory.Sample sample) {}
    /** Immutable endpoint captured by {@link #tick}, never reconstructed from live entity state. */
    public record AuthoritativeFrame(long tick,AnatomyPoseHistory.Sample sample) {
        public AuthoritativeFrame {
            if(tick<0 || sample==null)throw new IllegalArgumentException("Invalid authoritative frame");
        }
    }
    /** Compatibility view while callers migrate to the complete immutable endpoint. */
    @Deprecated public record AuthoritativeInputs(long tick,PoseProvider.Inputs inputs) {}
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
        // Material Q1 endpoints may later replace only root TRS in this tick.
        // Cache the authority joint endpoint now, so that queryFrame/sampleAt
        // rebuilds convexes with the captured root without advancing joints.
        joints(entity,inputs);
        if(previous!=null && previous.tick()+1==tick && previous.sample().gravity().equals(frame.gravity()))
            tickMotions.put(entity,motionBetween(entity,previous.sample(),frame).map(m->new MotionSnapshot(revision,previous.tick(),tick,
                previous.sample().origin(),frame.origin(),m.pieces())));
        else tickMotions.put(entity,Optional.empty());
        tickFrames.put(entity,new TickFrame(tick,frame));
    }
    public Optional<MotionSnapshot> motion(LivingEntity entity) {
        var frame=tickFrames.get(entity);
        if(frame!=null && frame.tick()==entity.level().getGameTime())return tickMotions.getOrDefault(entity,Optional.empty());
        return Optional.empty();
    }
    /**
     * Last server authority frame for this bound support. Reading it never
     * samples joints, advances the tracker, or substitutes render state.
     */
    public Optional<AuthoritativeFrame> authoritativeFrame(LivingEntity entity) {
        var frame=tickFrames.get(entity);
        return frame==null?Optional.empty():Optional.of(new AuthoritativeFrame(frame.tick(),frame.sample()));
    }
    /** @deprecated Consumers need origin/yaw/scale/gravity from {@link #authoritativeFrame}. */
    @Deprecated public Optional<AuthoritativeInputs> authoritativeInputs(LivingEntity entity) {
        return authoritativeFrame(entity).map(frame->new AuthoritativeInputs(frame.tick(),frame.sample().inputs()));
    }
    /** Convex/root rebuilds; distinct from deterministic joint-channel evaluation. */
    public long evaluations(){return evaluations;}
    /** Number of pose-provider evaluations, bounded to one per cached authority input endpoint. */
    public long jointEvaluations(){return jointEvaluations;}
    /** Diagnostics for the two-frame per-support joint retention bound. */
    public int cachedJointEndpoints(LivingEntity entity) {
        synchronized(jointCache) {
            var endpoints=jointCache.get(entity);
            return endpoints==null?0:(endpoints.previous==null?0:1)+(endpoints.current==null?0:1);
        }
    }
    /** Catalog revision captured with this immutable model/provider binding. */
    public long revision(){return revision;}
    /** Same joint-space trajectory as continuous collision; cached per support and sample time. */
    public Optional<Snapshot> sampleInterpolated(LivingEntity entity,AnatomyPoseHistory history,double tick) {
        if(!entity.isAlive() || history.current()==null || history.current().revision()!=revision)return Optional.empty();
        var segment=history.segment(tick);var trajectory=trajectories.get(entity);
        if(trajectory==null || !trajectory.before.equals(segment.before()) || !trajectory.after.equals(segment.after())) {
            trajectory=new Trajectory(segment,motionBetween(entity,segment.before(),segment.after()));trajectories.put(entity,trajectory);
        }
        if(trajectory.fraction!=segment.fraction()) {
            trajectory.fraction=segment.fraction();
            trajectory.sample=trajectory.motion.map(motion->{
                evaluations++;return new Snapshot(revision,motion.evaluate(segment.fraction()).pieces());
            });
        }
        return trajectory.sample;
    }
    /**
     * Current-only presentation seam. cacheKey selects cached joints and is never
     * observed for live root TRS; endpoint.sample is the complete authority frame.
     */
    public Optional<HierarchyMotion.EvaluatedFrame> evaluatePresentation(LivingEntity cacheKey,GeometryProvider.CausalEndpoint endpoint) {
        if(cacheKey==null || endpoint==null || endpoint.availability()!=GeometryProvider.Availability.AVAILABLE)return Optional.empty();
        var sample=endpoint.sample();var transforms=joints(cacheKey,sample.inputs());
        if(transforms.isEmpty())return Optional.empty();
        Matrix4f root=sample.gravity().matrix().rotateY((float)Math.toRadians(180-sample.yaw())).scale(sample.scale());
        evaluations++;
        return Optional.of(HierarchyMotion.withRootTrs(geometry,transforms.get(),transforms.get(),root,root,sample.origin(),sample.origin(),filter).evaluate(1));
    }
    /** The physical interpolation is joint TRS between authoritative endpoint poses, not matrix lerp. */
    public Optional<HierarchyMotion> motionBetween(AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after) {
        return motionBetween(before,after,evaluateJoints(before.inputs()),evaluateJoints(after.inputs()));
    }
    /** Physical path: retain only the two authority endpoints needed by this support. */
    public Optional<HierarchyMotion> motionBetween(LivingEntity entity,AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after) {
        return motionBetween(before,after,joints(entity,before.inputs()),joints(entity,after.inputs()));
    }
    private Optional<HierarchyMotion> motionBetween(AnatomyPoseHistory.Sample before,AnatomyPoseHistory.Sample after,
            Optional<Map<String,Matrix4f>> a,Optional<Map<String,Matrix4f>> b) {
        if(before.origin().distanceToSqr(after.origin())>16 || !before.gravity().equals(after.gravity()))return Optional.empty();
        if(a.isEmpty() || b.isEmpty())return Optional.empty();
        Matrix4f rootA=before.gravity().matrix().rotateY((float)Math.toRadians(180-before.yaw())).scale(before.scale());
        Matrix4f rootB=after.gravity().matrix().rotateY((float)Math.toRadians(180-after.yaw())).scale(after.scale());
        return Optional.of(HierarchyMotion.withRootTrs(geometry,a.get(),b.get(),rootA,rootB,before.origin(),after.origin(),filter));
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
        var transforms=joints(entity,inputs);
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
    private Optional<Map<String,Matrix4f>> joints(LivingEntity entity,PoseProvider.Inputs inputs) {
        synchronized(jointCache) {
            var endpoints=jointCache.computeIfAbsent(entity,ignored->new JointEndpoints());
            if(endpoints.current!=null && endpoints.current.inputs().equals(inputs))return endpoints.current.transforms();
            if(endpoints.previous!=null && endpoints.previous.inputs().equals(inputs))return endpoints.previous.transforms();
            var endpoint=new JointEndpoint(inputs,evaluateJoints(inputs));
            endpoints.previous=endpoints.current;endpoints.current=endpoint;return endpoint.transforms();
        }
    }
    /** Standalone proof has no support identity; runtime callers retain per-support endpoints. */
    private Optional<Map<String,Matrix4f>> evaluateJoints(PoseProvider.Inputs inputs) {
        var evaluated=poses.evaluate(geometry,inputs).map(transforms->{
            Map<String,Matrix4f> copy=new LinkedHashMap<>();
            transforms.forEach((id,matrix)->copy.put(id,new Matrix4f(matrix)));
            return Collections.unmodifiableMap(copy);
        });
        jointEvaluations++;return evaluated;
    }
}
