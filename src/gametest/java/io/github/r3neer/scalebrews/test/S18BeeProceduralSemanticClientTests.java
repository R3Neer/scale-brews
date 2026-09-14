package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.bee.AdultBeeModel;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Original-source differential holdout for collision-relevant vanilla bee wing motion. */
public final class S18BeeProceduralSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "minecraft:bee";
    private static final String VERSION = "26.2";
    private static final String BONE = "root/bone";
    private static final String RIGHT_WING = "root/bone/right_wing";
    private static final String LEFT_WING = "root/bone/left_wing";
    private static final float EPS = 3e-5f;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var baselineRoot = AdultBeeModel.createBodyLayer().bakeRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());

            // Wings are not degenerate renderer-only planes: CubeDeformation(0.001) gives them
            // non-zero material thickness, and the public AnatomyFilter include contract can
            // deliberately retain them even though DEFAULT rejects such thin anatomy.
            var includeWings = new AnatomyFilter(
                AnatomyFilter.DEFAULT.minThickness(), AnatomyFilter.DEFAULT.minAspect(), AnatomyFilter.DEFAULT.minVolumeRatio(),
                Set.of(RIGHT_WING, LEFT_WING), Set.of());
            var report = baseline.filterReport(includeWings);
            assertRetainedPart(baseline, report, RIGHT_WING);
            assertRetainedPart(baseline, report, LEFT_WING);

            var nativeRoot = AdultBeeModel.createBodyLayer().bakeRoot();
            var nativeModel = new AdultBeeModel(nativeRoot);
            var state = new BeeRenderState();
            state.ageInTicks = 7.5f;
            state.isOnGround = false;
            state.isAngry = false;
            state.rollAmount = .25f;
            nativeModel.setupAnim(state);
            var expectedGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());

            var expected = expectedGeometry.transforms(Map.of());
            var rest = baseline.transforms(Map.of());
            assertSeparated(rest.get(RIGHT_WING), expected.get(RIGHT_WING),
                "precondition: airborne Mojang bee must animate collision-selectable right_wing away from rest");
            assertSeparated(rest.get(LEFT_WING), expected.get(LEFT_WING),
                "precondition: airborne Mojang bee must animate collision-selectable left_wing away from rest");

            var engine = CollisionEngines.pose(Identifier.parse("scalebrews:bee")).orElseThrow();
            var inputs = new PoseEngine.Inputs(0, 0, state.ageInTicks, 0, 0, true,
                Map.of("on_ground", 0f, "angry", 0f, "roll", state.rollAmount));
            var replacements = engine.evaluate(baseline, inputs, Map.of()).orElseThrow();
            var actual = baseline.transforms(replacements);

            for (String part : new String[] {BONE, RIGHT_WING, LEFT_WING}) {
                var expectedPart = expected.get(part);
                var actualPart = actual.get(part);
                if (expectedPart == null || actualPart == null)
                    throw new AssertionError("Bee parity fixture missing part " + part);
                assertNear(expectedPart, actualPart, "bee procedural parity " + part);
            }
            System.out.println("S18_BEE_PROCEDURAL PASS canonical bee engine matches Minecraft 26.2 for selectable material wings");
        });
    }

    private static void assertRetainedPart(ModelGeometry geometry, Map<String, String> report, String partId) {
        boolean hasPiece = false;
        boolean retained = false;
        for (var piece : geometry.pieces()) if (piece.part().equals(partId)) {
            hasPiece = true;
            retained |= "retained".equals(report.get(piece.id()));
        }
        if (!hasPiece) throw new AssertionError("Bee fixture has no non-degenerate material piece for " + partId);
        if (!retained) throw new AssertionError("Explicit AnatomyFilter include failed to retain collision-selectable " + partId);
    }

    private static void assertSeparated(Matrix4f rest, Matrix4f animated, String message) {
        if (rest == null || animated == null) throw new AssertionError(message + " (missing fixture part)");
        float[] a = rest.get(new float[16]);
        float[] b = animated.get(new float[16]);
        float max = 0;
        for (int i = 0; i < a.length; i++) max = Math.max(max, Math.abs(a[i] - b[i]));
        if (max <= .01f) throw new AssertionError(message + ": max matrix delta=" + max);
    }

    private static void assertNear(Matrix4f expected, Matrix4f actual, String label) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > EPS)
                throw new AssertionError(label + " differs at matrix[" + i + "]: expected=" + a[i] + " actual=" + b[i]);
        }
    }
}
