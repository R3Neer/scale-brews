package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.BuiltInPoseEngines;
import io.github.r3neer.scalebrews.collision.pose.FelinePoseEngine;
import io.github.r3neer.scalebrews.collision.pose.MojangKeyframePoseEngine;
import io.github.r3neer.scalebrews.collision.pose.PlayerWalkingPoseEngine;
import io.github.r3neer.scalebrews.collision.pose.QuadrupedPoseEngine;
import io.github.r3neer.scalebrews.collision.pose.VanillaFamilyPoseEngine;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Implementer-owned S18 regression coverage discovered during the final architecture reread. */
public final class S18ImplementerRegressionTests {
    private static final float EPS = 2e-6f;
    private static final ModelGeometry.SourcePose SIGNED_HEAD = new ModelGeometry.SourcePose(
        16f, 8f, -4f, 0f, 0f, 0f, -1.5f, .75f, 1.25f);

    @GameTest
    public void finitePoseInputsDoNotBecomeInvalidOnlyBecauseTheirSumOverflows(GameTestHelper h) {
        var inputs = new PoseEngine.Inputs(
            Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, true, Map.of());
        h.assertTrue(Float.isFinite(inputs.walkPhase()) && Float.isFinite(inputs.walkAmount())
                && Float.isFinite(inputs.age()) && Float.isFinite(inputs.headYaw()) && Float.isFinite(inputs.headPitch()),
            "PoseEngine.Inputs must validate each finite authoritative component independently instead of summing them first");

        boolean rejectedInfinity = false;
        try {
            new PoseEngine.Inputs(Float.POSITIVE_INFINITY, 0, 0, 0, 0, true, Map.of());
        } catch (IllegalArgumentException expected) {
            rejectedInfinity = true;
        }
        h.assertTrue(rejectedInfinity, "Non-finite pose inputs must still fail closed");
        h.succeed();
    }

    @GameTest
    public void authorityAgeMatchesCompletedRendererEndpoint(GameTestHelper h) {
        var bee = h.spawn(net.minecraft.world.entity.EntityTypes.BEE, 1, 2, 1);
        bee.tickCount = 37;
        var inputs = new AuthorityPoseTracker().tick(bee, h.getLevel().getGameTime(), true);
        h.assertTrue(inputs.age() == 38f,
            "END_LEVEL_TICK authority must match the current renderer endpoint: ageInTicks = tickCount + partialTicks with partialTicks=1");
        h.succeed();
    }

    @GameTest
    public void authorityWalkPhasePreservesVanillaBabyPositionScale(GameTestHelper h) {
        var cow = h.spawn(net.minecraft.world.entity.EntityTypes.COW, 1, 2, 1);
        cow.setBaby(true);
        var tracker = new AuthorityPoseTracker();
        long tick = h.getLevel().getGameTime();
        tracker.tick(cow, tick, true);
        cow.setPos(cow.position().add(.25, 0, 0));
        var inputs = tracker.tick(cow, tick + 1, true);
        h.assertTrue(Math.abs(inputs.walkAmount() - .4f) < 1e-6f,
            "Vanilla walk smoothing must keep the 0.4 update factor at the current endpoint");
        h.assertTrue(Math.abs(inputs.walkPhase() - 1.2f) < 1e-6f,
            "Baby LivingEntity walkAnimation uses positionScale=3; authority must not animate baby legs at adult phase speed");
        h.succeed();
    }

    @GameTest
    public void builtInPoseInitializationIsIdempotentAndKeepsCanonicalOwners(GameTestHelper h) {
        var ids = List.of(
            Identifier.parse("scalebrews:player_walking"), Identifier.parse("scalebrews:quadruped"),
            Identifier.parse("scalebrews:chicken"), Identifier.parse("scalebrews:villager"),
            Identifier.parse("scalebrews:iron_golem"), Identifier.parse("scalebrews:ghast"),
            Identifier.parse("scalebrews:feline"), Identifier.parse("scalebrews:equine"),
            Identifier.parse("scalebrews:bee"), Identifier.parse("scalebrews:static"),
            Identifier.parse("scalebrews:mojang_keyframes"));
        var before = new LinkedHashMap<Identifier, PoseEngine>();
        for (var id : ids) before.put(id, CollisionEngines.pose(id).orElseThrow());

        BuiltInPoseEngines.initialize();
        for (var entry : before.entrySet())
            h.assertTrue(CollisionEngines.pose(entry.getKey()).orElseThrow() == entry.getValue(),
                "Repeated built-in initialization must preserve the exact canonical owner: " + entry.getKey());

        h.assertTrue(before.get(Identifier.parse("scalebrews:player_walking")) instanceof PlayerWalkingPoseEngine,
            "player_walking must be owned by PlayerWalkingPoseEngine");
        h.assertTrue(before.get(Identifier.parse("scalebrews:quadruped")) instanceof QuadrupedPoseEngine,
            "quadruped must be owned by QuadrupedPoseEngine");
        for (String path : List.of("chicken", "villager", "iron_golem", "ghast", "equine", "bee"))
            h.assertTrue(before.get(Identifier.fromNamespaceAndPath("scalebrews", path)) instanceof VanillaFamilyPoseEngine,
                path + " must be owned by the canonical VanillaFamilyPoseEngine");
        h.assertTrue(before.get(Identifier.parse("scalebrews:feline")) instanceof FelinePoseEngine,
            "feline must be owned by the exact common-side FelinePoseEngine");
        h.assertTrue(before.get(Identifier.parse("scalebrews:mojang_keyframes")) instanceof MojangKeyframePoseEngine,
            "mojang_keyframes must be owned by the common canonical keyframe engine");
        h.succeed();
    }

