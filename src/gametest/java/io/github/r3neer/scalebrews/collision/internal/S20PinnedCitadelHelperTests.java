package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.CitadelPoseProgram.Condition;
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

/** Regressions copied from the exact shaded Citadel dialect bundled by Alex's Mobs 2.1.9. */
public final class S20PinnedCitadelHelperTests {
    private static final Identifier PROGRAM = Identifier.parse("test:citadel_2_1_9_helpers");

    @GameTest
    public void progressBobAndFaceTargetMatchShadedCitadel219(GameTestHelper h) {
        var bodyRest = new ModelGeometry.SourcePose(3, 5, -2, .31f, -.22f, .17f, 1, 1, 1);
        var neckRest = new ModelGeometry.SourcePose(0, -4, -3, -.15f, .27f, -.08f, 1, 1, 1);
        var progress = Scalar.channel("citadel.progress");
        var program = new CitadelPoseProgram(1, "test:citadel", "2.1.9", List.of(), List.of(
            new Operation(CitadelPoseProgram.OperationType.PROGRESS_ROTATION, "root/body", List.of(), Condition.always(),
                Scalar.constant(.5f), Scalar.constant(-.25f), Scalar.constant(.1f), null, null, progress,
                0, 0, 0, 0, 10, false, false),
            new Operation(CitadelPoseProgram.OperationType.PROGRESS_POSITION, "root/body", List.of(), Condition.always(),
                Scalar.constant(2), Scalar.constant(-3), Scalar.constant(4), null, null, progress,
                0, 0, 0, 0, 10, false, false),
            new Operation(CitadelPoseProgram.OperationType.BOB, "root/body", List.of(), Condition.always(),
                null, null, null, Scalar.builtin(CitadelPoseProgram.ScalarSource.WALK_PHASE),
                Scalar.builtin(CitadelPoseProgram.ScalarSource.WALK_AMOUNT), null,
                .7f, .4f, 0, 0, 0, false, false),
            new Operation(CitadelPoseProgram.OperationType.FACE_TARGET, null, List.of("root/body", "root/body/neck"),
                Condition.always(), null, null, null, null, null, null,
                0, 0, 0, 0, 2, false, false)
        ));
        var engine = CollisionEngines.pose(Identifier.parse("scalebrews:citadel_program")).orElseThrow();
        var bound = engine.bind(geometry(bodyRest, neckRest), Map.of("program", PROGRAM.toString()),
            Set.of("citadel.progress"), resources(program)).orElseThrow();

        float walkPhase = 1.3f, walkAmount = .6f, headYaw = 20, headPitch = -10, p = 5;
        var actual = bound.evaluate(new PoseEngine.Inputs(walkPhase, walkAmount, 0, headYaw, headPitch, true,
            Map.of("citadel.progress", p))).orElseThrow();

        float sharedYaw = headYaw * Mth.DEG_TO_RAD / 4f;   // divisor 2 * two target boxes
        float sharedPitch = headPitch * Mth.DEG_TO_RAD / 4f;
        float bob = (float)Math.sin(walkPhase * .7f) * walkAmount * .4f - walkAmount * .4f;
        var expectedBody = new ModelGeometry.SourcePose(
            bodyRest.x() + p * 2 / 10f,
            bodyRest.y() + p * -3 / 10f + bob,
            bodyRest.z() + p * 4 / 10f,
            bodyRest.xRot() + p * .5f / 10f + sharedPitch,
            bodyRest.yRot() + p * -.25f / 10f + sharedYaw,
            bodyRest.zRot() + p * .1f / 10f,
            1, 1, 1);
        var expectedNeck = new ModelGeometry.SourcePose(neckRest.x(), neckRest.y(), neckRest.z(),
            neckRest.xRot() + sharedPitch, neckRest.yRot() + sharedYaw, neckRest.zRot(), 1, 1, 1);
        assertMatrixNear(h, expectedBody.matrix(), actual.get("root/body"),
            "progress*Prev must add raw deltas and non-bounce bob must use the shaded Citadel sine formula");
        assertMatrixNear(h, expectedNeck.matrix(), actual.get("root/body/neck"),
            "faceTarget must divide yaw/pitch by rotationDivisor * targetCount");
        h.succeed();
    }

    @GameTest
    public void bounceBobMatchesShadedCitadel219(GameTestHelper h) {
        var bodyRest = new ModelGeometry.SourcePose(0, 7, 0, 0, 0, 0, 1, 1, 1);
        var program = new CitadelPoseProgram(1, "test:citadel", "2.1.9", List.of(), List.of(
            new Operation(CitadelPoseProgram.OperationType.BOB, "root/body", List.of(), Condition.always(),
                null, null, null, Scalar.builtin(CitadelPoseProgram.ScalarSource.WALK_PHASE),
                Scalar.builtin(CitadelPoseProgram.ScalarSource.WALK_AMOUNT), null,
                .8f, .3f, 0, 0, 0, false, true)
        ));
        var engine = CollisionEngines.pose(Identifier.parse("scalebrews:citadel_program")).orElseThrow();
        var bound = engine.bind(geometry(bodyRest, new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1)),
            Map.of("program", PROGRAM.toString()), Set.of(), resources(program)).orElseThrow();
        float phase = 2.2f, amount = .75f;
        var actual = bound.evaluate(new PoseEngine.Inputs(phase, amount, 0, 0, 0, true)).orElseThrow().get("root/body");
        float bob = -Math.abs((float)Math.sin(phase * .8f) * amount * .3f);
        var expected = new ModelGeometry.SourcePose(0, 7 + bob, 0, 0, 0, 0, 1, 1, 1).matrix();
        assertMatrixNear(h, expected, actual, "bounce bob must use -abs(sin(...)) in shaded Citadel 2.1.9");
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

    private static ModelGeometry geometry(ModelGeometry.SourcePose body, ModelGeometry.SourcePose neck) {
        var root = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(2, "test:citadel", "2.1.9", List.of(
            new ModelGeometry.Part("root", null, ModelGeometry.values(root.matrix()), root),
            new ModelGeometry.Part("root/body", "root", ModelGeometry.values(body.matrix()), body),
            new ModelGeometry.Part("root/body/neck", "root/body", ModelGeometry.values(neck.matrix()), neck)
        ), List.of(), ModelGeometry.values(new Matrix4f()));
    }

    private static void assertMatrixNear(GameTestHelper h, Matrix4f expected, Matrix4f actual, String message) {
        h.assertTrue(actual != null, message + " (missing matrix)");
        float[] a = expected.get(new float[16]), b = actual.get(new float[16]);
        for (int i = 0; i < a.length; i++)
            h.assertTrue(Math.abs(a[i] - b[i]) < 2e-5f,
                message + " at matrix index " + i + ": expected=" + a[i] + " actual=" + b[i]);
    }
}
