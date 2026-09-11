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

/** FR-058 causal half: rejecting a derived carry never leaves a serial that can be replayed later as transport debt. */
public final class S08RejectedDerivedIntervalNoDebtTests {
    private static final long REVISION=310;
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void consumedRejectedDerivedSerialCannotReappearAfterTheBlockerDisappears(GameTestHelper h) {
        var level=h.getLevel();var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);AnatomyMovement.activate(level);
        AnatomyMovement.register(support,provider(),new GeometryProvider.GeometryIdentityDescriptor(
            UUID.randomUUID(),REVISION,Identifier.parse("test:s08_no_debt"),Identifier.parse("test:s08_no_debt_pose"),1));
        try {
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"Initial observation must only seed continuity");

            var rejectedBefore=MaterialIntervalRuntime.captureRoot(support);
            support.setPos(support.position().add(.4,0,0));
            var rejected=MaterialIntervalRuntime.deriveRoot(support,rejectedBefore).orElseThrow();
            h.assertTrue(rejected.materialSerial()==1,"Rejected physical attempt must still consume its causal serial exactly once");

            // A caller may reject the derived carry because of obstruction and never enqueue it.
            // Removing that obstruction later is deliberately represented here by doing nothing to
            // the material endpoint. There must be no hidden pending or reconstructed interval.
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"Rejected derived carry must not be queued as debt");
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "After the blocker disappears, the already-consumed endpoint must remain a replay/no-op");

            var nextBefore=MaterialIntervalRuntime.captureRoot(support);
            support.setPos(support.position().add(.1,0,0));
            var next=MaterialIntervalRuntime.deriveRoot(support,nextBefore).orElseThrow();
            h.assertTrue(next.materialSerial()==2,
                "Only a genuinely new material contribution may create the next derived interval");
        } finally {
            MaterialIntervalRuntime.clear(level);AnatomyMovement.deactivate(level);support.discard();
        }
        h.succeed();
    }

    private static GeometryProvider provider() {
        return new GeometryProvider() {
            private AnatomyMovement.RootFrame previous;private PoseProvider.Inputs previousInputs;private long serial;
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
                if(previousInputs==null || !previousInputs.equals(INPUTS) || !sameRoot)serial++;
                previousInputs=INPUTS;
                var sample=new AnatomyPoseHistory.Sample(INPUTS,previous.origin(),previous.yaw(),previous.scale(),previous.gravity());
                return Optional.of(new CausalEndpoint(serial,entity.level().getGameTime(),entity.level().getGameTime(),previous,sample,Availability.AVAILABLE));
            }
        };
    }
}
