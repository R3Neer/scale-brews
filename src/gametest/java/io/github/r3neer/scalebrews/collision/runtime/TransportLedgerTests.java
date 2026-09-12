package io.github.r3neer.scalebrews.collision.runtime;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Direct S10 holdouts for the extracted passive-transport identity ledger. */
public final class TransportLedgerTests {
    @GameTest
    public void contiguousWindowSumsEveryAppliedContributionExactlyOnce(GameTestHelper h) {
        Entity body=body(h);long tick=h.getLevel().getGameTime();
        TransportLedger.record(body,transport(tick,1,.10,.10));
        TransportLedger.record(body,transport(tick,2,.30,.20));

        var all=TransportLedger.since(body,0);var tail=TransportLedger.since(body,1);
        h.assertTrue(all.contiguous() && all.latestSequence()==2 && close(all.appliedDelta(),new Vec3(.30,0,0)),
            "A contiguous cursor must expose the exact sum of every unconsumed applied contribution: "+all);
        h.assertTrue(tail.contiguous() && tail.latestSequence()==2 && close(tail.appliedDelta(),new Vec3(.20,0,0)),
            "Advancing the cursor must consume only the remaining contribution: "+tail);
        h.assertTrue(TransportLedger.current(body).sequence()==2,"Current transport must be the latest recorded sequence");
        TransportLedger.invalidate(body,true);body.discard();h.succeed();
    }

    @GameTest
    public void gapsAndRewindsFailClosedWithoutPartialDelta(GameTestHelper h) {
        Entity body=body(h);long tick=h.getLevel().getGameTime();
        TransportLedger.record(body,transport(tick,1,.10,.10));
        TransportLedger.record(body,transport(tick,3,.30,.20));
        var gap=TransportLedger.since(body,0);
        h.assertTrue(!gap.contiguous() && gap.latestSequence()==3 && gap.appliedDelta().lengthSqr()==0,
            "A missing sequence must fail closed without publishing a partial transport delta: "+gap);

        TransportLedger.record(body,transport(tick,2,.05,.05));
        var rewind=TransportLedger.since(body,3);
        h.assertTrue(!rewind.contiguous() && rewind.latestSequence()==2 && rewind.appliedDelta().lengthSqr()==0,
            "A non-growing record must not fabricate continuity for an already consumed newer cursor: "+rewind);
        TransportLedger.invalidate(body,true);body.discard();h.succeed();
    }

    @GameTest
    public void lifecycleGenerationCanRetainOrDiscardAppliedHistory(GameTestHelper h) {
        Entity body=body(h);long tick=h.getLevel().getGameTime();
        TransportLedger.record(body,transport(tick,1,.25,.25));
        long initial=TransportLedger.generation(body);

        TransportLedger.invalidate(body,false);
        var retained=TransportLedger.since(body,0);
        h.assertTrue(TransportLedger.generation(body)==initial+1 && TransportLedger.current(body)!=null,
            "A non-destructive lifecycle discontinuity must advance generation without erasing applied transport");
        h.assertTrue(retained.contiguous() && retained.latestSequence()==1 && close(retained.appliedDelta(),new Vec3(.25,0,0)),
            "Already-applied carry must remain consumable after contact/support release: "+retained);

        TransportLedger.invalidate(body,true);
        var discarded=TransportLedger.since(body,1);
        h.assertTrue(TransportLedger.generation(body)==initial+2 && TransportLedger.current(body)==null,
            "A destructive lifecycle discontinuity must advance generation and discard current transport");
        h.assertTrue(!discarded.contiguous() && discarded.latestSequence()==0 && discarded.appliedDelta().lengthSqr()==0,
            "Discarded history must fail closed for a cursor from the prior lifecycle: "+discarded);
        body.discard();h.succeed();
    }

    @GameTest
    public void historyCapDropsOnlyTheUnprovablePrefix(GameTestHelper h) {
        Entity body=body(h);long tick=h.getLevel().getGameTime();
        for(int sequence=1;sequence<=TransportLedger.HISTORY_ENTRIES+1;sequence++) {
            double cumulative=sequence*.01;
            TransportLedger.record(body,transport(tick,sequence,cumulative,.01));
        }
        var lostPrefix=TransportLedger.since(body,0);
        var retainedTail=TransportLedger.since(body,1);
        h.assertTrue(!lostPrefix.contiguous() && lostPrefix.latestSequence()==65 && lostPrefix.appliedDelta().lengthSqr()==0,
            "The cap+1 case must not invent continuity across the evicted prefix: "+lostPrefix);
        h.assertTrue(retainedTail.contiguous() && retainedTail.latestSequence()==65
                && close(retainedTail.appliedDelta(),new Vec3(.64,0,0)),
            "The still-complete 64-entry tail must remain exactly consumable: "+retainedTail);
        TransportLedger.invalidate(body,true);body.discard();h.succeed();
    }

    @GameTest(maxTicks=TransportLedger.HISTORY_TICKS+10)
    public void historyTtlExpiresOldPrefixWithoutErasingCurrentWatermark(GameTestHelper h) {
        var isolated=isolatedLevel(h);Entity body=body(isolated,h);long tick=isolated.getGameTime();
        TransportLedger.record(body,transport(tick,1,.125,.125));
        h.runAfterDelay(TransportLedger.HISTORY_TICKS,()->{
            try {
                var expired=TransportLedger.since(body,0);
                h.assertTrue(!expired.contiguous() && expired.latestSequence()==1 && expired.appliedDelta().lengthSqr()==0,
                    "TTL expiry must keep the current watermark but refuse an unprovable old cursor: "+expired);
                h.assertTrue(TransportLedger.current(body)!=null && TransportLedger.current(body).sequence()==1,
                    "TTL bounds history, not the latest transport watermark");
                TransportLedger.invalidate(body,true);body.discard();h.succeed();
            } catch(Throwable failure) {TransportLedger.invalidate(body,true);body.discard();throw failure;}
        });
    }

