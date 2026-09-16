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

/** S21 discontinuity holdout: a real teleport must cut external-root material continuity, then recover. */
public final class S21AdversarialExternalRootTeleportTests {
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_external_root_teleport");
    private static final Identifier POSE = Identifier.parse("scalebrews:static");
    private static final Identifier ROOT = Identifier.parse("scalebrews_test:s21_external_root_teleport_provider");

    @GameTest(maxTicks = 80)
    public void teleportMustCutExternalRootIntervalAndAllowNextContinuousInterval(GameTestHelper h) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, 3, 2, 3);
        support.setNoGravity(true);
        var mode = new AtomicInteger();
        var rootCalls = new AtomicInteger();
        RootTransformProvider roots = entity -> {
            rootCalls.incrementAndGet();
            Quaternionf q = switch (mode.get()) {
                case 0 -> new Quaternionf();
                case 1 -> new Quaternionf().rotateZ((float)(Math.PI * .5));
                default -> new Quaternionf().rotateX((float)(Math.PI * .5));
            };
            return Optional.of(new RootTransformProvider.RootTransform(entity.position(), q, entity.getScale()));
        };
        PoseEngine.Bound poses = inputs -> Optional.of(Map.of("root", new Matrix4f()));
        var provider = new ModelGeometryProvider(model(), poses, roots, AnatomyFilter.DEFAULT, 41);
        var inputs = new PoseEngine.Inputs(0, 0, 0, 0, 0, true);
        var level = support.level();

        try {
            AnatomyMovement.activate(level);
            provider.pose(support, inputs);
            long tick = level.getGameTime();
            provider.tick(support, tick);
            AnatomyMovement.register(support, provider,
                new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(), 41, MODEL, POSE, ROOT, 1));

            var before = AnatomyMovement.queryFrame(support).orElseThrow();
            long jointsBefore = provider.jointEvaluations();
            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "Seeding the external-root tracker must not fabricate an interval");

            // Keep the jump deliberately below the automatic >4-block RootHistory threshold.
            // The real Entity.teleportTo mixin must therefore provide the discontinuity fence.
            mode.set(1);
            support.teleportTo(support.getX() + .5, support.getY(), support.getZ());

            var teleported = AnatomyMovement.queryFrame(support).orElseThrow(
                () -> new AssertionError("S21 teleport must publish a fresh external-root endpoint"));
            h.assertTrue(teleported.endpoint().frameSerial() > before.endpoint().frameSerial(),
                "Teleport must advance the material endpoint serial");
            h.assertTrue(teleported.rootTransform().origin().distanceToSqr(before.rootTransform().origin()) > .2,
                "Teleport fixture must materially move the root origin");
            assertSameRotation(h, new Quaternionf().rotateZ((float)(Math.PI * .5)),
                teleported.rootTransform().quaternion(),
                "Teleport endpoint must carry the post-teleport external root quaternion");
            h.assertTrue(provider.jointEvaluations() == jointsBefore,
                "Teleport/root-only discontinuity must not reevaluate local joints");

            MaterialIntervalRuntime.observe(support);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                "A teleport discontinuity must never publish a material interval from the pre-teleport external root");

            // The cut is a barrier, not a permanent tracker death. A subsequent contiguous
            // external-root change in the same binding must again become certifiable.
            mode.set(2);
            AnatomyMovement.spatialMutation(support);
            var continuous = AnatomyMovement.queryFrame(support).orElseThrow(
                () -> new AssertionError("S21 tracker recovery requires a post-teleport root-only endpoint"));
            h.assertTrue(continuous.endpoint().frameSerial() == teleported.endpoint().frameSerial() + 1,
                "Post-teleport root-only change must be the next contiguous material serial");
            assertSameRotation(h, new Quaternionf().rotateX((float)(Math.PI * .5)),
                continuous.rootTransform().quaternion(),
                "Recovered endpoint must use the new external root quaternion");
            h.assertTrue(provider.jointEvaluations() == jointsBefore,
                "Post-teleport root-only recovery must still reuse local joints");

            MaterialIntervalRuntime.observe(support);
            var pending = MaterialIntervalRuntime.poll(level);
            h.assertTrue(pending.size() == 1,
                "The first continuous root change after teleport must publish exactly one interval");
            var interval = pending.getFirst();
            h.assertTrue(interval.support() == support,
                "Recovered interval must belong to the same live support");
            h.assertTrue(interval.handle().before().equals(teleported)
                    && interval.handle().after().equals(continuous),
                "Recovered interval must start at the post-teleport seed and end at the next external-root endpoint");
            h.assertTrue(interval.handle().identity().rootProvider().equals(ROOT),
                "Recovered interval must retain the external root-provider identity");

            h.assertTrue(rootCalls.get() >= 3,
                "Fixture must actually sample initial, teleport and recovery root authority");
        } finally {
            MaterialIntervalRuntime.clear(level);
            support.discard();
            AnatomyMovement.deactivate(level);
        }
        h.succeed();
    }

    private static void assertSameRotation(GameTestHelper h, Quaternionf expected, Quaternionf actual, String message) {
        var left = new Quaternionf(expected).normalize();
        var right = new Quaternionf(actual).normalize();
        h.assertTrue(Math.abs(left.dot(right)) > 0.999999f,
            message + ": expected " + left + " but got " + right);
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
