package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Development regressions for material retained across a presentation discontinuity. */
public final class S29ImplementerEndpointRetentionTests {
    @GameTest
    public void explicitBarrierClosesQuietWindowButAllowsFreshTracking(GameTestHelper h) {
        var fixture=new Fixture(h);var fence=new TrackingReplayFence();
        var old=fixture.scopedFrame(fixture.support,fixture.dimension,3);
        var forward=new AnatomyPosePayload(old.epoch(),old.revision(),old.dimension(),old.entityId(),old.entity(),
            old.model(),old.provider(),2,11,11,2,11,old.bindingGeneration(),old.trackingGeneration(),true,
            new PoseProvider.Inputs(0,0,11,0,0,true,Map.of()),Vec3.ZERO,0,1,Direction.DOWN);
        fence.retire(old);
        h.assertTrue(!fence.rejects(forward),"Quiet expiry permits forward recovery in the same window");
        fence.closeWindows();
        h.assertTrue(fence.rejects(forward),"Disconnect also closes histories already evicted by TTL");
        h.assertTrue(!fence.rejects(fixture.scopedFrame(fixture.support,fixture.dimension,4)),
            "Fresh recipient tracking can recover the persistent binding after reconnect");
        fence.clear();fence.retireWindow(old);
        h.assertTrue(fence.rejects(forward),"An explicitly retired live history rejects later old-window frames");
        fence.retire(forward);
        h.assertTrue(fence.rejects(forward),"Quiet retirement cannot reopen an explicitly closed window");
        h.succeed();
    }
    @GameTest
    public void replayScopesKeepDimensionMaximaAndExactRemoval(GameTestHelper h) {
        var fixture=new Fixture(h);var fence=new TrackingReplayFence();
        var first=fixture.scopedFrame(fixture.support,fixture.dimension,3);
        var second=fixture.scopedFrame(fixture.support,Identifier.parse("minecraft:the_nether"),7);
        var unrelated=fixture.scopedFrame(UUID.randomUUID(),fixture.dimension,99);
        h.assertTrue(fence.retire(first) && fence.retire(second) && fence.retire(unrelated),"Distinct replay scopes must fit");
        h.assertTrue(fence.entries()==3 && fence.retiredGeneration(fixture.support)==7,
            "Generation maximum belongs only to this UUID across its dimensions");
        fence.accepted(second);
        h.assertTrue(fence.entries()==2 && fence.retiredGeneration(fixture.support)==3 && fence.rejects(first),
            "Accepting one dimension must retain the other dimension's replay fence and maximum");
        fence.accepted(second);
        h.assertTrue(fence.entries()==2,"Removing an absent scope must not decrement capacity accounting twice");
        fence.accepted(first);
        h.assertTrue(fence.entries()==1 && fence.retiredGeneration(fixture.support)==0,
            "Final scope removal must release only that UUID's generation");
        fence.clear();
        h.assertTrue(fence.entries()==0 && !fence.saturated(),"Session reset must clear scope accounting");
        h.succeed();
    }
    @GameTest
    public void nextLifecyclePreservesOnlyItsAlreadyReceivedCertificate(GameTestHelper h) {
        var fixture=new Fixture(h);var inbox=new AnatomyCertifiedIntervalInbox();
        var certificate=new AnatomyMaterialIntervalPayload(fixture.epoch,3,fixture.dimension,7,fixture.support,
            2,2,1,1,2,10,11,10,11);
        h.assertTrue(inbox.accept(certificate,0),"Next-window certificate may precede its endpoints");
        inbox.retainLifecycle(fixture.support,7,2,2);
        h.assertTrue(inbox.exact(fixture.support,1,1).isPresent(),"Installing its exact lifecycle must preserve a staged certificate");
        inbox.retainLifecycle(fixture.support,7,3,2);
        h.assertTrue(inbox.exact(fixture.support,1,1).isEmpty(),"Installing another lifecycle must discard the old certificate");
        h.succeed();
    }

    @GameTest
    public void unavailableCannotRevivePreviouslyCertifiedMaterial(GameTestHelper h) {
        var fixture=new Fixture(h);var history=new AnatomyFrameHistory();
        history.accept(fixture.frame(1,10,true,Direction.DOWN));
        history.accept(fixture.frame(2,11,true,Direction.DOWN));
        h.assertTrue(history.packet(1).isPresent(),"Continuous endpoints must be retained for exact certificates");
        history.accept(fixture.frame(3,12,false,Direction.DOWN));
        history.accept(fixture.frame(4,13,true,Direction.DOWN));
        h.assertTrue(history.packet(1).isEmpty() && history.packet(2).isEmpty(),
            "Unavailable/recovery must not revive endpoints named by an old certificate");
        h.assertTrue(history.current().frameSerial()==4,"Recovery must preserve the fresh current endpoint");
        h.succeed();
    }

    @GameTest
    public void gravityRoundTripCannotReviveOldPresentationInterval(GameTestHelper h) {
        var fixture=new Fixture(h);var history=new AnatomyFrameHistory();
        history.accept(fixture.frame(1,10,true,Direction.DOWN));
        history.accept(fixture.frame(2,11,true,Direction.DOWN));
        history.accept(fixture.frame(3,12,true,Direction.WEST));
        history.accept(fixture.frame(4,13,true,Direction.DOWN));
        h.assertTrue(history.packet(1).isEmpty() && history.packet(2).isEmpty(),
            "A gravity round-trip cannot make pre-barrier certified material available again");
        h.succeed();
    }

    @GameTest
    public void activeSupportDoesNotKeepExpiredHistoricalEndpoints(GameTestHelper h) {
        var fixture=new Fixture(h);var history=new AnatomyFrameHistory();
        history.accept(fixture.frame(1,10,true,Direction.DOWN));
        history.accept(fixture.frame(2,60,true,Direction.DOWN));
        history.accept(fixture.frame(3,111,true,Direction.DOWN));
        h.assertTrue(history.packet(1).isEmpty(),"Historical endpoint must expire despite a live current support");
        h.assertTrue(history.packet(2).isPresent() && history.current().frameSerial()==3,
            "Endpoint expiration must preserve unexpired material and current authority");
        h.succeed();
    }

    private static final class Fixture {
        final UUID epoch=UUID.randomUUID(),support=UUID.randomUUID();
        final Identifier dimension;
        Fixture(GameTestHelper h){dimension=h.getLevel().dimension().identifier();}
        AnatomyPosePayload scopedFrame(UUID entity,Identifier scope,long tracking) {
            return new AnatomyPosePayload(epoch,3,scope,7,entity,Identifier.parse("minecraft:cow"),
                Identifier.parse("scalebrews:static"),1,10,10,1,10,1,tracking,true,
                new PoseProvider.Inputs(0,0,10,0,0,true,Map.of()),Vec3.ZERO,0,1,Direction.DOWN);
        }
        AnatomyPosePayload frame(long serial,long tick,boolean available,Direction gravity) {
            return new AnatomyPosePayload(epoch,3,dimension,7,support,Identifier.parse("minecraft:cow"),
                Identifier.parse("scalebrews:static"),serial,tick,tick,serial,tick,1,1,available,
                new PoseProvider.Inputs(0,0,tick,0,0,true,Map.of()),Vec3.ZERO,0,1,gravity);
        }
    }
}
