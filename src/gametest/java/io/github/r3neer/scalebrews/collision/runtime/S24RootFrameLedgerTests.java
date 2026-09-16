package io.github.r3neer.scalebrews.collision.runtime;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

/** Pure ownership proof for the bounded root-provenance ledger extracted from AnatomyMovement. */
public final class S24RootFrameLedgerTests {
    @GameTest
    public void sequenceDedupDiscontinuityAndRetentionRemainBounded(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,2,2);
        RootFrameLedger.clear(support);

        var origin=new Vec3(10,20,30);
        var first=RootFrameLedger.observe(support,0,origin,0,1,GravityFrame.VANILLA);
        h.assertTrue(first.frame().sequence()==0 && !first.discontinuity(),
            "First root observation must start at sequence zero without inventing a discontinuity");

        var duplicate=RootFrameLedger.observe(support,1,origin,0,1,GravityFrame.VANILLA);
        h.assertTrue(duplicate.frame().equals(first.frame()) && RootFrameLedger.retainedFrames(support)==1,
            "Identical root observations must reuse the exact causal frame instead of consuming sequence/history");

        var changed=RootFrameLedger.observe(support,1,origin,15,1,GravityFrame.VANILLA);
        h.assertTrue(changed.frame().sequence()==1 && !changed.discontinuity(),
            "Continuous root mutation must advance sequence exactly once");

        // A stale callback-before frame may not fork provenance away from the canonical ledger tail.
        var staleBefore=first.frame();
        var afterStaleBefore=RootFrameLedger.observe(support,2,origin,30,1,GravityFrame.VANILLA,staleBefore);
        h.assertTrue(afterStaleBefore.frame().sequence()==2 && !afterStaleBefore.discontinuity(),
            "Stale callback provenance must fall back to the canonical tail instead of creating an alternate root history");

        var jump=RootFrameLedger.observe(support,3,origin.add(5,0,0),30,1,GravityFrame.VANILLA);
        h.assertTrue(jump.frame().sequence()==3 && jump.discontinuity() && RootFrameLedger.retainedFrames(support)==1,
            "A >4-block root jump must flag discontinuity, preserve monotonic sequence and prune pre-jump history");

        var sideways=new GravityFrame(Direction.EAST);
        var gravityChange=RootFrameLedger.observe(support,4,jump.frame().origin(),30,1,sideways);
        h.assertTrue(gravityChange.frame().sequence()==4 && gravityChange.discontinuity() && RootFrameLedger.retainedFrames(support)==1,
            "Root gravity change must be a discontinuity without rewinding root sequence");

        RootFrameLedger.clear(support);
        RootFrameLedger.observe(support,0,origin,0,1,GravityFrame.VANILLA);
        RootFrame last=null;
        for(int tick=1;tick<=200;tick++)
            last=RootFrameLedger.observe(support,tick,origin,tick,1,GravityFrame.VANILLA).frame();
        h.assertTrue(last!=null && last.sequence()==200,
            "Long continuous observation must preserve monotonic provenance sequence");
        h.assertTrue(RootFrameLedger.retainedFrames(support)<=21,
            "Root provenance must retain only the configured recent 20-tick window, never session-age history");

        RootFrameLedger.clear(support);
        h.assertTrue(RootFrameLedger.retainedFrames(support)==0,
            "Explicit lifecycle clear must release the support root history");
        h.succeed();
    }
}
