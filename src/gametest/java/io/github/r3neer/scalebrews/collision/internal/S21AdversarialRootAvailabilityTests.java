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

/** S21 fail-closed holdouts for root authority loss after an accepted endpoint. */
public final class S21AdversarialRootAvailabilityTests {
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_root_availability");
    private static final Identifier POSE = Identifier.parse("scalebrews:static");
    private static final Identifier ROOT = Identifier.parse("scalebrews_test:s21_root_availability_provider");

    @GameTest
    public void emptyRootAfterValidSampleMustPublishUnavailableWithoutStaleFallback(GameTestHelper h) {
        var mode = new AtomicInteger(0);
        RootTransformProvider roots = entity -> mode.get() == 0
            ? Optional.of(new RootTransformProvider.RootTransform(entity.position(), new Quaternionf(), entity.getScale()))
            : Optional.empty();
        exerciseLoss(h, roots, mode, false);
    }

    @GameTest
    public void throwingRootAfterValidSampleMustPublishUnavailableWithoutStaleFallback(GameTestHelper h) {
        var mode = new AtomicInteger(0);
        RootTransformProvider roots = entity -> {
            if (mode.get() != 0) throw new IllegalStateException("adversarial root provider failure");
            return Optional.of(new RootTransformProvider.RootTransform(entity.position(), new Quaternionf(), entity.getScale()));
        };
        exerciseLoss(h, roots, mode, true);
    }

    private static void exerciseLoss(GameTestHelper h, RootTransformProvider roots, AtomicInteger mode, boolean throwing) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, throwing ? 4 : 2, 2, 2);
        PoseEngine.Bound poses = inputs -> Optional.of(Map.of("root", new Matrix4f()));
        var provider = new ModelGeometryProvider(model(), poses, roots, AnatomyFilter.DEFAULT, 11);
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true);
        try {
            provider.pose(support, inputs);
            long tick = support.level().getGameTime();
            provider.tick(support, tick);
            AnatomyMovement.register(support, provider,
                new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(), 11, MODEL, POSE, ROOT, 1));

            h.assertTrue(provider.authoritativeFrame(support).isPresent(),
                "Fixture must establish one valid authoritative root before loss");
            var before = AnatomyMovement.publishedFrame(support).orElseThrow();
            h.assertTrue(before.endpoint().availability() == GeometryProvider.Availability.AVAILABLE,
                "Fixture must publish one available causal endpoint before root loss");
            h.assertTrue(AnatomyMovement.queryFrame(support).isPresent(),
                "Fixture must expose available query geometry before root loss");

            mode.set(1);
            provider.tick(support, tick + 1);

            h.assertTrue(provider.authoritativeFrame(support).isEmpty(),
                "Root loss must remove the provider's authoritative frame instead of retaining stale authority");
            h.assertTrue(provider.sample(support).isEmpty(),
                "Root loss must make direct geometry sampling unavailable");
            h.assertTrue(AnatomyMovement.queryFrame(support).isEmpty(),
                "Root loss must make causal query geometry unavailable instead of reusing the previously accepted endpoint");

            var after = AnatomyMovement.publishedFrame(support).orElseThrow(
                () -> new AssertionError("FR-029/S21: root loss must publish an UNAVAILABLE endpoint so clients can clear stale geometry"));
            h.assertTrue(after.endpoint().availability() == GeometryProvider.Availability.UNAVAILABLE,
                "Root loss must publish endpoint UNAVAILABLE, not another AVAILABLE collider");
            h.assertTrue(after.endpoint().frameSerial() > before.endpoint().frameSerial(),
                "Root-loss invalidation must advance causal identity rather than replay the previous endpoint serial");
        } finally {
            support.discard();
        }
        h.succeed();
    }

    private static ModelGeometry model() {
        var rest = new ModelGeometry.SourcePose(0, 0, 0, 0, 0, 0, 1, 1, 1);
        return new ModelGeometry(1, MODEL.toString(), "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(rest.matrix()), rest)),
            List.of(new ModelGeometry.Piece("probe", "root",
                List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
