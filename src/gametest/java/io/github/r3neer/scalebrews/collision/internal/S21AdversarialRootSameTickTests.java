package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** FR-030 holdout: a root-only external mutation may advance material identity within one game tick. */
public final class S21AdversarialRootSameTickTests {
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_same_tick_root");
    private static final Identifier POSE = Identifier.parse("scalebrews:static");
    private static final Identifier ROOT = Identifier.parse("scalebrews_test:s21_same_tick_root_provider");

    @GameTest
    public void sameTickExternalRootMutationMustAdvanceSerialWithoutReevaluatingJoints(GameTestHelper h) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, 3, 2, 3);
        var mode = new AtomicInteger();
        var rootCalls = new AtomicInteger();
        RootTransformProvider roots = entity -> {
            rootCalls.incrementAndGet();
            var compatible = BuiltInRootTransformProviders.entityRoot().sample(entity).orElseThrow();
            Quaternionf q = new Quaternionf(compatible.quaternion());
            if (mode.get() != 0) q.rotateZ((float)(Math.PI * .5));
            return Optional.of(new RootTransformProvider.RootTransform(entity.position(), q, entity.getScale()));
        };
        PoseEngine.Bound poses = inputs -> Optional.of(Map.of("root", new Matrix4f()));
        var provider = new ModelGeometryProvider(model(), poses, roots, AnatomyFilter.DEFAULT, 29);
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true);
        var level = support.level();

        try {
            AnatomyMovement.activate(level);
            support.yBodyRot = 0;
            provider.pose(support, inputs);
            long tick = level.getGameTime();
            provider.tick(support, tick);
            AnatomyMovement.register(support, provider,
                new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(), 29, MODEL, POSE, ROOT, 1));

            var before = AnatomyMovement.queryFrame(support).orElseThrow();
            long jointsBefore = provider.jointEvaluations();
            int callsBefore = rootCalls.get();
            var centerBefore = before.snapshot().pieces().get("probe").bounds().getCenter();

            mode.set(1);
            AnatomyMovement.spatialMutation(support);

            var after = AnatomyMovement.queryFrame(support).orElseThrow();
            var centerAfter = after.snapshot().pieces().get("probe").bounds().getCenter();

            h.assertTrue(level.getGameTime() == tick,
                "Fixture must exercise a root mutation without advancing the game tick");
            h.assertTrue(after.endpoint().frameSerial() > before.endpoint().frameSerial(),
                "FR-030: root-only same-tick mutation must advance causal/material frame identity");
            h.assertTrue(after.endpoint().authorityTick() == before.endpoint().authorityTick(),
                "Same-tick root mutation must not fabricate a later authority tick");
            h.assertTrue(after.endpoint().jointSampleTick() == before.endpoint().jointSampleTick(),
                "Root-only mutation must reuse the already accepted joint sample");
            h.assertTrue(provider.jointEvaluations() == jointsBefore,
                "NFR-009: root-only same-tick mutation must not reevaluate local joints");
            h.assertTrue(centerBefore.distanceToSqr(centerAfter) > 1e-4,
                "Root-only quaternion mutation must actually change the asymmetric collider");

            var expected = roots.sample(support).orElseThrow();
            assertSameRotation(h, expected.quaternion(), after.rootTransform().quaternion(),
                "Published same-tick endpoint did not capture the mutated external root quaternion");
            h.assertTrue(rootCalls.get() == callsBefore + 2,
                "The mutation hook plus this explicit oracle sample should add exactly two provider reads; repeated causal query must reuse the accepted endpoint");
        } finally {
            support.discard();
            AnatomyMovement.deactivate(level);
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
                List.of(2d, .25d, .1d), List.of(2.5d, 1.5d, .4d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
