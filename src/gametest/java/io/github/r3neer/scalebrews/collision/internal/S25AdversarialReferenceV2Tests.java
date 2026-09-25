package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
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

/** Independent G4.1 V2 owner holdouts. V1 evidence does not substitute for the token claim path. */
public final class S25AdversarialReferenceV2Tests {
    @GameTest
    public void exactServerReceiptTokenIsOnceOnlyAndNeverReappliesTransport(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyTransportReceipts.clear(server);AnatomyMovementReference.clear(server);
        var support=h.spawn(EntityTypes.COW,2,4,2);
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);player.setPos(3,4,2);
        try {
            var first=record(player,player,support,1,1,new Vec3(.125,0,0));
            Vec3 afterReceipt=player.position();AABB box=player.getBoundingBox();
            var reference=new AnatomyMoveReferenceV2Payload(false,first.transportSequence());

            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,reference));
            h.assertTrue(consumed(player,player,1) && pending(player),
                "V2 exact server receipt token must consume exactly one receipt and stage a pending baseline");
            h.assertTrue(player.position().equals(afterReceipt) && player.getBoundingBox().equals(box)
                    && TransportLedger.current(player).sequence()==1,
                "Accepting a V2 token must not reapply its already-applied transport");

            applyTransportOnly(player,2,new Vec3(.25,0,0));
            Vec3 expected=player.position();
            var resolved=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->resolved.set(AnatomyMovementReference.resolve(player,player,afterReceipt)));
            h.assertTrue(resolved.get().equals(expected) && player.position().equals(expected),
                "V2 resolve may add only server transport after the claimed receipt and may not move the body itself");

            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,reference));
            var replay=new AtomicReference<Vec3>();
            S24TrackingAuthorityTestSeam.run(player,player,1,
                ()->replay.set(AnatomyMovementReference.resolve(player,player,expected)));
            h.assertTrue(replay.get().equals(expected) && !pending(player),
                "V2 receipt token replay must remain exactly-once");

            var fake=new AnatomyMoveReferenceV2Payload(false,999_999);
            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,fake));
            h.assertTrue(!pending(player),
                "Invented V2 receipt sequence must not stage authority");
        } finally {cleanup(server,support,player);}
        h.succeed();
    }

    @GameTest
    public void tokenFromRetiredTrackingWindowAndNonControllerFailClosed(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyTransportReceipts.clear(server);AnatomyMovementReference.clear(server);
        var support=h.spawn(EntityTypes.COW,2,4,2);
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);player.setPos(3,4,2);
        var controller=(ServerPlayer)h.makeMockServerPlayerInLevel();
        var passenger=(ServerPlayer)h.makeMockServerPlayerInLevel();
        var boat=h.spawn(EntityTypes.OAK_BOAT,7,4,2);
        try {
            var stale=record(player,player,support,1,1,new Vec3(.03125,0,0));
            S24TrackingAuthorityTestSeam.runOwned(player,player,2,
                ()->AnatomyMovementReference.accept(player,new AnatomyMoveReferenceV2Payload(false,stale.transportSequence())));
            h.assertTrue(!consumed(player,player,1) && !pending(player),
                "V2 token from a retired tracking generation must not consume historical receipt authority");

            h.assertTrue(controller.startRiding(boat,true,true) && passenger.startRiding(boat,true,true)
                    && boat.getControllingPassenger()==controller,
                "V2 controlled-vehicle fixture requires one controller and one non-controller");
            recordTwo(controller,passenger,boat,support,1,1,new Vec3(.0625,0,0));

            S24TrackingAuthorityTestSeam.runOwned(passenger,boat,1,
                ()->AnatomyMovementReference.accept(passenger,new AnatomyMoveReferenceV2Payload(true,1)));
            h.assertTrue(!consumed(passenger,boat,1) && !pending(passenger),
                "Receipt possession alone must not let a non-controller use the V2 vehicle reference");

            S24TrackingAuthorityTestSeam.runOwned(controller,boat,1,
                ()->AnatomyMovementReference.accept(controller,new AnatomyMoveReferenceV2Payload(true,1)));
            h.assertTrue(consumed(controller,boat,1) && pending(controller),
                "Actual controller must be able to consume its own exact V2 receipt token");
        } finally {cleanup(server,support,player,controller,passenger,boat);}
        h.succeed();
    }

    private static AnatomyTransportReceipts.Receipt record(ServerPlayer recipient,Entity body,LivingEntity support,
            long tracking,long sequence,Vec3 delta) {
        body.setPos(body.position().add(delta));long tick=body.level().getGameTime();
        long revision=AnatomyNetworking.revision(body.level().getServer());
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,sequence);
        var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(sequence,tick,support.position(),0,1,GravityFrame.VANILLA);
        var before=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        var after=before.move(delta);var transport=new SupportTransport(tick,sequence,root.sequence(),delta,delta);
        TransportLedger.record(body,transport);
        S24TrackingAuthorityTestSeam.run(recipient,body,tracking,
            ()->AnatomyTransportReceipts.record(body,contact,surface,root,100+sequence,transport,before,after));
        return AnatomyTransportReceipts.history(recipient,body.getUUID()).stream()
            .filter(r->r.transportSequence()==sequence).findFirst().orElseThrow();
    }

    private static void recordTwo(ServerPlayer a,ServerPlayer b,Entity body,LivingEntity support,
            long tracking,long sequence,Vec3 delta) {
        body.setPos(body.position().add(delta));long tick=body.level().getGameTime();
        long revision=AnatomyNetworking.revision(body.level().getServer());
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,sequence);
        var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(sequence,tick,support.position(),0,1,GravityFrame.VANILLA);
        var before=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        var after=before.move(delta);var transport=new SupportTransport(tick,sequence,root.sequence(),delta,delta);
        TransportLedger.record(body,transport);
        S24TrackingAuthorityTestSeam.run(a,body,tracking,
            ()->S24TrackingAuthorityTestSeam.run(b,body,tracking,
                ()->AnatomyTransportReceipts.record(body,contact,surface,root,100+sequence,transport,before,after)));
    }

    private static void applyTransportOnly(Entity body,long sequence,Vec3 delta) {
        body.setPos(body.position().add(delta));long tick=body.level().getGameTime();
        var current=TransportLedger.current(body);
        long root=current==null?sequence:Math.max(sequence,current.rootFrameSequence());
        TransportLedger.record(body,new SupportTransport(tick,sequence,root,delta,delta));
    }

    @SuppressWarnings("unchecked")
    private static boolean consumed(ServerPlayer recipient,Entity body,long sequence) {
        try {
            var historiesField=AnatomyTransportReceipts.class.getDeclaredField("HISTORIES");historiesField.setAccessible(true);
            var histories=(java.util.Map<ServerPlayer,java.util.Map<java.util.UUID,Object>>)historiesField.get(null);
            var roots=histories.get(recipient);if(roots==null)return false;
            var history=roots.get(body.getUUID());if(history==null)return false;
            var field=history.getClass().getDeclaredField("consumedTransportSequences");field.setAccessible(true);
            return ((java.util.Set<Long>)field.get(history)).contains(sequence);
        } catch(ReflectiveOperationException e) {throw new AssertionError(e);}
    }

    @SuppressWarnings("unchecked")
    private static boolean pending(ServerPlayer recipient) {
        try {
            var statesField=AnatomyMovementReference.class.getDeclaredField("STATES");statesField.setAccessible(true);
            var states=(java.util.Map<ServerPlayer,Object>)statesField.get(null);var state=states.get(recipient);
            if(state==null)return false;var field=state.getClass().getDeclaredField("pending");field.setAccessible(true);
            return field.get(state)!=null;
        } catch(ReflectiveOperationException e) {throw new AssertionError(e);}
    }

    private static void cleanup(net.minecraft.server.MinecraftServer server,Entity... entities) {
        AnatomyMovementReference.clear(server);AnatomyTransportReceipts.clear(server);
        for(var e:entities)if(e!=null && !e.isRemoved())e.discard();
    }
}
