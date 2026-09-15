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

    @GameTest
    public void duplicateInsideAdapterRollsBackWholeSample(GameTestHelper h) {
        var entity = h.spawn(EntityTypes.COW, 2, 2, 2);
        try {
            Identifier adapterId = Identifier.parse("scalebrews_test:s20_adversarial_atomic_internal_duplicate");
            CollisionAdapters.registerPoseChannels(adapterId, (ignored, sink) -> {
                sink.put("citadel.duplicate", 1f);
                sink.put("citadel.valid_prefix", 2f);
                sink.put("citadel.duplicate", 3f);
            });

            var channels = new HashMap<String, Float>();
            channels.put("crouching", 0f);
            var before = new HashMap<>(channels);
            boolean accepted = AuthorityPoseTracker.sampleExternalChannels(adapterId, entity, channels);

            h.assertTrue(!accepted,
                "An adapter must not be allowed to overwrite one of its own authoritative channels");
            h.assertTrue(channels.equals(before),
                "Internal duplicate failure must roll back every staged channel, not just the duplicate key");
        } finally {
            entity.discard();
        }
        h.succeed();
    }

    @GameTest
    public void lateChannelBudgetOverflowRollsBackWholeSample(GameTestHelper h) {
        var entity = h.spawn(EntityTypes.COW, 2, 2, 2);
        try {
            Identifier adapterId = Identifier.parse("scalebrews_test:s20_adversarial_atomic_budget");
            CollisionAdapters.registerPoseChannels(adapterId, (ignored, sink) -> {
                sink.put("citadel.fills_last_slot", 1f);
                sink.put("citadel.over_budget", 2f);
            });

            var channels = new HashMap<String, Float>();
            for (int i = 0; i < 63; i++) channels.put("scale.preexisting." + i, (float)i);
            var before = new HashMap<>(channels);
            boolean accepted = AuthorityPoseTracker.sampleExternalChannels(adapterId, entity, channels);

            h.assertTrue(!accepted,
                "The 65th authoritative channel must fail the external adapter endpoint closed");
            h.assertTrue(channels.equals(before),
                "Late channel-budget overflow must not publish the staged value that filled slot 64");
        } finally {
            entity.discard();
        }
        h.succeed();
    }
}
