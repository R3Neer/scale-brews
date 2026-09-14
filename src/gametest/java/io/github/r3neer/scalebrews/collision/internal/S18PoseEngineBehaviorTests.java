package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Adversarial S18 holdouts against wrapper-only migrations and permissive procedural engines. */
public final class S18PoseEngineBehaviorTests {
    private static final String LEGACY_PROVIDER = "io.github.r3neer.scalebrews.collision.pose.PoseProvider";
    private static final List<String> PROCEDURAL = List.of(
        "scalebrews:player_walking",
        "scalebrews:quadruped",
        "scalebrews:chicken",
        "scalebrews:villager",
        "scalebrews:iron_golem",
        "scalebrews:ghast",
        "scalebrews:feline",
        "scalebrews:equine",
        "scalebrews:bee"
    );

    @GameTest
    public void canonicalVanillaEnginesDoNotOwnLegacyProviderState(GameTestHelper h) {
        for (String text : PROCEDURAL) {
            var id = Identifier.parse(text);
            var engine = CollisionEngines.pose(id).orElse(null);
            h.assertTrue(engine != null, "Missing canonical procedural PoseEngine: " + id);
            var type = engine.getClass();
            boolean capturedLegacyProvider = java.util.Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.getType().getName().equals(LEGACY_PROVIDER));
            h.assertTrue(!capturedLegacyProvider,
                "Canonical PoseEngine must not capture a legacy PoseProvider owner: " + id);

            // Named classes can be inspected more strongly; synthetic lambdas remain valid if they capture no legacy owner.
            try (var input = type.getResourceAsStream(type.getSimpleName() + ".class")) {
                if (input != null) {
                    String pool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                    h.assertTrue(!pool.contains("io/github/r3neer/scalebrews/collision/pose/PoseProvider"),
                        "Named canonical PoseEngine bytecode must not call the legacy PoseProvider API: " + id);
                }
            } catch (IOException unreadable) {
                throw new AssertionError("Could not inspect canonical pose engine bytecode: " + id, unreadable);
            }
        }
        h.succeed();
    }

    @GameTest
    public void proceduralEngineIsDeterministicAndOrdinaryFailClosed(GameTestHelper h) {
        var engine = CollisionEngines.pose(Identifier.parse("scalebrews:quadruped")).orElse(null);
        h.assertTrue(engine != null, "Quadruped canonical engine is required for deterministic behavior proof");
        var geometry = quadrupedGeometry();
        var input = new PoseEngine.Inputs(2.75f, .45f, 31f, -23f, 11f, true, Map.of());
        var first = engine.evaluate(geometry, input, Map.of()).orElse(null);
        var second = engine.evaluate(geometry, input, Map.of()).orElse(null);
        h.assertTrue(first != null && second != null && sameMatrices(first, second),
            "Same authoritative inputs must produce exactly the same procedural pose");
        var unsupported = engine.evaluate(geometry,
            new PoseEngine.Inputs(input.walkPhase(), input.walkAmount(), input.age(), input.headYaw(), input.headPitch(), false, input.channels()),
            Map.of());
        h.assertTrue(unsupported.isEmpty(), "ordinary=false must fail closed instead of freezing/reusing the ordinary pose");
        h.succeed();
    }

    @GameTest
    public void proceduralEngineRejectsUnknownParameters(GameTestHelper h) {
        var engine = CollisionEngines.pose(Identifier.parse("scalebrews:quadruped")).orElse(null);
        h.assertTrue(engine != null, "Quadruped canonical engine is required for strict-parameter proof");
        var result = engine.evaluate(quadrupedGeometry(), new PoseEngine.Inputs(1, .5f, 10, 0, 0, true), Map.of("invented", "1"));
        h.assertTrue(result.isEmpty(), "Unknown procedural pose parameters must fail closed, not be silently ignored");
        h.succeed();
    }

    @GameTest
    public void sheepFinalEatTickIsNotOrdinaryQuadruped(GameTestHelper h) {
        var sheep = h.spawn(net.minecraft.world.entity.EntityTypes.SHEEP, 1, 2, 1);
        try {
            var field = net.minecraft.world.entity.animal.sheep.Sheep.class.getDeclaredField("eatAnimationTick");
            field.setAccessible(true);
            field.setInt(sheep, 1);
        } catch (ReflectiveOperationException inaccessible) {
            throw new AssertionError("Could not construct final sheep-eating frame", inaccessible);
        }

        float positionScale = sheep.getHeadEatPositionScale(1f);
        float angleScale = sheep.getHeadEatAngleScale(1f);
        h.assertTrue(Math.abs(positionScale) < 1e-7f,
            "Holdout precondition: final eating tick must have zero head position offset at the authoritative endpoint");
        h.assertTrue(Math.abs(angleScale - (float)(Math.PI / 5)) < 1e-6f
                && Math.abs(angleScale - sheep.getXRot() * (float)(Math.PI / 180.0)) > .1f,
            "Holdout precondition: SheepModel must still own a distinct eating head angle when position offset has reached zero");

        h.assertTrue(!AnatomyPoseEligibility.supported(Identifier.parse("scalebrews:quadruped"), sheep),
            "Quadruped eligibility must fail closed while SheepModel still overrides head rotation, even when getHeadEatPositionScale(1) is already zero");
        h.succeed();
    }

    private static ModelGeometry quadrupedGeometry() {
        var identity = ModelGeometry.values(new Matrix4f());
        return new ModelGeometry(1, "minecraft:cow", "26.2",
            List.of(
                new ModelGeometry.Part("root", null, identity),
                new ModelGeometry.Part("root/body", "root", identity),
                new ModelGeometry.Part("root/head", "root", identity),
                new ModelGeometry.Part("root/right_hind_leg", "root", identity),
                new ModelGeometry.Part("root/left_hind_leg", "root", identity),
                new ModelGeometry.Part("root/right_front_leg", "root", identity),
                new ModelGeometry.Part("root/left_front_leg", "root", identity)
            ),
            List.of(new ModelGeometry.Piece("body", "root/body", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            identity);
    }

    private static boolean sameMatrices(Map<String, Matrix4f> left, Map<String, Matrix4f> right) {
        if (!left.keySet().equals(right.keySet())) return false;
        for (var id : left.keySet()) if (!left.get(id).equals(right.get(id))) return false;
        return true;
    }
}
