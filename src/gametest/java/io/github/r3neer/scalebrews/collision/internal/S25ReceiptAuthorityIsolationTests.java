package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** G4.1 foundation: receipt authority is recipient-local even for the same transported body. */
public final class S25ReceiptAuthorityIsolationTests {
    @GameTest
    public void sameBodyReceiptsAreRecipientLocalAndDisconnectScoped(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,2,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.spawn(EntityTypes.OAK_BOAT,3,2,2);
        var first=h.makeMockServerPlayerInLevel();
        var second=h.makeMockServerPlayerInLevel();

        h.assertTrue(first.startRiding(body,true,true) && second.startRiding(body,true,true),
            "Fixture requires two recipients observing the same controlled/passenger body");

        S24TrackingAuthorityTestSeam.run(first,body,
            ()->S24TrackingAuthorityTestSeam.run(second,body,()->record(body,support,1)));

        h.assertTrue(AnatomyTransportReceipts.history(first,body.getUUID()).size()==1,
            "First recipient must own exactly the server-issued receipt recorded for the shared body");
        h.assertTrue(AnatomyTransportReceipts.history(second,body.getUUID()).size()==1,
            "Second recipient must own its own receipt entry for the same shared body");
        h.assertTrue(AnatomyTransportReceipts.history(first,support.getUUID()).isEmpty()
                && AnatomyTransportReceipts.history(second,support.getUUID()).isEmpty(),
            "Receipt lookup must remain body-keyed and cannot alias another entity UUID");

        var secondBody=h.spawn(EntityTypes.OAK_BOAT,4,2,2);
        first.stopRiding();
        h.assertTrue(first.startRiding(secondBody,true,true),
            "Fixture first recipient must transfer control/passenger identity to the second body");
        S24TrackingAuthorityTestSeam.run(first,secondBody,()->record(secondBody,support,1));
        long saturatedTick=secondBody.level().getGameTime();
        S24TrackingAuthorityTestSeam.run(first,secondBody,()->{
            for(int sequence=2;sequence<=AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK+1;sequence++)
                record(secondBody,support,sequence);
        });

        h.assertTrue(AnatomyTransportReceipts.history(first,body.getUUID()).size()==1
                && AnatomyTransportReceipts.history(first,secondBody.getUUID()).size()==AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK,
            "One recipient must retain independent bounded receipt histories for distinct body UUIDs");
        h.assertTrue(AnatomyTransportReceipts.history(second,body.getUUID()).size()==1
                && AnatomyTransportReceipts.history(second,secondBody.getUUID()).isEmpty(),
            "A second recipient must not inherit another recipient's different-body receipt");
        h.assertTrue(AnatomyTransportReceipts.saturated(first,secondBody.getUUID(),saturatedTick),
            "Overflow must mark only the exact recipient/body/tick history as saturated");
        h.assertTrue(!AnatomyTransportReceipts.saturated(second,secondBody.getUUID(),saturatedTick)
                && !AnatomyTransportReceipts.saturated(first,body.getUUID(),saturatedTick),
            "Saturation must not leak across recipient or body authority keys");

        AnatomyTransportReceipts.disconnect(first);
        h.assertTrue(AnatomyTransportReceipts.history(first,body.getUUID()).isEmpty()
                && AnatomyTransportReceipts.history(first,secondBody.getUUID()).isEmpty(),
            "Disconnect must retire every body receipt owned by the dead recipient");
        h.assertTrue(AnatomyTransportReceipts.history(second,body.getUUID()).size()==1,
            "Disconnecting one recipient must not erase another recipient's receipt authority for the same body");

        AnatomyTransportReceipts.disconnect(second);
        first.discard();second.discard();secondBody.discard();body.discard();support.discard();
        h.succeed();
    }

    private static void record(Entity body,LivingEntity support,long sequence) {
        long tick=body.level().getGameTime();
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",1,normal,1);
        var surface=new SurfaceContact(support.getUUID(),1,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var material=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        var transport=new SupportTransport(tick,sequence,root.sequence(),Vec3.ZERO,Vec3.ZERO);
        AnatomyTransportReceipts.record(body,contact,surface,root,transport,material,material);
    }
}
