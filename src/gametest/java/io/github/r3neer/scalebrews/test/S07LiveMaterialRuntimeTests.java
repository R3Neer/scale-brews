package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.Platforms;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Adversarial S07 holdouts for the live S06 -> dispatcher integration path. */
public final class S07LiveMaterialRuntimeTests {
    private static final PoseProvider.Inputs INPUTS = new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void ordinaryLevelTickMustDrainPublishedMaterialIntervals(GameTestHelper h) {
        var level = h.getLevel();
        var support = h.spawn(EntityTypes.COW, 2, 20, 2);
        support.setNoAi(true);
        support.setNoGravity(true);
        var provider = endpointProvider(70);
        AnatomyMovement.activate(level);
        AnatomyMovement.register(support, provider, new GeometryProvider.GeometryIdentityDescriptor(
            UUID.randomUUID(), 70, Identifier.parse("test:s07_model"), Identifier.parse("test:s07_pose"), 1));
        try {
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "Initial observation must seed the causal fence without publishing work");

            var capture = MaterialIntervalRuntime.captureRoot(support);
            support.setPos(support.position().add(.125, 0, 0));
            MaterialIntervalRuntime.commitRoot(support, capture);

            // Do not call MaterialPhysicsRuntime.drain here. The production level-tick path
            // itself must consume S06 work; otherwise S07 exists only when invoked by tests.
            Platforms.tick(level);

            var leaked = MaterialIntervalRuntime.poll(level);
            h.assertTrue(leaked.isEmpty(),
                "The ordinary production level tick must drain every published S06 interval exactly once");
        } finally {
            MaterialIntervalRuntime.clear(level);
            MaterialPhysicsRuntime.clear(level);
            AnatomyMovement.deactivate(level);
            support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void farApartSimultaneousJointSupportsRemainLocal(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var first=h.spawn(EntityTypes.COW,2,20,2);
        var second=h.spawn(EntityTypes.COW,3,20,2);
        first.setNoAi(true);first.setNoGravity(true);second.setNoAi(true);second.setNoGravity(true);
        second.setPos(first.position().add(100,0,0));
        try {
            var events=List.of(
                jointEvent(first,1,0),
                jointEvent(second,1,1)
            );
            // Backend is deliberately internal. Reflect only enough to exercise the real
            // capture policy: this holdout is about locality, not its private class shape.
            var backendClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
            var constructor=backendClass.getDeclaredConstructor(ServerLevel.class,List.class);
            constructor.setAccessible(true);
            var backend=constructor.newInstance(level,List.of());
            var capture=backendClass.getDeclaredMethod("captureJointBatch",List.class,int.class);
            capture.setAccessible(true);
            @SuppressWarnings("unchecked")
            var result=(MaterialEventDispatcher.Candidates<Entity>)capture.invoke(backend,events,128);
            h.assertTrue(!result.overflow(),
                "Two individually bounded simultaneous joint supports must not overflow merely because their world-space union spans 100 blocks");
        } finally {
            first.discard();second.discard();
        }
        h.succeed();
    }

    private static MaterialEventDispatcher.Event<Entity> jointEvent(LivingEntity support,long sequence,int member) {
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),70,
            Identifier.parse("test:s07_model"),Identifier.parse("test:s07_pose"),1,1);
        var origin=support.position();
        var beforeRoot=new AnatomyMovement.RootFrame(1,support.level().getGameTime(),origin,0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,support.level().getGameTime(),origin,0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,origin,0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,origin,0,1,GravityFrame.VANILLA);
        var snapshot=new GeometryProvider.Snapshot(70,Map.of("body",ConvexBox.of(new AABB(-.5,-.5,-.5,.5,.5,.5),new Matrix4f()).move(origin)));
        var before=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(1,support.level().getGameTime(),support.level().getGameTime(),beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        var after=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(2,support.level().getGameTime(),support.level().getGameTime(),afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        var envelope=new AABB(origin.x-.75,origin.y-.75,origin.z-.75,origin.x+.75,origin.y+.75,origin.z+.75);
        return new MaterialEventDispatcher.Event<>(new MaterialEventDispatcher.EventId(sequence,member),support,
            MaterialEventDispatcher.Source.JOINT_BATCH,new MaterialEventDispatcher.MaterialInterval(handle,envelope),Set.of(support.getUUID()));
    }

    private static GeometryProvider endpointProvider(long revision) {
        return new GeometryProvider() {
            private AnatomyMovement.RootFrame previous;
            private long serial;

            @Override public Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity entity) {
                return Optional.of(new Snapshot(revision, Map.of("body",
                    ConvexBox.of(new AABB(-.5, -.5, -.5, .5, .5, .5), new Matrix4f()).move(entity.position()))));
            }

            @Override public Optional<CausalEndpoint> causalEndpoint(net.minecraft.world.entity.LivingEntity entity) {
                var observed = new AnatomyMovement.RootFrame(previous == null ? 0 : previous.sequence() + 1,
                    entity.level().getGameTime(), entity.position(), entity.yBodyRot, entity.getScale(), GravityFrame.VANILLA);
                boolean changed = previous == null || !previous.origin().equals(observed.origin())
                    || Float.compare(previous.yaw(), observed.yaw()) != 0
                    || Float.compare(previous.scale(), observed.scale()) != 0
                    || !previous.gravity().equals(observed.gravity());
                if (changed) {
                    previous = observed;
                    serial++;
                }
                var root = previous;
                var sample = new AnatomyPoseHistory.Sample(INPUTS, root.origin(), root.yaw(), root.scale(), root.gravity());
                return Optional.of(new CausalEndpoint(serial, entity.level().getGameTime(), entity.level().getGameTime(),
                    root, sample, Availability.AVAILABLE));
            }
        };
    }
}
