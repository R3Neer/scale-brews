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

/** Differential holdout for Mojang applyWalk speedFactor + scaleFactor semantics. */
public final class S18ApplyWalkSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_apply_walk";
    private static final String VERSION = "26.2";
    private static final Identifier ENGINE = Identifier.parse("scalebrews:mojang_keyframes");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:s18_apply_walk_program");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = new AnimationDefinition(2f, false, Map.of("head", List.of(
                new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2f, KeyframeAnimations.posVec(16, 0, 0), AnimationChannel.Interpolations.LINEAR))
            )));
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);
            var baselineRoot = CowModel.createBodyLayer().bakeRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());
            var engine = CollisionEngines.pose(ENGINE).orElseThrow();
            PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? java.util.Optional.of(program) : java.util.Optional.empty();

            // Native applyWalk(pos, speed, 2, 2.5) means:
            //   timeSeconds = pos * 50ms * 2 / 1000 = pos * 0.1
            //   amplitude = min(speed * 2.5, 1)
            // The current neutral binding can express the time factor but only raw walk_amount amplitude.
            var bound = engine.bind(baseline,
                Map.of("program",PROGRAM.toString(),"clock","walk_phase","clock_scale","0.1","amplitude","walk_amount"),
                Set.of(), resources).orElseThrow();

            compareNative(definition, baseline, bound, 5f, .2f, 2f, 2.5f);
            compareNative(definition, baseline, bound, 8f, .6f, 2f, 2.5f);
            System.out.println("S18_APPLY_WALK PASS Minecraft 26.2 speedFactor/scaleFactor parity");
        });
    }

    private static void compareNative(AnimationDefinition definition, ModelGeometry baseline, PoseEngine.Bound bound,
                                      float walkPhase, float walkAmount, float speedFactor, float scaleFactor) {
        var nativeRoot = CowModel.createBodyLayer().bakeRoot();
        definition.bake(nativeRoot).applyWalk(walkPhase, walkAmount, speedFactor, scaleFactor);
        var nativeGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());
        var expected = partMatrix(nativeGeometry, "root/head");
        var inputs = new PoseEngine.Inputs(walkPhase, walkAmount, 0, 0, 0, true);
        var actual = bound.evaluate(inputs).orElseThrow().get("root/head");
        if (actual == null) throw new AssertionError("Neutral evaluator omitted root/head for applyWalk oracle");
        compare(expected, actual, 3e-5f, walkPhase, walkAmount);
    }

    private static Matrix4f partMatrix(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static void compare(Matrix4f expected, Matrix4f actual, float tolerance, float walkPhase, float walkAmount) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) if (Math.abs(a[i] - b[i]) > tolerance)
            throw new AssertionError("applyWalk parity phase=" + walkPhase + " amount=" + walkAmount
                + " matrix[" + i + "] expected=" + a[i] + " actual=" + b[i]);
    }
}
