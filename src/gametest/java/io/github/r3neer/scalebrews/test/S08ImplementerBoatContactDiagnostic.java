package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.ModelGeometryProvider;
import io.github.r3neer.scalebrews.collision.pose.PlayerWalkingPose;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** IMPLEMENTER-only diagnostic. Remove once the occupied-boat contact owner is classified. */
public final class S08ImplementerBoatContactDiagnostic {
    private static final Set<String> PLAYER_COSMETIC_PARTS = Set.of(
        "root/head/hat",
        "root/body/jacket",
        "root/left_arm/left_sleeve",
        "root/right_arm/right_sleeve",
        "root/left_leg/left_pants",
        "root/right_leg/right_pants"
    );

    public S08ImplementerBoatContactDiagnostic() {}

    private static AnatomyFilter playerPhysicalFilter() {
        var defaults = AnatomyFilter.DEFAULT;
        return new AnatomyFilter(defaults.minThickness(), defaults.minAspect(), defaults.minVolumeRatio(),
            Set.of(), PLAYER_COSMETIC_PARTS);
    }

    @GameTest(maxTicks = 80)
    public void explicitCosmeticFilterRestoresBaseHeadContact(GameTestHelper h) throws IOException {
        String directory = System.getProperty("scalebrews.anatomyCatalog");
        if (directory == null || directory.isBlank())
            throw new IllegalStateException("S08 implementer diagnostic requires scalebrews.anatomyCatalog");

        var gson = new com.google.gson.Gson();
        ModelGeometry geometry = gson.fromJson(
            Files.readString(Path.of(directory, "minecraft_player_wide.json")), ModelGeometry.class);
        var filter = playerPhysicalFilter();
        var profile = new PlatformDefinition(
            Identifier.parse("minecraft:player"), true, .6, Optional.empty(), List.of(),
            Optional.of(new AnatomyDefinition(
                Identifier.parse(geometry.source()), Identifier.parse("scalebrews:player_walking"), filter)));

        try (var session = AnatomyPreparedSession.start(
                h.getLevel().getServer(), Map.of(geometry.source(), geometry), Map.of("scalebrews:s08_implementer_boat", profile))) {
            var giant = h.makeMockPlayer(GameType.SURVIVAL);
            giant.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(3);
            giant.refreshDimensions();
            giant.setPos(h.absoluteVec(new Vec3(2, 20, 2)));
            giant.yBodyRot = 0;
            giant.setNoGravity(true);
            h.getLevel().addFreshEntity(giant);

            var provider = new ModelGeometryProvider(geometry, new PlayerWalkingPose(), filter, 1);
            provider.pose(giant, new PoseEngine.Inputs(0, 0, 0, 0, 0, true));
            var snapshot = provider.sample(giant).orElseThrow();
            var head = snapshot.pieces().get("root/head/cube_0");
            h.assertTrue(head != null, "Filtered player anatomy retains root/head/cube_0");
            h.assertTrue(snapshot.pieces().keySet().stream().noneMatch(id ->
                    PLAYER_COSMETIC_PARTS.stream().anyMatch(part -> id.equals(part) || id.startsWith(part + "/"))),
                "FR-020 filter removes the visible second skin layer from physical anatomy");

            System.out.println("S08_IMPLEMENTER_FILTERED_PLAYER_PIECES " + snapshot.pieces().keySet());

            var boat = h.spawn(EntityTypes.OAK_BOAT, 2, 27, 2);
            var rider = h.makeMockPlayer(GameType.SURVIVAL);
            rider.startRiding(boat, true, true);
            AnatomyMovement.register(giant, provider);
            try {
                var center = head.bounds().getCenter();
                boat.setPos(center.x, head.bounds().maxY + 1, center.z);
                boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -2, 0));
                var contact = AnatomyMovement.contact(boat);
                System.out.println("S08_IMPLEMENTER_FILTERED_BOAT_CONTACT contact=" + contact
                    + " boatY=" + boat.getY() + " headMaxY=" + head.bounds().maxY);
                h.assertTrue(contact != null && contact.piece().equals("root/head/cube_0"),
                    "Explicit FR-020 cosmetic filter makes the occupied boat contact the base player head");
                h.assertTrue(Math.abs(boat.getY() - head.bounds().maxY) < 1e-5,
                    "Filtered contact height matches the base exported player head");
            } finally {
                boat.discard();
                rider.discard();
                giant.discard();
            }
        }
        h.succeed();
    }
}
