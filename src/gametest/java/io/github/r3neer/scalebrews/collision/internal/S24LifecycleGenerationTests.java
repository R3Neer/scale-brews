package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Implementer regressions for G3.9 tracking/unload/reuse lifecycle fencing. */
public final class S24LifecycleGenerationTests {
    @GameTest
    public void unloadRetiresAcceptedTrackingGenerationUntilServerRestartsTracking(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        long bindingGeneration=5,trackingGeneration=7;

        var accepted=frame(epoch,dimension,7,entity,model,provider,1,10,bindingGeneration,trackingGeneration,inputs);
        var delayedAfterUnload=frame(epoch,dimension,7,entity,model,provider,2,11,bindingGeneration,trackingGeneration,inputs);
        var history=new AnatomyFrameHistory();
        h.assertTrue(history.accept(accepted),"Initial tracked generation must be accepted");

        history.retireCurrentTrackingGeneration();
        h.assertTrue(!history.accept(delayedAfterUnload) && history.current()==accepted,
            "ENTITY_UNLOAD must retain a tracking tombstone: a later serial from the retired recipient generation cannot resurrect material");

        long restartedTracking=AnatomyRuntime.nextTrackingGeneration(trackingGeneration);
        var restarted=frame(epoch,dimension,7,entity,model,provider,3,12,bindingGeneration,restartedTracking,inputs);
        h.assertTrue(history.transition(restarted)==AnatomyFrameHistory.LifecycleTransition.RESTART,
            "A strictly newer server-owned recipient tracking generation must request a fresh history");
        var restartedHistory=new AnatomyFrameHistory();
        h.assertTrue(restartedHistory.accept(restarted) && restartedHistory.current().bindingGeneration()==bindingGeneration
                && restartedHistory.current().trackingGeneration()==restartedTracking,
            "Re-tracking may preserve the physical binding generation but must carry a strictly newer recipient tracking generation");
        h.succeed();
    }

    @GameTest
    public void retiredTrackingCannotBeRevivedByNewBindingGeneration(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var first=frame(epoch,dimension,7,entity,model,provider,1,10,5,7,inputs);
        var history=new AnatomyFrameHistory();
        h.assertTrue(history.accept(first),"Fixture must install the pre-unload binding/tracking identity");
        history.retireCurrentTrackingGeneration();

        var forgedRebind=frame(epoch,dimension,8,entity,model,provider,2,11,6,7,inputs);
        h.assertTrue(history.transition(forgedRebind)==AnatomyFrameHistory.LifecycleTransition.REJECT,
            "Advancing binding generation must not erase an explicit tombstone for the same retired recipient tracking window");
        h.assertTrue(!history.accept(forgedRebind) && history.current()==first,
            "A replacement/rebind packet from a retired tracking generation must leave the accepted watermark untouched");

        var legitimateRebindAfterRetrack=frame(epoch,dimension,8,entity,model,provider,3,12,6,8,inputs);
        h.assertTrue(history.transition(legitimateRebindAfterRetrack)==AnatomyFrameHistory.LifecycleTransition.RESTART,
            "A new binding becomes admissible only after the server also advances beyond the retired tracking window");
        h.succeed();
    }

