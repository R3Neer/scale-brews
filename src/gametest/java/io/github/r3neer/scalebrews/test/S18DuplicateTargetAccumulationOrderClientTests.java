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

/** Differential holdout for sequential ModelPart float addition across duplicate target channels. */
public final class S18DuplicateTargetAccumulationOrderClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_duplicate_target_order";
    private static final String VERSION = "26.2";
    private static final String BODY = "root/body";
    private static final float REST_X = 0.00390625f; // half an ULP at 65536f

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var plus = new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0f, KeyframeAnimations.posVec(65_536, 0, 0), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1f, KeyframeAnimations.posVec(65_536, 0, 0), AnimationChannel.Interpolations.LINEAR));
            var minus = new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0f, KeyframeAnimations.posVec(-65_536, 0, 0), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1f, KeyframeAnimations.posVec(-65_536, 0, 0), AnimationChannel.Interpolations.LINEAR));
            var definition = new AnimationDefinition(1f, false, Map.of("body", List.of(plus, minus)));
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);
            if (program.tracks().size() != 2)
                throw new AssertionError("Precondition failed: compiler must preserve both duplicate POSITION channels");

            var baselineRoot = cowRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());
            var bound = PoseProgramEvaluator.bind(baseline, program).orElseThrow();

            var nativeRoot = cowRoot();
            definition.bake(nativeRoot).apply(500L, 1f);
            var expected = partMatrix(GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of()), BODY);
            var actual = bound.evaluate(baseline, .5f, 1f).orElseThrow().get(BODY);
            if (actual == null) throw new AssertionError("Neutral evaluator omitted body for duplicate-target oracle");

            float expectedX = expected.m30();
            float actualX = actual.m30();
            if (Math.abs(expectedX - actualX) <= 1e-5f)
                throw new AssertionError("Holdout precondition failed: fixture did not expose sequential float-addition order");
            compare(expected, actual, 1e-5f);
            System.out.println("S18_DUPLICATE_TARGET_ORDER PASS neutral evaluator preserves sequential ModelPart float-addition semantics");
        });
    }

    private static net.minecraft.client.model.geom.ModelPart cowRoot() {
        var root = CowModel.createBodyLayer().bakeRoot();
        root.getChild("body").x = REST_X;
        return root;
    }

    private static Matrix4f partMatrix(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static void compare(Matrix4f expected, Matrix4f actual, float tolerance) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) if (Math.abs(a[i] - b[i]) > tolerance)
            throw new AssertionError("duplicate target accumulation order matrix[" + i + "] expected=" + a[i] + " actual=" + b[i]);
    }
}
