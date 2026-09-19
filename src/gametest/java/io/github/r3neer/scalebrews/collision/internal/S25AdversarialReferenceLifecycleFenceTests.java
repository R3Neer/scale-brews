package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Owner-level G4.1 holdouts: every receipt lifecycle axis is independently claim-authoritative. */
public final class S25AdversarialReferenceLifecycleFenceTests {
    @GameTest public void staleReceiptEpochCannotBeClaimed(GameTestHelper h) {
        rejectsMutatedReceipt(h,receipt->copy(receipt,UUID.randomUUID(),receipt.dimension(),receipt.bodyNetworkId()),
            "A receipt from another server epoch must not be consumed or staged");
    }

    @GameTest public void staleReceiptDimensionCannotBeClaimed(GameTestHelper h) {
        rejectsMutatedReceipt(h,receipt->copy(receipt,receipt.epoch(),Identifier.parse("minecraft:the_nether"),receipt.bodyNetworkId()),
            "A receipt from another dimension must not be consumed or staged");
    }

    @GameTest public void staleReceiptNetworkIdCannotBeClaimed(GameTestHelper h) {
        rejectsMutatedReceipt(h,receipt->copy(receipt,receipt.epoch(),receipt.dimension(),receipt.bodyNetworkId()+1),
            "A receipt from a replaced/reused network entity id must not be consumed or staged");
    }

    private static void rejectsMutatedReceipt(GameTestHelper h,UnaryOperator<AnatomyTransportReceipts.Receipt> mutation,String message) {
        var support=h.spawn(EntityTypes.COW,2,3,2);
        support.setNoAi(true);support.setNoGravity(true);
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(3,3,2);
        long frameSerial=11,sequence=1;
        try {
            issue(player,support,sequence,frameSerial);
            rewrite(player,sequence,mutation);
            h.assertTrue(!consumed(player,sequence) && !pending(player),
                "Fixture must begin with unconsumed receipt and no staged reference");

            var reference=new AnatomyMoveReferencePayload(false,support.getUUID(),frameSerial);
            S24TrackingAuthorityTestSeam.runOwned(player,player,1,
                ()->AnatomyMovementReference.accept(player,reference));

            h.assertTrue(!consumed(player,sequence) && !pending(player),message);
        } finally {
            AnatomyTransportReceipts.disconnect(player);
            AnatomyMovementReference.disconnect(player);
            if(!player.isRemoved())player.discard();
            if(!support.isRemoved())support.discard();
        }
        h.succeed();
    }

    private static void issue(ServerPlayer player,net.minecraft.world.entity.LivingEntity support,long sequence,long frameSerial) {
        Vec3 applied=new Vec3(.0625,0,0);
        player.setPos(player.position().add(applied));
        long tick=player.level().getGameTime();
        long revision=AnatomyNetworking.revision(player.level().getServer());
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,sequence);
        var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(sequence,tick,support.position(),0,1,GravityFrame.VANILLA);
        var before=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        var after=before.move(applied);
        var transport=new SupportTransport(tick,sequence,root.sequence(),applied,applied);
        S24TrackingAuthorityTestSeam.run(player,player,1,
            ()->AnatomyTransportReceipts.record(player,contact,surface,root,frameSerial,transport,before,after));
        if(AnatomyTransportReceipts.history(player,player.getUUID()).stream().noneMatch(r->r.transportSequence()==sequence))
            throw new AssertionError("Fixture failed to issue a referenceable receipt");
    }

    @SuppressWarnings("unchecked")
    private static void rewrite(ServerPlayer recipient,long sequence,UnaryOperator<AnatomyTransportReceipts.Receipt> mutation) {
        try {
            Field historiesField=AnatomyTransportReceipts.class.getDeclaredField("HISTORIES");
            historiesField.setAccessible(true);
            var histories=(Map<ServerPlayer,Map<UUID,Object>>)historiesField.get(null);
            var roots=histories.get(recipient);
            if(roots==null)throw new AssertionError("Missing receipt roots");
            var history=roots.get(recipient.getUUID());
            if(history==null)throw new AssertionError("Missing receipt history");
            Field entriesField=history.getClass().getDeclaredField("entries");
            entriesField.setAccessible(true);
            var entries=(ArrayDeque<AnatomyTransportReceipts.Receipt>)entriesField.get(history);
            var replacement=new ArrayDeque<AnatomyTransportReceipts.Receipt>();
            boolean changed=false;
            for(var receipt:entries) {
                if(receipt.transportSequence()==sequence) {
                    replacement.addLast(mutation.apply(receipt));
                    changed=true;
                } else replacement.addLast(receipt);
            }
            if(!changed)throw new AssertionError("Missing receipt sequence "+sequence);
            entries.clear();entries.addAll(replacement);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not rewrite isolated receipt lifecycle field",failure);
        }
    }

    private static AnatomyTransportReceipts.Receipt copy(AnatomyTransportReceipts.Receipt r,UUID epoch,Identifier dimension,int bodyNetworkId) {
        return new AnatomyTransportReceipts.Receipt(epoch,r.catalogRevision(),dimension,bodyNetworkId,r.body(),r.trackingGeneration(),
            r.support(),r.piece(),r.face(),r.localPoint(),r.normal(),r.surfaceTick(),r.contactSequence(),
            r.rootFrameSequence(),r.transportSequence(),r.tick(),r.bodyBefore(),r.bodyAfter(),
            r.rootFrame(),r.materialBefore(),r.materialAfter(),r.appliedDelta());
    }

    @SuppressWarnings("unchecked")
    private static boolean consumed(ServerPlayer recipient,long sequence) {
        try {
            Field historiesField=AnatomyTransportReceipts.class.getDeclaredField("HISTORIES");
            historiesField.setAccessible(true);
            var histories=(Map<ServerPlayer,Map<UUID,Object>>)historiesField.get(null);
            var roots=histories.get(recipient);if(roots==null)return false;
            var history=roots.get(recipient.getUUID());if(history==null)return false;
            Field consumedField=history.getClass().getDeclaredField("consumedTransportSequences");
            consumedField.setAccessible(true);
            return ((java.util.Set<Long>)consumedField.get(history)).contains(sequence);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect receipt consumption metadata",failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean pending(ServerPlayer recipient) {
        try {
            Field statesField=AnatomyMovementReference.class.getDeclaredField("STATES");
            statesField.setAccessible(true);
            var states=(Map<ServerPlayer,Object>)statesField.get(null);
            var state=states.get(recipient);if(state==null)return false;
            Field pendingField=state.getClass().getDeclaredField("pending");
            pendingField.setAccessible(true);
            return pendingField.get(state)!=null;
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect movement-reference staging",failure);
        }
    }
}
