package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * IMPLEMENTER regression for S25/G4.1.
 *
 * <p>This deliberately proves only the production contract needed while building
 * the candidate. Independent A-G holdouts and mutation adequacy remain adversary-owned.</p>
 */
public final class S25ImplementerReferenceTests {
    @GameTest
    public void serverFrameClaimPreservesReceiptAndRejectsReplay(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,2,2);
        support.setNoAi(true);support.setNoGravity(true);
        var player=h.makeMockServerPlayerInLevel();
        player.setPos(3,2,2);

        long revision=AnatomyNetworking.revision(player.level().getServer());
        long frameSerial=37;
        S24TrackingAuthorityTestSeam.run(player,player,()->{
            record(player,support,revision,frameSerial);

            var before=AnatomyTransportReceipts.history(player,player.getUUID());
            h.assertTrue(before.size()==1,"Fixture must mint one server-issued receipt");

            h.assertTrue(AnatomyTransportReceipts.claim(player,player,UUID.randomUUID(),frameSerial)==null,
                "Wrong support metadata must not claim a receipt");
            h.assertTrue(AnatomyTransportReceipts.claim(player,player,support.getUUID(),frameSerial+1)==null,
                "Wrong server frame serial must not claim a receipt");
            h.assertTrue(AnatomyTransportReceipts.history(player,player.getUUID()).equals(before),
                "Rejected metadata must leave the historical receipt unchanged");

            var claimed=AnatomyTransportReceipts.claim(player,player,support.getUUID(),frameSerial);
            h.assertTrue(claimed!=null && claimed.equals(before.getFirst()),
                "Exact server-issued support endpoint must resolve the authoritative receipt");
            h.assertTrue(AnatomyTransportReceipts.history(player,player.getUUID()).equals(before),
                "Claiming a reference may change consumption metadata, never delete or rewrite the receipt");

            h.assertTrue(AnatomyTransportReceipts.claim(player,player,support.getUUID(),frameSerial)==null,
                "The same receipt reference must be rejected after its first successful claim");
            h.assertTrue(AnatomyTransportReceipts.history(player,player.getUUID()).equals(before),
                "Replay rejection must also preserve immutable historical receipt evidence");
        });

        AnatomyTransportReceipts.disconnect(player);
        player.discard();support.discard();
        h.succeed();
    }

    private static void record(net.minecraft.server.level.ServerPlayer body,
            net.minecraft.world.entity.LivingEntity support,long revision,long frameSerial) {
        long tick=body.level().getGameTime();
        var normal=new Vec3(0,1,0);
        var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,1);
        var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
        var root=new RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var material=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
        var transport=new SupportTransport(tick,1,root.sequence(),Vec3.ZERO,Vec3.ZERO);
        AnatomyTransportReceipts.record(body,contact,surface,root,frameSerial,transport,material,material);
    }
}
