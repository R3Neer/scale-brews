package io.github.r3neer.scalebrews.collision.internal;

import java.util.ArrayList;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Implementer stress for the bounded recipient tracking-generation authority. */
public final class S24TrackingGenerationLedgerTests {
    @GameTest
    public void sequentialTrackingWindowsRemainConstantMemoryAndNeverReuseGeneration(GameTestHelper h) {
        var ledger=new TrackingGenerationLedger();
        var first=new UUID(1,1);long firstGeneration=ledger.acquire(first);
        h.assertTrue(firstGeneration>0 && ledger.activeEntries()==1,"First tracking window must acquire one active generation");
        ledger.release(first);
        h.assertTrue(ledger.activeEntries()==0,"STOP must remove the retired UUID instead of retaining a tombstone");

        long previous=firstGeneration;
        for(int i=2;i<=50_000;i++) {
            var id=new UUID(1,i);long generation=ledger.acquire(id);
            h.assertTrue(generation>previous,"Connection-local generations must be strictly monotonic across distinct retired UUIDs");
            h.assertTrue(ledger.activeEntries()==1,"Sequential world traversal must retain only the currently active tracking window");
            ledger.release(id);previous=generation;
            h.assertTrue(ledger.activeEntries()==0,"Released tracking windows must not accumulate with session age");
        }

        long reacquired=ledger.acquire(first);
        h.assertTrue(reacquired>previous,
            "Reacquiring a UUID after its map entry was freed must still receive a generation newer than every retired window");
        h.assertTrue(reacquired!=firstGeneration,"A pruned UUID may never recover its original tracking authority");
        h.succeed();
    }

    @GameTest
    public void activeTrackingCapFailsClosedWithoutEvictingAuthoritativeWindows(GameTestHelper h) {
        var ledger=new TrackingGenerationLedger();var ids=new ArrayList<UUID>();long last=0;
        for(int i=0;i<TrackingGenerationLedger.MAX_ACTIVE;i++) {
            var id=new UUID(2,i);ids.add(id);long generation=ledger.acquire(id);
            h.assertTrue(generation>last,"Each active tracking window must receive a unique monotonic generation");
            last=generation;
        }
        h.assertTrue(ledger.activeEntries()==TrackingGenerationLedger.MAX_ACTIVE,
            "Ledger must expose the documented constant active-window cap");
        var first=ids.getFirst();long firstGeneration=ledger.current(first);
        var overflow=new UUID(3,1);
        h.assertTrue(ledger.acquire(overflow)==TrackingGenerationLedger.UNAVAILABLE,
            "The first pair beyond the active cap must fail closed instead of evicting another authoritative window");
        h.assertTrue(ledger.activeEntries()==TrackingGenerationLedger.MAX_ACTIVE && ledger.current(first)==firstGeneration,
            "Saturation must not mutate or evict an existing tracking authority");

        ledger.release(first);
        long admitted=ledger.acquire(overflow);
        h.assertTrue(admitted>last && ledger.activeEntries()==TrackingGenerationLedger.MAX_ACTIVE,
            "After an explicit STOP frees capacity, the waiting UUID may enter only with a fresh post-saturation generation");
        h.assertTrue(ledger.acquire(overflow)==admitted,
            "Repeated publication inside one active tracking window must reuse its generation rather than advance the counter");
        h.succeed();
    }
}
