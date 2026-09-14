package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** FR-053 holdout: an eligible pair has exactly one geometric blocker, the selected anatomy. */
public final class G2SingleBlockingOwnerReadyTests {
    private static final Identifier MODEL=Identifier.parse("test:g2_single_blocker_model");
    private static final Identifier STATIC_POSE=Identifier.parse("scalebrews:static");

    private static ModelGeometry catalogModel() {
        return new ModelGeometry(1,"test:g2_single_blocker_model","1",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("wall","root",List.of(1.5d,-1d,-1d),List.of(1.7d,3d,1d),null)),
            ModelGeometry.values(new Matrix4f()));
    }

    private static PlatformDefinition shulkerProfile() {
        return new PlatformDefinition(Identifier.parse("minecraft:shulker"),true,.6,Optional.of(.85),List.of(),
            Optional.of(new AnatomyDefinition(MODEL,STATIC_POSE,AnatomyFilter.DEFAULT)));
    }

    @GameTest
    public void entityMoveSuppressesVanillaAabbAndStopsAtSelectedAnatomy(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyRuntime.startPrepared(server,Map.of(MODEL.toString(),catalogModel()),Map.of("shulker",shulkerProfile()));
        // Shulkers, unlike ordinary mobs such as cows, are rigid vanilla entity blockers. This
        // makes revived vanilla AABB ownership observable when the pair-suppression route is mutated.
        var support=h.spawn(EntityTypes.SHULKER,6,20,2);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        try {
            support.setNoAi(true);support.setNoGravity(true);
            body.setNoGravity(true);
            body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();

            AABB supportBox=support.getBoundingBox();
            double wallMinX=supportBox.maxX+1.25;
            // Build the thin wall near the origin before translating it. ConvexBox.of uses the
            // model-style float matrix path, so embedding multi-million-block world coordinates
            // directly in the AABB can collapse a 0.2-block thickness to zero at float precision.
            double wallHeight=supportBox.getYsize()+2;
            var wall=ConvexBox.of(new AABB(0,0,-1,.2,wallHeight,1),new Matrix4f())
                .move(new Vec3(wallMinX,supportBox.minY-1,support.getZ()));
            GeometryProvider provider=entity->Optional.of(new GeometryProvider.Snapshot(0,Map.of("wall",wall)));
            AnatomyMovement.register(support,provider);

            h.assertTrue(AnatomyApi.ready(body) && AnatomyApi.ready(support),
                "Holdout must traverse the READY shared-physics route used by Entity.move");
            h.assertTrue(Platforms.eligible(body,support) && AnatomyMovement.replacesPair(body,support),
                "Holdout requires a canonically eligible pair whose vanilla blocker is explicitly replaced by anatomy");

            double startX=supportBox.minX-1;
            body.setPos(startX,supportBox.minY+.2,support.getZ());
            AABB startBox=body.getBoundingBox();
            Vec3 requested=new Vec3(wall.bounds().maxX-startX+1,0,0);
            h.assertTrue(startBox.expandTowards(requested).intersects(supportBox),
                "Precondition: the actual Entity.move path must cross the support's rigid vanilla AABB");
            h.assertTrue(wall.bounds().minX>supportBox.maxX+.5,
                "Precondition: selected anatomy must sit distinctly beyond the vanilla support AABB");

            body.move(MoverType.SELF,requested);

            h.assertTrue(body.getX()>supportBox.maxX+.5,
                "Eligible Entity.move must pass through the replaced rigid vanilla support AABB before reaching anatomy: supportMax="
                    +supportBox.maxX+" finalX="+body.getX());
            h.assertTrue(!wall.overlaps(body.getBoundingBox()),
                "Entity.move must stop non-penetrating at the selected anatomical blocker");
            var separation=wall.separation(body.getBoundingBox());
            h.assertTrue(Math.abs(separation.gap())<5e-3,
                "Final position must be determined by anatomy rather than an earlier vanilla AABB: gap="+separation.gap()
                    +" finalX="+body.getX()+" wallMin="+wall.bounds().minX);
        } finally {
            body.discard();support.discard();AnatomyRuntime.stop(server);
        }
        h.succeed();
    }
}
