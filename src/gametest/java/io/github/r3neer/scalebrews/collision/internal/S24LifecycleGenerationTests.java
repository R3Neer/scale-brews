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

        var accepted=frame(epoch,dimension,entity,model,provider,1,10,bindingGeneration,trackingGeneration,inputs);
        var delayedAfterUnload=frame(epoch,dimension,entity,model,provider,2,11,bindingGeneration,trackingGeneration,inputs);
        var history=new AnatomyFrameHistory();
        h.assertTrue(history.accept(accepted),"Initial tracked generation must be accepted");

        history.retireCurrentTrackingGeneration();
        h.assertTrue(!history.accept(delayedAfterUnload) && history.current()==accepted,
            "ENTITY_UNLOAD must retain a tracking tombstone: a later serial from the retired recipient generation cannot resurrect material");

        long restartedTracking=AnatomyRuntime.nextTrackingGeneration(trackingGeneration);
        var restarted=frame(epoch,dimension,entity,model,provider,3,12,bindingGeneration,restartedTracking,inputs);
        var restartedHistory=new AnatomyFrameHistory();
        h.assertTrue(restartedHistory.accept(restarted) && restartedHistory.current().bindingGeneration()==bindingGeneration
                && restartedHistory.current().trackingGeneration()==restartedTracking,
            "Re-tracking may preserve the physical binding generation but must carry a strictly newer recipient tracking generation");
        h.succeed();
    }

    @GameTest
    public void explicitHistoryResetDropsRetiredTrackingGenerationForNewConnection(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var first=frame(epoch,dimension,entity,model,provider,1,10,3,4,inputs);
        var replay=frame(epoch,dimension,entity,model,provider,2,11,3,4,inputs);
        var history=new AnatomyFrameHistory();

        h.assertTrue(history.accept(first),"Fixture tracking generation must be accepted before reset");
        history.retireCurrentTrackingGeneration();
        h.assertTrue(!history.accept(replay),"Retirement must reject same-tracking-generation replay before reset");
        history.clear();
        h.assertTrue(history.accept(replay),"A full connection/catalog reset must drop the prior connection's tracking retirement watermark");
        h.succeed();
    }

    private static AnatomyPosePayload frame(UUID epoch,Identifier dimension,UUID entity,Identifier model,Identifier provider,
                                             long serial,long tick,long bindingGeneration,long trackingGeneration,PoseProvider.Inputs inputs) {
        return new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,serial,tick,tick,4,tick,bindingGeneration,trackingGeneration,true,
            inputs,Vec3.ZERO,0,1,net.minecraft.core.Direction.DOWN);
    }
}
