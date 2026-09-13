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
    public void canonicalVanillaEnginesDoNotDependOnLegacyPoseProvider(GameTestHelper h) {
        for (String text : PROCEDURAL) {
            var id = Identifier.parse(text);
            var engine = CollisionEngines.pose(id).orElse(null);
            h.assertTrue(engine != null, "Missing canonical procedural PoseEngine: " + id);
            var type = engine.getClass();
            try (var input = type.getResourceAsStream(type.getSimpleName() + ".class")) {
                h.assertTrue(input != null, "Canonical pose engine must be an auditable common class: " + id);
                String pool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                h.assertTrue(!pool.contains("io/github/r3neer/scalebrews/collision/pose/PoseProvider"),
                    "Canonical PoseEngine must own formula behavior instead of wrapping legacy PoseProvider: " + id);
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

    private static ModelGeometry quadrupedGeometry() {
        var identity = ModelGeometry.values(new Matrix4f());
        return new ModelGeometry(1, "minecraft:cow", "26.2",
            List.of(
                new ModelGeometry.Part("root", null, identity),
                new ModelGeometry.Part("root/head", "root", identity),
                new ModelGeometry.Part("root/right_hind_leg", "root", identity),
                new ModelGeometry.Part("root/left_hind_leg", "root", identity),
                new ModelGeometry.Part("root/right_front_leg", "root", identity),
                new ModelGeometry.Part("root/left_front_leg", "root", identity)
            ),
            List.of(new ModelGeometry.Piece("body", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            identity);
    }

    private static boolean sameMatrices(Map<String, Matrix4f> left, Map<String, Matrix4f> right) {
        if (!left.keySet().equals(right.keySet())) return false;
        for (var id : left.keySet()) if (!left.get(id).equals(right.get(id))) return false;
        return true;
    }
}
