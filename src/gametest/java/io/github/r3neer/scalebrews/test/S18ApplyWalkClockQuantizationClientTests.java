package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AnimationDefinitionCompiler;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
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
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Differential holdout for applyWalk's millisecond truncation before keyframe sampling. */
public final class S18ApplyWalkClockQuantizationClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_apply_walk_clock_quantization";
    private static final String VERSION = "26.2";
    private static final Identifier ENGINE = Identifier.parse("scalebrews:mojang_keyframes");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:s18_apply_walk_clock_quantization_program");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = new AnimationDefinition(2f, false, Map.of("head", List.of(
                new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2f, KeyframeAnimations.posVec(1600, 0, 0), AnimationChannel.Interpolations.LINEAR))
            )));
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);
            var baselineRoot = CowModel.createBodyLayer().bakeRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());
            var engine = CollisionEngines.pose(ENGINE).orElseThrow();
            PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? java.util.Optional.of(program) : java.util.Optional.empty();
            var bound = engine.bind(baseline,
                Map.of("program",PROGRAM.toString(),"clock","walk_phase","clock_scale","0.1","amplitude","one"),
                Set.of(), resources).orElseThrow();

            // Native: (long)(5.019 * 50 * 2) = 501 ms before sampling.
            // Neutral today: 5.019 * 0.1 = 0.5019 s, retaining the discarded fraction.
            compareNative(definition, baseline, bound, 5.019f, 2f);
            System.out.println("S18_APPLY_WALK_CLOCK_QUANTIZATION PASS Minecraft 26.2 millisecond-truncation parity");
        });
    }

    private static void compareNative(AnimationDefinition definition, ModelGeometry baseline, PoseEngine.Bound bound,
                                      float walkPhase, float speedFactor) {
        var nativeRoot = CowModel.createBodyLayer().bakeRoot();
        definition.bake(nativeRoot).applyWalk(walkPhase, 1f, speedFactor, 1f);
        var nativeGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());
        var expected = partMatrix(nativeGeometry, "root/head");
        var actual = bound.evaluate(new PoseEngine.Inputs(walkPhase, 1f, 0, 0, 0, true))
            .orElseThrow().get("root/head");
        if (actual == null) throw new AssertionError("Neutral evaluator omitted root/head for applyWalk clock oracle");
        compare(expected, actual, 3e-5f, walkPhase);
    }

    private static Matrix4f partMatrix(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static void compare(Matrix4f expected, Matrix4f actual, float tolerance, float walkPhase) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) if (Math.abs(a[i] - b[i]) > tolerance)
            throw new AssertionError("applyWalk clock quantization parity phase=" + walkPhase
                + " matrix[" + i + "] expected=" + a[i] + " actual=" + b[i]);
    }
}
