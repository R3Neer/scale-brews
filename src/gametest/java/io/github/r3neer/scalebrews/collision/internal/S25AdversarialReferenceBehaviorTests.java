package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Adversarial G4.1 reference behavior over the production accept/claim/resolve path. */
public final class S25AdversarialReferenceBehaviorTests {
    @GameTest
    public void adjacentSupportFrameCannotAliasExactReceipt(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,4,6);
        support.setNoAi(true);support.setNoGravity(true);
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(3,4,6);
        long exactFrame=700;
        try {
            record(player,player,support,1,exactFrame,1,new Vec3(.03125,0,0));
            var adjacent=new AnatomyMoveReferencePayload(false,support.getUUID(),exactFrame+1);
            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,adjacent));
            h.assertTrue(!consumed(player,player,1) && !pending(player),
                "Adjacent support frame serial must not alias or consume the exact server-issued receipt");

            var exactClaim=new AtomicReference<AnatomyTransportReceipts.Receipt>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->exactClaim.set(AnatomyTransportReceipts.claim(player,player,support.getUUID(),exactFrame)));
            h.assertTrue(exactClaim.get()!=null,
                "Rejecting an adjacent serial must leave the exact receipt independently claimable");
        } finally {
            cleanup(h.getLevel().getServer(),support,player);
        }
        h.succeed();
    }

    @GameTest
    public void oneTickContactAndRootCanContainMultipleDistinctTransportReceipts(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,4,10);
        support.setNoAi(true);support.setNoGravity(true);
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(3,4,10);
        long tick=h.getLevel().getGameTime();
        long revision=AnatomyNetworking.revision(h.getLevel().getServer());
        long contactSequence=7,rootSequence=9;
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,contactSequence);
        var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(rootSequence,tick,support.position(),0,1,GravityFrame.VANILLA);
        var material=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        try {
            player.setPos(player.position().add(.03125,0,0));
            var firstTransport=new SupportTransport(tick,1,rootSequence,new Vec3(.03125,0,0),new Vec3(.03125,0,0));
            TransportLedger.record(player,firstTransport);
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->AnatomyTransportReceipts.record(player,contact,surface,root,801,firstTransport,material,material.move(new Vec3(.03125,0,0))));

            player.setPos(player.position().add(.0625,0,0));
            var secondTransport=new SupportTransport(tick,2,rootSequence,new Vec3(.09375,0,0),new Vec3(.0625,0,0));
            TransportLedger.record(player,secondTransport);
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->AnatomyTransportReceipts.record(player,contact,surface,root,802,secondTransport,material,material.move(new Vec3(.0625,0,0))));

            var receipts=AnatomyTransportReceipts.history(player,player.getUUID());
            h.assertTrue(receipts.size()==2
                    && receipts.getFirst().tick()==receipts.getLast().tick()
                    && receipts.getFirst().contactSequence()==receipts.getLast().contactSequence()
                    && receipts.getFirst().rootFrameSequence()==receipts.getLast().rootFrameSequence()
                    && receipts.getFirst().support().equals(receipts.getLast().support())
                    && receipts.getFirst().transportSequence()!=receipts.getLast().transportSequence(),
                "Tick/contact/root/support identity is deliberately too coarse: one causal interval can contain multiple distinct transports");

            var firstClaim=new AtomicReference<AnatomyTransportReceipts.Receipt>();
            var secondClaim=new AtomicReference<AnatomyTransportReceipts.Receipt>();
            S24TrackingAuthorityTestSeam.run(player,player,1,()->{
                firstClaim.set(AnatomyTransportReceipts.claim(player,player,support.getUUID(),801));
                secondClaim.set(AnatomyTransportReceipts.claim(player,player,support.getUUID(),802));
            });
            h.assertTrue(firstClaim.get()!=null && secondClaim.get()!=null
                    && firstClaim.get().transportSequence()==1 && secondClaim.get().transportSequence()==2,
                "Exact server-issued transport identities must keep same-tick/same-contact/same-root receipts independently claimable");
        } finally {
            cleanup(h.getLevel().getServer(),support,player);
        }
        h.succeed();
    }

    @GameTest(maxTicks=90)
    public void referencePathIsExactOnceLifecycleFencedControlledAndNonApplying(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyTransportReceipts.clear(server);
        AnatomyMovementReference.clear(server);

        var support=h.spawn(EntityTypes.COW,2,4,2);
        support.setNoAi(true);support.setNoGravity(true);
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(3,4,2);

        try {
            // Happy path: receipt #1 is already applied. Only later transport #2 may rebase
            // the client's absolute movement; receipt #1 itself must never be applied twice.
            Vec3 firstDelta=new Vec3(.125,0,0);
            long frameOne=11;
            var firstReceipt=record(player,player,support,1,frameOne,1,firstDelta);
            Vec3 afterReceipt=player.position();
            AABB afterReceiptBox=player.getBoundingBox();
            var reference=new AnatomyMoveReferencePayload(false,support.getUUID(),frameOne);

            long transportGenerationAtAccept=AnatomyMovement.transportGeneration(player);
            var currentTrackingAtAccept=new java.util.concurrent.atomic.AtomicLong();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->currentTrackingAtAccept.set(AnatomyRuntime.trackingGeneration(player,player)));
            h.assertTrue(server.isSameThread() && !player.isRemoved() && player.getRootVehicle()==player
                    && firstReceipt.epoch().equals(AnatomyNetworking.epoch(server))
                    && firstReceipt.catalogRevision()==AnatomyNetworking.revision(server)
                    && firstReceipt.dimension().equals(player.level().dimension().identifier())
                    && firstReceipt.bodyNetworkId()==player.getId() && firstReceipt.body().equals(player.getUUID())
                    && firstReceipt.trackingGeneration()==currentTrackingAtAccept.get()
                    && firstReceipt.support().equals(support.getUUID())
                    && supportFrame(player,player,1)==frameOne
                    && !AnatomyTransportReceipts.saturated(player,player.getUUID(),firstReceipt.tick())
                    && !consumed(player,player,1) && !pending(player),
                "Fixture must satisfy every production accept/claim precondition before the reference is submitted: receipt="
                    +firstReceipt+" tracking="+currentTrackingAtAccept.get()+" supportFrame="+supportFrame(player,player,1)
                    +" active="+AnatomyMovement.active(player)
                    +" pending="+pending(player));
            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,reference));

            h.assertTrue(consumed(player,player,1) && pending(player),
                "Valid accept must consume exactly the matched receipt and stage one pending movement cursor");
            var postAcceptClaim=new AtomicReference<AnatomyTransportReceipts.Receipt>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->postAcceptClaim.set(AnatomyTransportReceipts.claim(player,player,support.getUUID(),frameOne)));
            h.assertTrue(postAcceptClaim.get()==null,
                "Consumed receipt must reject a second direct claim without deleting historical evidence");

            h.assertTrue(player.position().equals(afterReceipt) && player.getBoundingBox().equals(afterReceiptBox)
                    && TransportLedger.current(player).sequence()==1
                    && AnatomyTransportReceipts.history(player,player.getUUID()).size()==1,
                "Accepting a valid metadata reference must not reapply transport, move the body or delete its receipt");

            Vec3 secondDelta=new Vec3(.25,0,0);
            applyTransportOnly(player,2,secondDelta);
            var postReceiptWindow=TransportLedger.since(player,1);
            h.assertTrue(postReceiptWindow.contiguous() && postReceiptWindow.latestSequence()==2
                    && postReceiptWindow.appliedDelta().equals(secondDelta)
                    && AnatomyMovement.transportGeneration(player)==transportGenerationAtAccept,
                "Fixture must expose one contiguous post-receipt transport without changing transport lifecycle: window="
                    +postReceiptWindow+" generation="+AnatomyMovement.transportGeneration(player)
                    +" acceptedGeneration="+transportGenerationAtAccept);
            Vec3 serverAfterSecond=player.position();
            var observedTracking=new java.util.concurrent.atomic.AtomicLong();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->observedTracking.set(AnatomyRuntime.trackingGeneration(player,player)));
            h.assertTrue(pending(player) && player.getRootVehicle()==player
                    && !player.isRemoved() && firstReceipt.bodyNetworkId()==player.getId()
                    && firstReceipt.body().equals(player.getUUID())
                    && firstReceipt.epoch().equals(AnatomyNetworking.epoch(server))
                    && firstReceipt.catalogRevision()==AnatomyNetworking.revision(server)
                    && firstReceipt.dimension().equals(player.level().dimension().identifier())
                    && firstReceipt.trackingGeneration()==observedTracking.get(),
                "Pre-resolve identity fences must all remain current: receipt="+firstReceipt
                    +" currentTracking="+observedTracking.get()+" pending="+pending(player)
                    +" owns="+AnatomyRuntime.owns(player));
            var resolved=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->resolved.set(AnatomyMovementReference.resolve(player,player,afterReceipt)));
            h.assertTrue(resolved.get().equals(serverAfterSecond),
                "Valid reference must rebase only transport applied after the claimed receipt: expected="
                    +serverAfterSecond+" resolved="+resolved.get());
            h.assertTrue(player.position().equals(serverAfterSecond)
                    && TransportLedger.current(player).sequence()==2
                    && AnatomyTransportReceipts.history(player,player.getUUID()).getFirst().equals(firstReceipt),
                "Resolving a reference is metadata-only and must not mutate position, transport cursor or receipt evidence");

            // Exactly once. If the consumed receipt were accepted again, since(seq=1) would add
            // the already-known seq=2 delta to this probe.
            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,reference));
            var replayResolved=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->replayResolved.set(AnatomyMovementReference.resolve(player,player,serverAfterSecond)));
            h.assertTrue(replayResolved.get().equals(serverAfterSecond),
                "Identical reference replay must not regain an already consumed receipt");

            // Plausible but invented support endpoint cannot create authority even while a
            // fresh, otherwise valid receipt for this support is still unconsumed.
            long frameTracking=12;
            record(player,player,support,1,frameTracking,3,new Vec3(.0625,0,0));
            Vec3 afterFreshReceipt=player.position();
            var fake=new AnatomyMoveReferencePayload(false,support.getUUID(),999_999);
            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,fake));
            h.assertTrue(!consumed(player,player,3) && !pending(player),
                "Invented support-frame cursor must not consume or stage a different valid server receipt");
            var fakeResolved=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->fakeResolved.set(AnatomyMovementReference.resolve(player,player,afterFreshReceipt)));
            h.assertTrue(fakeResolved.get().equals(afterFreshReceipt),
                "Reference without an exact server-issued receipt must leave movement unchanged");

            // Tracking generation is live authority, not historical receipt presence.
            var staleTracking=new AnatomyMoveReferencePayload(false,support.getUUID(),frameTracking);
            S24TrackingAuthorityTestSeam.runOwned(player,player,2,
                ()->AnatomyMovementReference.accept(player,staleTracking));
            applyTransportOnly(player,4,new Vec3(.03125,0,0));
            Vec3 afterTrackingAdvance=player.position();
            var staleTrackingResolved=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(player,player,2,
                ()->staleTrackingResolved.set(AnatomyMovementReference.resolve(player,player,afterTrackingAdvance)));
            h.assertTrue(staleTrackingResolved.get().equals(afterTrackingAdvance),
                "Receipt from retired tracking generation must not stage or rebase a movement reference");

            // Saturated ticks are historical evidence but never admissible reference authority.
            var saturated=(ServerPlayer)h.makeMockServerPlayerInLevel();saturated.setPos(5,4,2);
            long saturatedFrame=100;
            for(int sequence=1;sequence<=AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK;sequence++)
                record(saturated,saturated,support,1,saturatedFrame+sequence-1,sequence,new Vec3(.015625,0,0));
            recordUnverified(saturated,saturated,support,1,
                saturatedFrame+AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK,
                AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK+1,new Vec3(.015625,0,0));
            long saturatedTick=h.getLevel().getGameTime();
            h.assertTrue(AnatomyTransportReceipts.saturated(saturated,saturated.getUUID(),saturatedTick),
                "Fixture must saturate the exact recipient/body/tick before testing reference rejection");
            var saturatedReference=new AnatomyMoveReferencePayload(false,support.getUUID(),saturatedFrame);
            Vec3 saturatedProbe=saturated.position();
            S24TrackingAuthorityTestSeam.runOwned(saturated,saturated,1,
                ()->AnatomyMovementReference.accept(saturated,saturatedReference));
            var saturatedResolved=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(saturated,saturated,1,
                ()->saturatedResolved.set(AnatomyMovementReference.resolve(saturated,saturated,saturatedProbe)));
            h.assertTrue(saturatedResolved.get().equals(saturatedProbe),
                "Reference into a saturated receipt tick must fail closed");

            // Receipt existence is not control authority: both passengers receive a receipt, but
            // only the controlling passenger may use the vehicle reference.
            var controller=(ServerPlayer)h.makeMockServerPlayerInLevel();
            var passenger=(ServerPlayer)h.makeMockServerPlayerInLevel();passenger.setGameMode(GameType.SURVIVAL);
            var boat=h.spawn(EntityTypes.OAK_BOAT,7,4,2);
            h.assertTrue(controller.startRiding(boat,true,true) && passenger.startRiding(boat,true,true)
                    && boat.getControllingPassenger()==controller,
                "Controlled-vehicle fixture requires one controller and one non-controlling passenger");
            long vehicleFrame=200;
            recordForTwoRecipients(controller,passenger,boat,support,1,vehicleFrame,1,new Vec3(.125,0,0));
            h.assertTrue(AnatomyTransportReceipts.history(controller,boat.getUUID()).size()==1
                    && AnatomyTransportReceipts.history(passenger,boat.getUUID()).size()==1,
                "Both passengers must possess server-issued provenance before control-authority test");

            var vehicleReference=new AnatomyMoveReferencePayload(true,support.getUUID(),vehicleFrame);
            S24TrackingAuthorityTestSeam.runOwned(passenger,boat,1,
                ()->AnatomyMovementReference.accept(passenger,vehicleReference));
            var passengerClaim=new AtomicReference<AnatomyTransportReceipts.Receipt>();
            S24TrackingAuthorityTestSeam.run(passenger,boat,1,
                ()->passengerClaim.set(AnatomyTransportReceipts.claim(passenger,boat,support.getUUID(),vehicleFrame)));
            h.assertTrue(passengerClaim.get()!=null,
                "Non-controller reference must be rejected before consuming its otherwise valid receipt");

            S24TrackingAuthorityTestSeam.runOwned(controller,boat,1,
                ()->AnatomyMovementReference.accept(controller,vehicleReference));
            var controllerClaim=new AtomicReference<AnatomyTransportReceipts.Receipt>();
            S24TrackingAuthorityTestSeam.run(controller,boat,1,
                ()->controllerClaim.set(AnatomyTransportReceipts.claim(controller,boat,support.getUUID(),vehicleFrame)));
            h.assertTrue(controllerClaim.get()==null,
                "Controller reference must consume exactly its own receipt authority");

            // Per-recipient rate budget: sixteen rejected metadata attempts may consume only
            // the attempt budget, never a valid receipt. The seventeenth attempt in the same tick
            // must fail closed before claim/consumption.
            var budgetPlayer=(ServerPlayer)h.makeMockServerPlayerInLevel();budgetPlayer.setPos(8,4,2);
            long budgetFrame=400;
            record(budgetPlayer,budgetPlayer,support,1,budgetFrame,1,new Vec3(.03125,0,0));
            for(int attempt=0;attempt<AnatomyMovementReference.MAX_REFERENCES_PER_TICK;attempt++) {
                var miss=new AnatomyMoveReferencePayload(false,support.getUUID(),400_000L+attempt);
                S24TrackingAuthorityTestSeam.runOwned(budgetPlayer,budgetPlayer,1,
                    ()->AnatomyMovementReference.accept(budgetPlayer,miss));
            }
            h.assertTrue(!pending(budgetPlayer) && !consumed(budgetPlayer,budgetPlayer,1),
                "Rejected references may spend the rate budget but must not consume unrelated receipt authority");
            var budgetValid=new AnatomyMoveReferencePayload(false,support.getUUID(),budgetFrame);
            S24TrackingAuthorityTestSeam.runOwned(budgetPlayer,budgetPlayer,1,
                ()->AnatomyMovementReference.accept(budgetPlayer,budgetValid));
            var budgetClaim=new AtomicReference<AnatomyTransportReceipts.Receipt>();
            S24TrackingAuthorityTestSeam.run(budgetPlayer,budgetPlayer,1,
                ()->budgetClaim.set(AnatomyTransportReceipts.claim(budgetPlayer,budgetPlayer,support.getUUID(),budgetFrame)));
            h.assertTrue(budgetClaim.get()!=null && !pending(budgetPlayer),
                "Attempt beyond MAX_REFERENCES_PER_TICK must fail before consuming/staging the valid receipt");

            // Catalog revision is part of the claim and staged resolve identity.
            long currentRevision=AnatomyNetworking.revision(server);
            long frameRevision=13;
            record(player,player,support,1,frameRevision,5,new Vec3(.03125,0,0));
            AnatomyNetworking.acceptCatalogRevision(server,currentRevision+1);
            var staleRevision=new AnatomyMoveReferencePayload(false,support.getUUID(),frameRevision);
            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,staleRevision));
            applyTransportOnly(player,6,new Vec3(.015625,0,0));
            Vec3 afterRevisionAdvance=player.position();
            var staleRevisionResolved=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->staleRevisionResolved.set(AnatomyMovementReference.resolve(player,player,afterRevisionAdvance)));
            h.assertTrue(staleRevisionResolved.get().equals(afterRevisionAdvance),
                "Receipt from a prior catalog revision must not stage or rebase a movement reference");

            // Pending reference TTL is independent from receipt TTL. A valid cursor may be
            // consumed/staged, but if its following vanilla movement packet arrives too late the
            // staging must fail closed rather than rebase an unrelated later packet.
            var pendingPlayer=(ServerPlayer)h.makeMockServerPlayerInLevel();pendingPlayer.setPos(9,4,2);
            long pendingFrame=500;
            record(pendingPlayer,pendingPlayer,support,1,pendingFrame,1,new Vec3(.03125,0,0));
            Vec3 pendingAbsolute=pendingPlayer.position();
            var pendingReference=new AnatomyMoveReferencePayload(false,support.getUUID(),pendingFrame);
            S24TrackingAuthorityTestSeam.runOwned(pendingPlayer,pendingPlayer,1,
                ()->AnatomyMovementReference.accept(pendingPlayer,pendingReference));
            h.assertTrue(pending(pendingPlayer),
                "Valid pending-TTL fixture reference must stage before the delay");
            applyTransportOnly(pendingPlayer,2,new Vec3(.0625,0,0));

            h.runAfterDelay(AnatomyMovementReference.PENDING_TICKS+1,()->{
                try {
                    var expiredPending=new AtomicReference<Vec3>();
                    S24TrackingAuthorityTestSeam.run(pendingPlayer,pendingPlayer,1,
                        ()->expiredPending.set(AnatomyMovementReference.resolve(pendingPlayer,pendingPlayer,pendingAbsolute)));
                    h.assertTrue(expiredPending.get().equals(pendingAbsolute) && !pending(pendingPlayer),
                        "Pending reference older than PENDING_TICKS must not rebase a later vanilla movement packet");

                    // Receipt TTL owner-level check. The receipt remains untouched until it naturally expires.
                    var ttlPlayer=(ServerPlayer)h.makeMockServerPlayerInLevel();ttlPlayer.setPos(10,4,2);
                    long ttlFrame=300;
                    record(ttlPlayer,ttlPlayer,support,1,ttlFrame,1,new Vec3(.03125,0,0));
                    h.runAfterDelay(AnatomyTransportReceipts.HISTORY_TICKS,()->{
                        try {
                            var expired=new AtomicReference<AnatomyTransportReceipts.Receipt>();
                            S24TrackingAuthorityTestSeam.run(ttlPlayer,ttlPlayer,1,
                                ()->expired.set(AnatomyTransportReceipts.claim(ttlPlayer,ttlPlayer,support.getUUID(),ttlFrame)));
                            h.assertTrue(expired.get()==null,
                                "Receipt at or beyond HISTORY_TICKS must be pruned before reference claim");
                            cleanup(server,support,player,saturated,controller,passenger,boat,budgetPlayer,pendingPlayer,ttlPlayer);
                            h.succeed();
                        } catch(Throwable failure) {
                            cleanup(server,support,player,saturated,controller,passenger,boat,budgetPlayer,pendingPlayer,ttlPlayer);
                            throw failure;
                        }
                    });
                } catch(Throwable failure) {
                    cleanup(server,support,player,saturated,controller,passenger,boat,budgetPlayer,pendingPlayer);
                    throw failure;
                }
            });
        } catch(Throwable failure) {
            cleanup(server,support,player);
            throw failure;
        }
    }

    private static AnatomyTransportReceipts.Receipt record(ServerPlayer recipient,Entity body,LivingEntity support,
            long trackingGeneration,long supportFrameSerial,long sequence,Vec3 applied) {
        recordUnverified(recipient,body,support,trackingGeneration,supportFrameSerial,sequence,applied);
        return AnatomyTransportReceipts.history(recipient,body.getUUID()).stream()
            .filter(receipt->receipt.transportSequence()==sequence).findFirst()
            .orElseThrow(()->new AssertionError("Fixture failed to issue referenceable receipt sequence "+sequence));
    }

    private static void recordUnverified(ServerPlayer recipient,Entity body,LivingEntity support,
            long trackingGeneration,long supportFrameSerial,long sequence,Vec3 applied) {
        body.setPos(body.position().add(applied));
        long tick=body.level().getGameTime();
        long revision=AnatomyNetworking.revision(body.level().getServer());
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,sequence);
        var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(sequence,tick,support.position(),0,1,GravityFrame.VANILLA);
        var materialBefore=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        var materialAfter=materialBefore.move(applied);
        var transport=new SupportTransport(tick,sequence,root.sequence(),applied,applied);
        TransportLedger.record(body,transport);
        S24TrackingAuthorityTestSeam.run(recipient,body,trackingGeneration,
            ()->AnatomyTransportReceipts.record(body,contact,surface,root,supportFrameSerial,transport,materialBefore,materialAfter));
    }

    private static void recordForTwoRecipients(ServerPlayer first,ServerPlayer second,Entity body,LivingEntity support,
            long generation,long supportFrameSerial,long sequence,Vec3 applied) {
        body.setPos(body.position().add(applied));
        long tick=body.level().getGameTime();
        long revision=AnatomyNetworking.revision(body.level().getServer());
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,sequence);
        var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(sequence,tick,support.position(),0,1,GravityFrame.VANILLA);
        var materialBefore=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        var materialAfter=materialBefore.move(applied);
        var transport=new SupportTransport(tick,sequence,root.sequence(),applied,applied);
        TransportLedger.record(body,transport);
        S24TrackingAuthorityTestSeam.run(first,body,generation,
            ()->S24TrackingAuthorityTestSeam.run(second,body,generation,
                ()->AnatomyTransportReceipts.record(body,contact,surface,root,supportFrameSerial,transport,materialBefore,materialAfter)));
    }

    private static void applyTransportOnly(Entity body,long sequence,Vec3 applied) {
        body.setPos(body.position().add(applied));
        long tick=body.level().getGameTime();
        var current=TransportLedger.current(body);
        long rootSequence=current==null?sequence:Math.max(sequence,current.rootFrameSequence());
        TransportLedger.record(body,new SupportTransport(tick,sequence,rootSequence,applied,applied));
    }



    @SuppressWarnings("unchecked")
    private static long supportFrame(ServerPlayer recipient,Entity body,long sequence) {
        try {
            var historiesField=AnatomyTransportReceipts.class.getDeclaredField("HISTORIES");
            historiesField.setAccessible(true);
            var histories=(Map<ServerPlayer,Map<UUID,Object>>)historiesField.get(null);
            var roots=histories.get(recipient);if(roots==null)return 0;
            var history=roots.get(body.getUUID());if(history==null)return 0;
            var field=history.getClass().getDeclaredField("supportFrameSerials");
            field.setAccessible(true);
            return ((Map<Long,Long>)field.get(history)).getOrDefault(sequence,0L);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect receipt support-frame metadata",failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean consumed(ServerPlayer recipient,Entity body,long sequence) {
        try {
            var historiesField=AnatomyTransportReceipts.class.getDeclaredField("HISTORIES");
            historiesField.setAccessible(true);
            var histories=(Map<ServerPlayer,Map<UUID,Object>>)historiesField.get(null);
            var roots=histories.get(recipient);if(roots==null)return false;
            var history=roots.get(body.getUUID());if(history==null)return false;
            var consumedField=history.getClass().getDeclaredField("consumedTransportSequences");
            consumedField.setAccessible(true);
            return ((java.util.Set<Long>)consumedField.get(history)).contains(sequence);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect receipt consumption metadata",failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean pending(ServerPlayer recipient) {
        try {
            var statesField=AnatomyMovementReference.class.getDeclaredField("STATES");
            statesField.setAccessible(true);
            var states=(Map<ServerPlayer,Object>)statesField.get(null);
            var state=states.get(recipient);if(state==null)return false;
            var pendingField=state.getClass().getDeclaredField("pending");
            pendingField.setAccessible(true);
            return pendingField.get(state)!=null;
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect movement-reference staging",failure);
        }
    }

    private static void cleanup(net.minecraft.server.MinecraftServer server,Entity... entities) {
        for(var entity:entities)if(entity!=null && !entity.isRemoved())entity.discard();
        AnatomyTransportReceipts.clear(server);
        AnatomyMovementReference.clear(server);
    }
}
