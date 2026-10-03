package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyCertifiedIntervalInbox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMaterialIntervalPayload;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * Test-only, virtual-clock receiver retention probe. It models 16 independent
 * recipients of 64 supports; no network or renderer timing claim follows from it.
 */
public final class S29ImplementerIntervalSoak {
    private static final int RECIPIENTS=16;
    private static final int SUPPORTS=64;
    private static final int TICKS=12_000;
    private static volatile long BLACKHOLE;

    @GameTest public void certifiedInboxRetainsBoundedMaterialAcrossLongFanout(GameTestHelper h) {
        var inboxes=new AnatomyCertifiedIntervalInbox[RECIPIENTS];
        for(int observer=0;observer<RECIPIENTS;observer++)inboxes[observer]=new AnatomyCertifiedIntervalInbox();
        var epoch=UUID.fromString("70ac4a6f-8207-458d-ab28-f986623def29");
        var supports=new UUID[SUPPORTS];
        for(int support=0;support<SUPPORTS;support++)supports[support]=new UUID(0x5290000000000000L,support+1);
        var dimension=Identifier.parse("minecraft:overworld");
        var gcBefore=collections();
        long accepted=0;
        long middleHeap=-1;
        for(int tick=0;tick<=TICKS;tick++) {
            // A certificate every four ticks stresses retention for the full
            // virtual duration without pretending every simulation tick was a real server tick.
            if(tick%4==0)for(int support=0;support<SUPPORTS;support++) {
                long serial=tick/4L+1;
                for(var inbox:inboxes) {
                    // Each receiver deserializes a distinct payload object on the real wire.
                    var token=new AnatomyMaterialIntervalPayload(epoch,1,dimension,support+1,supports[support],
                        1,1,serial,serial*2-1,serial*2,tick,tick+1,tick,tick+1);
                    if(!inbox.accept(token,tick))throw new AssertionError("Live certificate rejected at tick "+tick);
                    accepted++;
                }
            }
            for(var inbox:inboxes)inbox.prune(tick);
            if(tick==TICKS/2) {
                assertBounds(inboxes,supports,tick);
                middleHeap=settledHeap();
            }
        }
        assertBounds(inboxes,supports,TICKS);
        long finalHeap=settledHeap();
        long gcAfter=collections();
        System.out.println("S29_INTERVAL_SOAK virtualTicks="+TICKS+" recipients="+RECIPIENTS+
            " supports="+SUPPORTS+" accepted="+accepted+" middleHeap="+middleHeap+
            " finalHeap="+finalHeap+" heapDelta="+(finalHeap-middleHeap)+" gcCycles="+(gcAfter-gcBefore));
        h.assertTrue(accepted==(TICKS/4L+1)*SUPPORTS*RECIPIENTS,"Fan-out workload did not complete");
        h.assertTrue(finalHeap-middleHeap<48L*1024*1024,"Certified inbox retained growing material after equal-length windows");
        BLACKHOLE=accepted+finalHeap;
        h.succeed();
    }

    private static void assertBounds(AnatomyCertifiedIntervalInbox[] inboxes,UUID[] supports,long tick) {
        for(var inbox:inboxes) {
            if(inbox.saturated() || inbox.supports()!=SUPPORTS)throw new AssertionError("Support fan-out state drift at "+tick);
            for(var support:supports) {
                int entries=inbox.entries(support);
                if(entries<1 || entries>26)throw new AssertionError("Unbounded certificate history "+entries+" at "+tick);
            }
        }
    }

    private static long settledHeap() {
        System.gc();System.gc();
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }
    private static long collections() {
        long result=0;
        for(var bean:ManagementFactory.getGarbageCollectorMXBeans())if(bean.getCollectionCount()>0)result+=bean.getCollectionCount();
        return result;
    }
}
