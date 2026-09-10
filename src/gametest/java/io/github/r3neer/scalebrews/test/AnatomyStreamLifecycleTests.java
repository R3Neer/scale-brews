package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.anatomy.AnatomyStreamLifecycle;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Pure N3 stream-lifecycle regressions; no payload type or Runtime hook yet. */
public final class AnatomyStreamLifecycleTests {
    @GameTest
    public void retiredStreamRejectsOldPacketsAndLateServerCallbacks(GameTestHelper h) {
        var epoch=UUID.randomUUID();var identity=identity(epoch,UUID.randomUUID(),17,4,1,Identifier.parse("minecraft:overworld"));
        var server=new AnatomyStreamLifecycle.ServerGate(epoch,2);var client=new AnatomyStreamLifecycle.ClientLedger(2);long generation=client.beginConnection(epoch);
        var start=server.start(identity,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();
        h.assertTrue(client.receive(generation,start)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,"Explicit start opens the only client stream life");
        var frame=server.frame(identity,start.supportStreamLife(),1,10,10,4,10,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();
        h.assertTrue(client.receive(generation,frame)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,"Current server stream publishes its first frame");
        var retire=server.retire(identity,start.supportStreamLife(),AnatomyStreamLifecycle.RetireReason.UNLOAD).orElseThrow();
        h.assertTrue(client.receive(generation,retire)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED && client.activeStreams()==0,"Retirement prunes the per-support client fence");
        long beforeRejected=server.nextPublicationSequence();
        h.assertTrue(server.frame(identity,start.supportStreamLife(),2,11,11,5,11,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).isEmpty()
                && server.nextPublicationSequence()==beforeRejected && server.rejectedStaleCallbacks()==1,
            "A callback after retirement is rejected before it consumes a publication sequence");
        h.assertTrue(client.receive(generation,frame)==AnatomyStreamLifecycle.ReceiveResult.STALE_SEQUENCE && client.activeStreams()==0,
            "An encoded old frame cannot reopen a retired life");
        var forgedNewSequence=new AnatomyStreamLifecycle.Frame(epoch,client.expectedSequence(),identity,start.supportStreamLife(),2,11,11,5,11,
            AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true);
        h.assertTrue(client.receive(generation,forgedNewSequence)==AnatomyStreamLifecycle.ReceiveResult.REJECTED_SEMANTICS && client.activeStreams()==0,
            "Even a fresh sequence cannot make an ordinary frame recreate a retired stream");
        h.succeed();
    }

    @GameTest
    public void lifecyclePrunesBeyond4096AndSeparatesUuidAndIdReuse(GameTestHelper h) {
        var epoch=UUID.randomUUID();var server=new AnatomyStreamLifecycle.ServerGate(epoch,8);var client=new AnatomyStreamLifecycle.ClientLedger(8);long generation=client.beginConnection(epoch);
        for(int index=0;index<5000;index++) {
            var identity=identity(epoch,UUID.randomUUID(),index,1,1,Identifier.parse("minecraft:overworld"));
            var start=server.start(identity,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();
            h.assertTrue(client.receive(generation,start)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,"Bounded stream opens at iteration "+index);
            var frame=server.frame(identity,start.supportStreamLife(),1,index,index,0,index,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();
            h.assertTrue(client.receive(generation,frame)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,"Frame accepts at iteration "+index);
            var retire=server.retire(identity,start.supportStreamLife(),AnatomyStreamLifecycle.RetireReason.STOP_TRACKING).orElseThrow();
            h.assertTrue(client.receive(generation,retire)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,"Retire accepts at iteration "+index);
        }
        h.assertTrue(server.activeStreams()==0 && client.activeStreams()==0,"More than 4096 visited supports leave no unbounded active tombstones");

        UUID reusedUuid=UUID.randomUUID();var first=identity(epoch,reusedUuid,41,3,1,Identifier.parse("minecraft:overworld"));
        var startFirst=server.start(first,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();client.receive(generation,startFirst);
        var frameFirst=server.frame(first,startFirst.supportStreamLife(),1,1,1,0,1,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();client.receive(generation,frameFirst);
        client.receive(generation,server.retire(first,startFirst.supportStreamLife(),AnatomyStreamLifecycle.RetireReason.UNLOAD).orElseThrow());
        var sameUuidAndIdNewBinding=identity(epoch,reusedUuid,41,3,2,Identifier.parse("minecraft:overworld"));
        var startSame=server.start(sameUuidAndIdNewBinding,1,AnatomyStreamLifecycle.StartReason.REBIND).orElseThrow();
        h.assertTrue(client.receive(generation,startSame)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED && startSame.supportStreamLife()>startFirst.supportStreamLife(),
            "Same UUID and network ID require a new binding/stream life after unload");
        h.assertTrue(client.receive(generation,frameFirst)==AnatomyStreamLifecycle.ReceiveResult.STALE_SEQUENCE,"Old UUID/ID frame stays fenced after replacement");
        h.succeed();
    }

    @GameTest
    public void connectionCatalogAndRootDiscontinuityHaveExplicitLives(GameTestHelper h) {
        var epoch=UUID.randomUUID();var server=new AnatomyStreamLifecycle.ServerGate(epoch,4);var client=new AnatomyStreamLifecycle.ClientLedger(4);long oldConnection=client.beginConnection(epoch);
        var overworld=identity(epoch,UUID.randomUUID(),8,7,1,Identifier.parse("minecraft:overworld"));
        var start=server.start(overworld,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();client.receive(oldConnection,start);
        var first=server.frame(overworld,start.supportStreamLife(),1,20,20,6,20,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();client.receive(oldConnection,first);
        var illegalReset=new AnatomyStreamLifecycle.Frame(epoch,client.expectedSequence(),overworld,start.supportStreamLife(),2,21,20,0,0,
            AnatomyStreamLifecycle.FrameKind.ROOT_ONLY,true);
        h.assertTrue(client.receive(oldConnection,illegalReset)==AnatomyStreamLifecycle.ReceiveResult.REJECTED_CLOCK,
            "Root sequence cannot reset inside one active stream life");
        // The rejected forged sequence is deliberately consumed. Keep the server
        // allocator on its real sequence and prove that its next publication is
        // stale, rather than silently realigning a client after malformed input.
        var serverContinues=server.frame(overworld,start.supportStreamLife(),2,21,20,7,21,
            AnatomyStreamLifecycle.FrameKind.ROOT_ONLY,true).orElseThrow();
        h.assertTrue(client.receive(oldConnection,serverContinues)==AnatomyStreamLifecycle.ReceiveResult.STALE_SEQUENCE,
            "A server publication already consumed by a forged current sequence cannot be replayed");
        var retire=server.retire(overworld,start.supportStreamLife(),AnatomyStreamLifecycle.RetireReason.DISCONTINUITY).orElseThrow();client.receive(oldConnection,retire);
        var replacement=identity(epoch,overworld.support(),8,7,2,Identifier.parse("minecraft:overworld"));
        var restart=server.start(replacement,1,AnatomyStreamLifecycle.StartReason.DISCONTINUITY).orElseThrow();client.receive(oldConnection,restart);
        var resetAtNewLife=server.frame(replacement,restart.supportStreamLife(),1,22,22,0,0,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();
        h.assertTrue(client.receive(oldConnection,resetAtNewLife)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,"Explicit discontinuity permits a new root clock life");

        long newConnection=client.resetConnection();newConnection=client.beginConnection(epoch);
        h.assertTrue(client.receive(oldConnection,restart)==AnatomyStreamLifecycle.ReceiveResult.STALE_CONNECTION,"A queued task from the old connection cannot enter the new ledger");
        var reconnectServer=new AnatomyStreamLifecycle.ServerGate(epoch,4);var reconnectStart=reconnectServer.start(replacement,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();
        h.assertTrue(client.receive(newConnection,reconnectStart)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,"A new socket can begin the same server epoch from its own sequence one");
        h.succeed();
    }

    @GameTest
    public void dimensionAndReloadLifecyclesRetainOrReplaceOnlyExplicitStreams(GameTestHelper h) {
        var epoch=UUID.randomUUID();var server=new AnatomyStreamLifecycle.ServerGate(epoch,4);var client=new AnatomyStreamLifecycle.ClientLedger(4);long connection=client.beginConnection(epoch);
        UUID support=UUID.randomUUID();var overworld=identity(epoch,support,19,4,1,Identifier.parse("minecraft:overworld"));
        var start=server.start(overworld,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();client.receive(connection,start);
        var initial=server.frame(overworld,start.supportStreamLife(),1,30,30,4,30,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();client.receive(connection,initial);

        // An invalid reload allocates no replacement publication: the accepted
        // catalog/binding remains usable and a later current frame still arrives.
        var retained=server.frame(overworld,start.supportStreamLife(),2,31,30,5,31,AnatomyStreamLifecycle.FrameKind.ROOT_ONLY,true).orElseThrow();
        h.assertTrue(client.receive(connection,retained)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,
            "An invalid catalog reload retains the previous accepted stream rather than entering a binding gap");

        var catalogRetire=server.retire(overworld,start.supportStreamLife(),AnatomyStreamLifecycle.RetireReason.CATALOG_REPLACED).orElseThrow();client.receive(connection,catalogRetire);
        var reloaded=identity(epoch,support,19,5,2,Identifier.parse("minecraft:overworld"));
        var reloadStart=server.start(reloaded,1,AnatomyStreamLifecycle.StartReason.CATALOG_REPLACED).orElseThrow();
        h.assertTrue(client.receive(connection,reloadStart)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED,
            "A valid accepted catalog replacement opens an explicit new binding life");

        var afterReload=server.frame(reloaded,reloadStart.supportStreamLife(),1,40,40,0,40,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();client.receive(connection,afterReload);
        var dimensionRetire=server.retire(reloaded,reloadStart.supportStreamLife(),AnatomyStreamLifecycle.RetireReason.DIMENSION_LEAVE).orElseThrow();client.receive(connection,dimensionRetire);
        var nether=identity(epoch,support,19,5,3,Identifier.parse("minecraft:the_nether"));
        var dimensionStart=server.start(nether,1,AnatomyStreamLifecycle.StartReason.DIMENSION_ENTER).orElseThrow();
        h.assertTrue(client.receive(connection,dimensionStart)==AnatomyStreamLifecycle.ReceiveResult.ACCEPTED && client.activeStreams()==1,
            "Dimension change retires the old material life before opening the target-dimension binding");
        h.succeed();
    }

    @GameTest
    public void publicationGapFailsClosedWithoutSyntheticBootstrap(GameTestHelper h) {
        var epoch=UUID.randomUUID();var server=new AnatomyStreamLifecycle.ServerGate(epoch,2);var client=new AnatomyStreamLifecycle.ClientLedger(2);long connection=client.beginConnection(epoch);
        var identity=identity(epoch,UUID.randomUUID(),23,1,1,Identifier.parse("minecraft:overworld"));
        var start=server.start(identity,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();client.receive(connection,start);
        var skipped=new AnatomyStreamLifecycle.Frame(epoch,3,identity,start.supportStreamLife(),1,50,50,0,50,
            AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true);
        h.assertTrue(client.receive(connection,skipped)==AnatomyStreamLifecycle.ReceiveResult.GAP_FAIL_CLOSED
                && client.failed() && client.activeStreams()==0,
            "A publication gap clears active material and fails closed without a client bootstrap request");
        var current=server.frame(identity,start.supportStreamLife(),1,50,50,0,50,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).orElseThrow();
        h.assertTrue(client.receive(connection,current)==AnatomyStreamLifecycle.ReceiveResult.FAILED_CONNECTION,
            "A later ordinary frame cannot heal a failed stream gap; recovery requires the connection lifecycle");
        h.succeed();
    }

    @GameTest
    public void gateValidationCapacityCloseAndThreadOwnershipAreTransactional(GameTestHelper h) {
        var epoch=UUID.randomUUID();var gate=new AnatomyStreamLifecycle.ServerGate(epoch,2);
        var firstIdentity=identity(epoch,UUID.randomUUID(),31,1,1,Identifier.parse("minecraft:overworld"));
        var first=gate.start(firstIdentity,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();
        long beforeMalformed=gate.nextPublicationSequence();
        h.assertTrue(gate.start(firstIdentity,0,AnatomyStreamLifecycle.StartReason.INITIAL).isEmpty()
                && gate.frame(firstIdentity,first.supportStreamLife(),0,1,1,0,1,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).isEmpty()
                && gate.frame(firstIdentity,first.supportStreamLife(),1,1,1,0,1,null,true).isEmpty()
                && gate.retire(firstIdentity,first.supportStreamLife(),null).isEmpty()
                && gate.nextPublicationSequence()==beforeMalformed && gate.activeStreams()==1,
            "Malformed start, frame and retire requests are rejected before they alter a sequence or active entry");

        var secondIdentity=identity(epoch,UUID.randomUUID(),32,1,1,Identifier.parse("minecraft:overworld"));
        gate.start(secondIdentity,1,AnatomyStreamLifecycle.StartReason.INITIAL).orElseThrow();
        long beforeCapacity=gate.nextPublicationSequence();
        var excessIdentity=identity(epoch,UUID.randomUUID(),33,1,1,Identifier.parse("minecraft:overworld"));
        h.assertTrue(gate.start(excessIdentity,1,AnatomyStreamLifecycle.StartReason.INITIAL).isEmpty()
                && gate.nextPublicationSequence()==beforeCapacity && gate.activeStreams()==2,
            "Capacity rejection leaves no partially started stream and consumes no publication sequence");

        var ownershipFailure=new AtomicReference<Throwable>();
        var foreign=new Thread(()->{try {gate.activeStreams();}catch(Throwable throwable) {ownershipFailure.set(throwable);}},"anatomy-stream-lifecycle-foreign");
        foreign.start();
        try {foreign.join();}catch(InterruptedException exception) {Thread.currentThread().interrupt();throw new AssertionError("Interrupted while checking stream-gate confinement",exception);}
        h.assertTrue(ownershipFailure.get() instanceof IllegalStateException,
            "A server gate remains confined to its owner thread");

        gate.close();long beforeClosed=gate.nextPublicationSequence();
        h.assertTrue(gate.closed() && gate.activeStreams()==0
                && gate.start(firstIdentity,2,AnatomyStreamLifecycle.StartReason.RETRACK).isEmpty()
                && gate.frame(firstIdentity,first.supportStreamLife(),1,1,1,0,1,AnatomyStreamLifecycle.FrameKind.JOINT_UPDATE,true).isEmpty()
                && gate.retire(firstIdentity,first.supportStreamLife(),AnatomyStreamLifecycle.RetireReason.STOP_TRACKING).isEmpty()
                && gate.nextPublicationSequence()==beforeClosed,
            "A closed recipient gate rejects every late callback without allocating a publication sequence");
        h.succeed();
    }

    private static AnatomyStreamLifecycle.StreamIdentity identity(UUID epoch,UUID support,int entityId,long revision,long binding,Identifier dimension) {
        return new AnatomyStreamLifecycle.StreamIdentity(epoch,dimension,support,entityId,revision,Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),binding);
    }
}
