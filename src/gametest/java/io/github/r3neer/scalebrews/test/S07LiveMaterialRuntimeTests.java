package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
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
        var first=h.spawn(EntityTypes.COW,2,20,2);
        var near=h.spawn(EntityTypes.COW,3,20,2);
        var far=h.spawn(EntityTypes.COW,4,20,2);
        first.setNoAi(true);first.setNoGravity(true);
        near.setNoAi(true);near.setNoGravity(true);
        far.setNoAi(true);far.setNoGravity(true);
        near.setPos(first.position().add(1,0,0));
        far.setPos(first.position().add(100,0,0));
        try {
            var preparedClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
            var clusters=MaterialPhysicsRuntime.class.getDeclaredMethod("jointClusters",List.class);
            clusters.setAccessible(true);
            // Deliberately permute input order. Temporal simultaneity may group the nearby
            // pair, but the far support must remain an independent spatial component.
            var input=List.of(prepared(preparedClass,far),prepared(preparedClass,near),prepared(preparedClass,first));
            @SuppressWarnings("unchecked")
            var result=(List<List<?>>)clusters.invoke(null,input);
            var sizes=new ArrayList<Integer>();
            for(var cluster:result)sizes.add(cluster.size());
            Collections.sort(sizes);
            h.assertTrue(sizes.equals(List.of(1,2)),
                "A simultaneous cadence must partition by local swept connectivity: expected one nearby pair plus one far support, got "+sizes);
        } finally {
            first.discard();near.discard();far.discard();
        }
        h.succeed();
    }

    private static Object prepared(Class<?> preparedClass,LivingEntity support) throws Exception {
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),70,
            Identifier.parse("test:s07_model"),Identifier.parse("test:s07_pose"),1,1);
        var origin=support.position();long tick=support.level().getGameTime();
        var beforeRoot=new AnatomyMovement.RootFrame(1,tick,origin,0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,tick,origin,0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,origin,0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,origin,0,1,GravityFrame.VANILLA);
        var box=ConvexBox.of(new AABB(-.5,-.5,-.5,.5,.5,.5),new Matrix4f()).move(origin);
        var snapshot=new GeometryProvider.Snapshot(70,Map.of("body",box));
        var before=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        var after=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),snapshot);
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        var envelope=new AABB(origin.x-.75,origin.y-.75,origin.z-.75,origin.x+.75,origin.y+.75,origin.z+.75);
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.JOINT,handle);
        var motion=new GeometryProvider.MotionSnapshot(70,tick,tick,origin,origin,
            Map.of("body",new ConservativeSweep.Motion(ignored->box,0)));
        var constructor=preparedClass.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
        constructor.setAccessible(true);
        return constructor.newInstance(pending,interval,motion);
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
