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
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Adversarial FR-053 holdout: prepared anatomy is physical only for an eligible moving-platform pair. */
public final class G2EligibilityNoWallReadyTests {
    private static final Identifier MODEL=Identifier.parse("test:g2_eligibility_model");
    private static final Identifier STATIC_POSE=Identifier.parse("scalebrews:static");

    private static ModelGeometry catalogModel() {
        return new ModelGeometry(1,"test:g2_eligibility_model","1",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("wall","root",List.of(-.1d,-.1d,-1d),List.of(.1d,4d,1d),null)),
            ModelGeometry.values(new Matrix4f()));
    }

    private static PlatformDefinition cowProfile() {
        return new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.of(.85),List.of(),
            Optional.of(new AnatomyDefinition(MODEL,STATIC_POSE,AnatomyFilter.DEFAULT)));
    }

    @GameTest
    public void preparedGeometryIsNotAWallForIneligibleBodies(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyRuntime.startPrepared(server,Map.of(MODEL.toString(),catalogModel()),Map.of("cow",cowProfile()));
        var support=h.spawn(EntityTypes.COW,6,20,2);
        var eligible=h.makeMockPlayer(GameType.SURVIVAL);
        var ineligible=h.makeMockPlayer(GameType.SURVIVAL);
        try {
            support.setNoAi(true);support.setNoGravity(true);
            eligible.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);eligible.refreshDimensions();
            ineligible.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(2);ineligible.refreshDimensions();

            Vec3 wallCenter=support.position().add(-4,0,0);
            var wall=ConvexBox.of(new AABB(-.1,-.1,-1,.1,4,1),new Matrix4f()).move(wallCenter);
            GeometryProvider provider=entity->Optional.of(new GeometryProvider.Snapshot(0,Map.of("wall",wall)));
            AnatomyMovement.register(support,provider);

            h.assertTrue(AnatomyApi.ready(eligible) && AnatomyApi.ready(ineligible) && AnatomyApi.ready(support),
                "Holdout must traverse READY shared physics rather than legacy fallback");
            h.assertTrue(Platforms.eligible(eligible,support),
                "Small control body must qualify for the canonical moving-platform relation");
            h.assertTrue(!Platforms.eligible(ineligible,support),
                "Large holdout body must exceed canonical max_width_ratio and remain outside moving-platform ownership");
            h.assertTrue(AnatomyMovement.replacesPair(eligible,support),
                "Eligible control pair must actually select the prepared anatomy as its physical blocker");
            h.assertTrue(!AnatomyMovement.replacesPair(ineligible,support),
                "Prepared geometry alone must not replace vanilla physics for an ineligible pair");

            Vec3 requested=new Vec3(2,0,0);
            eligible.setPos(wallCenter.x-1,wallCenter.y,wallCenter.z);
            Vec3 eligibleAllowed=AnatomyMovement.collide(eligible,requested);
            h.assertTrue(eligibleAllowed.distanceToSqr(requested)>1e-8 && eligibleAllowed.x<requested.x-.05,
                "Control precondition: eligible body must be stopped by the prepared anatomical wall, requested="
                    +requested+" allowed="+eligibleAllowed);

            eligible.discard();
            ineligible.setPos(wallCenter.x-1,wallCenter.y,wallCenter.z);
            Vec3 ineligibleAllowed=AnatomyMovement.collide(ineligible,requested);
            h.assertTrue(ineligibleAllowed.distanceToSqr(requested)<=1e-12,
                "An ineligible entity must not acquire an anatomical wall merely because geometry is prepared: requested="
                    +requested+" allowed="+ineligibleAllowed);
            h.assertTrue(AnatomyMovement.contact(ineligible)==null,
                "Ineligible body must not acquire retained material contact while crossing prepared geometry");
        } finally {
            eligible.discard();ineligible.discard();support.discard();AnatomyRuntime.stop(server);
        }
        h.succeed();
    }
}
