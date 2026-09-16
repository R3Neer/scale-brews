package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Adversarial G3.9 holdouts: lifecycle RESTART is receiver control flow, never an
 * admissible mutation of the currently accepted causal frame history.
 */
public final class S24AdversarialHistoryRestartTests {
    @GameTest
    public void trackingRestartCannotBeFoldedOrSoftRejectedInsideOldHistory(GameTestHelper h) {
        var epoch = UUID.randomUUID();
        var entity = UUID.randomUUID();
        var dimension = h.getLevel().dimension().identifier();
        var model = Identifier.parse("minecraft:cow");
        var provider = Identifier.parse("scalebrews:static");
        var inputs = new PoseProvider.Inputs(0, 0, 10, 0, 0, true, Map.of());

        var first = frame(epoch, dimension, 7, entity, model, provider, 1, 10, 5, 7, inputs);
        var retrack = frame(epoch, dimension, 7, entity, model, provider, 2, 11, 5, 8, inputs);
        var history = new AnatomyFrameHistory();
        h.assertTrue(history.accept(first), "Fixture identity must install before the adversarial restart");
        h.assertTrue(history.transition(retrack) == AnatomyFrameHistory.LifecycleTransition.RESTART,
            "A newer recipient tracking generation must be classified as receiver-level RESTART");

        boolean hardRejected = false;
        try {
            history.accept(retrack);
        } catch (IllegalArgumentException expected) {
            hardRejected = true;
        }
        h.assertTrue(hardRejected,
            "A RESTART packet must hard-reject if offered to the old frame history; return-false is too weak because it hides an identity fork");
        h.assertTrue(history.current() == first,
            "Rejected tracking restart must leave the old history byte-for-byte on its prior accepted payload");

        var fresh = new AnatomyFrameHistory();
        h.assertTrue(fresh.accept(retrack) && fresh.current() == retrack,
            "The same packet must become admissible only after the receiver installs a fresh history");
        h.assertTrue(fresh.transition(first) == AnatomyFrameHistory.LifecycleTransition.REJECT,
            "Once the new tracking generation is installed, the previous generation cannot re-enter through ordering alone");
        h.succeed();
    }

    @GameTest
    public void bindingRestartCannotBeSilentlyAbsorbedByOldHistory(GameTestHelper h) {
        var epoch = UUID.randomUUID();
        var entity = UUID.randomUUID();
        var dimension = h.getLevel().dimension().identifier();
        var model = Identifier.parse("minecraft:cow");
        var provider = Identifier.parse("scalebrews:static");
        var inputs = new PoseProvider.Inputs(0, 0, 10, 0, 0, true, Map.of());

        var first = frame(epoch, dimension, 7, entity, model, provider, 1, 10, 5, 7, inputs);
        var rebind = frame(epoch, dimension, 7, entity, model, provider, 2, 11, 6, 7, inputs);
        var history = new AnatomyFrameHistory();
        h.assertTrue(history.accept(first), "Fixture binding generation must install");
        h.assertTrue(history.transition(rebind) == AnatomyFrameHistory.LifecycleTransition.RESTART,
            "A strictly newer binding generation must request a fresh receiver history");

        boolean hardRejected = false;
        try {
            history.accept(rebind);
        } catch (IllegalArgumentException expected) {
            hardRejected = true;
        }
        h.assertTrue(hardRejected,
            "Binding-generation RESTART must not be folded into the current causal history");
        h.assertTrue(history.current() == first,
            "A failed direct rebind must not advance serials or replace the old accepted identity");
        h.succeed();
    }

    private static AnatomyPosePayload frame(UUID epoch, Identifier dimension, int entityId, UUID entity,
                                             Identifier model, Identifier provider, long serial, long tick,
                                             long bindingGeneration, long trackingGeneration, PoseProvider.Inputs inputs) {
        return new AnatomyPosePayload(epoch, 3, dimension, entityId, entity, model, provider,
            serial, tick, tick, 4, tick, bindingGeneration, trackingGeneration, true,
            inputs, Vec3.ZERO, 0, 1, net.minecraft.core.Direction.DOWN);
    }
}
