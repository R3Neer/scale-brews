package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** A01/A10/H02: receipt DTO invariants, retention and replay on the actual recorder. */
public final class S00ReceiptTests {
    private static final Vec3 UP=new Vec3(0,1,0),POINT=new Vec3(.5,1,.5),DELTA=new Vec3(.125,0,0);
    private static AnatomyTransportReceipts.Receipt receipt(Vec3 point,Vec3 normal,Vec3 before,Vec3 after,long rootSequence,long rootTick,long surfaceTick) {
        var root=new AnatomyMovement.RootFrame(2,rootTick,Vec3.ZERO,0,1,GravityFrame.VANILLA);
        return new AnatomyTransportReceipts.Receipt(EPOCH,1,Identifier.parse("minecraft:overworld"),1,new UUID(0,3),1,
            SUPPORT,"piece",3,point,normal,surfaceTick,1,rootSequence,1,10,before,after,root,box(UNIT),box(UNIT).move(DELTA),DELTA);
    }
    @GameTest public void receiptCannotClaimInvalidSurface(GameTestHelper h) {
        receipt(POINT,UP,Vec3.ZERO,DELTA,2,10,10);
        rejects(()->receipt(new Vec3(.5,.9,.5),UP,Vec3.ZERO,DELTA,2,10,10));
        rejects(()->receipt(POINT,Vec3.ZERO,Vec3.ZERO,DELTA,2,10,10));h.succeed();
    }
    @GameTest public void receiptCannotDisagreeWithAppliedDelta(GameTestHelper h) {
        rejects(()->receipt(POINT,UP,Vec3.ZERO,Vec3.ZERO,2,10,10));
        // Translation near the world border must not require exact binary decimal equality.
        var before=new Vec3(29000000,20,-29000000);receipt(POINT,UP,before,before.add(DELTA),2,10,10);h.succeed();
    }
    @GameTest public void receiptCannotClaimFutureOrContradictoryRoot(GameTestHelper h) {
        rejects(()->receipt(POINT,UP,Vec3.ZERO,DELTA,3,10,10));
        rejects(()->receipt(POINT,UP,Vec3.ZERO,DELTA,2,11,10));
        rejects(()->receipt(POINT,UP,Vec3.ZERO,DELTA,2,10,11));h.succeed();
    }
    private static void record(ServerPlayer body,LivingEntity support,long tick,long sequence) {
        var surface=new SurfaceContact(support.getUUID(),1,"piece",3,POINT,UP,Math.min(tick,body.level().getGameTime()));
        var contact=new AnatomyMovement.Contact(support,"piece",1,UP,1);
        var root=new AnatomyMovement.RootFrame(1,body.level().getGameTime(),support.position(),0,1,GravityFrame.VANILLA);
        var material=box(UNIT).move(support.position());
        AnatomyTransportReceipts.record(body,contact,surface,root,new SupportTransport(tick,sequence,1,Vec3.ZERO,Vec3.ZERO),material,material);
    }
    @GameTest public void receiptRecorderRejectsAResultFromAnotherTick(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,20,2);var body=h.makeMockServerPlayerInLevel();
        try {
            record(body,support,h.getLevel().getGameTime()+1,1);
            check(AnatomyTransportReceipts.history(body,body.getUUID()).isEmpty(),"Recorder gave current authority to future transport");
        }finally{AnatomyTransportReceipts.invalidate(body);support.discard();body.discard();}h.succeed();
    }
    @GameTest(maxTicks=50) public void holdoutH02SaturationSurvivesBoundaryAndRejectsExpiredReplay(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();long tick=h.getLevel().getGameTime();
        for(int i=0;i<=AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK;i++)record(body,support,tick,1+i);
        check(AnatomyTransportReceipts.history(body,body.getUUID()).size()==AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK,"Saturation retained unbounded receipt prefix");
        h.runAfterDelay(AnatomyTransportReceipts.HISTORY_TICKS-1,()->{
            try {check(AnatomyTransportReceipts.saturated(body,body.getUUID(),tick),"Saturation fence expired before its full TTL");}
            catch(RuntimeException | AssertionError failure){AnatomyTransportReceipts.invalidate(body);support.discard();body.discard();h.assertTrue(false,String.valueOf(failure.getMessage()));return;}
            h.runAfterDelay(1,()->{
                try {
                    check(!AnatomyTransportReceipts.saturated(body,body.getUUID(),tick) && AnatomyTransportReceipts.history(body,body.getUUID()).isEmpty(),"TTL did not expire at its declared boundary");
                    record(body,support,tick,1);
                    check(AnatomyTransportReceipts.history(body,body.getUUID()).isEmpty(),"Expired transport replay regained current authority");
                    record(body,support,h.getLevel().getGameTime(),100);
                    check(AnatomyTransportReceipts.history(body,body.getUUID()).size()==1,"Fresh post-TTL contribution was incorrectly rejected");h.succeed();
                }catch(RuntimeException | AssertionError failure){h.assertTrue(false,String.valueOf(failure.getMessage()));}
                finally{AnatomyTransportReceipts.invalidate(body);support.discard();body.discard();}
            });
        });
    }
}
