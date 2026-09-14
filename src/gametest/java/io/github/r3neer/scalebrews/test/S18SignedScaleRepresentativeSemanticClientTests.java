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
 * S18 architecture holdout: distinct signed-scale source states can collapse to
 * the same local matrix while Mojang additive SCALE targets distinguish them.
 */
public final class S18SignedScaleRepresentativeSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_signed_scale_representative";
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
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);

            // A = I * scale(-1.5, +0.75, 1.25)
            var aRest = GeometryExtractor.vanilla(SOURCE, VERSION,
                cowRoot(0f, -1.5f, .75f, 1.25f), Set.of());
            // B = Rz(pi) * scale(+1.5, -0.75, 1.25), which is exactly the
            // same initial affine matrix as A.
            var bRest = GeometryExtractor.vanilla(SOURCE, VERSION,
                cowRoot((float)Math.PI, 1.5f, -.75f, 1.25f), Set.of());
            assertMatrixNear(part(aRest), part(bRest),
                "precondition: distinct signed-scale representatives must collapse to the same rest matrix");

            var aNativeRoot = cowRoot(0f, -1.5f, .75f, 1.25f);
            var bNativeRoot = cowRoot((float)Math.PI, 1.5f, -.75f, 1.25f);
            definition.bake(aNativeRoot).apply(500L, 1f);
            definition.bake(bNativeRoot).apply(500L, 1f);
            var aNative = part(GeometryExtractor.vanilla(SOURCE, VERSION, aNativeRoot, Set.of()));
            var bNative = part(GeometryExtractor.vanilla(SOURCE, VERSION, bNativeRoot, Set.of()));
            if (maxDiff(aNative, bNative) < .2f)
                throw new AssertionError("Holdout precondition failed: Mojang additive SCALE did not separate source representatives");

            var aNeutral = PoseProgramEvaluator.bind(aRest, program).orElseThrow()
                .evaluate(aRest, .5f, 1f).orElseThrow().get(BODY);
            var bNeutral = PoseProgramEvaluator.bind(bRest, program).orElseThrow()
                .evaluate(bRest, .5f, 1f).orElseThrow().get(BODY);
            if (aNeutral == null || bNeutral == null)
                throw new AssertionError("Neutral evaluator did not publish body SCALE target");

            // Because aRest and bRest are identical at the neutral boundary, an
            // evaluator that only sees the matrix cannot reproduce both native
            // outputs. These assertions make that information loss executable.
            assertMatrixNear(aNative, aNeutral, "signed-scale representative A");
            assertMatrixNear(bNative, bNeutral, "signed-scale representative B");
            System.out.println("S18_SIGNED_SCALE_REPRESENTATIVE PASS neutral material preserves source signed-scale identity");
        });
    }

    private static net.minecraft.client.model.geom.ModelPart cowRoot(float zRot, float xScale, float yScale, float zScale) {
        var root = CowModel.createBodyLayer().bakeRoot();
        var body = root.getChild("body");
        body.xRot = 0f;
        body.yRot = 0f;
        body.zRot = zRot;
        body.xScale = xScale;
        body.yScale = yScale;
        body.zScale = zScale;
        return root;
    }

    private static Matrix4f part(ModelGeometry geometry) {
        return geometry.parts().stream().filter(part -> part.id().equals(BODY)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static void assertMatrixNear(Matrix4f expected, Matrix4f actual, String label) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) {
            if (Math.abs(a[i] - b[i]) > EPS)
                throw new AssertionError(label + " differs at matrix[" + i + "]: expected=" + a[i] + " actual=" + b[i]);
        }
    }

    private static float maxDiff(Matrix4f a, Matrix4f b) {
        float[] left = a.get(new float[16]);
        float[] right = b.get(new float[16]);
        float max = 0f;
        for (int i = 0; i < 16; i++) max = Math.max(max, Math.abs(left[i] - right[i]));
        return max;
    }
}
