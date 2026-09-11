package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** S06/S07 adversarial holdout for same-tick causal membership changes in the live spatial index. */
public final class S06SpatialMembershipCausalityTests {
    @GameTest
    public void sameTickUnavailableToAvailableSupportMustEnterBroadphaseWithoutRebind(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);
        support.setNoGravity(true);
        support.yBodyRot=0;
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);
        body.refreshDimensions();

        long tick=level.getGameTime();
        var available=new AtomicBoolean(false);
        var piece=ConvexBox.of(new AABB(-.5,0,-.5,.5,.5,.5),new Matrix4f()).move(support.position());
        var snapshot=new GeometryProvider.Snapshot(1,Map.of("body",piece));
        var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
        var root=new AnatomyMovement.RootFrame(1,tick,support.position(),0,support.getScale(),GravityFrame.VANILLA);
        var sample=new AnatomyPoseHistory.Sample(inputs,root.origin(),root.yaw(),root.scale(),root.gravity());
        var unavailableEndpoint=new GeometryProvider.CausalEndpoint(1,tick,tick,root,sample,GeometryProvider.Availability.UNAVAILABLE);
        var availableEndpoint=new GeometryProvider.CausalEndpoint(2,tick,tick,root,sample,GeometryProvider.Availability.AVAILABLE);
        GeometryProvider provider=new GeometryProvider() {
            @Override public Optional<Snapshot> sample(net.minecraft.world.entity.LivingEntity entity) {
                return available.get()?Optional.of(snapshot):Optional.empty();
            }
            @Override public Optional<CausalEndpoint> causalEndpoint(net.minecraft.world.entity.LivingEntity entity) {
                return Optional.of(available.get()?availableEndpoint:unavailableEndpoint);
            }
        };
        var descriptor=new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(),1,
            Identifier.parse("test:s06_spatial_membership"),Identifier.parse("test:s06_spatial_pose"));

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,provider,descriptor);
        try {
            h.assertTrue(Platforms.eligible(body,support),
                "Fixture body must be eligible so the holdout isolates spatial membership rather than policy");
            var query=piece.bounds();
            h.assertTrue(AnatomyMovement.spaceClear(body,query),
                "An explicitly unavailable causal endpoint must publish no collider");

            available.set(true);
            // Do NOT call AnatomyMovement.queryFrame here: accepting the endpoint through that
            // route is itself allowed to invalidate the spatial cache.  The broadphase query below
            // must be able to discover that an omitted registered provider changed membership.
            h.assertTrue(provider.causalEndpoint(support).orElseThrow().availability()==GeometryProvider.Availability.AVAILABLE
                    && provider.sample(support).isPresent(),
                "Fixture source must have advanced to AVAILABLE in the same authority tick before any runtime consumer observes it");
            h.assertTrue(!AnatomyMovement.spaceClear(body,query),
                "A support that becomes AVAILABLE in the same tick must enter the broadphase when spaceClear is the first runtime consumer; an index that omitted it cannot be reused vacuously");
            h.assertTrue(AnatomyMovement.queryFrame(support).isPresent(),
                "After the spatial query, the runtime must expose the same accepted AVAILABLE causal frame");
        } finally {
            AnatomyMovement.deactivate(level);
            support.discard();
            body.discard();
        }
        h.succeed();
    }
}
