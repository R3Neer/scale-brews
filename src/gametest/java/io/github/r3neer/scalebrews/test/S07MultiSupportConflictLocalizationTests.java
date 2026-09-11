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

/** S07/NFR-004 holdout: a body-level plan conflict may not quarantine an unrelated support relation. */
public final class S07MultiSupportConflictLocalizationTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void conflictOnOneSupportMustNotReleaseStableContactOnAnotherSupport(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var supportA=h.spawn(EntityTypes.COW,2,20,2);
        var supportB=h.spawn(EntityTypes.COW,22,20,2);
        supportA.setNoAi(true);supportA.setNoGravity(true);
        supportB.setNoAi(true);supportB.setNoGravity(true);
        var left=h.makeMockServerPlayerInLevel();
        var right=h.makeMockServerPlayerInLevel();
        for(var body:List.of(left,right)) {
            body.setNoGravity(true);
            body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        }

        Vec3 base=supportA.position().add(0,1,0);
        left.setPos(base.add(-.30,0,0));
        right.setPos(base.add(.30,0,0));
        var leftBox=left.getBoundingBox();
        var rightBox=right.getBoundingBox();
        double leftHalfX=leftBox.getXsize()*.5,leftHalfZ=leftBox.getZsize()*.5,leftHeight=leftBox.getYsize();
        double rightHalfX=rightBox.getXsize()*.5,rightHalfZ=rightBox.getZsize()*.5,rightHeight=rightBox.getYsize();
        double leftFace=-.30-leftHalfX,rightFace=.30+rightHalfX;
        var leftWall=ConvexBox.of(new AABB(leftFace-.5,-.5,-leftHalfZ-.5,leftFace,leftHeight+.5,leftHalfZ+.5),new Matrix4f()).move(base);
        var rightWall=ConvexBox.of(new AABB(rightFace,-.5,-rightHalfZ-.5,rightFace+.5,rightHeight+.5,rightHalfZ+.5),new Matrix4f()).move(base);
        var floor=ConvexBox.of(new AABB(-.30-leftHalfX-.4,-.5,-leftHalfZ-.4,-.30+leftHalfX+.4,0,leftHalfZ+.4),new Matrix4f()).move(base);
        var marker0=ConvexBox.of(new AABB(-.2,.8,1.2,.2,1.2,1.6),new Matrix4f()).move(base);
        var marker1=marker0.move(new Vec3(0,0,.15));

        var leftDelta=new Vec3(.30,0,0);
        var rightDelta=new Vec3(-.30,0,0);
        var leftMotion=new ConservativeSweep.Motion(t->leftWall.move(leftDelta.scale(t)),0,leftDelta);
        var rightMotion=new ConservativeSweep.Motion(t->rightWall.move(rightDelta.scale(t)),0,rightDelta);
        var floorMotion=new ConservativeSweep.Motion(t->floor,0,Vec3.ZERO);
        var markerMotion=new ConservativeSweep.Motion(t->marker0.move(new Vec3(0,0,.15*t)),.15,new Vec3(0,0,.15));
        var allPieces=Map.of("a/left",leftMotion,"a/right",rightMotion,"b/floor",floorMotion,"b/marker",markerMotion);

        AnatomyMovement.activate(level);
        AnatomyMovement.register(supportA,e->Optional.of(new GeometryProvider.Snapshot(101,Map.of("left",leftWall,"right",rightWall))));
        AnatomyMovement.register(supportB,e->Optional.of(new GeometryProvider.Snapshot(102,Map.of("floor",floor,"marker",marker0))));
        AnatomyMovement.gravity(left,GravityFrame.VANILLA);
        AnatomyMovement.gravity(right,new GravityFrame(Direction.EAST));
        try {
            h.assertTrue(Platforms.eligible(left,supportA) && Platforms.eligible(left,supportB) && Platforms.eligible(right,supportA),
                "Both supports must be eligible so pair-local quarantine, not policy, is under test");
            h.assertTrue(confirm(h,left,supportB,floor,"floor",102),"Left body must start with a valid retained contact on stable support B");
            h.assertTrue(confirm(h,right,supportA,rightWall,"right",101),"Right body must start with a valid retained contact on conflicting support A");
            AnatomyMovement.afterMove(left);AnatomyMovement.afterMove(right);
            h.assertTrue(AnatomyMovement.supported(left) && AnatomyMovement.contact(left).support()==supportB,
                "Stable B contact must be retained before the joint batch");
            h.assertTrue(AnatomyMovement.supported(right) && AnatomyMovement.contact(right).support()==supportA,
                "A contact must be retained before the joint batch");

            var leftPlan=resolveIndividually(level,left,leftBox,allPieces);
            var rightPlan=resolveIndividually(level,right,rightBox,allPieces);
            h.assertTrue(leftPlan.status()==TemporalResponse.Status.COMPLETE && rightPlan.status()==TemporalResponse.Status.COMPLETE,
                "Each body must have a complete individual plan before the simultaneous conflict: left="+leftPlan+" right="+rightPlan);
            h.assertTrue(leftPlan.displacement().x>.25 && rightPlan.displacement().x<-.25,
                "Support A must drive the two bodies toward one another while B remains materially stable");
            h.assertTrue(overlapVolume(leftBox.move(leftPlan.displacement()),rightBox.move(rightPlan.displacement()))>1e-12,
                "Individual plans must become mutually incompatible only when combined");

            var preparedA=prepared(supportA,101,"support_a",1,
                Map.of("left",leftWall,"right",rightWall),
                Map.of("left",leftMotion.at().apply(1),"right",rightMotion.at().apply(1)),
                Map.of("left",leftMotion,"right",rightMotion));
            var preparedB=prepared(supportB,102,"support_b",1,
                Map.of("floor",floor,"marker",marker0),
                Map.of("floor",floor,"marker",marker1),
                Map.of("floor",floorMotion,"marker",markerMotion));
            var events=List.of(event(1,0,supportA,preparedA),event(1,1,supportB,preparedB));
            h.assertTrue(events.get(0).interval().envelope().inflate(.05).intersects(events.get(1).interval().envelope()),
                "Both supports must form one plausible spatial joint cluster");

            Vec3 leftBefore=left.position(),rightBefore=right.position();
            var result=resolveBatch(level,List.of(preparedA,preparedB),events,List.of(left,right));
            h.assertTrue(result.outcomes().size()==2 && result.outcomes().stream().allMatch(outcome->
                    outcome.status()==MaterialEventDispatcher.Status.QUARANTINED
                        && outcome.reason()==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED),
                "Mutually incompatible complete plans must fail closed as BACKEND_EXHAUSTED for the simultaneous batch: "+result.outcomes());
            h.assertTrue(left.position().equals(leftBefore) && right.position().equals(rightBefore),
                "Rejected joint plans must not apply a partial body displacement");
            h.assertTrue(!AnatomyMovement.supported(right),
                "The retained A relation participating in the conflict must be released or suspended");
            h.assertTrue(AnatomyMovement.supported(left) && AnatomyMovement.contact(left)!=null && AnatomyMovement.contact(left).support()==supportB,
                "NFR-004 pair locality requires the stable retained relation with support B to survive a conflict caused by support A");
        } finally {
            AnatomyMovement.clear(left);AnatomyMovement.clear(right);
            AnatomyMovement.gravity(left,GravityFrame.VANILLA);AnatomyMovement.gravity(right,GravityFrame.VANILLA);
            AnatomyMovement.deactivate(level);
            supportA.discard();supportB.discard();left.discard();right.discard();
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

    private static TemporalResponse.Result resolveIndividually(ServerLevel level,Entity body,AABB captured,
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
            Identifier.parse("test:s07_"+id),Identifier.parse("test:s07_"+id+"_pose"),1,registration);
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
            List<MaterialEventDispatcher.Event<Entity>> events,List<Entity> bodies) throws Exception {
        Class<?> backendType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        Constructor<?> ctor=backendType.getDeclaredConstructor(ServerLevel.class,List.class);ctor.setAccessible(true);
        Object backend=ctor.newInstance(level,prepared);
        Method resolve=backendType.getDeclaredMethod("resolveJointBatch",List.class,List.class);resolve.setAccessible(true);
        var candidates=bodies.stream().map(body->new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox())).toList();
        return (MaterialEventDispatcher.BatchResolution<Entity>)resolve.invoke(backend,events,candidates);
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
