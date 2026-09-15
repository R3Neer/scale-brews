package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Positive S21 holdout: external root orientation and physical GravityFrame are independent authorities. */
public final class S21AdversarialRootGravityIndependenceTests {
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_root_gravity_independence");

    @GameTest
    public void fixedExternalRootMustIgnoreDifferentPhysicalGravityFrames(GameTestHelper h) {
        var down = h.spawn(EntityTypes.ARMOR_STAND, 2, 2, 2);
        var east = h.spawn(EntityTypes.ARMOR_STAND, 4, 2, 2);
        var calls = new AtomicInteger();
        var expectedQuaternion = new Quaternionf().rotateZ((float)(Math.PI * .5));
        var commonOrigin = new Vec3(12.25, 7.5, -3.75);
        RootTransformProvider roots = entity -> {
            calls.incrementAndGet();
            return Optional.of(new RootTransformProvider.RootTransform(commonOrigin, expectedQuaternion, 1.0f));
        };
        PoseEngine.Bound poses = inputs -> Optional.of(Map.of("root", new Matrix4f()));
        var provider = new ModelGeometryProvider(model(), poses, roots, AnatomyFilter.DEFAULT, 17);
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true);

        try {
            GravityFrames.overrideForTests(down, new GravityFrame(Direction.DOWN));
            GravityFrames.overrideForTests(east, new GravityFrame(Direction.EAST));
            provider.pose(down, inputs);
            provider.pose(east, inputs);
            long tick = down.level().getGameTime();
            provider.tick(down, tick);
            provider.tick(east, tick);

            var a = provider.authoritativeFrame(down).orElseThrow();
            var b = provider.authoritativeFrame(east).orElseThrow();
            h.assertTrue(a.sample().gravity().down() == Direction.DOWN
                    && b.sample().gravity().down() == Direction.EAST,
                "Fixture must actually present different physical gravity frames to the pose/root pipeline");
            h.assertTrue(calls.get() == 2,
                "Each authoritative endpoint must sample the external root provider exactly once");
            assertSameRotation(h, expectedQuaternion, a.root().quaternion(),
                "DOWN physical gravity changed the external root quaternion");
            assertSameRotation(h, expectedQuaternion, b.root().quaternion(),
                "EAST physical gravity changed the external root quaternion");

            var first = provider.sampleAt(down, a.sample(), a.root()).orElseThrow();
            var second = provider.sampleAt(east, b.sample(), b.root()).orElseThrow();
            var firstBox = first.pieces().get("probe");
            var secondBox = second.pieces().get("probe");
            h.assertTrue(firstBox != null && secondBox != null,
                "Gravity-independence fixture must materialize the probe piece");
            h.assertTrue(firstBox.bounds().getCenter().distanceToSqr(secondBox.bounds().getCenter()) < 1e-10,
                "With identical external root authority and local joints, physical GravityFrame must not rotate/translate support geometry");
            h.assertTrue(Math.abs(firstBox.bounds().getXsize() - secondBox.bounds().getXsize()) < 1e-10
                    && Math.abs(firstBox.bounds().getYsize() - secondBox.bounds().getYsize()) < 1e-10
                    && Math.abs(firstBox.bounds().getZsize() - secondBox.bounds().getZsize()) < 1e-10,
                "Physical GravityFrame must not reshape geometry governed by an independent external root quaternion");
        } finally {
            GravityFrames.clearOverrideForTests(down);
            GravityFrames.clearOverrideForTests(east);
            down.discard();
            east.discard();
        }
        h.succeed();
    }

    private static void assertSameRotation(GameTestHelper h, Quaternionf expected, Quaternionf actual, String message) {
        float dot = expected.x * actual.x + expected.y * actual.y + expected.z * actual.z + expected.w * actual.w;
        h.assertTrue(Float.isFinite(dot) && Math.abs(dot) > 0.999999f,
            message + ": expected " + expected + " but got " + actual);
    }

    private static ModelGeometry model() {
        var rest = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(1, MODEL.toString(), "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(rest.matrix()), rest)),
            List.of(new ModelGeometry.Piece("probe", "root",
                List.of(1d, .25d, .1d), List.of(2d, 1.25d, .6d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