    @GameTest
    public void proceduralEnginesRejectAccidentalSourceMatches(GameTestHelper h) {
        var quadruped = CollisionEngines.pose(Identifier.parse("scalebrews:quadruped")).orElseThrow();
        var fakeQuadruped = ordinaryGeometry("minecraft:zombie",
            List.of("head", "right_hind_leg", "left_hind_leg", "right_front_leg", "left_front_leg"));
        h.assertTrue(quadruped.evaluate(fakeQuadruped, new PoseEngine.Inputs(1, .5f, 10, 0, 0, true), Map.of()).isEmpty(),
            "Matching part names are not authority to reinterpret an unrelated model as the vanilla quadruped family");

        var chicken = CollisionEngines.pose(Identifier.parse("scalebrews:chicken")).orElseThrow();
        var fakeChicken = ordinaryGeometry("example:chicken_shaped",
            List.of("head", "right_leg", "left_leg", "right_wing", "left_wing"));
        h.assertTrue(chicken.evaluate(fakeChicken,
            new PoseEngine.Inputs(1, .5f, 10, 0, 0, true, Map.of("flap", .4f, "flap_speed", .8f)), Map.of()).isEmpty(),
            "Procedural family engines must bind to known vanilla model sources, not structural coincidence");
        h.succeed();
    }

    @GameTest
    public void equineAgeScaleIsARequiredSemanticChannel(GameTestHelper h) {
        var equine = CollisionEngines.pose(Identifier.parse("scalebrews:equine")).orElseThrow();
        var geometry = ordinaryGeometry("minecraft:horse",
            List.of("body", "head_parts", "left_hind_leg", "right_hind_leg", "left_front_leg", "right_front_leg", "tail"));
        var missing = new PoseEngine.Inputs(1, .5f, 10, 0, 0, true,
            Map.of("eat", 0f, "stand", 0f, "mouth", 0f, "tail", 0f, "water", 0f));
        h.assertTrue(equine.evaluate(geometry, missing, Map.of()).isEmpty(),
            "Equine age_scale changes vanilla tail translation and must not silently default to adult scale when absent");
        var complete = new PoseEngine.Inputs(1, .5f, 10, 0, 0, true,
            Map.of("eat", 0f, "stand", 0f, "mouth", 0f, "tail", 0f, "water", 0f, "age_scale", 1f));
        h.assertTrue(equine.evaluate(geometry, complete, Map.of()).isPresent(),
            "Equine required-channel fixture must remain otherwise executable");
        h.succeed();
    }

    @GameTest
    public void proceduralPoseFamiliesPreserveExactSignedSourceScale(GameTestHelper h) {
        float yaw = 17f;
        float pitch = -8f;
        var expectedHead = new Matrix4f().translation(
                SIGNED_HEAD.x() / 16f, SIGNED_HEAD.y() / 16f, SIGNED_HEAD.z() / 16f)
            .rotateZYX(0f, yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD)
            .scale(SIGNED_HEAD.xScale(), SIGNED_HEAD.yScale(), SIGNED_HEAD.zScale());

        assertHead(h, "scalebrews:quadruped", ordinaryGeometry("minecraft:cow",
            List.of("head", "right_hind_leg", "left_hind_leg", "right_front_leg", "left_front_leg")),
            new PoseEngine.Inputs(1.25f, .6f, 20f, yaw, pitch, true), expectedHead);

        assertHead(h, "scalebrews:player_walking", ordinaryGeometry("minecraft:player_wide",
            List.of("head", "right_arm", "left_arm", "right_leg", "left_leg")),
            new PoseEngine.Inputs(1.25f, .6f, 20f, yaw, pitch, true), expectedHead);

        assertHead(h, "scalebrews:chicken", ordinaryGeometry("minecraft:chicken",
            List.of("head", "right_leg", "left_leg", "right_wing", "left_wing")),
            new PoseEngine.Inputs(1.25f, .6f, 20f, yaw, pitch, true, Map.of("flap", .4f, "flap_speed", .8f)), expectedHead);
        h.succeed();
    }

    private static void assertHead(GameTestHelper h, String engineId, ModelGeometry geometry,
                                   PoseEngine.Inputs inputs, Matrix4f expected) {
        var engine = CollisionEngines.pose(Identifier.parse(engineId)).orElseThrow();
        var result = engine.evaluate(geometry, inputs, Map.of()).orElseThrow();
        var actual = result.get("root/head");
        h.assertTrue(actual != null, "Procedural engine must publish the animated head: " + engineId);
        assertMatrixNear(h, expected, actual, engineId + " must preserve exact signed SourcePose scale and translation");
        h.assertTrue(actual.determinant3x3() < 0f,
            "Procedural engine must not erase a reflected rest part while applying its ordinary rotation: " + engineId);
    }

    private static ModelGeometry ordinaryGeometry(String source, List<String> animatedParts) {
        var identity = ModelGeometry.values(new Matrix4f());
        var parts = new java.util.ArrayList<ModelGeometry.Part>();
        parts.add(new ModelGeometry.Part("root", null, identity));
        for (String name : animatedParts) {
            if (name.equals("head")) {
                parts.add(new ModelGeometry.Part("root/head", "root", ModelGeometry.values(SIGNED_HEAD.matrix()), SIGNED_HEAD));
            } else {
                parts.add(new ModelGeometry.Part("root/" + name, "root", identity));
            }
        }
        return new ModelGeometry(2, source, "26.2", parts,
            List.of(new ModelGeometry.Piece("piece", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            identity);
    }

    private static void assertMatrixNear(GameTestHelper h, Matrix4f expected, Matrix4f actual, String message) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < a.length; i++)
            h.assertTrue(Math.abs(a[i] - b[i]) <= EPS,
                message + " at matrix[" + i + "]: expected=" + a[i] + " actual=" + b[i]);
    }
}
