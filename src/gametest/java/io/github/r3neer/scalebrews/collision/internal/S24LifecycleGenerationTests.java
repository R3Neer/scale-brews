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
    public void unloadRetiresAcceptedGenerationUntilServerRestartsTracking(GameTestHelper h) {
        var epoch=UUID.randomUUID();
        var entity=UUID.randomUUID();
        var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");
        var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        long generation=7;

        var accepted=frame(epoch,dimension,entity,model,provider,1,10,generation,inputs);
        var delayedAfterUnload=frame(epoch,dimension,entity,model,provider,2,11,generation,inputs);
        var history=new AnatomyFrameHistory();
        h.assertTrue(history.accept(accepted),"Initial tracked generation must be accepted");

        history.retireCurrentGeneration();
        h.assertTrue(!history.accept(delayedAfterUnload) && history.current()==accepted,
            "ENTITY_UNLOAD must retain an ordering tombstone: a later serial from the retired generation cannot resurrect material");

        long restartedGeneration=AnatomyRuntime.nextTrackingGeneration(generation);
        var restarted=frame(epoch,dimension,entity,model,provider,1,12,restartedGeneration,inputs);
        var restartedHistory=new AnatomyFrameHistory();
        h.assertTrue(restartedHistory.accept(restarted) && restartedHistory.current().bindingGeneration()==restartedGeneration,
            "Only the strictly newer server-owned tracking generation may start a fresh frame history after unload");
        h.succeed();
    }

    @GameTest
    public void explicitHistoryResetDropsRetiredGenerationForNewConnection(GameTestHelper h) {
        var epoch=UUID.randomUUID();
        var entity=UUID.randomUUID();
        var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");
        var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var first=frame(epoch,dimension,entity,model,provider,1,10,3,inputs);
        var replay=frame(epoch,dimension,entity,model,provider,2,11,3,inputs);
        var history=new AnatomyFrameHistory();

        h.assertTrue(history.accept(first),"Fixture generation must be accepted before reset");
        history.retireCurrentGeneration();
        h.assertTrue(!history.accept(replay),"Retirement must reject same-generation replay before reset");
        history.clear();
        h.assertTrue(history.accept(replay),
            "A full connection/catalog reset must drop the prior connection's retirement watermark");
        h.succeed();
    }

    private static AnatomyPosePayload frame(UUID epoch,Identifier dimension,UUID entity,Identifier model,Identifier provider,
                                             long serial,long tick,long generation,PoseProvider.Inputs inputs) {
        return new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,serial,tick,tick,4,tick,generation,true,
            inputs,Vec3.ZERO,0,1,net.minecraft.core.Direction.DOWN);
    }
}
