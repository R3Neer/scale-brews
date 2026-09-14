package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Permanent common-side checks for explicit Mojang keyframe binding transforms. */
public final class MojangKeyframeBindingParameterTests {
    private static final Identifier ENGINE = Identifier.parse("scalebrews:mojang_keyframes");
    private static final Identifier PROGRAM = Identifier.parse("scalebrews_test:binding_parameters");

    @GameTest
    public void walkAmplitudeScaleAndCapAreExplicitBindingData(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        var geometry = geometry();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program()) : Optional.empty();
        var bound = engine.bind(geometry, Map.of(
                "program", PROGRAM.toString(),
                "clock", "walk_phase",
                "clock_scale", "0.1",
                "amplitude", "walk_amount",
                "amplitude_scale", "2.5",
                "amplitude_max", "1"),
            Set.of(), resources).orElseThrow();

        assertTranslation(h, bound, .2f, .25f);
        assertTranslation(h, bound, .6f, .5f);
        h.succeed();
    }

    @GameTest
    public void ageClockPreservesAnimationStateMillisecondTruncation(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        var geometry = geometry();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program()) : Optional.empty();
        var bound = engine.bind(geometry, Map.of(
                "program", PROGRAM.toString(),
                "clock", "age",
                "clock_scale", "0.333",
                "amplitude", "one"),
            Set.of(), resources).orElseThrow();

        // AnimationState.getTimeInMillis(1 tick) -> 50 ms, then KeyframeAnimation applies
        // speedFactor=.333 and truncates again: (long)(50*.333)=16 ms. The fixture reaches
        // one block at t=2s, hence 0.5 block/s * .016s = .008 blocks.
        var result = bound.evaluate(new PoseEngine.Inputs(0, 0, 1f, 0, 0, true)).orElseThrow();
        var matrix = result.get("root");
        h.assertTrue(matrix != null, "Age-clock evaluator must produce the referenced root transform");
        h.assertTrue(Math.abs(matrix.m30() - .008f) < 1e-6f,
            "Age clock must preserve AnimationState/KeyframeAnimation integer-millisecond truncation: actual X=" + matrix.m30());
        h.succeed();
    }

    @GameTest
    public void malformedAmplitudeTransformsFailClosedAtBind(GameTestHelper h) {
        var engine = CollisionEngines.pose(ENGINE).orElseThrow();
        var geometry = geometry();
        PoseEngine.Resources resources = id -> id.equals(PROGRAM) ? Optional.of(program()) : Optional.empty();
        var base = new java.util.LinkedHashMap<String, String>();
        base.put("program", PROGRAM.toString());
        base.put("clock", "walk_phase");
        base.put("clock_scale", "0.1");
        base.put("amplitude", "walk_amount");

        var invalidScale = new java.util.LinkedHashMap<>(base);
        invalidScale.put("amplitude_scale", "NaN");
        h.assertTrue(engine.bind(geometry, invalidScale, Set.of(), resources).isEmpty(),
            "Non-finite amplitude_scale must fail closed while binding");

        var invalidMax = new java.util.LinkedHashMap<>(base);
        invalidMax.put("amplitude_max", "Infinity");
        h.assertTrue(engine.bind(geometry, invalidMax, Set.of(), resources).isEmpty(),
            "Non-finite amplitude_max must fail closed while binding");
        h.succeed();
    }

    private static void assertTranslation(GameTestHelper h, PoseEngine.Bound bound, float walkAmount, float expectedBlocks) {
        var result = bound.evaluate(new PoseEngine.Inputs(10f, walkAmount, 0, 0, 0, true)).orElseThrow();
        var matrix = result.get("root");
        h.assertTrue(matrix != null, "Bound evaluator must produce the referenced root transform");
        h.assertTrue(Math.abs(matrix.m30() - expectedBlocks) < 1e-6f,
            "Explicit amplitude transform mismatch: expected X=" + expectedBlocks + " actual=" + matrix.m30());
    }

    private static ModelGeometry geometry() {
        var pose = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(2, "proof:binding-parameters", "26.2",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(pose.matrix()), pose)),
            List.of(new ModelGeometry.Piece("piece", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new org.joml.Matrix4f()));
    }

    private static PoseProgram program() {
        var zero = new PoseProgram.Vector(0, 0, 0);
        var end = new PoseProgram.Vector(16, 0, 0);
        return new PoseProgram(PoseProgram.SCHEMA_VERSION, PROGRAM.toString(), "26.2", 2f, false, List.of(
            new PoseProgram.Track("root", PoseProgram.Target.TRANSLATION, List.of(
                new PoseProgram.Keyframe(0, zero, zero, PoseProgram.Interpolation.LINEAR),
                new PoseProgram.Keyframe(2, end, end, PoseProgram.Interpolation.LINEAR)))));
    }
}
