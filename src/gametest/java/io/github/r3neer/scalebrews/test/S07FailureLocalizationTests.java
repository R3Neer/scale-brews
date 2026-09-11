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
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
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

/** S07 adversarial holdouts for pair-local failure instead of support-wide invalidation. */
public final class S07FailureLocalizationTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void irresolvableMaterialOverlapMustNotInvalidateOtherBodyOnSameSupport(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var trapped=h.makeMockServerPlayerInLevel();
        var safe=h.makeMockServerPlayerInLevel();
        trapped.setNoGravity(true);safe.setNoGravity(true);
        trapped.getAttribute(Attributes.SCALE).setBaseValue(.2);trapped.refreshDimensions();
        safe.getAttribute(Attributes.SCALE).setBaseValue(.2);safe.refreshDimensions();

        Vec3 center=support.position();
        // Separation recovery is hard-capped at four blocks.  Enclose the trapped body
        // more deeply than that so BACKEND_EXHAUSTED is deterministic and does not
        // depend on block clipping, GameTest coordinate transforms or search order.
        var trap=ConvexBox.of(new AABB(-5,-5,-5,5,5,5),new Matrix4f()).move(center);
        Vec3 safeOrigin=center.add(8,0,0);
        var floor=ConvexBox.of(new AABB(-1,-.5,-1,1,0,1),new Matrix4f()).move(safeOrigin);
        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,entity->Optional.of(new GeometryProvider.Snapshot(96,Map.of("floor",floor,"trap",trap))));
        trapped.setPos(center);
        safe.setPos(safeOrigin);

        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(trapped,support)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(safe,support),
                "Both fixture bodies must be eligible so the test isolates failure localization");

            var separation=floor.separation(safe.getBoundingBox());
            int face=floor.closestFace(separation.normal());
            Vec3 normal=floor.faceNormal(face);
            Vec3 local=floor.facePoint(face,safe.getBoundingBox().getCenter());
            var contact=new SurfaceContact(support.getUUID(),96,"floor",face,local,normal,level.getGameTime());
            h.assertTrue(AnatomyMovement.confirm(safe,support,contact),
                "Fixture bystander must begin with a valid material contact on the same support");
            AnatomyMovement.afterMove(safe);
            h.assertTrue(AnatomyMovement.supported(safe),
                "Fixture bystander contact must be materially valid before the unrelated failure");

            var afterTrap=trap.move(new Vec3(.1,0,0));
            var prepared=prepared(support,trap,afterTrap);
            var outcome=resolve(level,support,prepared,trapped);
            h.assertTrue(outcome.status()==MaterialEventDispatcher.Status.QUARANTINED
                    && outcome.reason()==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED,
                "Fixture must exercise the unresolvable material-overlap failure path: "+outcome);

            h.assertTrue(AnatomyMovement.contact(safe)!=null && AnatomyMovement.supported(safe),
                "FR-052 requires pair-local failure: an unresolvable trapped body must not invalidate another body's valid contact on the same support");
        } finally {
            AnatomyMovement.clear(trapped);AnatomyMovement.clear(safe);
            AnatomyMovement.deactivate(level);
            support.discard();trapped.discard();safe.discard();
        }
        h.succeed();
    }

    private static Object prepared(LivingEntity support,ConvexBox beforeBox,ConvexBox afterBox) throws Exception {
        long tick=support.level().getGameTime();
        long registration=AnatomyMovement.registrationGeneration(support);
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),96,
            Identifier.parse("test:s07_failure_model"),Identifier.parse("test:s07_failure_pose"),1,registration);
        var beforeRoot=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,tick,support.position(),0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,beforeRoot.origin(),0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,afterRoot.origin(),0,1,GravityFrame.VANILLA);
        var before=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(96,Map.of("trap",beforeBox)));
        var after=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(96,Map.of("trap",afterBox)));
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        var envelope=union(beforeBox.bounds(),afterBox.bounds()).inflate(.5);
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.JOINT,handle);
        Vec3 delta=new Vec3(.1,0,0);
        var motion=new GeometryProvider.MotionSnapshot(96,tick,tick,support.position(),support.position(),
            Map.of("trap",new ConservativeSweep.Motion(t->beforeBox.move(delta.scale(t)),0,delta)));
        Class<?> type=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Constructor<?> constructor=type.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
        constructor.setAccessible(true);
        return constructor.newInstance(pending,interval,motion);
    }

    @SuppressWarnings("unchecked")
    private static MaterialEventDispatcher.Outcome resolve(ServerLevel level,LivingEntity support,Object prepared,Entity body) throws Exception {
        Class<?> backendType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        Constructor<?> backendCtor=backendType.getDeclaredConstructor(ServerLevel.class,List.class);
        backendCtor.setAccessible(true);
        Object backend=backendCtor.newInstance(level,List.of(prepared));
        Class<?> preparedType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Method intervalAccessor=preparedType.getDeclaredMethod("interval");intervalAccessor.setAccessible(true);
        var interval=(MaterialEventDispatcher.MaterialInterval)intervalAccessor.invoke(prepared);
        var event=new MaterialEventDispatcher.Event<Entity>(new MaterialEventDispatcher.EventId(1,0),support,
            MaterialEventDispatcher.Source.JOINT_BATCH,interval,Set.of(support.getUUID()));
        Method resolve=backendType.getDeclaredMethod("resolve",MaterialEventDispatcher.Event.class,List.class);
        resolve.setAccessible(true);
        var candidate=new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox());
        var result=(MaterialEventDispatcher.Resolution<Entity>)resolve.invoke(backend,event,List.of(candidate));
        return result.outcome();
    }

    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }
}
