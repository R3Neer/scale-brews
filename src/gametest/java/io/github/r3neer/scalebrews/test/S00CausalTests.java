package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** A01/A07..A12: identity, clock, transaction and replay properties, not test-only ledgers. */
public final class S00CausalTests {
    private static final Identifier DIM=Identifier.parse("minecraft:overworld");
    private static final Vec3 POINT=new Vec3(.5,1,.5),UP=new Vec3(0,1,0);
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);
    private static AnatomyPosePayload packet(long serial,long authority,long joint,long rootTick,long binding,double x) {
        return new AnatomyPosePayload(EPOCH,1,DIM,1,SUPPORT,MODEL,STATIC,serial,authority,joint,serial,rootTick,binding,true,INPUTS,new Vec3(x,0,0),0,1,Direction.DOWN);
    }
    private static AnatomyContactPayload contact(long generation,long sequence,long tick) {
        return new AnatomyContactPayload(EPOCH,1,DIM,2,UUID.fromString("00000000-0000-0000-0000-000000000300"),generation,sequence,tick,1,SUPPORT,"piece",3,POINT,UP);
    }
    @GameTest public void surfaceContactRejectsNegativeProvenance(GameTestHelper h) {
        rejects(()->new SurfaceContact(SUPPORT,-1,"piece",3,POINT,UP,0));
        rejects(()->new SurfaceContact(SUPPORT,0,"piece",3,POINT,UP,-1));
        rejects(()->new SurfaceContact(SUPPORT,0,"piece",3,null,UP,0));h.succeed();
    }
    @GameTest public void rootFrameRejectsNegativeTime(GameTestHelper h) {
        rejects(()->new AnatomyMovement.RootFrame(0,-1,Vec3.ZERO,0,1,GravityFrame.VANILLA));h.succeed();
    }
    @GameTest public void transportRejectsNegativeTimeAndMissingDelta(GameTestHelper h) {
        rejects(()->new SupportTransport(-1,1,0,Vec3.ZERO,Vec3.ZERO));
        rejects(()->new SupportTransport(0,1,0,null,Vec3.ZERO));h.succeed();
    }
    @GameTest public void snapshotRejectsNegativeRevisionAndInvalidPieceKeys(GameTestHelper h) {
        rejects(()->new GeometryProvider.Snapshot(-1,Map.of("piece",box(UNIT))));
        rejects(()->new GeometryProvider.Snapshot(1,Map.of("",box(UNIT))));
        rejects(()->new GeometryProvider.Snapshot(1,Map.of("x".repeat(257),box(UNIT))));h.succeed();
    }
    @GameTest public void motionSnapshotRejectsClockOverflow(GameTestHelper h) {
        rejects(()->new GeometryProvider.MotionSnapshot(1,Long.MAX_VALUE,Long.MIN_VALUE,Vec3.ZERO,Vec3.ZERO,Map.of()));h.succeed();
    }
    @GameTest public void poseSampleRejectsNonfiniteOrMissingPhysicalState(GameTestHelper h) {
        rejects(()->new AnatomyPoseHistory.Sample(INPUTS,new Vec3(Double.NaN,0,0),0,1));
        rejects(()->new AnatomyPoseHistory.Sample(null,Vec3.ZERO,0,1));
        rejects(()->new AnatomyPoseHistory.Sample(INPUTS,Vec3.ZERO,0,0));h.succeed();
    }
    @GameTest public void payloadCannotPublishFutureJointOrRoot(GameTestHelper h) {
        rejects(()->packet(10,10,11,10,1,0));rejects(()->packet(10,10,10,11,1,0));h.succeed();
    }
    @GameTest public void endpointCannotPublishFutureJointOrRoot(GameTestHelper h) {
        var root=new AnatomyMovement.RootFrame(1,10,Vec3.ZERO,0,1,GravityFrame.VANILLA);
        var sample=new AnatomyPoseHistory.Sample(INPUTS,Vec3.ZERO,0,1);
        rejects(()->new GeometryProvider.CausalEndpoint(1,10,11,root,sample,GeometryProvider.Availability.AVAILABLE));
        rejects(()->new GeometryProvider.CausalEndpoint(1,9,9,root,sample,GeometryProvider.Availability.AVAILABLE));h.succeed();
    }
    @GameTest public void motionHandleRequiresDistinctOrderedEndpoints(GameTestHelper h) {
        var first=frame(1,10,1,0);var second=frame(2,10,2,.2);
        new GeometryProvider.MotionIntervalHandle(identity(),1,first,second);
        rejects(()->new GeometryProvider.MotionIntervalHandle(identity(),1,second,first));
        rejects(()->new GeometryProvider.MotionIntervalHandle(identity(),1,first,first));h.succeed();
    }
    @GameTest public void higherPublicationCannotRewindAuthority(GameTestHelper h) {
        var history=new AnatomyFrameHistory();var first=packet(10,10,8,10,1,0);history.accept(first);
        check(!history.accept(packet(12,9,8,9,1,1)) && history.current()==first,"Higher serial rewound authority");h.succeed();
    }
    @GameTest public void higherPublicationCannotRewindJoint(GameTestHelper h) {
        var history=new AnatomyFrameHistory();var first=packet(10,10,8,10,1,0);history.accept(first);
        check(!history.accept(packet(12,11,7,11,1,1)) && history.current()==first,"Higher serial rewound joint provenance");h.succeed();
    }
    @GameTest public void rootOnlyAndGappedSerialsRemainValid(GameTestHelper h) {
        var history=new AnatomyFrameHistory();var first=packet(10,10,8,10,1,0);history.accept(first);
        var rootOnly=packet(12,10,8,10,1,.2);
        check(history.accept(rootOnly) && history.current()==rootOnly,"Legitimate same-tick root-only update was collapsed");
        check(!history.accept(first) && !history.accept(rootOnly),"Duplicate or reordered publication was applied again");
        check(history.accept(packet(20,11,9,11,1,.3)),"Publication gaps were mistaken for identity change");h.succeed();
    }
    @GameTest public void jointHistoryCannotInterpolateAcrossBindingReuse(GameTestHelper h) {
        var history=new AnatomyPoseHistory();var first=packet(10,10,10,10,1,0);history.accept(first);
        rejects(()->history.accept(packet(11,11,11,11,2,1)));
        check(history.current()==first,"Rejected generation mutated joint history");h.succeed();
    }
    @GameTest public void frameHistoryRejectsEachIdentityAxisAtomically(GameTestHelper h) {
        var first=packet(10,10,10,10,1,0);
        for(int axis=0;axis<8;axis++) {
            var history=new AnatomyFrameHistory();history.accept(first);
            var next=new AnatomyPosePayload(axis==0?new UUID(0,999):EPOCH,axis==1?2:1,
                axis==2?Identifier.parse("minecraft:the_nether"):DIM,axis==3?9:1,axis==4?new UUID(0,999):SUPPORT,
                axis==5?Identifier.parse("test:other"):MODEL,axis==6?Identifier.parse("test:other"):STATIC,
                11,11,11,11,11,axis==7?2:1,true,INPUTS,Vec3.ZERO,0,1,Direction.DOWN);
            rejects(()->history.accept(next));check(history.current()==first,"Identity axis "+axis+" mutated current frame");
        }h.succeed();
    }
    @GameTest public void acceptedContactPayloadAlwaysHasAValidMaterialContact(GameTestHelper h) {
        var good=contact(1,1,1);
        new SurfaceContact(good.support(),good.revision(),good.piece(),good.face(),good.localPoint(),good.normal(),good.tick());
        rejects(()->new AnatomyContactPayload(EPOCH,1,DIM,2,good.body(),1,2,1,1,SUPPORT,"piece",3,new Vec3(.5,.9,.5),UP));
        rejects(()->new AnatomyContactPayload(EPOCH,1,DIM,2,good.body(),1,2,1,1,SUPPORT,"piece",3,POINT,UP.scale(1.001)));
        rejects(()->new AnatomyContactPayload(EPOCH,1,DIM,2,good.body(),1,2,1,1,SUPPORT,"x".repeat(257),3,POINT,UP));h.succeed();
    }
    @GameTest public void inboxTtlDoesNotOverflowNearMaximumTick(GameTestHelper h) {
        var inbox=new AnatomyContactInbox();var recent=contact(1,1,Long.MAX_VALUE-10);inbox.accept(recent);
        inbox.prune(Long.MAX_VALUE,40);
        check(inbox.pending().get(recent.body())==recent,"Recent maximum-tick contact was spuriously expired");
        rejects(()->inbox.prune(-1,40));rejects(()->inbox.prune(1,-1));h.succeed();
    }
    @GameTest public void inboxExpiryBoundaryAndConsumePreserveReplayFence(GameTestHelper h) {
        var inbox=new AnatomyContactInbox();var old=contact(1,1,10);var next=contact(1,2,11);
        inbox.accept(old);inbox.accept(next);inbox.consume(old);
        check(inbox.pending().get(next.body())==next,"Consuming old packet erased newer pending state");
        inbox.consume(next);check(!inbox.accept(old) && !inbox.accept(next),"Consumed contact lost its replay fence");
        var fresh=contact(1,3,11);inbox.accept(fresh);inbox.prune(51,40);
        check(inbox.pending().get(fresh.body())==fresh,"TTL boundary expired early");
        inbox.prune(52,40);check(inbox.pending().isEmpty(),"Expired pending contact survived");h.succeed();
    }
    @GameTest public void holdoutH03IdentityReuseCannotRestorePriorGeneration(GameTestHelper h) {
        var inbox=new AnatomyContactInbox();var old=contact(1,Long.MAX_VALUE,10);inbox.accept(old);
        var replacement=contact(2,1,11);check(inbox.accept(replacement),"New generation with reused UUID/id was rejected");
        check(!inbox.accept(old) && inbox.pending().get(replacement.body())==replacement,"Old generation revived despite new authority");
        inbox.consume(old);check(inbox.pending().get(replacement.body())==replacement,"Old-generation receipt cleared replacement");h.succeed();
    }
    @GameTest public void rejectedRegistrationMustPreserveValidBinding(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,4,2);support.setNoAi(true);support.setNoGravity(true);
        AnatomyMovement.activate(h.getLevel());
        try {
            var provider=new ModelGeometryProvider(model(),(m,i)->Optional.of(Map.of()),AnatomyFilter.DEFAULT,1);
            provider.pose(support,INPUTS);provider.tick(support,h.getLevel().getGameTime());
            var descriptor=new GeometryProvider.GeometryIdentityDescriptor(EPOCH,1,MODEL,STATIC,1);
            AnatomyMovement.register(support,provider,descriptor);
            long generation=AnatomyMovement.registrationGeneration(support);
            var prior=AnatomyMovement.queryFrame(support).orElseThrow();
            rejects(()->AnatomyMovement.register(support,provider,null));
            check(AnatomyMovement.registrationGeneration(support)==generation,"Rejected binding advanced registration generation");
            check(AnatomyMovement.queryFrame(support).orElseThrow().identity().equals(prior.identity()),"Rejected registration destroyed prior valid descriptor");
        } finally {AnatomyMovement.deactivate(h.getLevel());support.discard();}
        h.succeed();
    }
    private static List<AnatomyCatalogPayload> fragments(long revision,byte[] bytes) {
        try {
            var digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));var out=new ArrayList<AnatomyCatalogPayload>();
            int count=(bytes.length+AnatomyCatalogPayload.CHUNK-1)/AnatomyCatalogPayload.CHUNK;
            for(int i=0;i<count;i++)out.add(new AnatomyCatalogPayload(EPOCH,AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),revision,i,count,bytes.length,digest,
                Arrays.copyOfRange(bytes,i*AnatomyCatalogPayload.CHUNK,Math.min(bytes.length,(i+1)*AnatomyCatalogPayload.CHUNK))));
            return out;
        }catch(java.security.NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}
    }
    @GameTest public void catalogTransferSnapshotUsesServerAuthorityRevision(GameTestHelper h) {
        var transfer=new AnatomyCatalogTransfer();var models=Map.of("test:model",model());
        for(var packet:AnatomyCatalogTransfer.encode(EPOCH,7,models))transfer.accept(packet);
        var seven=transfer.snapshot();
        check(transfer.revision()==7 && seven.revision()==7,"Accepted catalog split transport and snapshot revision identity");
        for(var packet:AnatomyCatalogTransfer.encode(EPOCH,12,models))transfer.accept(packet);
        check(transfer.revision()==12 && transfer.snapshot().revision()==12 && transfer.snapshot()!=seven,"Later authoritative revision did not publish atomically");
        h.succeed();
    }
    @GameTest public void holdoutH06InvalidFinalFragmentPreservesAcceptedCatalog(GameTestHelper h) {
        var transfer=new AnatomyCatalogTransfer();var models=new TreeMap<String,ModelGeometry>();
        for(int i=0;i<64;i++)models.put("test:m"+i,model());
        var good=AnatomyCatalogTransfer.encode(EPOCH,3,models);check(good.size()>1,"Fragment fixture was not fragmented");
        var permuted=new ArrayList<>(good);Collections.reverse(permuted);
        for(var packet:permuted){transfer.accept(packet);transfer.accept(packet);}
        var accepted=transfer.snapshot();check(transfer.mode()==AnatomyMode.READY && transfer.revision()==3,"Initial catalog failed to become ready");
        var json=new com.google.gson.JsonObject();json.add("models",new com.google.gson.Gson().toJsonTree(models));json.add("profiles",new com.google.gson.JsonObject());
        var last=json.getAsJsonObject("models").getAsJsonObject("test:m9");
        last.getAsJsonArray("pieces").get(0).getAsJsonObject().addProperty("part","missing_parent");
        var invalid=fragments(7,json.toString().getBytes(StandardCharsets.UTF_8));
        for(int i=0;i<invalid.size()-1;i++)transfer.accept(invalid.get(i));
        check(transfer.mode()==AnatomyMode.BINDING && transfer.snapshot()==accepted,"Candidate staging changed accepted snapshot");
        check(thrown(()->transfer.accept(invalid.getLast())) instanceof RuntimeException,"Invalid candidate did not fail validation");
        // Receiver owns the error boundary and calls rejectPending on a rejected candidate.
        transfer.rejectPending();
        check(transfer.mode()==AnatomyMode.READY && transfer.snapshot()==accepted && transfer.revision()==3,"Rejected candidate replaced the last valid revision");
        for(var packet:AnatomyCatalogTransfer.encode(EPOCH,12,models))transfer.accept(packet);
        var newest=transfer.snapshot();for(var packet:invalid)check(!transfer.accept(packet),"Delayed rejected revision replaced a newer catalog");
        check(transfer.revision()==12 && transfer.snapshot()==newest,"Delayed fragments mutated accepted catalog");h.succeed();
    }
    @GameTest public void fragmentMutationAndConflictingReplayCannotMutateAcceptedData(GameTestHelper h) {
        var models=new TreeMap<String,ModelGeometry>();for(int i=0;i<64;i++)models.put("test:m"+i,model());
        var packets=AnatomyCatalogTransfer.encode(EPOCH,1,models);var transfer=new AnatomyCatalogTransfer();var first=packets.getFirst();
        transfer.accept(first);byte[] changed=first.fragment();changed[0]^=1;
        check(first.fragment()[0]!=changed[0],"Fragment accessor leaked mutable bytes");
        var conflict=new AnatomyCatalogPayload(first.epoch(),first.protocolVersion(),first.requiredCapabilities(),first.revision(),first.index(),first.count(),first.totalBytes(),first.digest(),changed);
        rejects(()->transfer.accept(conflict));transfer.rejectPending();
        check(transfer.mode()==AnatomyMode.DISABLED && transfer.revision()==-1,"Rejected first catalog fabricated ready state");
        for(var packet:packets)transfer.accept(packet);
        var snapshot=transfer.snapshot();rejects(()->transfer.accept(new AnatomyCatalogPayload(new UUID(0,999),first.protocolVersion(),first.requiredCapabilities(),2,first.index(),first.count(),first.totalBytes(),first.digest(),first.fragment())));
        check(transfer.snapshot()==snapshot && transfer.revision()==1,"Wrong epoch mutated catalog");h.succeed();
    }
}
