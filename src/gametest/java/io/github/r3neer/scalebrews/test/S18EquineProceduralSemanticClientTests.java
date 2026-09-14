package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Original-source differential holdout for authoritative equine standing/rearing pose. */
public final class S18EquineProceduralSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "minecraft:horse";
    private static final String VERSION = "26.2";
    private static final float EPS = 4e-5f;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var baselineRoot = horseRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());

            var nativeRoot = horseRoot();
            var nativeModel = new HorseModel(nativeRoot);
            var state = new EquineRenderState();
            state.ageInTicks = 10f;
            state.walkAnimationPos = 0f;
            state.walkAnimationSpeed = 0f;
            state.xRot = 0f;
            state.yRot = 0f;
            state.eatAnimation = 0f;
            state.standAnimation = 1f;
            state.feedingAnimation = 0f;
            state.animateTail = false;
            state.isInWater = false;
            nativeModel.setupAnim(state);
            var expected = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of()).transforms(Map.of());

            var rest = baseline.transforms(Map.of());
            assertSeparated(rest.get("root/body"), expected.get("root/body"),
                "precondition: standing horse must rotate volumetric body away from rest");
            assertSeparated(rest.get("root/right_front_leg"), expected.get("root/right_front_leg"),
                "precondition: standing horse must move/rotate volumetric front leg away from rest");

            var engine = CollisionEngines.pose(Identifier.parse("scalebrews:equine")).orElseThrow();
            var inputs = new PoseEngine.Inputs(
                state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot, state.xRot, true,
                Map.of("eat", state.eatAnimation,
                       "stand", state.standAnimation,
                       "mouth", state.feedingAnimation,
                       "tail", state.animateTail ? 1f : 0f,
                       "water", state.isInWater ? 1f : 0f));
            var replacements = engine.evaluate(baseline, inputs, Map.of()).orElseThrow();
            var actual = baseline.transforms(replacements);

            for (String part : new String[] {
                    "root/body",
                    "root/head_parts",
                    "root/right_hind_leg",
                    "root/left_hind_leg",
                    "root/right_front_leg",
                    "root/left_front_leg"}) {
                assertNear(expected.get(part), actual.get(part), "equine standing parity " + part);
            }
            System.out.println("S18_EQUINE_PROCEDURAL PASS canonical equine engine matches Minecraft 26.2 standing pose");
        });
    }

    private static net.minecraft.client.model.geom.ModelPart horseRoot() {
        return LayerDefinition.create(AbstractEquineModel.createBodyMesh(CubeDeformation.NONE), 64, 64).bakeRoot();
    }

    private static void assertSeparated(Matrix4f rest, Matrix4f animated, String message) {
        if (rest == null || animated == null) throw new AssertionError(message + " (missing fixture part)");
        float[] a = rest.get(new float[16]);
        float[] b = animated.get(new float[16]);
        float max = 0f;
        for (int i = 0; i < a.length; i++) max = Math.max(max, Math.abs(a[i] - b[i]));
        if (max <= .01f) throw new AssertionError(message + ": max matrix delta=" + max);
    }

    private static void assertNear(Matrix4f expected, Matrix4f actual, String label) {
        if (expected == null || actual == null) throw new AssertionError(label + " (missing fixture part)");
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > EPS)
                throw new AssertionError(label + " differs at matrix[" + i + "]: expected=" + a[i] + " actual=" + b[i]);
        }
    }
}
