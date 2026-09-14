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
import net.minecraft.client.model.animal.cow.CowModel;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Post-fix S18 adversarial holdout for Euler-representative loss.
 *
 * <p>ModelPart animates its xRot/yRot/zRot fields additively. Two different
 * Euler triples can encode the same rest orientation, so collapsing rest pose
 * to a matrix and later choosing a principal Euler decomposition is not in
 * general sufficient to recover Mojang's target semantics.</p>
 */
public final class S18EulerRepresentativeSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_euler_representative";
    private static final String VERSION = "26.2";
    private static final String BODY = "root/body";
    private static final float EPS = 4e-5f;

    private static final float X = .3f;
    private static final float Y = .8f;
    private static final float Z = -.6f;
    // For ZYX Euler rotations, (x + pi, pi - y, z + pi) represents the same
    // initial orientation as (x, y, z), but component-wise additive deltas do
    // not preserve that equivalence in general.
    private static final float ALT_X = X + (float) Math.PI;
    private static final float ALT_Y = (float) Math.PI - Y;
    private static final float ALT_Z = Z + (float) Math.PI;

    private static final Vector3f DELTA = new Vector3f(.2f, -.15f, .1f);

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = new AnimationDefinition(1f, false, Map.of("body", List.of(
                new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0f, new Vector3f(DELTA), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1f, new Vector3f(DELTA), AnimationChannel.Interpolations.LINEAR))
            )));
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);

            var canonicalRest = GeometryExtractor.vanilla(SOURCE, VERSION, cowRoot(X, Y, Z), Set.of());
            var alternateRest = GeometryExtractor.vanilla(SOURCE, VERSION, cowRoot(ALT_X, ALT_Y, ALT_Z), Set.of());
            assertMatrixNear(part(canonicalRest, BODY), part(alternateRest, BODY),
                "precondition: equivalent Euler rest representations must collapse to the same local matrix");

            var canonicalNativeRoot = cowRoot(X, Y, Z);
            var alternateNativeRoot = cowRoot(ALT_X, ALT_Y, ALT_Z);
            definition.bake(canonicalNativeRoot).apply(500L, 1f);
            definition.bake(alternateNativeRoot).apply(500L, 1f);
            var canonicalNative = part(GeometryExtractor.vanilla(SOURCE, VERSION, canonicalNativeRoot, Set.of()), BODY);
            var alternateNative = part(GeometryExtractor.vanilla(SOURCE, VERSION, alternateNativeRoot, Set.of()), BODY);
            if (maxDiff(canonicalNative, alternateNative) < .05f)
                throw new AssertionError("Holdout precondition failed: additive Euler delta did not separate equivalent rest representatives");

            var canonicalNeutral = PoseProgramEvaluator.bind(canonicalRest, program).orElseThrow()
                .evaluate(canonicalRest, .5f, 1f).orElseThrow().get(BODY);
            var alternateNeutral = PoseProgramEvaluator.bind(alternateRest, program).orElseThrow()
                .evaluate(alternateRest, .5f, 1f).orElseThrow().get(BODY);
            if (canonicalNeutral == null || alternateNeutral == null)
                throw new AssertionError("Neutral evaluator did not publish body ROTATION target");

            assertMatrixNear(canonicalNative, canonicalNeutral, "canonical Euler representative");
            assertMatrixNear(alternateNative, alternateNeutral, "alternate equivalent Euler representative");
            System.out.println("S18_EULER_REPRESENTATIVE PASS neutral program preserves Mojang additive Euler semantics");
        });
    }

    private static net.minecraft.client.model.geom.ModelPart cowRoot(float x, float y, float z) {
        var root = CowModel.createBodyLayer().bakeRoot();
        var body = root.getChild("body");
        body.xRot = x;
        body.yRot = y;
        body.zRot = z;
        return root;
    }

    private static Matrix4f part(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
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