    @GameTest
    public void bindingAndTrackingAxesClassifyIndependently(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var first=frame(epoch,dimension,7,entity,model,provider,1,10,5,7,inputs);
        var history=new AnatomyFrameHistory();h.assertTrue(history.accept(first),"Fixture identity must install");

        var continuation=frame(epoch,dimension,7,entity,model,provider,2,11,5,7,inputs);
        var retrack=frame(epoch,dimension,7,entity,model,provider,3,12,5,8,inputs);
        var rebind=frame(epoch,dimension,7,entity,model,provider,3,12,6,7,inputs);
        var bothAdvance=frame(epoch,dimension,8,entity,model,provider,3,12,6,8,inputs);
        var bindingRollback=frame(epoch,dimension,7,entity,model,provider,3,12,4,8,inputs);
        var trackingRollback=frame(epoch,dimension,7,entity,model,provider,3,12,6,6,inputs);

        h.assertTrue(history.transition(continuation)==AnatomyFrameHistory.LifecycleTransition.CONTINUE,
            "Stable binding and tracking generations stay in the current ordered frame history");
        h.assertTrue(history.transition(retrack)==AnatomyFrameHistory.LifecycleTransition.RESTART,
            "Recipient re-tracking restarts temporal history without pretending the server binding changed");
        h.assertTrue(history.transition(rebind)==AnatomyFrameHistory.LifecycleTransition.RESTART,
            "A strictly newer server binding restarts temporal history without requiring a fabricated local tracking increment");
        h.assertTrue(history.transition(bothAdvance)==AnatomyFrameHistory.LifecycleTransition.RESTART,
            "Replacement may advance both independent lifecycle axes in one authoritative packet");
        h.assertTrue(history.transition(bindingRollback)==AnatomyFrameHistory.LifecycleTransition.REJECT,
            "A newer tracking window cannot legalize rollback to an older server binding generation");
        h.assertTrue(history.transition(trackingRollback)==AnatomyFrameHistory.LifecycleTransition.REJECT,
            "A newer binding cannot legalize rollback to an older recipient tracking generation");
        h.succeed();
    }

    @GameTest
    public void pureRetrackCannotSmuggleAReplacementNetworkIdentity(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var first=frame(epoch,dimension,7,entity,model,provider,1,10,5,7,inputs);
        var history=new AnatomyFrameHistory();h.assertTrue(history.accept(first),"Fixture identity must install");

        var changedIdWithoutRebind=frame(epoch,dimension,8,entity,model,provider,2,11,5,8,inputs);
        h.assertTrue(history.transition(changedIdWithoutRebind)==AnatomyFrameHistory.LifecycleTransition.REJECT,
            "Tracking generation alone cannot smuggle a replacement entity/network identity through an unchanged binding generation");

        var changedIdWithRebind=frame(epoch,dimension,8,entity,model,provider,2,11,6,8,inputs);
        h.assertTrue(history.transition(changedIdWithRebind)==AnatomyFrameHistory.LifecycleTransition.RESTART,
            "The same UUID may acquire a replacement network identity only across an explicit newer server binding lifecycle");
        h.succeed();
    }

    @GameTest
    public void explicitHistoryResetDropsRetiredTrackingGenerationForNewConnection(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var first=frame(epoch,dimension,7,entity,model,provider,1,10,3,4,inputs);
        var replay=frame(epoch,dimension,7,entity,model,provider,2,11,3,4,inputs);
        var history=new AnatomyFrameHistory();

        h.assertTrue(history.accept(first),"Fixture tracking generation must be accepted before reset");
        history.retireCurrentTrackingGeneration();
        h.assertTrue(!history.accept(replay),"Retirement must reject same-tracking-generation replay before reset");
        history.clear();
        h.assertTrue(history.accept(replay),"A full connection/catalog reset must drop the prior connection's tracking retirement watermark");
        h.succeed();
    }

    @GameTest
    public void compactReplayFenceRejectsExpiredPacketButAllowsProvablyNewerContinuation(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var expired=frame(epoch,dimension,7,entity,model,provider,10,100,5,7,inputs);
        var delayed=frame(epoch,dimension,7,entity,model,provider,9,99,5,7,inputs);
        var fresh=frame(epoch,dimension,7,entity,model,provider,11,101,5,7,inputs);
        var forgedIdentity=frame(epoch,dimension,8,entity,model,provider,11,101,5,7,inputs);
        var fence=new TrackingReplayFence();

        h.assertTrue(fence.retire(expired) && fence.entries()==1,"Expiring a full frame history must leave one compact replay watermark");
        h.assertTrue(fence.rejects(expired) && fence.rejects(delayed),"The expired packet and every older ordering watermark must remain rejected after history eviction");
        h.assertTrue(fence.rejects(forgedIdentity),"The same lifecycle generation cannot change network identity after its full history was evicted");
        h.assertTrue(!fence.rejects(fresh),"A strictly newer frame in the same still-valid tracking window may recover after a quiet TTL");
        fence.accepted(fresh);
        h.assertTrue(fence.entries()==0 && !fence.rejects(fresh),"Once a fresh live history owns the watermark, the compact tombstone must be released");
        h.succeed();
    }

