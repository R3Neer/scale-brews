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
import net.minecraft.core.Direction;
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

/** S07 adversarial holdout for pair-local fail-closed semantics after mutually incompatible valid plans. */
public final class S07PlanConflictLocalizationTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void simultaneousValidPlansThatWouldOverlapMustReleaseOnlyAffectedPairs(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var leftBody=h.makeMockServerPlayerInLevel();
        var rightBody=h.makeMockServerPlayerInLevel();
        var safe=h.makeMockServerPlayerInLevel();
        for(var body:List.of(leftBody,rightBody,safe)) {
            body.setNoGravity(true);
            body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        }

        Vec3 base=support.position().add(0,1,0);
        leftBody.setPos(base.add(-.30,0,0));
        rightBody.setPos(base.add(.30,0,0));
        safe.setPos(base.add(0,0,8));
        var leftBox=leftBody.getBoundingBox();
        var rightBox=rightBody.getBoundingBox();
        var safeBox=safe.getBoundingBox();

        // ConvexBox.of routes coordinates through JOML floats. GameTest worlds can sit at very
        // large absolute X/Z, where sub-block world coordinates collapse at float precision.
        // Build exact fixture geometry in a small local frame and translate in double afterwards.
        double leftHalfX=leftBox.getXsize()*.5,leftHalfZ=leftBox.getZsize()*.5,leftHeight=leftBox.getYsize();
        double rightHalfX=rightBox.getXsize()*.5,rightHalfZ=rightBox.getZsize()*.5,rightHeight=rightBox.getYsize();
        double safeHalfX=safeBox.getXsize()*.5,safeHalfZ=safeBox.getZsize()*.5;
        double leftFace=-.30-leftHalfX,rightFace=.30+rightHalfX;
        var leftWall=ConvexBox.of(new AABB(leftFace-.5,-.5,-leftHalfZ-.5,
            leftFace,leftHeight+.5,leftHalfZ+.5),new Matrix4f()).move(base);
        var rightWall=ConvexBox.of(new AABB(rightFace,-.5,-rightHalfZ-.5,
            rightFace+.5,rightHeight+.5,rightHalfZ+.5),new Matrix4f()).move(base);
        var safeFloor=ConvexBox.of(new AABB(-safeHalfX-.5,-.5,8-safeHalfZ-.5,
            safeHalfX+.5,0,8+safeHalfZ+.5),new Matrix4f()).move(base);
        var leftDelta=new Vec3(.30,0,0);
        var rightDelta=new Vec3(-.30,0,0);
        var leftMotion=new ConservativeSweep.Motion(t->leftWall.move(leftDelta.scale(t)),0,leftDelta);
        var rightMotion=new ConservativeSweep.Motion(t->rightWall.move(rightDelta.scale(t)),0,rightDelta);
        var pieces=Map.of("left",leftMotion,"right",rightMotion);

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,e->Optional.of(new GeometryProvider.Snapshot(98,
            Map.of("left",leftWall,"right",rightWall,"safe",safeFloor))));
        AnatomyMovement.gravity(leftBody,new GravityFrame(Direction.WEST));
        AnatomyMovement.gravity(rightBody,new GravityFrame(Direction.EAST));
        AnatomyMovement.gravity(safe,GravityFrame.VANILLA);
        try {
            h.assertTrue(Platforms.eligible(leftBody,support) && Platforms.eligible(rightBody,support) && Platforms.eligible(safe,support),
                "All bodies must be eligible so only the material-plan conflict is under test");
            h.assertTrue(confirm(h,leftBody,support,leftWall,"left",98)
                    && confirm(h,rightBody,support,rightWall,"right",98)
                    && confirm(h,safe,support,safeFloor,"safe",98),
                "All three fixture contacts must be valid before the material event");

            h.assertTrue(AnatomyMovement.supported(leftBody),fixtureState("left",leftBody,leftWall));
            h.assertTrue(AnatomyMovement.supported(rightBody),fixtureState("right",rightBody,rightWall));
            h.assertTrue(AnatomyMovement.supported(safe),fixtureState("safe",safe,safeFloor));
            AnatomyMovement.afterMove(leftBody);AnatomyMovement.afterMove(rightBody);AnatomyMovement.afterMove(safe);
            h.assertTrue(AnatomyMovement.supported(leftBody),"Left retained contact became invalid after afterMove: "+fixtureState("left",leftBody,leftWall));
            h.assertTrue(AnatomyMovement.supported(rightBody),"Right retained contact became invalid after afterMove: "+fixtureState("right",rightBody,rightWall));
            h.assertTrue(AnatomyMovement.supported(safe),"Safe retained contact became invalid after afterMove: "+fixtureState("safe",safe,safeFloor));

            // Prove each plan is individually complete under the same world clip. The only reason
            // the backend may reject the pair together is the simultaneous final body/body overlap.
            var leftPlan=resolveIndividually(level,leftBody,leftBox,pieces);
            var rightPlan=resolveIndividually(level,rightBody,rightBox,pieces);
            h.assertTrue(leftPlan.status()==TemporalResponse.Status.COMPLETE && rightPlan.status()==TemporalResponse.Status.COMPLETE,
                "Each candidate must have a complete bounded temporal plan before the batch conflict: left="+leftPlan+" right="+rightPlan);
            h.assertTrue(leftPlan.displacement().x>.25 && rightPlan.displacement().x<-.25,
                "Fixture plans must move the bodies toward one another: left="+leftPlan.displacement()+" right="+rightPlan.displacement());
            var finalLeft=leftBox.move(leftPlan.displacement());
            var finalRight=rightBox.move(rightPlan.displacement());
            h.assertTrue(overlapVolume(finalLeft,finalRight)>1e-12,
                "Fixture plans must be individually valid but mutually incompatible at the final instant");

            var prepared=prepared(support,leftWall,rightWall,leftMotion,rightMotion);
            Vec3 leftBefore=leftBody.position(),rightBefore=rightBody.position(),safeBefore=safe.position();
            var outcome=resolve(level,support,prepared,List.of(leftBody,rightBody));
            h.assertTrue(outcome.status()==MaterialEventDispatcher.Status.QUARANTINED
                    && outcome.reason()==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED,
                "Mutually incompatible plans must fail closed as explicit BACKEND_EXHAUSTED: "+outcome);
            h.assertTrue(leftBody.position().equals(leftBefore) && rightBody.position().equals(rightBefore) && safe.position().equals(safeBefore),
                "Rejected simultaneous plans must not apply partial displacement");

            h.assertTrue(!AnatomyMovement.supported(leftBody) && !AnatomyMovement.supported(rightBody),
                "NFR-004 requires both unresolved retained body/support relations to be released or suspended after a plan-conflict exhaustion");
            h.assertTrue(AnatomyMovement.contact(safe)!=null && AnatomyMovement.supported(safe),
                "A third body not involved in the conflicting candidate set must retain its valid contact on the same support");
        } finally {
            AnatomyMovement.clear(leftBody);AnatomyMovement.clear(rightBody);AnatomyMovement.clear(safe);
            AnatomyMovement.gravity(leftBody,GravityFrame.VANILLA);AnatomyMovement.gravity(rightBody,GravityFrame.VANILLA);
            AnatomyMovement.deactivate(level);
            support.discard();leftBody.discard();rightBody.discard();safe.discard();
        }
        h.succeed();
    }

    private static String fixtureState(String name,Entity body,ConvexBox piece) {
        var separation=piece.separation(body.getBoundingBox());
        return name+" gap="+separation.gap()+" sepNormal="+separation.normal()+" gravity="+AnatomyMovement.gravity(body).down()
            +" contact="+AnatomyMovement.contact(body);
    }

    private static TemporalResponse.Result resolveIndividually(ServerLevel level,Entity body,AABB captured,
            Map<String,ConservativeSweep.Motion> pieces) {
        Entity previous=PlatformPhysics.enter(body);
        try {
            return TemporalResponse.resolve(captured,Vec3.ZERO,pieces,32,256,
                (box,delta)->Entity.collideBoundingBox(body,delta,box,level,level.getEntityCollisions(body,box.expandTowards(delta))));
        } finally {PlatformPhysics.exit(previous);}
    }

    private static boolean confirm(GameTestHelper h,Entity body,LivingEntity support,ConvexBox piece,String id,long revision) {
        var separation=piece.separation(body.getBoundingBox());
        int face=piece.closestFace(separation.normal());
        Vec3 normal=piece.faceNormal(face);
        Vec3 local=piece.facePoint(face,body.getBoundingBox().getCenter());
        return AnatomyMovement.confirm(body,support,new SurfaceContact(support.getUUID(),revision,id,face,local,normal,h.getLevel().getGameTime()));
    }

    private static Object prepared(LivingEntity support,ConvexBox left,ConvexBox right,
            ConservativeSweep.Motion leftMotion,ConservativeSweep.Motion rightMotion) throws Exception {
        long tick=support.level().getGameTime();
        long registration=AnatomyMovement.registrationGeneration(support);
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),98,
            Identifier.parse("test:s07_plan_conflict"),Identifier.parse("test:s07_plan_conflict_pose"),1,registration);
        var beforeRoot=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,tick,support.position(),0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,beforeRoot.origin(),0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,afterRoot.origin(),0,1,GravityFrame.VANILLA);
        var beforePieces=Map.of("left",left,"right",right);
        var afterPieces=Map.of("left",leftMotion.at().apply(1),"right",rightMotion.at().apply(1));
        var before=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(98,beforePieces));
        var after=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(98,afterPieces));
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        AABB envelope=union(union(left.bounds(),leftMotion.at().apply(1).bounds()),union(right.bounds(),rightMotion.at().apply(1).bounds()))
            .inflate(ConservativeSweep.SKIN);
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.JOINT,handle);
        var motion=new GeometryProvider.MotionSnapshot(98,tick,tick,support.position(),support.position(),Map.of("left",leftMotion,"right",rightMotion));
        Class<?> preparedType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Constructor<?> ctor=preparedType.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
        ctor.setAccessible(true);
        return ctor.newInstance(pending,interval,motion);
    }

    @SuppressWarnings("unchecked")
    private static MaterialEventDispatcher.Outcome resolve(ServerLevel level,LivingEntity support,Object prepared,List<Entity> bodies) throws Exception {
        Class<?> backendType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        Constructor<?> backendCtor=backendType.getDeclaredConstructor(ServerLevel.class,List.class);
        backendCtor.setAccessible(true);
        Object backend=backendCtor.newInstance(level,List.of(prepared));
        Class<?> preparedType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Method intervalAccessor=preparedType.getDeclaredMethod("interval");intervalAccessor.setAccessible(true);
        var interval=(MaterialEventDispatcher.MaterialInterval)intervalAccessor.invoke(prepared);
        var event=new MaterialEventDispatcher.Event<Entity>(new MaterialEventDispatcher.EventId(1,0),support,
            MaterialEventDispatcher.Source.JOINT_BATCH,interval,Set.of(support.getUUID()));
        Method resolve=backendType.getDeclaredMethod("resolve",MaterialEventDispatcher.Event.class,List.class);resolve.setAccessible(true);
        var candidates=bodies.stream().map(body->new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox())).toList();
        var result=(MaterialEventDispatcher.Resolution<Entity>)resolve.invoke(backend,event,candidates);
        return result.outcome();
    }

    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }

    private static double overlapVolume(AABB a,AABB b) {
        double x=Math.max(0,Math.min(a.maxX,b.maxX)-Math.max(a.minX,b.minX));
        double y=Math.max(0,Math.min(a.maxY,b.maxY)-Math.max(a.minY,b.minY));
        double z=Math.max(0,Math.min(a.maxZ,b.maxZ)-Math.max(a.minZ,b.minZ));
        return x*y*z;
    }
}
