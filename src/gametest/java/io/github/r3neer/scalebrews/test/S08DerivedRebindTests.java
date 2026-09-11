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

/** S08/NFR-017 holdout: a derived capture may not cross a support rebind barrier. */
public final class S08DerivedRebindTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void staleDerivedCaptureCannotCrossRebindAndFreshIdentityRecovers(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        AnatomyMovement.activate(level);
        try {
            register(support,401,1,"old");
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"Initial old binding observation only seeds continuity");

            var stale=MaterialIntervalRuntime.captureRoot(support);
            h.assertTrue(stale!=null && stale.before()!=null,"Fixture requires a certified old-binding root capture");
            var oldIdentity=stale.before().identity();
            support.setPos(support.position().add(.25,0,0));

            // Rebinding after capture is a lifecycle barrier. The physical root has changed,
            // but no derived child may connect the old identity to the replacement binding.
            register(support,402,2,"new");
            var current=AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(!current.identity().equals(oldIdentity),"Fixture must actually replace the support identity");
            h.assertTrue(MaterialIntervalRuntime.deriveRoot(support,stale).isEmpty(),
                "A DERIVED_CARRY capture from the old binding must fail closed after rebind");
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "Rejected stale derivation must not leak an orphan ROOT/JOINT event");

            // The stale attempt must not poison the replacement stream. A fresh root mutation
            // on the new identity starts its own material serial sequence at one.
            var fresh=MaterialIntervalRuntime.captureRoot(support);
            h.assertTrue(fresh!=null && fresh.before().identity().equals(current.identity()),
                "Fresh capture must use the replacement binding identity");
            support.setPos(support.position().add(.25,0,0));
            var derived=MaterialIntervalRuntime.deriveRoot(support,fresh).orElseThrow();
            h.assertTrue(derived.identity().equals(current.identity()) && derived.materialSerial()==1,
                "Replacement binding must recover independently with material serial 1");
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "Fresh derived ownership remains parent-owned and must not duplicate into pending ROOT/JOINT work");
        } finally {
            MaterialIntervalRuntime.clear(level);
            AnatomyMovement.deactivate(level);
            support.discard();
        }
        h.succeed();
    }

    private static void register(net.minecraft.world.entity.LivingEntity support,long revision,long binding,String suffix) {
        AnatomyMovement.register(support,endpointProvider(revision),new GeometryProvider.GeometryIdentityDescriptor(
            UUID.randomUUID(),revision,Identifier.parse("test:s08_rebind_"+suffix),Identifier.parse("test:s08_rebind_pose_"+suffix),binding));
    }

    private static GeometryProvider endpointProvider(long revision) {
        return new GeometryProvider() {
            private AnatomyMovement.RootFrame previous;
            private PoseProvider.Inputs previousInputs;
            private long serial;

            @Override public Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity entity) {
                return Optional.of(new Snapshot(revision,Map.of("body",
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
