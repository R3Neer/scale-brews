package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import org.joml.Matrix4f;

/** S18 must add revision-aware preparation without breaking the public PoseEngine SAM used by external mods/lambdas. */
public final class S18PoseEngineSpiCompatibilityTests {
    @GameTest
    public void poseEngineRemainsAFunctionalInterface(GameTestHelper h) {
        long abstractInstanceMethods = Arrays.stream(PoseEngine.class.getDeclaredMethods())
            .filter(method -> Modifier.isAbstract(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()))
            .count();
        h.assertTrue(abstractInstanceMethods == 1,
            "PoseEngine must retain exactly one abstract instance method; revision-aware preparation belongs in default methods or neutral helpers");
        h.assertTrue(PoseEngine.class.isAnnotationPresent(FunctionalInterface.class),
            "PoseEngine should keep its explicit @FunctionalInterface contract for external engine authors");
        h.succeed();
    }

    @GameTest
    public void existingLambdaEngineStillEvaluatesThroughThePublicSam(GameTestHelper h) {
        PoseEngine external = (geometry, inputs, parameters) -> {
            if (!parameters.isEmpty() || !inputs.ordinary()) return Optional.empty();
            return Optional.of(Map.of());
        };
        var geometry = minimalGeometry();
        var supported = external.evaluate(geometry, new PoseEngine.Inputs(0, 0, 0, 0, 0, true), Map.of());
        var unsupported = external.evaluate(geometry, new PoseEngine.Inputs(0, 0, 0, 0, 0, false), Map.of());
        h.assertTrue(supported.isPresent() && unsupported.isEmpty(),
            "A pre-S18 lambda PoseEngine must remain source-compatible and executable after the new preparation seam is added");
        h.succeed();
    }

    private static ModelGeometry minimalGeometry() {
        var identity = ModelGeometry.values(new Matrix4f());
        return new ModelGeometry(1, "proof:external_pose_engine", "1",
            List.of(new ModelGeometry.Part("root", null, identity)),
            List.of(new ModelGeometry.Piece("body", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            identity);
    }
}
