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

/** Differential holdout for negative time on looping AnimationDefinition programs. */
public final class S18NegativeLoopSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_negative_loop";
    private static final String VERSION = "26.2";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = new AnimationDefinition(2f, true, Map.of("head", List.of(
                new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1f, KeyframeAnimations.posVec(8, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2f, KeyframeAnimations.posVec(16, 0, 0), AnimationChannel.Interpolations.LINEAR))
            )));
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);
            var baselineRoot = CowModel.createBodyLayer().bakeRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());
            var bound = PoseProgramEvaluator.bind(baseline, program).orElseThrow();

            for (float seconds : new float[]{-.25f, -1f, -2.25f, -4.25f})
                compareNative(definition, baseline, bound, seconds);

            System.out.println("S18_NEGATIVE_LOOP PASS original Minecraft 26.2 negative looping-time parity");
        });
    }

    private static void compareNative(AnimationDefinition definition, ModelGeometry baseline,
                                      PoseProgramEvaluator.Bound bound, float seconds) {
        var nativeRoot = CowModel.createBodyLayer().bakeRoot();
        definition.bake(nativeRoot).apply((long)Math.floor(seconds * 1000), 1f);
        var nativeGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());
        var expected = partMatrix(nativeGeometry, "root/head");
        var actual = bound.evaluate(baseline, seconds, 1f).orElseThrow().get("root/head");
        if (actual == null) throw new AssertionError("Neutral evaluator omitted root/head at negative looping seconds=" + seconds);
        compare(expected, actual, 3e-5f, seconds);
    }

    private static Matrix4f partMatrix(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static void compare(Matrix4f expected, Matrix4f actual, float tolerance, float seconds) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) if (Math.abs(a[i] - b[i]) > tolerance)
            throw new AssertionError("negative loop parity seconds=" + seconds + " matrix[" + i + "] expected=" + a[i] + " actual=" + b[i]);
    }
}
