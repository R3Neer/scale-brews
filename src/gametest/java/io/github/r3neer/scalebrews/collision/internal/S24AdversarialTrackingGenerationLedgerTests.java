package io.github.r3neer.scalebrews.collision.internal;

import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Adversarial NFR-011 / FR-080 proof for the bounded recipient tracking-generation kernel. */
public final class S24AdversarialTrackingGenerationLedgerTests {
    @GameTest
    public void releasedWindowsNeverReuseConnectionLocalGenerations(GameTestHelper h) {
        var ledger = new TrackingGenerationLedger();
        var first = new UUID(0L, 1L);
        var second = new UUID(0L, 2L);

        long firstGeneration = ledger.acquire(first);
        h.assertTrue(firstGeneration == 1L && ledger.acquire(first) == firstGeneration,
            "An active tracking window must keep one stable generation");

        ledger.release(first);
        h.assertTrue(ledger.current(first) == TrackingGenerationLedger.UNAVAILABLE && ledger.activeEntries() == 0,
            "Releasing a support must free its active UUID entry");

        long secondGeneration = ledger.acquire(second);
        ledger.release(second);
        long reacquiredFirst = ledger.acquire(first);
        h.assertTrue(secondGeneration > firstGeneration && reacquiredFirst > secondGeneration,
            "A released UUID may leave the bounded map, but its generation must never be reused within the connection lifetime");
        h.succeed();
    }

    @GameTest
    public void activeWindowCapFailsClosedAndRecoversAfterRelease(GameTestHelper h) {
        var ledger = new TrackingGenerationLedger();
        UUID first = null;
        long last = 0;
        for (int i = 0; i < TrackingGenerationLedger.MAX_ACTIVE; i++) {
            var id = new UUID(1L, i + 1L);
            if (i == 0) first = id;
            long generation = ledger.acquire(id);
            h.assertTrue(generation != TrackingGenerationLedger.UNAVAILABLE && generation > last,
                "Every admitted active window must receive a fresh monotonic generation");
            last = generation;
        }
        h.assertTrue(ledger.activeEntries() == TrackingGenerationLedger.MAX_ACTIVE,
            "Fixture must exactly fill the documented active-window cap");

        var overflow = new UUID(2L, 1L);
        h.assertTrue(ledger.acquire(overflow) == TrackingGenerationLedger.UNAVAILABLE,
            "MAX_ACTIVE+1 must fail closed instead of growing recipient memory");
        h.assertTrue(ledger.activeEntries() == TrackingGenerationLedger.MAX_ACTIVE,
            "Rejected overflow must not mutate active recipient state");

        ledger.release(first);
        long recovered = ledger.acquire(overflow);
        h.assertTrue(recovered != TrackingGenerationLedger.UNAVAILABLE && recovered > last,
            "Freeing one active window must restore capacity without recycling any previous generation");
        h.assertTrue(ledger.activeEntries() == TrackingGenerationLedger.MAX_ACTIVE,
            "Recovered admission must remain exactly at the documented cap");
        h.succeed();
    }
}
