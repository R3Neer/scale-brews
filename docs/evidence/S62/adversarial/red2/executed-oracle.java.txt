package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.spi.PoseChannelAdapter;
import io.github.r3neer.scalebrews.collision.internal.AuthorityPoseTracker;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;

/** Test-only adapter registry replacement restored exactly; exercises tracker transaction boundary. */
public final class S62AdversarialTrackerCommitTests {
    private static void check(boolean b,String message){if(!b)throw new AssertionError("S62 ADV "+message);}
    @GameTest @SuppressWarnings("unchecked") public void retiredFrameCannotCommitThroughTracker(GameTestHelper helper)throws Exception{
        var field=CollisionAdapters.class.getDeclaredField("POSE_CHANNELS");field.setAccessible(true);
        var adapters=(Map<Identifier,PoseChannelAdapter>)field.get(null);var type=Identifier.parse("minecraft:cow");
        PoseChannelAdapter prior;var invalidate=new AtomicBoolean(false);
        synchronized(CollisionAdapters.class){prior=adapters.put(type,(actor,sink)->{if(invalidate.get())TransportLedger.invalidate(actor,false);sink.put("probe",1f);});}
        var actor=EntityTypes.COW.create(helper.getLevel(),EntitySpawnReason.COMMAND);actor.setPos(3,3,3);
        try{
            var tracker=new AuthorityPoseTracker();var first=tracker.tick(actor,200,true);
            check(first.ordinary(),"positive adapter control unavailable");check(tracker.locomotion(actor).orElseThrow().validFor(actor,200),"positive control frame invalid");
            actor.setPos(3.125,3,3);invalidate.set(true);var result=tracker.tick(actor,201,true);
            var frame=tracker.locomotion(actor).orElseThrow();check(!frame.validFor(actor,201),"adapter did not retire ledger generation");
            check(!result.ordinary(),"tracker published retired frame as ordinary; amount="+result.walkAmount()+" phase="+result.walkPhase());
            helper.succeed();
        }finally{
            synchronized(CollisionAdapters.class){if(prior==null)adapters.remove(type);else adapters.put(type,prior);}
            actor.discard();
        }
    }
}
