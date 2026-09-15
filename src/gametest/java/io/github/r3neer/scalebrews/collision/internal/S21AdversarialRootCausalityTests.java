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

/** Independent S21 holdouts for the first live root-authority integration tranche. */
public final class S21AdversarialRootCausalityTests {
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_transverse_root");
    private static final Identifier POSE = Identifier.parse("scalebrews:static");
    private static final Identifier ROOT = Identifier.parse("scalebrews_test:transverse_root");

    @GameTest
    public void transverseRootMustSurviveCausalPublication(GameTestHelper h) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, 2, 2, 2);
        var calls = new AtomicInteger();
        RootTransformProvider roots = entity -> {
            calls.incrementAndGet();
            return Optional.of(new RootTransformProvider.RootTransform(
                entity.position(), new Quaternionf().rotateZ((float)(Math.PI * .5)), entity.getScale()));
        };
        PoseEngine.Bound poses = inputs -> Optional.of(Map.of("root", new Matrix4f()));
        var provider = new ModelGeometryProvider(fixtureModel(), poses, roots, AnatomyFilter.DEFAULT, 1);
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true);

        try {
            support.yBodyRot = 0;
            provider.pose(support, inputs);
            long tick = support.level().getGameTime();
            provider.tick(support, tick);

            var authoritative = provider.authoritativeFrame(support).orElseThrow();
            h.assertTrue(calls.get() == 1,
                "Root provider must be sampled exactly once while producing the authoritative endpoint");

            var expected = provider.sampleAt(support, authoritative.sample(), authoritative.root()).orElseThrow();
            var expectedBox = expected.pieces().get("probe");
            h.assertTrue(expectedBox != null, "Transverse-root reference sample must contain the probe piece");

            AnatomyMovement.register(support, provider,
                new GeometryProvider.GeometryIdentityDescriptor(
                    UUID.randomUUID(), 1, MODEL, POSE, ROOT, 1));
            var published = AnatomyMovement.queryFrame(support).orElseThrow();
            var actualBox = published.snapshot().pieces().get("probe");
            h.assertTrue(actualBox != null, "Published causal sample must contain the probe piece");

            var expectedCenter = expectedBox.bounds().getCenter();
            var actualCenter = actualBox.bounds().getCenter();
            h.assertTrue(actualCenter.distanceToSqr(expectedCenter) < 1e-8,
                "Causal publication discarded the accepted RootTransformProvider quaternion: expected center "
                    + expectedCenter + " but published " + actualCenter);
            h.assertTrue(calls.get() == 1,
                "Publishing/querying one causal endpoint must consume the already sampled root instead of resampling its provider");
        } finally {
            support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void rootOnlyOrientationChangeMustReuseJointEvaluation(GameTestHelper h) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, 3, 2, 3);
        PoseEngine.Bound poses = inputs -> Optional.of(Map.of("root", new Matrix4f()));
        var provider = new ModelGeometryProvider(fixtureModel(), poses,
            entity -> Optional.of(new RootTransformProvider.RootTransform(
                entity.position(), new Quaternionf(), entity.getScale())),
            AnatomyFilter.DEFAULT, 1);
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true);

        try {
            var ordinaryRoot = new RootTransformProvider.RootTransform(support.position(), new Quaternionf(), support.getScale());
            var transverseRoot = new RootTransformProvider.RootTransform(
                support.position(), new Quaternionf().rotateZ((float)(Math.PI * .5)), support.getScale());
            var frame = new AnatomyPoseHistory.Sample(inputs, support.position(), support.yBodyRot,
                support.getScale(), AnatomyMovement.gravity(support));

            var first = provider.sampleAt(support, frame, ordinaryRoot).orElseThrow();
            long jointsAfterFirst = provider.jointEvaluations();
            var second = provider.sampleAt(support, frame, transverseRoot).orElseThrow();

            h.assertTrue(provider.jointEvaluations() == jointsAfterFirst,
                "Changing only root orientation must not reevaluate local joints (NFR-009)");
            h.assertTrue(first.pieces().get("probe").bounds().getCenter()
                    .distanceToSqr(second.pieces().get("probe").bounds().getCenter()) > 1e-4,
                "The holdout fixture must actually distinguish ordinary and transverse root orientations");
        } finally {
            support.discard();
        }
        h.succeed();
    }

    private static ModelGeometry fixtureModel() {
        return new ModelGeometry(1, MODEL.toString(), "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("probe", "root",
                List.of(2d, .25d, .1d), List.of(.5d, 1.25d, .3d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
