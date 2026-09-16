package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Quaternionf;

/** S21 I4 holdouts: accepted root authority must survive server publication and client endpoint reconstruction. */
public final class S21AdversarialRootWireTests {
    private static final Identifier MODEL = Identifier.parse("scalebrews_test:s21_wire_model");
    private static final Identifier POSE = Identifier.parse("scalebrews:static");
    private static final Identifier ROOT = Identifier.parse("scalebrews_test:s21_wire_root");

    @GameTest
    public void customRootQuaternionMustRoundTripThroughPosePublication(GameTestHelper h) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, 2, 2, 2);
        try {
            support.yBodyRot = 0;
            long tick = support.level().getGameTime();
            var gravity = AnatomyMovement.gravity(support);
            var rootFrame = new RootFrame(1, tick, support.position(), support.yBodyRot,
                support.getScale(), gravity);
            var sample = new AnatomyPoseHistory.Sample(new PoseEngine.Inputs(0, 0, 0, 0, 0, true),
                rootFrame.origin(), rootFrame.yaw(), rootFrame.scale(), rootFrame.gravity());
            var customRoot = new RootTransformProvider.RootTransform(rootFrame.origin(),
                new Quaternionf().rotateZ((float)(Math.PI * .5)), rootFrame.scale());
            var endpoint = new GeometryProvider.CausalEndpoint(1, tick, tick, rootFrame, customRoot,
                sample, GeometryProvider.Availability.AVAILABLE);
            var identity = new GeometryProvider.GeometryIdentity(support.level().dimension(), support.getUUID(),
                support.getId(), UUID.randomUUID(), 1, MODEL, POSE, ROOT, 1, 1);

            var payload = AnatomyNetworking.posePayload(new GeometryProvider.PublishedFrame(identity, endpoint));
            var history = new AnatomyFrameHistory();
            h.assertTrue(history.accept(payload), "Fresh causal pose payload must be accepted by frame history");
            var recovered = history.endpoint().orElseThrow();

            assertSameRotation(h, customRoot.quaternion(), recovered.rootTransform().quaternion(),
                "Server -> pose payload -> frame history changed the authoritative root quaternion");
        } finally {
            support.discard();
        }
        h.succeed();
    }

    @GameTest
    public void posePayloadMustCarryRootProviderIdentity(GameTestHelper h) {
        var support = h.spawn(EntityTypes.ARMOR_STAND, 3, 2, 3);
        try {
            long tick = support.level().getGameTime();
            var rootFrame = new RootFrame(1, tick, support.position(), support.yBodyRot,
                support.getScale(), AnatomyMovement.gravity(support));
            var sample = new AnatomyPoseHistory.Sample(new PoseEngine.Inputs(0, 0, 0, 0, 0, true),
                rootFrame.origin(), rootFrame.yaw(), rootFrame.scale(), rootFrame.gravity());
            var customRoot = new RootTransformProvider.RootTransform(rootFrame.origin(), new Quaternionf(), rootFrame.scale());
            var endpoint = new GeometryProvider.CausalEndpoint(1, tick, tick, rootFrame, customRoot,
                sample, GeometryProvider.Availability.AVAILABLE);
            var identity = new GeometryProvider.GeometryIdentity(support.level().dimension(), support.getUUID(),
                support.getId(), UUID.randomUUID(), 1, MODEL, POSE, ROOT, 1, 1);
            var payload = AnatomyNetworking.posePayload(new GeometryProvider.PublishedFrame(identity, endpoint));

            final Object transmitted;
            try {
                Method accessor = AnatomyPosePayload.class.getMethod("rootProvider");
                transmitted = accessor.invoke(payload);
            } catch (ReflectiveOperationException missingRootIdentity) {
                throw new AssertionError("FR-033/I4: pose wire identity has no rootProvider accessor", missingRootIdentity);
            }
            h.assertTrue(ROOT.equals(transmitted),
                "Pose wire identity must carry the canonical root provider id; expected " + ROOT + " but got " + transmitted);
        } finally {
            support.discard();
        }
        h.succeed();
    }

    private static void assertSameRotation(GameTestHelper h, Quaternionf expected, Quaternionf actual, String message) {
        float dot = expected.x * actual.x + expected.y * actual.y + expected.z * actual.z + expected.w * actual.w;
        h.assertTrue(Float.isFinite(dot) && Math.abs(dot) > 0.999999f,
            message + ": expected " + expected + " but got " + actual);
    }
}
