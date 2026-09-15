package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Condition;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Delta;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Keyframe;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Operation;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Scalar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Implementer regression coverage for the reusable common Citadel evaluator. */
public final class S20ImplementerCitadelPoseTests {
    private static final Identifier PROGRAM = Identifier.parse("test:citadel_program");

    @GameTest
    public void builtInCitadelEngineIsRegistered(GameTestHelper h) {
        h.assertTrue(CollisionEngines.pose(Identifier.parse("scalebrews:citadel_program")).isPresent(),
            "Reusable Citadel pose family must be registered in common");
        h.succeed();
    }

    @GameTest
    public void proceduralWalkMatchesCitadelHelper(GameTestHelper h) {
        var rest = new ModelGeometry.SourcePose(0, 16, 0, .2f, -.1f, .05f, 1, 1, 1);
        var program = new CitadelPoseProgram(1, "test:citadel", "1", List.of(), List.of(
            new Operation(CitadelPoseProgram.OperationType.WALK, "root/body", List.of(), Condition.always(),
                null, null, null, Scalar.builtin(CitadelPoseProgram.ScalarSource.WALK_PHASE),
                Scalar.builtin(CitadelPoseProgram.ScalarSource.WALK_AMOUNT), null,
                .7f, .4f, .3f, .1f, 0, true, false)
        ));
        var engine = CollisionEngines.pose(Identifier.parse("scalebrews:citadel_program")).orElseThrow();
        var bound = engine.bind(geometry(rest), Map.of("program", PROGRAM.toString()), Set.of(), resources(program)).orElse(null);
        h.assertTrue(bound != null, "Citadel program must bind revision-locally");

        var inputs = new PoseEngine.Inputs(2.5f, .6f, 30, 0, 0, true);
        var actual = bound.evaluate(inputs).orElseThrow().get("root/body");
        float delta = -(Mth.cos(inputs.walkPhase() * .7f + .3f) * .4f * inputs.walkAmount() + .1f * inputs.walkAmount());
        var expectedPose = new ModelGeometry.SourcePose(rest.x(), rest.y(), rest.z(), rest.xRot() + delta,
            rest.yRot(), rest.zRot(), rest.xScale(), rest.yScale(), rest.zScale());
        assertMatrixNear(h, expectedPose.matrix(), actual, "Citadel walk formula must match AdvancedEntityModel exactly");
        h.succeed();
    }

    @GameTest
    public void modelAnimatorTweenUsesSineEaseAndFailsUnknownAnimation(GameTestHelper h) {
        var rest = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        var clip = new CitadelPoseProgram.Clip(7, List.of(
            new Keyframe(4, false, List.of(new Delta("root/body", 1, 0, 0, 0, 0, 0))),
            new Keyframe(4, false, List.of(new Delta("root/body", -1, 0, 0, 0, 0, 0))),
            new Keyframe(4, false, List.of())
        ));
        var program = new CitadelPoseProgram(1, "test:citadel", "1", List.of(clip), List.of());
        var engine = CollisionEngines.pose(Identifier.parse("scalebrews:citadel_program")).orElseThrow();
        var required = Set.of(CitadelPoseProgram.ANIMATION_CHANNEL, CitadelPoseProgram.ANIMATION_TICK_CHANNEL);
        var bound = engine.bind(geometry(rest), Map.of("program", PROGRAM.toString()), required, resources(program)).orElseThrow();

        var channels = Map.of(CitadelPoseProgram.ANIMATION_CHANNEL, 7f, CitadelPoseProgram.ANIMATION_TICK_CHANNEL, 2f);
        var actual = bound.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true, channels)).orElseThrow().get("root/body");
        float inc = Mth.sin((float)(.5 * Math.PI / 2.0));
        var expected = new ModelGeometry.SourcePose(0, 0, 0, inc, 0, 0, 1, 1, 1).matrix();
        assertMatrixNear(h, expected, actual, "ModelAnimator current keyframe must use sin(t*pi/2) easing");

        var unknown = Map.of(CitadelPoseProgram.ANIMATION_CHANNEL, 99f, CitadelPoseProgram.ANIMATION_TICK_CHANNEL, 0f);
        h.assertTrue(bound.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, true, unknown)).isEmpty(),
            "Unknown authoritative Citadel animation token must fail closed");
        h.succeed();
    }

    @GameTest
    public void bindingRejectsMissingSourcePoseAndUndeclaredChannels(GameTestHelper h) {
        var progress = Scalar.channel("citadel.stand_progress");
        var program = new CitadelPoseProgram(1, "test:citadel", "1", List.of(), List.of(
            new Operation(CitadelPoseProgram.OperationType.PROGRESS_ROTATION, "root/body", List.of(), Condition.always(),
                Scalar.constant(1), Scalar.constant(0), Scalar.constant(0), null, null, progress,
                0, 0, 0, 0, 10, false, false)
        ));
        var engine = CollisionEngines.pose(Identifier.parse("scalebrews:citadel_program")).orElseThrow();
        h.assertTrue(engine.bind(geometry(new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1)),
            Map.of("program", PROGRAM.toString()), Set.of(), resources(program)).isEmpty(),
            "Program-implied channels must be declared by the canonical binding");
        h.assertTrue(engine.bind(geometryWithoutSourcePose(), Map.of("program", PROGRAM.toString()),
            Set.of("citadel.stand_progress"), resources(program)).isEmpty(),
            "Citadel operations must not approximate a bone whose exact source pose is unavailable");
        h.succeed();
    }

    private static PoseEngine.Resources resources(CitadelPoseProgram program) {
        return new PoseEngine.Resources() {
            @Override public Optional<io.github.r3neer.scalebrews.collision.pose.PoseProgram> program(Identifier id) {
                return Optional.empty();
            }
            @Override public Optional<CitadelPoseProgram> citadelProgram(Identifier id) {
                return PROGRAM.equals(id) ? Optional.of(program) : Optional.empty();
            }
        };
    }

    private static ModelGeometry geometry(ModelGeometry.SourcePose body) {
        var root = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(2, "test:citadel", "1", List.of(
            new ModelGeometry.Part("root", null, ModelGeometry.values(root.matrix()), root),
            new ModelGeometry.Part("root/body", "root", ModelGeometry.values(body.matrix()), body)
        ), List.of(), ModelGeometry.values(new Matrix4f()));
    }

    private static ModelGeometry geometryWithoutSourcePose() {
        var root = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(2, "test:citadel", "1", List.of(
            new ModelGeometry.Part("root", null, ModelGeometry.values(root.matrix()), root),
            new ModelGeometry.Part("root/body", "root", ModelGeometry.values(new Matrix4f()), null)
        ), List.of(), ModelGeometry.values(new Matrix4f()));
    }

    private static void assertMatrixNear(GameTestHelper h, Matrix4f expected, Matrix4f actual, String message) {
        h.assertTrue(actual != null, message + " (missing matrix)");
        float[] a = expected.get(new float[16]), b = actual.get(new float[16]);
        for (int i = 0; i < a.length; i++)
            h.assertTrue(Math.abs(a[i] - b[i]) < 2e-5f, message + " at matrix index " + i + ": expected=" + a[i] + " actual=" + b[i]);
    }
}