    @GameTest
    public void compactReplayFencePreservesIndependentBindingAndTrackingSemantics(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var expired=frame(epoch,dimension,7,entity,model,provider,10,100,5,7,inputs);
        var pureRetrack=frame(epoch,dimension,7,entity,model,provider,1,101,5,8,inputs);
        var smuggledReplacement=frame(epoch,dimension,8,entity,model,provider,1,101,5,8,inputs);
        var explicitRebind=frame(epoch,dimension,8,entity,model,provider,1,101,6,7,inputs);
        var trackingRollback=frame(epoch,dimension,8,entity,model,provider,20,102,6,6,inputs);
        var fence=new TrackingReplayFence();h.assertTrue(fence.retire(expired),"Fixture tombstone must install");

        h.assertTrue(!fence.rejects(pureRetrack),"A newer recipient tracking window may restart the same physical binding");
        h.assertTrue(fence.rejects(smuggledReplacement),"Tracking generation alone cannot launder a changed network identity through the compact fence");
        h.assertTrue(!fence.rejects(explicitRebind),"A newer server binding may legitimately replace network identity inside the same tracking window");
        h.assertTrue(fence.rejects(trackingRollback),"A newer binding must not legalize rollback to an older recipient tracking generation");
        h.succeed();
    }

    @GameTest
    public void compactReplayFenceSaturatesWithoutEvictingAuthorityBarriers(GameTestHelper h) {
        var epoch=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());var fence=new TrackingReplayFence();
        for(int i=0;i<TrackingReplayFence.MAX_RETIRED;i++) {
            var id=new UUID(0x534234L,i+1L);
            h.assertTrue(fence.retire(frame(epoch,dimension,7,id,model,provider,1,10,5,i+1L,inputs)),"Replay fence rejected an entry before its documented cap");
        }
        h.assertTrue(fence.entries()==TrackingReplayFence.MAX_RETIRED && !fence.saturated(),"Exactly MAX_RETIRED compact tombstones must fit without saturation");

        var overflowId=new UUID(0x534234L,TrackingReplayFence.MAX_RETIRED+1L);
        var overflow=frame(epoch,dimension,7,overflowId,model,provider,1,10,5,TrackingReplayFence.MAX_RETIRED+1L,inputs);
        h.assertTrue(!fence.retire(overflow) && fence.saturated() && fence.entries()==TrackingReplayFence.MAX_RETIRED,
            "The first excess tombstone must saturate conservatively without eviction or growth");
        var arbitraryFresh=frame(epoch,dimension,7,UUID.randomUUID(),model,provider,999,999,99,999,inputs);
        h.assertTrue(fence.rejects(arbitraryFresh),"A saturated replay fence must fail closed rather than inventing authority for an untracked packet");

        fence.clear();
        h.assertTrue(!fence.saturated() && fence.entries()==0 && !fence.rejects(arbitraryFresh),
            "Only a full lifecycle reset may clear saturation and all prior-connection tombstones");
        h.succeed();
    }

    private static AnatomyPosePayload frame(UUID epoch,Identifier dimension,int entityId,UUID entity,Identifier model,Identifier provider,
                                             long serial,long tick,long bindingGeneration,long trackingGeneration,PoseProvider.Inputs inputs) {
        return new AnatomyPosePayload(epoch,3,dimension,entityId,entity,model,provider,serial,tick,tick,4,tick,bindingGeneration,trackingGeneration,true,
            inputs,Vec3.ZERO,0,1,net.minecraft.core.Direction.DOWN);
    }
}
