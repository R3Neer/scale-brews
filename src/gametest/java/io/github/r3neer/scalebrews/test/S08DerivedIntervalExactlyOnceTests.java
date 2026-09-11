package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** S08 regression: a DERIVED_CARRY root advance is fenced once and never republished as JOINT. */
public final class S08DerivedIntervalExactlyOnceTests {
    private static final long REVISION=308;
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void derivedRootThenObserveMustNotRepublishTheSameEndpoint(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,endpointProvider(),new GeometryProvider.GeometryIdentityDescriptor(
            UUID.randomUUID(),REVISION,Identifier.parse("test:s08_derived_once"),Identifier.parse("test:s08_derived_once_pose"),1));
        try {
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"Initial observation must only seed continuity");

            var capture=MaterialIntervalRuntime.captureRoot(support);
            support.setPos(support.position().add(.25,0,0));
            var derived=MaterialIntervalRuntime.deriveRoot(support,capture).orElseThrow();
            h.assertTrue(derived.materialSerial()==1,"First derived root advance must allocate material serial 1");
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "deriveRoot is already owned by its parent event and must never enter the ROOT/JOINT pending queue");

            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "Observing the same endpoint after deriveRoot must be a replay/no-op, never a duplicate JOINT contribution");

            var next=MaterialIntervalRuntime.captureRoot(support);
            support.setPos(support.position().add(.25,0,0));
            var second=MaterialIntervalRuntime.deriveRoot(support,next).orElseThrow();
            h.assertTrue(second.materialSerial()==2,
                "A later distinct derived root movement must advance the same causal serial stream exactly once");
        } finally {
            MaterialIntervalRuntime.clear(level);
            AnatomyMovement.deactivate(level);
            support.discard();
        }
        h.succeed();
    }

    private static GeometryProvider endpointProvider() {
        return new GeometryProvider() {
            private AnatomyMovement.RootFrame previous;
            private PoseProvider.Inputs previousInputs;
            private long serial;

            @Override public Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity entity) {
                return Optional.of(new Snapshot(REVISION,Map.of("body",
                    ConvexBox.of(new AABB(-.5,-.5,-.5,.5,.5,.5),new Matrix4f()).move(entity.position()))));
            }

            @Override public Optional<CausalEndpoint> causalEndpoint(net.minecraft.world.entity.LivingEntity entity) {
                var observed=new AnatomyMovement.RootFrame(previous==null?0:previous.sequence()+1,entity.level().getGameTime(),
                    entity.position(),entity.yBodyRot,entity.getScale(),GravityFrame.VANILLA);
                boolean sameRoot=previous!=null && previous.origin().equals(observed.origin()) && previous.yaw()==observed.yaw()
                    && previous.scale()==observed.scale() && previous.gravity().equals(observed.gravity());
                if(!sameRoot)previous=observed;
                var now=INPUTS;
                if(previousInputs==null || !previousInputs.equals(now) || !sameRoot)serial++;
                previousInputs=now;
                var root=previous;
                var sample=new AnatomyPoseHistory.Sample(now,root.origin(),root.yaw(),root.scale(),root.gravity());
                return Optional.of(new CausalEndpoint(serial,entity.level().getGameTime(),entity.level().getGameTime(),root,sample,Availability.AVAILABLE));
            }
        };
    }
}
