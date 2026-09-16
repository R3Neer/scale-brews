package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.ModelGeometryProvider;
import io.github.r3neer.scalebrews.collision.pose.PlayerWalkingPose;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * S08 adversarial holdout for the replacement-pair boundary used by a real occupied boat.
 *
 * <p>The prepared session owns shared physics, while the player support is deliberately
 * re-registered through the non-causal fixture seam used by the historical prepared proof.
 * The replacement geometry is already available before the boat moves. Therefore vanilla
 * entity collision must not retain the support AABB and clip the displacement before the
 * anatomical sweep can see the exported head.</p>
 */
public final class S08AdversarialBoatPairSuppressionTests {
    private S08AdversarialBoatPairSuppressionTests() {}

    @GameTest(maxTicks = 80)
    public void occupiedBoatReplacementPairSuppressesVanillaSupportAabb(GameTestHelper h) throws IOException {
        String directory = System.getProperty("scalebrews.anatomyCatalog");
        if (directory == null || directory.isBlank())
            throw new IllegalStateException("S08 boat pair holdout requires scalebrews.anatomyCatalog");

        var gson = new com.google.gson.Gson();
        ModelGeometry geometry = gson.fromJson(
            Files.readString(Path.of(directory, "minecraft_player_wide.json")), ModelGeometry.class);
        if (geometry == null || geometry.format() != 2 || !"minecraft:player_wide".equals(geometry.source()))
            throw new IllegalArgumentException("S08 boat pair holdout requires the exported wide player geometry");

        var profile = new PlatformDefinition(
            Identifier.parse("minecraft:player"), true, .6, Optional.empty(), List.of(),
            Optional.of(new AnatomyDefinition(
                Identifier.parse(geometry.source()), Identifier.parse("scalebrews:player_walking"), AnatomyFilter.DEFAULT)));

        try (var session = AnatomyPreparedSession.start(
                h.getLevel().getServer(), Map.of(geometry.source(), geometry), Map.of("scalebrews:s08_boat_pair", profile))) {
            var giant = h.makeMockPlayer(GameType.SURVIVAL);
            giant.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(3);
            giant.refreshDimensions();
            giant.setPos(h.absoluteVec(new Vec3(2, 20, 2)));
            giant.yBodyRot = 0;
            giant.setNoGravity(true);
            h.getLevel().addFreshEntity(giant);

            var provider = new ModelGeometryProvider(geometry, new PlayerWalkingPose(), AnatomyFilter.DEFAULT, 1);
            provider.pose(giant, new PoseEngine.Inputs(0, 0, 0, 0, 0, true));
            var head = provider.sample(giant).orElseThrow().pieces().get("root/head/cube_0");
            h.assertTrue(head != null, "Exported player geometry must expose root/head/cube_0");

            var boat = h.spawn(EntityTypes.OAK_BOAT, 2, 27, 2);
            var rider = h.makeMockPlayer(GameType.SURVIVAL);
            rider.startRiding(boat, true, true);
            AnatomyMovement.register(giant, provider);

            try {
                var center = head.bounds().getCenter();
                boat.setPos(center.x, head.bounds().maxY + 1, center.z);
                Vec3 drop = new Vec3(0, -2, 0);

                h.assertTrue(Platforms.eligible(boat, giant),
                    "Occupied boat must remain eligible for the prepared player support");
                h.assertTrue(AnatomyMovement.replacesPair(boat, giant),
                    "Available exported head geometry must own the boat/player replacement pair");

                Entity previous = PlatformPhysics.enter(boat);
                boolean vanillaStillSeesSupport;
                boolean movingSide;
                boolean supportSide;
                try {
                    movingSide = boat.canCollideWith(giant);
                    supportSide = giant.canCollideWith(boat);
                    vanillaStillSeesSupport = !h.getLevel().getEntityCollisions(
                        boat, boat.getBoundingBox().expandTowards(drop)).isEmpty();
                } finally {
                    PlatformPhysics.exit(previous);
                }

                h.assertFalse(vanillaStillSeesSupport,
                    "Replacement pair must suppress the vanilla support AABB before anatomical collision; "
                        + "movingSide=" + movingSide + ", supportSide=" + supportSide);

                boat.move(net.minecraft.world.entity.MoverType.SELF, drop);
                var contact = AnatomyMovement.contact(boat);
                h.assertTrue(contact != null && "root/head/cube_0".equals(contact.piece()),
                    "Once vanilla pair collision is suppressed, the occupied boat must land on the exported player head");
            } finally {
                boat.discard();
                rider.discard();
                giant.discard();
            }
        }
        h.succeed();
    }
}
