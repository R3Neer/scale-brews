package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AnimationDefinitionCompiler;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProgramEvaluator;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.animal.cow.CowModel;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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

            // Retroactive adversarial strengthening: the original S18 oracle animated
            // only head. A compiler that silently ignored every other bone therefore
            // survived. Compare independent original Minecraft evaluation for two
            // distinct bones and publish a deterministic transform contact sheet.
            var multiBone = multiBoneDefinition();
            var multiProgram = AnimationDefinitionCompiler.compile(SOURCE, VERSION, multiBone);
            check(multiProgram.tracks().stream().anyMatch(track -> track.bone().equals("body")),
                "Compiler must preserve a non-head animation track");
            var multiBound = PoseProgramEvaluator.bind(baseline, multiProgram).orElseThrow();
            // Capture the diagnostic before assertions so a red parity run still
            // leaves a reproducible visual artifact for the implementer.
            writeMultiBoneSnapshot(multiBone, baseline, multiBound);
            for (float seconds : new float[]{0f, .35f, .8f, 1f, 1.45f, 2f}) {
                compareNativeBone(multiBone, baseline, multiBound, "head", "root/head", seconds, .7f, "multi-bone");
                compareNativeBone(multiBone, baseline, multiBound, "body", "root/body", seconds, .7f, "multi-bone");
            }

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

            System.out.println("S18_ANIMATION_DEFINITION PASS original Minecraft 26.2 parity, multi-bone coverage, loop wrap and fail-closed compiler");
        });
    }

    private static void compareNative(AnimationDefinition definition, ModelGeometry baseline,
                                      PoseProgramEvaluator.Bound bound, float seconds, float amplitude, String mode) {
        compareNativeBone(definition, baseline, bound, "head", "root/head", seconds, amplitude, mode);
    }

    private static void compareNativeBone(AnimationDefinition definition, ModelGeometry baseline,
                                          PoseProgramEvaluator.Bound bound, String boneName, String partId,
                                          float seconds, float amplitude, String mode) {
        var nativeRoot = CowModel.createBodyLayer().bakeRoot();
        definition.bake(nativeRoot).apply((long)Math.floor(seconds * 1000), amplitude);
        var nativeGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());
        var nativeBone = partMatrix(nativeGeometry, partId);
        var neutralBone = bound.evaluate(baseline, seconds, amplitude).orElseThrow().get(partId);
        check(neutralBone != null, "Neutral evaluator must publish animated bone " + boneName + " at " + partId);
        compare(nativeBone, neutralBone, 3e-5f,
            "AnimationDefinition " + mode + " parity bone=" + boneName + " seconds=" + seconds + " amplitude=" + amplitude);
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

    private static AnimationDefinition multiBoneDefinition() {
        var bones = new LinkedHashMap<String, List<AnimationChannel>>();
        bones.put("head", fixtureDefinition(false).boneAnimations().get("head"));
        bones.put("body", List.of(
            new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0f, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1f, KeyframeAnimations.posVec(0, 3, -2), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2f, KeyframeAnimations.posVec(0, -1, 1), AnimationChannel.Interpolations.LINEAR)),
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0f, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(.8f, KeyframeAnimations.degreeVec(12, 18, -4), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.45f, KeyframeAnimations.degreeVec(-8, -11, 6), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2f, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM))
        ));
        return new AnimationDefinition(2f, false, Map.copyOf(bones));
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

    private static void writeMultiBoneSnapshot(AnimationDefinition definition, ModelGeometry baseline,
                                               PoseProgramEvaluator.Bound bound) {
        try {
            var times = new float[]{0f, .35f, .8f, 1f, 1.45f, 2f};
            var bones = List.of(new String[]{"head", "root/head"}, new String[]{"body", "root/body"});
            var out = new StringBuilder();
            out.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"960\" height=\"520\" viewBox=\"0 0 960 520\">\n")
                .append("<rect width=\"960\" height=\"520\" fill=\"white\"/>\n")
                .append("<text x=\"18\" y=\"24\" font-family=\"monospace\" font-size=\"16\">S18 multi-bone | original thick blue / neutral thin red | Minecraft 26.2</text>\n");
            for (int row = 0; row < bones.size(); row++) {
                String bone = bones.get(row)[0], part = bones.get(row)[1];
                out.append(String.format(Locale.ROOT, "<text x=\"14\" y=\"%.1f\" font-family=\"monospace\" font-size=\"14\">%s</text>\n", 92d + row * 205d, bone));
                for (int col = 0; col < times.length; col++) {
                    float seconds = times[col];
                    var nativeRoot = CowModel.createBodyLayer().bakeRoot();
                    definition.bake(nativeRoot).apply((long)Math.floor(seconds * 1000), .7f);
                    var nativeGeometry = GeometryExtractor.vanilla(SOURCE, VERSION, nativeRoot, Set.of());
                    Matrix4f original = partMatrix(nativeGeometry, part);
                    Matrix4f neutral = bound.evaluate(baseline, seconds, .7f).orElseThrow().get(part);
                    if (neutral == null) throw new AssertionError("Snapshot missing neutral bone " + part);
                    double cx = 110d + col * 140d, cy = 125d + row * 205d;
                    drawAxes(out, original, cx, cy, "#1f77b4", 5.0);
                    drawAxes(out, neutral, cx, cy, "#d62728", 1.7);
                    out.append(String.format(Locale.ROOT, "<text x=\"%.1f\" y=\"%.1f\" font-family=\"monospace\" font-size=\"11\">t=%.2f</text>\n", cx - 22, cy + 78, seconds));
                }
            }
            out.append("</svg>\n");
            var dir = FabricLoader.getInstance().getGameDir().resolve("s18-retro-snapshots");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("multi_bone.svg"), out.toString());
        } catch (java.io.IOException failure) {
            throw new AssertionError("Could not write S18 multi-bone snapshot", failure);
        }
    }

    private static void drawAxes(StringBuilder out, Matrix4f matrix, double cx, double cy, String color, double width) {
        Vector3f origin = matrix.transformPosition(new Vector3f(0, 0, 0));
        Vector3f x = matrix.transformPosition(new Vector3f(.7f, 0, 0));
        Vector3f y = matrix.transformPosition(new Vector3f(0, .7f, 0));
        double scale = 48d;
        double ox = cx + origin.x * scale, oy = cy - origin.y * scale;
        double xx = cx + x.x * scale, xy = cy - x.y * scale;
        double yx = cx + y.x * scale, yy = cy - y.y * scale;
        out.append(String.format(Locale.ROOT,
            "<path d=\"M %.3f %.3f L %.3f %.3f M %.3f %.3f L %.3f %.3f\" fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\" stroke-linecap=\"round\"/>\n",
            ox, oy, xx, xy, ox, oy, yx, yy, color, width));
        out.append(String.format(Locale.ROOT,
            "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"%.1f\" fill=\"%s\"/>\n", ox, oy, Math.max(1.5, width * .55), color));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
