package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AnimationDefinitionCompiler;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProgramEvaluator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.animal.cow.CowModel;
import org.joml.Vector3f;

/**
 * Retroactive S18 adversarial holdout for Mojang SCALE target semantics.
 *
 * <p>Minecraft 26.2's KeyframeAnimations.scaleVec stores additive deltas
 * around 1 and ModelPart.offsetScale adds those deltas to the current scale.
 * A neutral evaluator must therefore preserve a non-unit rest scale instead of
 * treating the sampled delta as a multiplicative percentage.</p>
 */
public final class S18NonUnitRestScaleSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_non_unit_rest_scale";
    private static final String VERSION = "26.2";
    private static final String BODY = "root/body";
    private static final float EPS = 3e-5f;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = new AnimationDefinition(1f, false, Map.of("body", List.of(
                new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0f, KeyframeAnimations.scaleVec(1, 1, 1), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1f, KeyframeAnimations.scaleVec(1.2, .8, 1.1), AnimationChannel.Interpolations.LINEAR))
            )));

            var baselineRoot = scaledCowRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);
            var bound = PoseProgramEvaluator.bind(baseline, program).orElseThrow();

            var nativeRoot = scaledCowRoot();
            definition.bake(nativeRoot).apply(1000L, 1f);
            var nativeGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());
            var expected = scale(part(nativeGeometry, BODY));
            var actualMatrix = bound.evaluate(baseline, 1f, 1f).orElseThrow().get(BODY);
            if (actualMatrix == null) throw new AssertionError("Neutral evaluator did not publish body SCALE target");
            var actual = scale(actualMatrix);

            assertNear(expected.x, actual.x, "xScale");
            assertNear(expected.y, actual.y, "yScale");
            assertNear(expected.z, actual.z, "zScale");
            System.out.println("S18_NON_UNIT_REST_SCALE PASS Mojang additive SCALE target preserves non-unit rest scale");
        });
    }

    private static net.minecraft.client.model.geom.ModelPart scaledCowRoot() {
        var root = CowModel.createBodyLayer().bakeRoot();
        var body = root.getChild("body");
        body.xScale = 1.5f;
        body.yScale = .75f;
        body.zScale = 1.25f;
        return root;
    }

    private static org.joml.Matrix4f part(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static Vector3f scale(org.joml.Matrix4f matrix) {
        return matrix.getScale(new Vector3f());
    }

    private static void assertNear(float expected, float actual, String axis) {
        if (Math.abs(expected - actual) > EPS)
            throw new AssertionError("Mojang SCALE target diverges for non-unit rest " + axis
                + ": expected=" + expected + " actual=" + actual);
    }
}
