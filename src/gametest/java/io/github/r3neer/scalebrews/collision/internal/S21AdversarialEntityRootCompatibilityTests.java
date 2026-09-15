package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.joml.Matrix4f;

/** I8 regression: scalebrews:entity_root must be exactly compatible with the pre-S21 root composition. */
public final class S21AdversarialEntityRootCompatibilityTests {
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_entity_root_compatibility");
    private static final double[] YAWS = {-179.0, -42.5, 0.0, 137.25, 180.0};
    private static final double[] SCALES = {0.5, 1.0, 2.0};

    @GameTest
    public void entityRootMatchesLegacyCompositionAcrossGravityYawAndScale(GameTestHelper h) {
        var entity = h.spawn(EntityTypes.COW, 3, 2, 3);
        PoseEngine.Bound poses = inputs -> Optional.of(Map.of("root", new Matrix4f()));
        var provider = new ModelGeometryProvider(model(), poses,
            BuiltInRootTransformProviders.entityRoot(), AnatomyFilter.DEFAULT, 21);
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true);
        try {
            long expectedJointEvaluations = -1;
            for (Direction gravity : Direction.values()) {
                GravityFrames.overrideForTests(entity, new GravityFrame(gravity));
                for (double yaw : YAWS) {
                    entity.yBodyRot = (float)yaw;
                    for (double scale : SCALES) {
                        var scaleAttribute = entity.getAttribute(Attributes.SCALE);
                        h.assertTrue(scaleAttribute != null, "Compatibility fixture requires the vanilla SCALE attribute");
                        scaleAttribute.setBaseValue(scale);
                        entity.refreshDimensions();

                        var frame = new AnatomyPoseHistory.Sample(inputs, entity.position(), entity.yBodyRot,
                            entity.getScale(), AnatomyMovement.gravity(entity));
                        var legacy = provider.sampleAt(entity, frame).orElseThrow();
                        var builtInRoot = BuiltInRootTransformProviders.entityRoot().sample(entity).orElseThrow();
                        var current = provider.sampleAt(entity, frame, builtInRoot).orElseThrow();

                        assertSameBox(h, legacy.pieces().get("probe"), current.pieces().get("probe"),
                            "entity_root diverged from legacy composition for gravity=" + gravity
                                + ", yaw=" + yaw + ", scale=" + scale);
                        if (expectedJointEvaluations < 0) expectedJointEvaluations = provider.jointEvaluations();
                        else h.assertTrue(provider.jointEvaluations() == expectedJointEvaluations,
                            "Root-only gravity/yaw/scale compatibility sweep must not reevaluate local joints (NFR-009)");
                    }
                }
            }
        } finally {
            GravityFrames.clearOverrideForTests(entity);
            entity.discard();
        }
        h.succeed();
    }

    private static void assertSameBox(GameTestHelper h, ConvexBox expected, ConvexBox actual, String message) {
        h.assertTrue(expected != null && actual != null, message + ": missing probe piece");
        h.assertTrue(expected.vertices().size() == actual.vertices().size(), message + ": vertex count changed");
        for (int i = 0; i < expected.vertices().size(); i++) {
            h.assertTrue(expected.vertices().get(i).distanceToSqr(actual.vertices().get(i)) < 1e-10,
                message + ": vertex " + i + " expected " + expected.vertices().get(i)
                    + " but got " + actual.vertices().get(i));
        }
    }

    private static ModelGeometry model() {
        var rest = new ModelGeometry.SourcePose(0.35f, -0.2f, 0.15f,
            0.17f, -0.31f, 0.23f, 1.0f, 1.0f, 1.0f);
        return new ModelGeometry(1, MODEL.toString(), "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(rest.matrix()), rest)),
            List.of(new ModelGeometry.Piece("probe", "root",
                List.of(-0.75d, 0.2d, 1.1d), List.of(1.8d, 2.4d, 3.7d), null)),
            ModelGeometry.values(new Matrix4f().translate(0.13f, -0.08f, 0.27f).rotateX(0.11f)));
    }
}