    @GameTest(maxTicks=TransportLedger.HISTORY_TICKS+10)
    public void historyTtlRetainsTheExactLastProvableTick(GameTestHelper h) {
        var isolated=isolatedLevel(h);Entity body=body(isolated,h);long tick=isolated.getGameTime();
        TransportLedger.record(body,transport(tick,1,.125,.125));
        h.runAfterDelay(TransportLedger.HISTORY_TICKS-1,()->{
            try {
                var boundary=TransportLedger.since(body,0);
                h.assertTrue(boundary.contiguous() && boundary.latestSequence()==1
                        && close(boundary.appliedDelta(),new Vec3(.125,0,0)),
                    "The final tick inside the 40-tick history window must remain exactly provable: "+boundary);
                TransportLedger.invalidate(body,true);body.discard();h.succeed();
            } catch(Throwable failure) {TransportLedger.invalidate(body,true);body.discard();throw failure;}
        });
    }

    @GameTest
    public void ordinaryContactReleaseBetweenContributionsPreservesLedgerContinuity(GameTestHelper h) {
        Entity body=body(h);long tick=h.getLevel().getGameTime();
        TransportLedger.record(body,transport(tick,1,.10,.10));
        AnatomyMovement.clear(body);
        TransportLedger.record(body,transport(tick,2,.30,.20));
        var window=TransportLedger.since(body,0);
        h.assertTrue(window.contiguous() && window.latestSequence()==2
                && close(window.appliedDelta(),new Vec3(.30,0,0)),
            "Ordinary contact release must not erase passive transport already applied between pose samples: "+window);
        TransportLedger.invalidate(body,true);body.discard();h.succeed();
    }

    @GameTest
    public void deactivatingOneLevelCannotEraseAnotherLevelsLedger(GameTestHelper h) {
        var deactivatedLevel=h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(deactivatedLevel!=null && deactivatedLevel!=h.getLevel(),"S10 deactivation holdout requires a second isolated server level");
        Entity survivor=body(h);Entity deactivated=body(deactivatedLevel,h);
        TransportLedger.record(survivor,transport(h.getLevel().getGameTime(),1,.10,.10));
        TransportLedger.record(deactivated,transport(deactivatedLevel.getGameTime(),1,.40,.40));

        // TTL holdouts use the Nether and ordinary GameTests share the overworld. Use the End here
        // so deactivation is pairwise observable without deleting any concurrent fixture's state.
        TransportLedger.deactivate(deactivatedLevel);
        h.assertTrue(TransportLedger.current(deactivated)==null && TransportLedger.generation(deactivated)==0,
            "Deactivating a level must remove ledger state owned by that level lifecycle");
        h.assertTrue(TransportLedger.current(survivor)!=null && TransportLedger.current(survivor).sequence()==1
                && close(TransportLedger.current(survivor).appliedDelta(),new Vec3(.10,0,0)),
            "Deactivating one level must not erase the independent ledger of another level");
        TransportLedger.invalidate(survivor,true);survivor.discard();deactivated.discard();h.succeed();
    }

    @GameTest
    public void sameNetworkIdDoesNotAliasLedgerState(GameTestHelper h) {
        Entity first=body(h),second=body(h);second.setId(first.getId());
        h.assertTrue(first!=second && first.equals(second),"Fixture must reproduce vanilla network-ID equality");
        long tick=h.getLevel().getGameTime();
        TransportLedger.record(first,transport(tick,1,.10,.10));
        TransportLedger.record(second,transport(tick,1,.40,.40));
        h.assertTrue(close(TransportLedger.current(first).appliedDelta(),new Vec3(.10,0,0)),
            "Weak identity keys must preserve first entity state despite equal network id");
        h.assertTrue(close(TransportLedger.current(second).appliedDelta(),new Vec3(.40,0,0)),
            "Weak identity keys must preserve second entity state despite equal network id");
        TransportLedger.invalidate(first,false);
        h.assertTrue(TransportLedger.generation(first)==1 && TransportLedger.generation(second)==0,
            "Lifecycle generation must also be identity-local");
        TransportLedger.invalidate(first,true);TransportLedger.invalidate(second,true);first.discard();second.discard();h.succeed();
    }

    private static Level isolatedLevel(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(level!=null && level!=h.getLevel(),"S10 delayed holdout requires a level isolated from ordinary GameTest deactivation");
        return level;
    }

    private static Entity body(GameTestHelper h) {
        return body(h.getLevel(),h);
    }

    private static Entity body(Level level,GameTestHelper h) {
        var body=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        h.assertTrue(body!=null,"S10 fixture requires a creatable entity");
        return body;
    }

    private static SupportTransport transport(long tick,long sequence,double displacement,double applied) {
        return new SupportTransport(tick,sequence,sequence,new Vec3(displacement,0,0),new Vec3(applied,0,0));
    }

    private static boolean close(Vec3 actual,Vec3 expected) {
        return actual.distanceToSqr(expected)<1e-18;
    }
}
