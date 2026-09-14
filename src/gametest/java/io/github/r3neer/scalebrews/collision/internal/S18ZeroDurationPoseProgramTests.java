package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import io.github.r3neer.scalebrews.collision.pose.PoseProgramEvaluator;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import org.joml.Matrix4f;

/** Common-side regression for Mojang's legitimate duration-zero static animation definitions. */
public final class S18ZeroDurationPoseProgramTests {
    @GameTest
    public void zeroDurationStaticProgramEvaluatesTimestampZeroTargets(GameTestHelper h) {
        var geometry = geometry();
        var program = program(false);
        var matrices = PoseProgramEvaluator.evaluate(geometry, program, 123.0f, 1.0f).orElseThrow();
        var head = matrices.get("root/head");
        h.assertTrue(head != null, "Zero-duration static program must evaluate its timestamp-zero track");
        h.assertTrue(Math.abs(head.m31() - 1f / 16f) < 1e-6f && Math.abs(head.m32() - 1f / 16f) < 1e-6f,
            "Static timestamp-zero position target must be applied in ModelPart pixel units");
        h.assertTrue(Math.abs(head.m00() - 1.2f) < 1e-6f && Math.abs(head.m11() - 1.2f) < 1e-6f && Math.abs(head.m22() - 1.2f) < 1e-6f,
            "Static timestamp-zero scale delta must be applied exactly");
        h.succeed();
    }

    @GameTest
    public void zeroDurationLoopingProgramStillEvaluatesItsOnlyStaticKeyframe(GameTestHelper h) {
        var matrices = PoseProgramEvaluator.evaluate(geometry(), program(true), 42.0f, 1.0f).orElseThrow();
        h.assertTrue(matrices.containsKey("root/head"),
            "Mojang permits zero-duration looping definitions; a single timestamp-zero keyframe must remain evaluable");
        h.succeed();
    }

    private static ModelGeometry geometry() {
        var identity = ModelGeometry.values(new Matrix4f());
        return new ModelGeometry(2, "minecraft:sniffer", "26.2",
            List.of(
                new ModelGeometry.Part("root", null, identity, ModelGeometry.SourcePose.identity()),
                new ModelGeometry.Part("root/head", "root", identity, ModelGeometry.SourcePose.identity())
            ),
            List.of(new ModelGeometry.Piece("head_piece", "root/head", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            identity);
    }

    private static PoseProgram program(boolean loop) {
        var linear = PoseProgram.Interpolation.LINEAR;
        var zero = new PoseProgram.Vector(0, 0, 0);
        return new PoseProgram(PoseProgram.SCHEMA_VERSION, "minecraft:sniffer", "26.2", 0.0f, loop, List.of(
            new PoseProgram.Track("head", PoseProgram.Target.TRANSLATION, List.of(
                new PoseProgram.Keyframe(0.0f, new PoseProgram.Vector(0, 1, 1), new PoseProgram.Vector(0, 1, 1), linear))),
            new PoseProgram.Track("head", PoseProgram.Target.SCALE, List.of(
                new PoseProgram.Keyframe(0.0f, new PoseProgram.Vector(.2f, .2f, .2f), new PoseProgram.Vector(.2f, .2f, .2f), linear)))
        ));
    }
}
