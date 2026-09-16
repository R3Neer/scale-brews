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
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** IMPLEMENTER-only diagnostic. Remove once the occupied-boat contact owner is identified. */
public final class S08ImplementerBoatContactDiagnostic {
    public S08ImplementerBoatContactDiagnostic() {}

    @GameTest(maxTicks = 80)
    public void reportOccupiedBoatContactOwner(GameTestHelper h) throws IOException {
        String directory = System.getProperty("scalebrews.anatomyCatalog");
        if (directory == null || directory.isBlank())
            throw new IllegalStateException("S08 implementer diagnostic requires scalebrews.anatomyCatalog");

        var gson = new com.google.gson.Gson();
        ModelGeometry geometry = gson.fromJson(
            Files.readString(Path.of(directory, "minecraft_player_wide.json")), ModelGeometry.class);
        var profile = new PlatformDefinition(
            Identifier.parse("minecraft:player"), true, .6, Optional.empty(), List.of(),
            Optional.of(new AnatomyDefinition(
                Identifier.parse(geometry.source()), Identifier.parse("scalebrews:player_walking"), AnatomyFilter.DEFAULT)));

        try (var session = AnatomyPreparedSession.start(
                h.getLevel().getServer(), Map.of(geometry.source(), geometry), Map.of("scalebrews:s08_implementer_boat", profile))) {
            var giant = h.makeMockPlayer(GameType.SURVIVAL);
            giant.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(3);
            giant.refreshDimensions();
            giant.setPos(h.absoluteVec(new Vec3(2, 20, 2)));
            giant.yBodyRot = 0;
            giant.setNoGravity(true);
            h.getLevel().addFreshEntity(giant);

            var provider = new ModelGeometryProvider(geometry, new PlayerWalkingPose(), AnatomyFilter.DEFAULT, 1);
            provider.pose(giant, new PoseEngine.Inputs(0, 0, 0, 0, 0, true));
            var snapshot = provider.sample(giant).orElseThrow();
            var head = snapshot.pieces().get("root/head/cube_0");
            h.assertTrue(head != null, "Diagnostic requires root/head/cube_0");

            System.out.println("S08_IMPLEMENTER_PLAYER_PIECES " + snapshot.pieces().keySet());
            snapshot.pieces().entrySet().stream()
                .filter(entry -> entry.getKey().contains("head") || entry.getKey().contains("hat"))
                .forEach(entry -> System.out.println("S08_IMPLEMENTER_PLAYER_PIECE " + entry.getKey()
                    + " bounds=" + entry.getValue().bounds()));

            var boat = h.spawn(EntityTypes.OAK_BOAT, 2, 27, 2);
            var rider = h.makeMockPlayer(GameType.SURVIVAL);
            rider.startRiding(boat, true, true);
            AnatomyMovement.register(giant, provider);
            try {
                var center = head.bounds().getCenter();
                boat.setPos(center.x, head.bounds().maxY + 1, center.z);
                boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -2, 0));
                var contact = AnatomyMovement.contact(boat);
                System.out.println("S08_IMPLEMENTER_BOAT_CONTACT contact=" + contact
                    + " boatY=" + boat.getY() + " headMaxY=" + head.bounds().maxY);
            } finally {
                boat.discard();
                rider.discard();
                giant.discard();
            }
        }
        h.succeed();
    }
}
