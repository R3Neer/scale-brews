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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S06 holdout for membership changes observed through the supported provider cadence. */
public final class S06SupportedMembershipCausalityTests {
    @GameTest
    public void tickDrivenUnavailableToAvailableMustIndexRemoteAnatomyInSameGameTick(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);support.yBodyRot=0;
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();

        long tick=level.getGameTime();
        var available=new AtomicBoolean(false);
        Vec3 remote=support.position().add(6,0,0);
        var piece=ConvexBox.of(new AABB(-.5,0,-.5,.5,.5,.5),new Matrix4f()).move(remote);
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
            @Override public void tick(net.minecraft.world.entity.LivingEntity entity,long authorityTick) {
                available.set(true);
            }
        };
        var descriptor=new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(),1,
            Identifier.parse("test:s06_supported_membership"),Identifier.parse("test:s06_supported_pose"));

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,provider,descriptor);
        try {
            h.assertTrue(Platforms.eligible(body,support),
                "Fixture body must be eligible so the test isolates causal membership");
            h.assertTrue(!support.getBoundingBox().inflate(Platforms.searchMargin(level)).intersects(piece.bounds()),
                "Remote anatomy must lie outside the support's vanilla discovery envelope");
            var query=piece.bounds();
            h.assertTrue(AnatomyMovement.spaceClear(body,query),
                "Before the supported cadence advances, the UNAVAILABLE endpoint must expose no collider");

            long beforeTick=level.getGameTime();
            AnatomyMovement.tick(level);
            h.assertTrue(level.getGameTime()==beforeTick,
                "Fixture must exercise a same-game-tick provider cadence rather than relying on tick rollover");
            h.assertTrue(available.get() && AnatomyMovement.queryFrame(support).isPresent(),
                "The supported provider cadence must publish the AVAILABLE causal frame");
            h.assertTrue(!AnatomyMovement.spaceClear(body,query),
                "After provider.tick and the canonical rebuild, remote anatomy outside the vanilla support AABB must participate immediately");
        } finally {
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }
}
