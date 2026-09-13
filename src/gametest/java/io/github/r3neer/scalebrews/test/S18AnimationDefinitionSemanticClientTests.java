package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AnimationDefinitionCompiler;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProgramEvaluator;
import java.lang.reflect.Proxy;
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

/** Semantic client acceptance against Minecraft 26.2's original AnimationDefinition implementation. */
public final class S18AnimationDefinitionSemanticClientTests implements FabricClientGameTest {
    private static final String SOURCE = "scalebrews_test:s18_animation_cow";
    private static final String VERSION = "26.2";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var definition = fixtureDefinition(false);
            var program = AnimationDefinitionCompiler.compile(SOURCE, VERSION, definition);
            check(program.durationSeconds() == definition.lengthInSeconds() && program.loop() == definition.looping(),
                "Compiler must preserve duration/loop metadata");
            check(program.tracks().size() == 4,
                "Compiler must preserve additive channels, including duplicate bone/target tracks");

            var baselineRoot = CowModel.createBodyLayer().bakeRoot();
            var baseline = GeometryExtractor.vanilla(SOURCE, VERSION, baselineRoot, Set.of());
            var bound = PoseProgramEvaluator.bind(baseline, program).orElseThrow();

            for (float amplitude : new float[]{.35f, 1f}) {
                for (float seconds : new float[]{0f, .25f, .75f, .999f, 1f, 1.25f, 1.75f, 2f})
                    compareNative(definition, baseline, bound, seconds, amplitude, "clamped");
            }

            var looping = fixtureDefinition(true);
            var loopProgram = AnimationDefinitionCompiler.compile(SOURCE, VERSION, looping);
            check(loopProgram.loop(), "Compiler must preserve looping=true");
            var loopBound = PoseProgramEvaluator.bind(baseline, loopProgram).orElseThrow();
            for (float seconds : new float[]{2.25f, 4.25f, 5.75f})
                compareNative(looping, baseline, loopBound, seconds, .6f, "looping");

            boolean customTargetRejected = false;
            try {
                var customTarget = (AnimationChannel.Target) Proxy.newProxyInstance(
                    S18AnimationDefinitionSemanticClientTests.class.getClassLoader(),
                    new Class<?>[]{AnimationChannel.Target.class}, (proxy, method, args) -> null);
                AnimationDefinitionCompiler.compile(SOURCE, VERSION,
                    new AnimationDefinition(1f, false, Map.of("head", List.of(new AnimationChannel(customTarget,
                        new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR))))));
            } catch (IllegalArgumentException expected) {
                customTargetRejected = true;
            }
            check(customTargetRejected, "Unknown AnimationDefinition target must fail preparation instead of being skipped");

            boolean customInterpolationRejected = false;
            try {
                var customInterpolation = (AnimationChannel.Interpolation) Proxy.newProxyInstance(
                    S18AnimationDefinitionSemanticClientTests.class.getClassLoader(),
                    new Class<?>[]{AnimationChannel.Interpolation.class}, (proxy, method, args) -> null);
                AnimationDefinitionCompiler.compile(SOURCE, VERSION,
                    new AnimationDefinition(1f, false, Map.of("head", List.of(new AnimationChannel(AnimationChannel.Targets.POSITION,
                        new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), customInterpolation))))));
            } catch (IllegalArgumentException expected) {
                customInterpolationRejected = true;
            }
            check(customInterpolationRejected, "Unknown AnimationDefinition interpolation must fail preparation instead of being approximated");

            System.out.println("S18_ANIMATION_DEFINITION PASS original Minecraft 26.2 parity, loop wrap and fail-closed compiler");
        });
    }

    private static void compareNative(AnimationDefinition definition, ModelGeometry baseline,
                                      PoseProgramEvaluator.Bound bound, float seconds, float amplitude, String mode) {
        var nativeRoot = CowModel.createBodyLayer().bakeRoot();
        definition.bake(nativeRoot).apply((long)Math.floor(seconds * 1000), amplitude);
        var nativeGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());
        var nativeHead = partMatrix(nativeGeometry, "root/head");
        var neutralHead = bound.evaluate(baseline, seconds, amplitude).orElseThrow().get("root/head");
        check(neutralHead != null, "Neutral evaluator must publish the animated head transform");
        compare(nativeHead, neutralHead, 3e-5f,
            "AnimationDefinition " + mode + " parity seconds=" + seconds + " amplitude=" + amplitude);
    }

    private static AnimationDefinition fixtureDefinition(boolean loop) {
        return new AnimationDefinition(2f, loop, Map.of("head", List.of(
            new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1f, KeyframeAnimations.posVec(4, 0, 0), KeyframeAnimations.posVec(8, 0, 0), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2f, KeyframeAnimations.posVec(16, 4, -2), AnimationChannel.Interpolations.LINEAR)),
            new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2f, KeyframeAnimations.posVec(0, 2, 0), AnimationChannel.Interpolations.LINEAR)),
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0f, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(.5f, KeyframeAnimations.degreeVec(15, -5, 3), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5f, KeyframeAnimations.degreeVec(-20, 10, -7), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2f, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM)),
            new AnimationChannel(AnimationChannel.Targets.SCALE,
                new Keyframe(0f, KeyframeAnimations.scaleVec(1, 1, 1), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1f, KeyframeAnimations.scaleVec(1.12, .94, 1.05), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2f, KeyframeAnimations.scaleVec(1, 1, 1), AnimationChannel.Interpolations.LINEAR))
        )));
    }

    private static Matrix4f partMatrix(ModelGeometry geometry, String id) {
        return geometry.parts().stream().filter(part -> part.id().equals(id)).findFirst()
            .map(part -> ModelGeometry.matrix(part.transform())).orElseThrow();
    }

    private static void compare(Matrix4f expected, Matrix4f actual, float tolerance, String label) {
        float[] a = expected.get(new float[16]);
        float[] b = actual.get(new float[16]);
        for (int i = 0; i < 16; i++) if (Math.abs(a[i] - b[i]) > tolerance)
            throw new AssertionError(label + " matrix[" + i + "] expected=" + a[i] + " actual=" + b[i]);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
