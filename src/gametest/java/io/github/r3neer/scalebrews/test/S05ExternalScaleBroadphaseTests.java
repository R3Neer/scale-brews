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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** FR-011 / FR-042 holdout: an external same-tick SCALE change may not leave broadphase membership stale. */
public final class S05ExternalScaleBroadphaseTests {
    @GameTest
    public void externalScaleGrowthMustExposeNewRemoteMaterialBoundsInSameTick(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(1);support.refreshDimensions();

        // Keep the material piece far from the root so SCALE=2 moves its bounds completely
        // outside the SCALE=1 broadphase cells. A vanilla-AABB fallback cannot rescue the test.
        var model=new ModelGeometry(2,"test:s05_external_scale","1",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("remote","root",List.of(4d,0d,0d),List.of(5d,1d,1d),null)),
            ModelGeometry.values(new Matrix4f()));
        var provider=new ModelGeometryProvider(model,(geometry,inputs)->Optional.of(Map.of()),AnatomyFilter.DEFAULT,41);
        var inputs=new PoseProvider.Inputs(0,0,0,0,0,true);
        provider.pose(support,inputs);
        var descriptor=new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(),41,
            Identifier.parse("test:s05_external_scale_model"),Identifier.parse("test:s05_external_scale_pose"));

        AnatomyMovement.activate(level);
        try {
            AnatomyMovement.register(support,provider,descriptor);
            AnatomyMovement.tick(level);
            var before=provider.sampleAt(support,new AnatomyPoseHistory.Sample(inputs,support.position(),support.yBodyRot,1,
                AnatomyMovement.gravity(support))).orElseThrow().pieces().get("remote").bounds();
            h.assertTrue(!AnatomyMovement.spaceClear(body,before),
                "Scale-1 material bounds must be present before the external scale mutation");

            // External mods are allowed to mutate the effective vanilla SCALE attribute directly.
            // Stay inside the same authority tick and do not call AnatomyMovement.tick(): the first
            // physical query after the supported mutation must not use a stale material membership.
            support.getAttribute(Attributes.SCALE).setBaseValue(2);support.refreshDimensions();
            var after=provider.sampleAt(support,new AnatomyPoseHistory.Sample(inputs,support.position(),support.yBodyRot,2,
                AnatomyMovement.gravity(support))).orElseThrow().pieces().get("remote").bounds();
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
}
