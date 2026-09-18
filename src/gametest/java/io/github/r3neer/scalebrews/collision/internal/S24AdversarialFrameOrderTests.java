package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Adversarial owner-level oracle for same-window pose endpoint ordering. */
public final class S24AdversarialFrameOrderTests {
    @GameTest
    public void sameWindowOrderingRejectsEveryRollbackAxisWithoutMutatingCurrent(GameTestHelper h) {
        var epoch=UUID.randomUUID();
        var entity=UUID.randomUUID();
        var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");
        var provider=Identifier.parse("scalebrews:static");
        var inputs=new PoseProvider.Inputs(0,0,100,0,0,true,Map.of());

        var history=new AnatomyFrameHistory();
        var accepted=frame(epoch,dimension,7,entity,model,provider,10,100,100,5,7,inputs);
        h.assertTrue(history.accept(accepted) && history.current()==accepted,
            "Fixture must install one exact same-window causal endpoint");

        var staleSerial=frame(epoch,dimension,7,entity,model,provider,9,101,101,5,7,inputs);
        h.assertTrue(!history.accept(staleSerial) && history.current()==accepted,
            "A lower frame serial in the same binding/tracking window must be rejected without mutating current");

        var duplicateSerial=frame(epoch,dimension,7,entity,model,provider,10,101,101,5,7,inputs);
        h.assertTrue(!history.accept(duplicateSerial) && history.current()==accepted,
            "An equal frame serial cannot carry a different later endpoint under the same causal identity");

        var authorityRollback=frame(epoch,dimension,7,entity,model,provider,11,99,99,5,7,inputs);
        h.assertTrue(!history.accept(authorityRollback) && history.current()==accepted,
            "A newer serial cannot legalize rollback of authority tick");

        var jointRollback=frame(epoch,dimension,7,entity,model,provider,11,101,99,5,7,inputs);
        h.assertTrue(!history.accept(jointRollback) && history.current()==accepted,
            "A newer serial/authority tick cannot legalize rollback of the joint sample clock");

        var next=frame(epoch,dimension,7,entity,model,provider,11,101,101,5,7,inputs);
        h.assertTrue(history.accept(next) && history.current()==next,
            "A strictly newer serial with non-decreasing authority and joint clocks must remain admissible");
        h.succeed();
    }

    private static AnatomyPosePayload frame(UUID epoch,Identifier dimension,int entityId,UUID entity,
            Identifier model,Identifier provider,long serial,long authorityTick,long jointSampleTick,
            long bindingGeneration,long trackingGeneration,PoseProvider.Inputs inputs) {
        return new AnatomyPosePayload(epoch,3,dimension,entityId,entity,model,provider,
            serial,authorityTick,jointSampleTick,serial,Math.min(authorityTick,jointSampleTick),
            bindingGeneration,trackingGeneration,true,inputs,Vec3.ZERO,0,1,net.minecraft.core.Direction.DOWN);
    }
}
