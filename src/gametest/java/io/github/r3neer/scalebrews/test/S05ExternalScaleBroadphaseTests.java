package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.ModelGeometryProvider;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.joml.Matrix4f;

/** FR-011 / FR-042 holdouts for root-transform changes and live broadphase membership. */
public final class S05ExternalScaleBroadphaseTests {
    @GameTest
    public void externalScaleGrowthMustExposeNewRemoteMaterialBoundsInSameTick(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);support.yBodyRot=0;
        support.getAttribute(Attributes.SCALE).setBaseValue(1);support.refreshDimensions();

        var fixture=fixture(support,"external_scale",41);
        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,fixture.provider(),fixture.descriptor());
            AnatomyMovement.tick(level);
            var before=sample(fixture,support,0,1);
            h.assertTrue(!AnatomyMovement.spaceClear(body,before),
                "Scale-1 material bounds must be present before the external scale mutation");

            // External mods are allowed to mutate the effective vanilla SCALE attribute directly.
            // Stay inside the same authority tick and do not call AnatomyMovement.tick(): the first
            // physical query after the supported mutation must not use a stale material membership.
            support.getAttribute(Attributes.SCALE).setBaseValue(2);support.refreshDimensions();
            var after=sample(fixture,support,0,2);
            h.assertTrue(!before.inflate(.5).intersects(after),
                "Fixture must move expanded anatomy outside the previous broadphase region: before="+before+" after="+after);

            h.assertTrue(!AnatomyMovement.spaceClear(body,after),
                "FR-011/FR-042 require same-tick external SCALE growth to expose the new remote material bounds instead of reusing stale broadphase membership");
        } finally {
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void externalScaleShrinkMustRemoveOldRemoteMaterialBoundsInSameTick(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);support.yBodyRot=0;
        support.getAttribute(Attributes.SCALE).setBaseValue(2);support.refreshDimensions();

        var fixture=fixture(support,"external_shrink",43);
        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,fixture.provider(),fixture.descriptor());
            AnatomyMovement.tick(level);
            var before=sample(fixture,support,0,2);
            h.assertTrue(!AnatomyMovement.spaceClear(body,before),
                "Scale-2 material bounds must be present before the external shrink");

            support.getAttribute(Attributes.SCALE).setBaseValue(1);support.refreshDimensions();
            var after=sample(fixture,support,0,1);
            h.assertTrue(!before.inflate(.5).intersects(after),
                "Shrink fixture must move anatomy out of the previous broadphase region: before="+before+" after="+after);
            h.assertTrue(AnatomyMovement.spaceClear(body,before),
                "FR-011/NFR-004 require same-tick shrink to remove the old material bounds instead of leaving a stale collider");
            h.assertTrue(!AnatomyMovement.spaceClear(body,after),
                "The shrunken material bounds must remain discoverable after the old entry is removed");
        } finally {
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void sameTickRootYawMustExposeNewRemoteMaterialBoundsWithoutManualPublication(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);support.yBodyRot=0;
        support.getAttribute(Attributes.SCALE).setBaseValue(1);support.refreshDimensions();

        var fixture=fixture(support,"same_tick_yaw",44);
        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,fixture.provider(),fixture.descriptor());
            AnatomyMovement.tick(level);
            var before=sample(fixture,support,0,1);
            h.assertTrue(!AnatomyMovement.spaceClear(body,before),
                "Yaw-0 material bounds must be indexed before the same-tick root rotation");

            // Yaw is a first-class RootFrame component (FR-030/FR-049), not provider-private state.
            // A living support may rotate during its normal tick; a later body query in that same tick
            // must not depend on an explicit networking/publication read to discover the rotated anatomy.
            support.yBodyRot=90;
            var after=sample(fixture,support,90,1);
            h.assertTrue(!before.inflate(.5).intersects(after),
                "Yaw fixture must move remote anatomy outside the previous indexed region: before="+before+" after="+after);
            h.assertTrue(!AnatomyMovement.spaceClear(body,after),
                "FR-030/FR-042/FR-049 require same-tick root yaw to expose new material bounds before any manual publishedFrame/tick observation");
        } finally {
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void publishedRootYawMustKeepUpdatedSupportDiscoverable(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);support.yBodyRot=0;
        support.getAttribute(Attributes.SCALE).setBaseValue(1);support.refreshDimensions();

        var fixture=fixture(support,"published_yaw",42);
        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,fixture.provider(),fixture.descriptor());
            AnatomyMovement.tick(level);
            var before=sample(fixture,support,0,1);
            h.assertTrue(!AnatomyMovement.spaceClear(body,before),
                "Starting rotated-material fixture must be indexed before publication");

            // Network/tracking code legitimately calls publishedFrame directly. If that observation
            // advances a root endpoint, incremental spatial maintenance must replace the entry rather
            // than merely remove it and leave future broadphase queries unable to rediscover support.
            support.yBodyRot=90;
            var after=sample(fixture,support,90,1);
            h.assertTrue(!before.inflate(.5).intersects(after),
                "Yaw fixture must move remote anatomy outside the previous indexed region: before="+before+" after="+after);
            var published=AnatomyMovement.publishedFrame(support).orElseThrow();
            h.assertTrue(Math.abs(published.endpoint().root().yaw()-90)<1e-5,
                "Direct publication must observe the new authoritative root yaw before the spatial query");

            h.assertTrue(!AnatomyMovement.spaceClear(body,after),
                "A root endpoint advanced by publishedFrame must remain discoverable at its new material bounds; publication cannot remove the index entry without reinserting it");
        } finally {
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }

    private static Fixture fixture(net.minecraft.world.entity.LivingEntity support,String name,long revision) {
        // Keep the piece far from root so scale/yaw changes move it between disjoint broadphase cells.
        var model=new ModelGeometry(2,"test:s05_"+name,"1",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("remote","root",List.of(4d,0d,0d),List.of(5d,1d,1d),null)),
            ModelGeometry.values(new Matrix4f()));
        var provider=new ModelGeometryProvider(model,(geometry,inputs)->Optional.of(Map.of()),AnatomyFilter.DEFAULT,revision);
        var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
        provider.pose(support,inputs);
        var descriptor=new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(),revision,
            Identifier.parse("test:s05_"+name+"_model"),Identifier.parse("test:s05_"+name+"_pose"));
        return new Fixture(provider,inputs,descriptor);
    }

    private static net.minecraft.world.phys.AABB sample(Fixture fixture,net.minecraft.world.entity.LivingEntity support,float yaw,float scale) {
        return fixture.provider().sampleAt(support,new AnatomyPoseHistory.Sample(fixture.inputs(),support.position(),yaw,scale,
            AnatomyMovement.gravity(support))).orElseThrow().pieces().get("remote").bounds();
    }

    private record Fixture(ModelGeometryProvider provider,PoseProvider.Inputs inputs,
            GeometryProvider.GeometryIdentityDescriptor descriptor) {}
}
