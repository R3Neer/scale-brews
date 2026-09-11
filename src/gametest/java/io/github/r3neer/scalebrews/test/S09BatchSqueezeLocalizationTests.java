package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 reserved holdout: an irrecoverable culprit in one joint batch may not destroy another valid support relation. */
public final class S09BatchSqueezeLocalizationTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void irrecoverableSqueezeMustLocalizeFailureWhenValidSupportSharesBatch(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var culprit=h.spawn(EntityTypes.COW,2,20,2);
        var stable=h.spawn(EntityTypes.COW,22,20,2);
        for(var support:List.of(culprit,stable)) {
            support.setNoAi(true);support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        }
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        Vec3 base=culprit.position().add(0,1,0);body.setPos(base);
        var captured=body.getBoundingBox();
        double halfX=captured.getXsize()*.5,halfZ=captured.getZsize()*.5,height=captured.getYsize();
        double gap=.08,inward=.18,floorGap=.01;

        // Two symmetric pieces of the same culprit support arrive at exactly the same
        // material time. Their required +X/-X advances are mutually infeasible.
        var left0=ConvexBox.of(new AABB(-halfX-gap-.3,-.3,-halfZ-.4,-halfX-gap,height+.3,halfZ+.4),new Matrix4f()).move(base);
        var right0=ConvexBox.of(new AABB(halfX+gap,-.3,-halfZ-.4,halfX+gap+.3,height+.3,halfZ+.4),new Matrix4f()).move(base);
        Vec3 leftDelta=new Vec3(inward,0,0),rightDelta=new Vec3(-inward,0,0);
        var left1=left0.move(leftDelta);var right1=right0.move(rightDelta);
        var leftMotion=new ConservativeSweep.Motion(t->left0.move(leftDelta.scale(t)),0,leftDelta);
        var rightMotion=new ConservativeSweep.Motion(t->right0.move(rightDelta.scale(t)),0,rightDelta);

        // The other support is a harmless retained floor. Keep it farther than CCD skin
        // yet inside the public retained-contact tolerance, so it is materially valid but
        // cannot be blamed for the squeeze solver failure.
        var floor=ConvexBox.of(new AABB(-halfX-.4,-floorGap-.2,-halfZ-.4,halfX+.4,-floorGap,halfZ+.4),new Matrix4f()).move(base);
        var floorMotion=new ConservativeSweep.Motion(t->floor,0,Vec3.ZERO);

        AnatomyMovement.activate(level);
        // Current culprit geometry is the squeezed endpoint. A correct pair-local failure
        // must therefore leave only this body/support pair suspended while the overlap lasts.
        AnatomyMovement.register(culprit,e->Optional.of(new GeometryProvider.Snapshot(121,Map.of("left",left1,"right",right1))));
        AnatomyMovement.register(stable,e->Optional.of(new GeometryProvider.Snapshot(122,Map.of("floor",floor))));
        try {
            h.assertTrue(Platforms.eligible(body,culprit) && Platforms.eligible(body,stable),
                "Fixture requires both supports to be real eligible participants in the same batch");
            h.assertTrue(left0.separation(captured).gap()>ConservativeSweep.SKIN
                    && right0.separation(captured).gap()>ConservativeSweep.SKIN
                    && left1.overlaps(captured) && right1.overlaps(captured),
                "Culprit pieces must start clear and finish as a genuine symmetric squeeze");
            h.assertTrue(!floor.overlaps(captured) && floor.separation(captured).gap()>ConservativeSweep.SKIN
                    && floor.separation(captured).gap()<.025,
                "Stable floor must be retained-valid without participating in the squeeze CCD");
            h.assertTrue(confirm(h,body,stable,floor,"floor",122),
                "Fixture must establish the valid retained relation before the joint batch");
            AnatomyMovement.afterMove(body);
            h.assertTrue(AnatomyMovement.supported(body) && AnatomyMovement.contact(body).support()==stable,
                "Stable relation must be authoritative before the culprit interval is resolved");

            var squeezeOnly=resolveKernel(level,body,captured,Map.of("a/left",leftMotion,"a/right",rightMotion));
            var stableOnly=resolveKernel(level,body,captured,Map.of("b/floor",floorMotion));
            h.assertTrue(squeezeOnly.status()==TemporalResponse.Status.ITERATION_LIMIT,
                "Calibration requires the culprit's opposing equal-time constraints to be irrecoverable: "+squeezeOnly);
            h.assertTrue(stableOnly.status()==TemporalResponse.Status.COMPLETE
                    && stableOnly.displacement().lengthSqr()<1e-20,
                "Calibration requires the retained support to be harmless by itself: "+stableOnly);

            var preparedA=prepared(culprit,121,"s09_squeeze",1,
                Map.of("left",left0,"right",right0),Map.of("left",left1,"right",right1),
                Map.of("left",leftMotion,"right",rightMotion));
            var preparedB=prepared(stable,122,"s09_stable",1,
                Map.of("floor",floor),Map.of("floor",floor),Map.of("floor",floorMotion));
            var events=List.of(event(2,0,culprit,preparedA),event(2,1,stable,preparedB));
            h.assertTrue(events.get(0).interval().envelope().inflate(.05).intersects(events.get(1).interval().envelope()),
                "Both supports must inhabit one plausible spatial joint cluster");

            Vec3 before=body.position();
            var result=resolveBatch(level,List.of(preparedA,preparedB),events,body);
            h.assertTrue(result.outcomes().size()==2 && result.outcomes().stream().allMatch(outcome->
                    outcome.status()==MaterialEventDispatcher.Status.QUARANTINED
                        && outcome.reason()==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED),
                "Irrecoverable simultaneous squeeze must fail closed as BACKEND_EXHAUSTED: "+result.outcomes());
            h.assertTrue(body.position().equals(before),
                "Rejected squeeze batch must not apply a partial displacement");

            var survivor=AnatomyMovement.contact(body);
            boolean culpritSuspended=AnatomyMovement.suspended(body,culprit);
            boolean stableSuspended=AnatomyMovement.suspended(body,stable);
            h.assertTrue(culpritSuspended && !stableSuspended && survivor!=null
                    && survivor.support()==stable && AnatomyMovement.supported(body),
                "FR-052/NFR-004 require pair-local failure: culprit must suspend while the valid retained support survives. culpritSuspended="
                    +culpritSuspended+", stableSuspended="+stableSuspended+", contact="+survivor);
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);
            culprit.discard();stable.discard();body.discard();
        }
        h.succeed();
    }

    private static boolean confirm(GameTestHelper h,Entity body,LivingEntity support,ConvexBox piece,String id,long revision) {
        var separation=piece.separation(body.getBoundingBox());
        int face=piece.closestFace(separation.normal());
        Vec3 normal=piece.faceNormal(face);
        Vec3 local=piece.facePoint(face,body.getBoundingBox().getCenter());
        return AnatomyMovement.confirm(body,support,new SurfaceContact(support.getUUID(),revision,id,face,local,normal,h.getLevel().getGameTime()));
    }

    private static TemporalResponse.Result resolveKernel(ServerLevel level,Entity body,AABB captured,
            Map<String,ConservativeSweep.Motion> pieces) {
        Entity previous=PlatformPhysics.enter(body);
        try {
            return TemporalResponse.resolve(captured,Vec3.ZERO,pieces,32,256,
                (box,delta)->Entity.collideBoundingBox(body,delta,box,level,level.getEntityCollisions(body,box.expandTowards(delta))));
        } finally {PlatformPhysics.exit(previous);}
    }

    private static Object prepared(LivingEntity support,long revision,String id,long materialSerial,
            Map<String,ConvexBox> beforePieces,Map<String,ConvexBox> afterPieces,
            Map<String,ConservativeSweep.Motion> motions) throws Exception {
        long tick=support.level().getGameTime();
        long registration=AnatomyMovement.registrationGeneration(support);
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
            Identifier.parse("test:"+id),Identifier.parse("test:"+id+"_pose"),1,registration);
        var beforeRoot=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,tick,support.position(),0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,beforeRoot.origin(),0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,afterRoot.origin(),0,1,GravityFrame.VANILLA);
        var before=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision,beforePieces));
        var after=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision,afterPieces));
        var handle=new GeometryProvider.MotionIntervalHandle(identity,materialSerial,before,after);
        AABB envelope=null;
        for(var motion:motions.values()) {
            var first=motion.at().apply(0);
            var bounds=first.bounds().inflate(motion.maxPointSpeed()+ConservativeSweep.SKIN);
            envelope=envelope==null?bounds:union(envelope,bounds);
        }
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.JOINT,handle);
        var snapshot=new GeometryProvider.MotionSnapshot(revision,tick,tick,support.position(),support.position(),motions);
        Class<?> preparedType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Constructor<?> ctor=preparedType.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
        ctor.setAccessible(true);
        return ctor.newInstance(pending,interval,snapshot);
    }

    private static MaterialEventDispatcher.Event<Entity> event(long sequence,int member,LivingEntity support,Object prepared) throws Exception {
        Class<?> preparedType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Method intervalAccessor=preparedType.getDeclaredMethod("interval");intervalAccessor.setAccessible(true);
        var interval=(MaterialEventDispatcher.MaterialInterval)intervalAccessor.invoke(prepared);
        return new MaterialEventDispatcher.Event<>(new MaterialEventDispatcher.EventId(sequence,member),support,
            MaterialEventDispatcher.Source.JOINT_BATCH,interval,Set.of(support.getUUID()));
    }

    @SuppressWarnings("unchecked")
    private static MaterialEventDispatcher.BatchResolution<Entity> resolveBatch(ServerLevel level,List<Object> prepared,
            List<MaterialEventDispatcher.Event<Entity>> events,Entity body) throws Exception {
        Class<?> backendType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        Constructor<?> ctor=backendType.getDeclaredConstructor(ServerLevel.class,List.class);ctor.setAccessible(true);
        Object backend=ctor.newInstance(level,prepared);
        Method resolve=backendType.getDeclaredMethod("resolveJointBatch",List.class,List.class);resolve.setAccessible(true);
        var candidate=new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox());
        return (MaterialEventDispatcher.BatchResolution<Entity>)resolve.invoke(backend,events,List.of(candidate));
    }

    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }
}
