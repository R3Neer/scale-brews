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

/** NFR-004/NFR-007 holdout: numerical exhaustion must fail closed for the uncertain pair only. */
public final class S07BudgetFailureLocalizationTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void solverBudgetExhaustionMustReleaseOnlyUncertainPair(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var uncertain=h.makeMockServerPlayerInLevel();
        var safe=h.makeMockServerPlayerInLevel();
        uncertain.setNoGravity(true);safe.setNoGravity(true);
        uncertain.getAttribute(Attributes.SCALE).setBaseValue(.1);uncertain.refreshDimensions();
        safe.getAttribute(Attributes.SCALE).setBaseValue(.1);safe.refreshDimensions();

        Vec3 base=support.position();
        uncertain.setPos(base.x,base.y+1.2,base.z);
        double bodyBottom=uncertain.getBoundingBox().minY;
        // Matrix4f exports through float. First align the actual SAT face to zero gap,
        // then move it down by an exact double 0.01 so the fixture cannot fail on
        // representation noise before reaching the budget-exhaustion path.
        var approximate=ConvexBox.of(new AABB(base.x-1,bodyBottom-.5,base.z-1,base.x+1,bodyBottom,base.z+1),new Matrix4f());
        double alignment=approximate.separation(uncertain.getBoundingBox()).gap();
        var budgetPiece=approximate.move(new Vec3(0,alignment-.01,0));
        Vec3 safeOrigin=base.add(8,0,0);
        safe.setPos(safeOrigin);
        var safePiece=ConvexBox.of(new AABB(safeOrigin.x-1,safeOrigin.y-.5,safeOrigin.z-1,
            safeOrigin.x+1,safeOrigin.y,safeOrigin.z+1),new Matrix4f());

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,e->Optional.of(new GeometryProvider.Snapshot(97,
            Map.of("budget",budgetPiece,"safe",safePiece))));
        try {
            h.assertTrue(Platforms.eligible(uncertain,support) && Platforms.eligible(safe,support),
                "Both bodies must be eligible so the holdout isolates failure locality");
            double actualGap=budgetPiece.separation(uncertain.getBoundingBox()).gap();
            h.assertTrue(Math.abs(actualGap-.01)<1e-9,
                "Uncertain pair must begin separated by the intended retained-contact gap, not in initial-overlap recovery: "+actualGap);

            h.assertTrue(confirm(h,uncertain,support,budgetPiece,"budget",97),
                "Uncertain fixture must start with a materially valid retained contact inside the .025 tolerance");
            h.assertTrue(confirm(h,safe,support,safePiece,"safe",97),
                "Bystander fixture must start with an independent valid contact on the same support");
            AnatomyMovement.afterMove(uncertain);AnatomyMovement.afterMove(safe);
            h.assertTrue(AnatomyMovement.supported(uncertain) && AnatomyMovement.supported(safe),
                "Both contacts must be valid before injecting numerical uncertainty");

            // The trajectory is actually static, but a deliberately loose *valid* point-speed
            // certificate forces ConservativeSweep to consume more than QUERY_BUDGET=256 samples.
            // maxPointSpeed=10 still gives a <64-block runtime envelope, so this is not an
            // oversize-envelope rejection masquerading as solver exhaustion.
            var motion=new ConservativeSweep.Motion(t->budgetPiece,10);
            var envelope=budgetPiece.bounds().inflate(motion.maxPointSpeed()+ConservativeSweep.SKIN);
            h.assertTrue(envelope.getXsize()<64 && envelope.getYsize()<64 && envelope.getZsize()<64,
                "Fixture must remain inside the live material envelope cap");
            var prepared=prepared(support,budgetPiece,motion,envelope);
            Vec3 before=uncertain.position();
            var outcome=resolve(level,support,prepared,uncertain);
            h.assertTrue(outcome.status()==MaterialEventDispatcher.Status.QUARANTINED
                    && outcome.reason()==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED,
                "Fixture must exhaust the bounded temporal solver without starting overlap: "+outcome);
            h.assertTrue(uncertain.position().equals(before),
                "Budget exhaustion must not apply an uncertified partial displacement");

            h.assertTrue(!AnatomyMovement.supported(uncertain),
                "NFR-004 requires budget uncertainty to release or locally quarantine the unresolved body/support relation; an old contact cannot remain authoritative");
            h.assertTrue(AnatomyMovement.contact(safe)!=null && AnatomyMovement.supported(safe),
                "Localized budget failure must not erase an unrelated body's valid contact on the same support");
        } finally {
            AnatomyMovement.clear(uncertain);AnatomyMovement.clear(safe);
            AnatomyMovement.deactivate(level);
            support.discard();uncertain.discard();safe.discard();
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

    private static Object prepared(LivingEntity support,ConvexBox piece,ConservativeSweep.Motion moving,AABB envelope) throws Exception {
        long tick=support.level().getGameTime();
        long registration=AnatomyMovement.registrationGeneration(support);
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),97,
            Identifier.parse("test:s07_budget_model"),Identifier.parse("test:s07_budget_pose"),1,registration);
        var beforeRoot=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,tick,support.position(),0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,beforeRoot.origin(),0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,afterRoot.origin(),0,1,GravityFrame.VANILLA);
        var before=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(97,Map.of("budget",piece)));
        var after=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(97,Map.of("budget",piece)));
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.ROOT,handle);
        var motion=new GeometryProvider.MotionSnapshot(97,tick,tick,support.position(),support.position(),Map.of("budget",moving));
        Class<?> preparedType=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Constructor<?> ctor=preparedType.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
        ctor.setAccessible(true);
        return ctor.newInstance(pending,interval,motion);
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
            MaterialEventDispatcher.Source.ROOT_MUTATION,interval,Set.of(support.getUUID()));
        Method resolve=backendType.getDeclaredMethod("resolve",MaterialEventDispatcher.Event.class,List.class);resolve.setAccessible(true);
        var candidate=new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox());
        var result=(MaterialEventDispatcher.Resolution<Entity>)resolve.invoke(backend,event,List.of(candidate));
        return result.outcome();
    }
}
