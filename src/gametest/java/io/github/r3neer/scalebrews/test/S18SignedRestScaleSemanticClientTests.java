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
import org.joml.Matrix4f;

/**
 * Post-fix S18 adversarial holdout for signed rest-scale preservation.
 *
 * <p>ModelPart accepts signed x/y/z scale fields and translateAndRotate applies
 * them directly. ModelGeometry also accepts non-degenerate affine transforms
 * with a negative determinant. A neutral pose evaluator must therefore not
 * silently turn a reflected rest part into an unreflected one when applying an
 * additive Mojang SCALE target.</p>
 */
public final class S18SignedRestScaleSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_signed_rest_scale";
    private static final String VERSION = "26.2";
    private static final String BODY = "root/body";
    private static final float EPS = 4e-5f;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = new AnimationDefinition(1f, false, Map.of("body", List.of(
                new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0f, KeyframeAnimations.scaleVec(1.2f, 1f, 1f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1f, KeyframeAnimations.scaleVec(1.2f, 1f, 1f), AnimationChannel.Interpolations.LINEAR))
            )));

            var restRoot = reflectedCowRoot();
            var rest = GeometryExtractor.vanilla(SOURCE, VERSION, restRoot, Set.of());
            var restMatrix = part(rest, BODY);
            if (!(restMatrix.determinant3x3() < 0f))
                throw new AssertionError("Holdout precondition failed: reflected ModelPart did not survive geometry extraction");

            var nativeRoot = reflectedCowRoot();
            definition.bake(nativeRoot).apply(500L, 1f);
            var nativeMatrix = part(GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of()), BODY);
            if (!(nativeMatrix.determinant3x3() < 0f))
                throw new AssertionError("Holdout precondition failed: Mojang SCALE target unexpectedly removed reflection");

            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);
            var neutral = PoseProgramEvaluator.bind(rest, program).orElseThrow()
                .evaluate(rest, .5f, 1f).orElseThrow().get(BODY);
            if (neutral == null) throw new AssertionError("Neutral evaluator did not publish body SCALE target");

            assertMatrixNear(nativeMatrix, neutral);
            System.out.println("S18_SIGNED_REST_SCALE PASS neutral SCALE target preserves signed ModelPart rest scale");
        });
    }

    private static net.minecraft.client.model.geom.ModelPart reflectedCowRoot() {
        var root = CowModel.createBodyLayer().bakeRoot();
        var body = root.getChild("body");
        body.xScale = -1.5f;
        body.yScale = .75f;
        body.zScale = 1.25f;
        return root;
    }

    private static Matrix4f part(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static void assertMatrixNear(Matrix4f expected, Matrix4f actual) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) {
            if (Math.abs(a[i] - b[i]) > EPS)
                throw new AssertionError("signed rest SCALE differs at matrix[" + i + "]: expected=" + a[i]
                    + " actual=" + b[i] + " expectedDet=" + expected.determinant3x3()
                    + " actualDet=" + actual.determinant3x3());
        }
    }
}
