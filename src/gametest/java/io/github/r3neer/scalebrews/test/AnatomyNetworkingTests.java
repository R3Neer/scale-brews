package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.platform.anatomy.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyPosePayload;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyFrameHistory;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyContactPayload;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyContactInbox;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyCatalogPayload;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyCatalogTransfer;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyApi;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyMode;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyClientSession;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyFilter;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyTransportReceipts;
import io.github.r3neer.scalebrews.platform.anatomy.ConvexBox;
import io.github.r3neer.scalebrews.platform.anatomy.GravityFrame;
import io.github.r3neer.scalebrews.platform.anatomy.GeometryProvider;
import io.github.r3neer.scalebrews.platform.anatomy.ModelGeometry;
import io.github.r3neer.scalebrews.platform.anatomy.ModelGeometryProvider;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyNetworking;
import io.github.r3neer.scalebrews.platform.anatomy.PoseProvider;
import io.github.r3neer.scalebrews.platform.anatomy.SurfaceContact;
import io.github.r3neer.scalebrews.platform.anatomy.SupportTransport;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Packet/history regressions owned by the anatomy networking boundary. */
public final class AnatomyNetworkingTests {
    @GameTest
    public void catalogAnnouncementOwnsOnlyAnnouncedSessions(GameTestHelper h) {
        var transfer=new AnatomyCatalogTransfer();var epoch=UUID.randomUUID();
        h.assertTrue(transfer.mode()==AnatomyMode.DISABLED && !transfer.announced() && !transfer.binding() && !transfer.ready(),
            "No catalog announcement leaves a no-feature server session disabled");
        var fragment=new AnatomyCatalogPayload(epoch,AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),1,0,2,
            AnatomyCatalogPayload.CHUNK+1,"0".repeat(64),new byte[AnatomyCatalogPayload.CHUNK]);
        h.assertTrue(!transfer.accept(fragment) && transfer.mode()==AnatomyMode.BINDING && transfer.announced() && transfer.binding() && !transfer.ready(),
            "First compatible server catalog fragment enters binding before acceptance");
        transfer.rejectPending();
        h.assertTrue(transfer.mode()==AnatomyMode.DISABLED && !transfer.announced() && !transfer.binding() && !transfer.ready(),
            "Rejected initial catalog returns to disabled rather than owning an ordinary server");
        var accepted=AnatomyCatalogTransfer.encode(epoch,1,Map.of(),Map.of()).getFirst();
        h.assertTrue(transfer.accept(accepted) && transfer.mode()==AnatomyMode.READY && transfer.ready() && !transfer.binding(),
            "Complete compatible catalog is the only transition to ready");
        var replacement=new AnatomyCatalogPayload(epoch,AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),2,0,2,
            AnatomyCatalogPayload.CHUNK+1,"0".repeat(64),new byte[AnatomyCatalogPayload.CHUNK]);
        h.assertTrue(!transfer.accept(replacement) && transfer.mode()==AnatomyMode.BINDING && transfer.binding(),
            "A newer announced catalog binds while the prior accepted snapshot remains atomic");
        transfer.rejectPending();
        h.assertTrue(transfer.mode()==AnatomyMode.READY && transfer.ready() && transfer.revision()==1 && transfer.snapshot().revision()==1,
            "Invalid replacement retains the previous ready revision instead of degrading it");
        var resetForWorldOrReconnect=new AnatomyCatalogTransfer();
        h.assertTrue(resetForWorldOrReconnect.mode()==AnatomyMode.DISABLED && !resetForWorldOrReconnect.announced() && !resetForWorldOrReconnect.ready(),
            "World/reconnect reset cannot inherit a prior host's ownership or catalog");
        h.succeed();
    }
    @GameTest
    public void clientSessionScopesCatalogToConnectionAndTemporalStateToLevel(GameTestHelper h) {
        var session=new AnatomyClientSession();var firstLevel=new Object();var secondLevel=new Object();var epoch=UUID.randomUUID();
        h.assertTrue(session.useLevel(firstLevel) && session.mode(firstLevel)==AnatomyMode.DISABLED,
            "A fresh client level has no anatomy ownership before a server announcement");
        var catalog=session.catalog();
        h.assertTrue(catalog.accept(AnatomyCatalogTransfer.encode(epoch,1,Map.of(),Map.of()).getFirst()),
            "Connection receives its authoritative catalog before a level transition");
        h.assertTrue(session.mode(firstLevel)==AnatomyMode.READY && session.mode(secondLevel)==AnatomyMode.DISABLED,
            "Ready catalog applies only to the active client level");
        h.assertTrue(session.useLevel(secondLevel) && session.catalog()==catalog && session.mode(secondLevel)==AnatomyMode.READY,
            "Dimension transition retains the same connection catalog without a server resend");
        h.assertTrue(session.mode(firstLevel)==AnatomyMode.DISABLED,
            "Old level loses temporal anatomy state after the transition");
        session.resetConnection();
        h.assertTrue(session.catalog()!=catalog && session.mode(secondLevel)==AnatomyMode.DISABLED,
            "Disconnect/host reset discards catalog ownership rather than carrying it to a new connection");
        h.succeed();
    }
    @GameTest
    public void poseHistoryPreservesV3ChannelsAtEndpointsAndMidpoint(GameTestHelper h) {
        var epoch=UUID.randomUUID();var body=UUID.randomUUID();
        var model=Identifier.parse("minecraft:bee");
        var provider=Identifier.parse("scalebrews:bee");
        var dimension=Identifier.parse("minecraft:overworld");
        var before=new PoseProvider.Inputs(1,.2f,10,5,2,true,Map.of("flap",2f,"roll",.2f,"crouching",0f,"unknown_mode",3f));
        var after=new PoseProvider.Inputs(3,.8f,11,15,4,true,Map.of("flap",6f,"roll",.8f,"crouching",1f,"unknown_mode",7f));
        var history=new AnatomyPoseHistory();
        history.accept(new AnatomyPosePayload(epoch,1,dimension,7,body,model,provider,10,before,Vec3.ZERO,0,1));
        history.accept(new AnatomyPosePayload(epoch,1,dimension,7,body,model,provider,11,after,new Vec3(1,0,0),10,2));
        var atStart=history.sample(10).inputs();
        var mid=history.sample(10.5).inputs();
        var atEnd=history.sample(11).inputs();
        h.assertTrue(atStart.channels().equals(before.channels()),"t0 preserves every authoritative channel");
        h.assertTrue(atEnd.channels().equals(after.channels()),"t1 preserves every authoritative channel");
        h.assertTrue(Math.abs(mid.channel("flap",-1)-4)<1e-6 && Math.abs(mid.channel("roll",-1)-.5)<1e-6,
            "declared continuous channels interpolate at t.5");
        h.assertTrue(mid.flag("crouching") && mid.channel("unknown_mode",-1)==7,
            "flags and unknown channels use a nearest authoritative sample");
        var segment=history.segment(10.5);
        h.assertTrue(segment.before().inputs().channels().equals(before.channels()) && segment.after().inputs().channels().equals(after.channels()),
            "joint trajectory retains complete endpoint channels");
        h.succeed();
    }
    @GameTest
    public void causalFramesKeepRootEventsSeparateFromJointSamples(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var firstInputs=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var nextInputs=new PoseProvider.Inputs(1,.5f,11,20,0,true,Map.of("flap",.25f));
        var first=new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,1,10,10,4,10,9,true,firstInputs,Vec3.ZERO,0,1,net.minecraft.core.Direction.DOWN);
        var rootOnly=new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,2,11,10,5,11,9,true,firstInputs,new Vec3(1,0,0),15,1,net.minecraft.core.Direction.DOWN);
        var jointsOnExistingRoot=new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,3,12,11,5,11,9,true,nextInputs,new Vec3(1,0,0),15,1,net.minecraft.core.Direction.DOWN);
        var frames=new AnatomyFrameHistory();var joints=new AnatomyPoseHistory();
        h.assertTrue(frames.accept(first) && joints.accept(first),"Initial endpoint installs its joint sample");
        h.assertTrue(frames.accept(rootOnly) && !joints.accept(rootOnly) && frames.current().frameSerial()==2
                && frames.root().sequence()==5 && joints.current().jointSampleTick()==10,
            "A newer root with the same joints advances only the causal-frame store");
        h.assertTrue(frames.accept(jointsOnExistingRoot) && joints.accept(jointsOnExistingRoot) && frames.current().frameSerial()==3
                && frames.root().sequence()==5 && joints.current().jointSampleTick()==11 && joints.current().inputs().equals(nextInputs),
            "A new joint sample on an unchanged root advances both stores without inventing a root change");
        h.succeed();
    }
    @GameTest
    public void unavailableCausalFrameClearsGeometryWithoutRollingBackWatermark(GameTestHelper h) {
        var epoch=UUID.randomUUID();var entity=UUID.randomUUID();var dimension=h.getLevel().dimension().identifier();
        var model=Identifier.parse("minecraft:cow");var provider=Identifier.parse("scalebrews:static");
        var valid=new PoseProvider.Inputs(0,0,10,0,0,true,Map.of());
        var unknown=new PoseProvider.Inputs(0,0,11,0,0,false,Map.of());
        var available=new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,1,10,10,4,10,9,true,valid,Vec3.ZERO,0,1,net.minecraft.core.Direction.DOWN);
        var unavailable=new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,2,11,11,4,10,9,false,unknown,Vec3.ZERO,0,1,net.minecraft.core.Direction.DOWN);
        var restored=new AnatomyPosePayload(epoch,3,dimension,7,entity,model,provider,3,12,12,5,12,9,true,valid,new Vec3(1,0,0),15,1,net.minecraft.core.Direction.DOWN);
        var frames=new AnatomyFrameHistory();var joints=new AnatomyPoseHistory();
        h.assertTrue(frames.accept(available) && joints.accept(available) && frames.endpoint().orElseThrow().availability()==GeometryProvider.Availability.AVAILABLE,
            "Initial available frame exposes a causal geometry endpoint");
        h.assertTrue(frames.accept(unavailable) && frames.endpoint().orElseThrow().availability()==GeometryProvider.Availability.UNAVAILABLE
                && joints.current().jointSampleTick()==10,
            "A newer unavailable endpoint advances the frame watermark but cannot replace a valid joint history with an unknown pose");
        h.assertTrue(!frames.accept(available),"A delayed available endpoint cannot roll back the unavailable frame watermark");
        h.assertTrue(frames.accept(restored) && joints.accept(restored) && frames.endpoint().orElseThrow().availability()==GeometryProvider.Availability.AVAILABLE
                && joints.current().jointSampleTick()==12,
            "A later server-valid frame restores geometry only with a new causal serial and joint sample");
        h.succeed();
    }
    @GameTest
    public void contactTrackingGenerationRejectsDelayedPreviousTracking(GameTestHelper h) {
        var epoch=UUID.randomUUID();var body=UUID.randomUUID();long tick=h.getLevel().getGameTime();
        var dimension=Identifier.parse("minecraft:overworld");var support=UUID.randomUUID();
        var normal=new Vec3(0,1,0);
        var old=new AnatomyContactPayload(epoch,1,dimension,7,body,1,1,tick,8,support,"body",3,Vec3.ZERO,normal);
        var clear=AnatomyContactPayload.clear(epoch,1,dimension,7,body,1,2,tick+1);
        long restartedGeneration=AnatomyRuntime.nextTrackingGeneration(old.trackingGeneration());
        var restarted=new AnatomyContactPayload(epoch,1,dimension,7,body,restartedGeneration,1,tick+2,8,support,"body",3,Vec3.ZERO,normal);
        var delayed=new AnatomyContactPayload(epoch,1,dimension,7,body,1,3,tick+3,8,support,"body",3,Vec3.ZERO,normal);
        var inbox=new AnatomyContactInbox();
        h.assertTrue(inbox.accept(dimension,old) && inbox.pending().containsKey(body),"Initial present contact enters the pending queue");
        inbox.consume(old);
        h.assertTrue(inbox.accept(dimension,clear),"Clear advances the accepted watermark after confirmation consumed its queue item");
        inbox.consume(clear);
        h.assertTrue(!inbox.accept(dimension,old),"Old present packet cannot restore a cleared contact after its queue item was consumed");
        h.assertTrue(!inbox.accept(Identifier.parse("minecraft:the_nether"),restarted),"Contact from the old world is rejected while its declared dimension is not active");
        h.assertTrue(inbox.accept(dimension,restarted),"Return to the same dimension requires the new authoritative tracking generation");
        inbox.consume(restarted);
        h.assertTrue(!inbox.accept(dimension,delayed),"Delayed prior-world tracking cannot overwrite a return-generation contact after confirmation");
        h.succeed();
    }
    @GameTest
    public void stationaryContactHeartbeatKeepsAcceptedWatermarkFresh(GameTestHelper h) {
        var epoch=UUID.randomUUID();var body=UUID.randomUUID();var support=UUID.randomUUID();
        var dimension=h.getLevel().dimension().identifier();var normal=new Vec3(0,1,0);var inbox=new AnatomyContactInbox();
        long first=h.getLevel().getGameTime();
        for(long elapsed=0;elapsed<=120;elapsed+=AnatomyRuntime.CONTACT_HEARTBEAT_TICKS) {
            var heartbeat=new AnatomyContactPayload(epoch,1,dimension,7,body,1,elapsed/AnatomyRuntime.CONTACT_HEARTBEAT_TICKS+1,first+elapsed,
                8,support,"body",3,Vec3.ZERO,normal);
            h.assertTrue(inbox.accept(dimension,heartbeat),"Monotonic stationary contact heartbeat is accepted");
            inbox.consume(heartbeat);inbox.prune(first+elapsed,100);
        }
        h.assertTrue(!inbox.accept(dimension,new AnatomyContactPayload(epoch,1,dimension,7,body,1,1,first,8,support,"body",3,Vec3.ZERO,normal)),
            "A stale pre-heartbeat packet cannot restore a quiet contact after more than 100 ticks");
        h.succeed();
    }
    @GameTest
    public void posePayloadRetainsOneCapturedAuthorityFrame(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setPos(2,20,2);support.yBodyRot=15;
        support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(1.25);support.refreshDimensions();
        AnatomyMovement.gravity(support,new GravityFrame(net.minecraft.core.Direction.EAST));
        var inputs=new PoseProvider.Inputs(3,.4f,7,18,4,true,Map.of("flap",.5f));
        var geometry=new ModelGeometry(1,"test:authority_frame","1",java.util.List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new org.joml.Matrix4f()))),
            java.util.List.of(new ModelGeometry.Piece("body","root",java.util.List.of(0d,0d,0d),java.util.List.of(1d,1d,1d),null)),ModelGeometry.values(new org.joml.Matrix4f()));
        var evaluator=new ModelGeometryProvider(geometry,(model,pose)->java.util.Optional.of(Map.of()),AnatomyFilter.DEFAULT,7);
        long tick=h.getLevel().getGameTime();evaluator.pose(support,inputs);evaluator.tick(support,tick);
        var frame=evaluator.authoritativeFrame(support).orElseThrow();var expected=frame.sample();
        support.setPos(9,25,9);support.yBodyRot=125;
        support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(2);support.refreshDimensions();
        AnatomyMovement.gravity(support,new GravityFrame(net.minecraft.core.Direction.DOWN));
        var root=new AnatomyMovement.RootFrame(17,tick,expected.origin(),expected.yaw(),expected.scale(),expected.gravity());
        var snapshot=evaluator.sampleAt(support,expected).orElseThrow();
        var endpoint=new GeometryProvider.CausalEndpoint(23,tick+1,frame.tick(),root,expected,GeometryProvider.Availability.AVAILABLE);
        var packet=AnatomyNetworking.posePayload(UUID.randomUUID(),7,h.getLevel().dimension().identifier(),support.getId(),support.getUUID(),
            Identifier.parse("test:authority_frame"),Identifier.parse("test:static"),5,endpoint);
        h.assertTrue(packet.revision()==7 && packet.entityId()==support.getId() && packet.entity().equals(support.getUUID())
                && packet.model().equals(Identifier.parse("test:authority_frame")) && packet.provider().equals(Identifier.parse("test:static"))
                && packet.frameSerial()==23 && packet.authorityTick()==tick+1 && packet.jointSampleTick()==frame.tick() && packet.rootFrameSequence()==17
                && packet.rootFrameTick()==tick && packet.bindingGeneration()==5 && packet.inputs().equals(expected.inputs()) && packet.origin().equals(expected.origin())
                && packet.yaw()==expected.yaw() && packet.scale()==expected.scale() && packet.gravity()==expected.gravity().down(),
            "Pose wire packet preserves identity and one immutable authority endpoint after live TRS/gravity mutate");
        h.assertTrue(!packet.origin().equals(support.position()) && packet.yaw()!=support.yBodyRot && packet.scale()!=support.getScale()
                && packet.gravity()!=AnatomyMovement.gravity(support).down(),
            "Payload never combines captured inputs with later entity root state");
        support.discard();h.succeed();
    }
    @GameTest
    public void transportReceiptRecordsOnlyAppliedBaselineProvenance(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        var body=(net.minecraft.server.level.ServerPlayer)h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        body.setPos(3,20,2);
        long tick=h.getLevel().getGameTime();var delta=new Vec3(.2,0,0);
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"body",7,normal,3);
        var surface=new SurfaceContact(support.getUUID(),7,"body",3,new Vec3(.5,1,.5),normal,tick);
        var root=new AnatomyMovement.RootFrame(4,tick,support.position(),0,1,GravityFrame.VANILLA);
        var materialBefore=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new org.joml.Matrix4f()).move(support.position());
        var materialAfter=materialBefore.move(delta);
        var first=new SupportTransport(tick,11,root.sequence(),delta,delta);
        AnatomyTransportReceipts.record(body,contact,surface,root,first,materialBefore,materialAfter);
        var receipts=AnatomyTransportReceipts.history(body,body.getUUID());
        h.assertTrue(receipts.size()==1,"Applied root delta creates one authoritative receipt");
        var receipt=receipts.getFirst();
        var observedDelta=receipt.bodyAfter().subtract(receipt.bodyBefore());
        boolean provenance=receipt.dimension().equals(h.getLevel().dimension().identifier()) && receipt.contactSequence()==3 && receipt.rootFrameSequence()==4
                && receipt.transportSequence()==11 && receipt.materialBefore().equals(materialBefore) && receipt.materialAfter().equals(materialAfter)
                && receipt.appliedDelta().equals(delta) && observedDelta.distanceToSqr(delta)<1.0E-18;
        h.assertTrue(provenance,"Receipt provenance mismatch: dimension="+receipt.dimension()+", contact="+receipt.contactSequence()+", root="+receipt.rootFrameSequence()
                +", transport="+receipt.transportSequence()+", materialBefore="+receipt.materialBefore().equals(materialBefore)+", materialAfter="+receipt.materialAfter().equals(materialAfter)
                +", applied="+receipt.appliedDelta()+", observed="+observedDelta+", expected="+delta);
        long duplicates=AnatomyTransportReceipts.metrics().duplicates();
        AnatomyTransportReceipts.record(body,contact,surface,root,first,materialBefore,materialAfter);
        h.assertTrue(AnatomyTransportReceipts.history(body,body.getUUID()).size()==1 && AnatomyTransportReceipts.metrics().duplicates()==duplicates+1,
            "Only the same applied transport serial is deduplicated");
        body.setPos(body.position().add(delta));
        var second=new SupportTransport(tick,12,root.sequence(),delta.scale(2),delta);
        AnatomyTransportReceipts.record(body,contact,surface,root,second,materialAfter,materialAfter.move(delta));
        h.assertTrue(AnatomyTransportReceipts.history(body,body.getUUID()).size()==2,
            "A second animated material contribution with unchanged contact/root provenance is retained");
        AnatomyTransportReceipts.invalidate(body);
        h.assertTrue(AnatomyTransportReceipts.history(body,body.getUUID()).isEmpty(),"Lifecycle invalidation removes retained receipt without transport");
        support.discard();body.discard();h.succeed();
    }
    @GameTest(maxTicks=60)
    public void receiptSaturationRetainsEachTickForTheFullWindow(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        var body=(net.minecraft.server.level.ServerPlayer)h.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        body.setPos(3,20,2);long firstTick=h.getLevel().getGameTime();
        try {
            saturateReceiptTick(body,support,1000);
            h.assertTrue(AnatomyTransportReceipts.saturated(body,body.getUUID(),firstTick),
                "First overflow rejects references from its material tick");
            h.runAfterDelay(1,()->{
                try {
                    long secondTick=h.getLevel().getGameTime();saturateReceiptTick(body,support,2000);
                    h.assertTrue(secondTick>firstTick && AnatomyTransportReceipts.saturated(body,body.getUUID(),firstTick)
                            && AnatomyTransportReceipts.saturated(body,body.getUUID(),secondTick),
                        "A later overflow cannot overwrite the earlier saturated tick still inside the receipt window");
                    h.runAfterDelay(AnatomyTransportReceipts.HISTORY_TICKS,()->{
                        try {
                            h.assertTrue(!AnatomyTransportReceipts.saturated(body,body.getUUID(),firstTick)
                                    && AnatomyTransportReceipts.history(body,body.getUUID()).isEmpty(),
                                "Expired entries and saturation markers release the UUID root instead of retaining it indefinitely");
                            support.discard();body.discard();h.succeed();
                        } catch(Throwable failure) {support.discard();body.discard();throw failure;}
                    });
                } catch(Throwable failure) {support.discard();body.discard();throw failure;}
            });
        } catch(Throwable failure) {support.discard();body.discard();throw failure;}
    }
    private static void saturateReceiptTick(net.minecraft.server.level.ServerPlayer body,net.minecraft.world.entity.LivingEntity support,long sequenceBase) {
        long tick=body.level().getGameTime();var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"body",7,normal,3);
        var surface=new SurfaceContact(support.getUUID(),7,"body",3,new Vec3(.5,1,.5),normal,tick);
        var root=new AnatomyMovement.RootFrame(sequenceBase,tick,support.position(),0,1,GravityFrame.VANILLA);
        var material=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new org.joml.Matrix4f()).move(support.position());
        for(int index=0;index<=AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK;index++) {
            var transport=new SupportTransport(tick,sequenceBase+index,root.sequence(),Vec3.ZERO,Vec3.ZERO);
            AnatomyTransportReceipts.record(body,contact,surface,root,transport,material,material);
        }
    }
}
