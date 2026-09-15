package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import java.util.HashMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;

/**
 * Adversarial S20 holdout for FR-029 fail-closed semantics at the external authoritative
 * pose-channel boundary. A failing adapter must not leave a prefix of its output published.
 */
public final class S20AdversarialPoseChannelAtomicityTests {
    @GameTest
    public void failingExternalAdapterPublishesNoPartialTruth(GameTestHelper h) {
        var entity = h.spawn(EntityTypes.COW, 2, 2, 2);
        try {
            Identifier adapterId = Identifier.parse("scalebrews_test:s20_adversarial_atomic_invalid");
            CollisionAdapters.registerPoseChannels(adapterId, (ignored, sink) -> {
                sink.put("citadel.valid_prefix", 7.25f);
                sink.put("citadel.invalid_tail", Float.NaN);
            });

            var channels = new HashMap<String, Float>();
            channels.put("crouching", 0f);
            boolean accepted = AuthorityPoseTracker.sampleExternalChannels(adapterId, entity, channels);

            h.assertTrue(!accepted,
                "Adapter with a non-finite tail must fail the authoritative endpoint closed");
            h.assertTrue(!channels.containsKey("citadel.valid_prefix"),
                "A failed external adapter must roll back values emitted before the failure");
            h.assertTrue(channels.size() == 1 && Float.compare(channels.get("crouching"), 0f) == 0,
                "A failed external adapter must preserve the pre-existing authoritative channel set exactly");
        } finally {
            entity.discard();
        }
        h.succeed();
    }

    @GameTest
    public void lateOwnershipConflictAlsoRollsBackEarlierOutput(GameTestHelper h) {
        var entity = h.spawn(EntityTypes.COW, 2, 2, 2);
        try {
            Identifier adapterId = Identifier.parse("scalebrews_test:s20_adversarial_atomic_duplicate");
            CollisionAdapters.registerPoseChannels(adapterId, (ignored, sink) -> {
                sink.put("citadel.valid_prefix", 3f);
                sink.put("crouching", 1f);
            });

            var channels = new HashMap<String, Float>();
            channels.put("crouching", 0f);
            boolean accepted = AuthorityPoseTracker.sampleExternalChannels(adapterId, entity, channels);

            h.assertTrue(!accepted,
                "Late ownership conflict must fail the external authoritative endpoint closed");
            h.assertTrue(!channels.containsKey("citadel.valid_prefix"),
                "Ownership failure after valid output must not leak a partially sampled adapter state");
            h.assertTrue(channels.size() == 1 && Float.compare(channels.get("crouching"), 0f) == 0,
                "Ownership failure must leave Scale-owned authoritative channels untouched");
        } finally {
            entity.discard();
        }
        h.succeed();
    }
}
