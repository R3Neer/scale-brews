package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** V2 receipt publication must not make the server-owned receipt store depend on a live network listener. */
public final class S25AdversarialReceiptPublicationBoundaryTests {
    @GameTest
    public void disconnectedRecipientStillRecordsServerReceiptWithoutPublicationCrash(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyTransportReceipts.clear(server);
        var support=h.spawn(EntityTypes.COW,2,4,2);
        var body=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
        body.setPos(3,4,2);

        h.assertTrue(body.connection==null,
            "Fixture requires a mock ServerPlayer without a network listener");

        try {
            long tick=h.getLevel().getGameTime();
            long revision=AnatomyNetworking.revision(server);
            var normal=new Vec3(0,1,0);
            var contact=new AnatomyMovement.Contact(support,"piece",revision,normal,1);
            var surface=new SurfaceContact(support.getUUID(),revision,"piece",3,new Vec3(.5,1,.5),normal,tick);
            var root=new RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
            var before=ConvexBox.of(new AABB(-1,0,-1,1,1,1),new Matrix4f()).move(support.position());
            var delta=new Vec3(.125,0,0);
            body.setPos(body.position().add(delta));
            var after=before.move(delta);
            var transport=new SupportTransport(tick,1,root.sequence(),delta,delta);

            S24TrackingAuthorityTestSeam.run(body,body,1,
                ()->AnatomyTransportReceipts.record(body,contact,surface,root,101,transport,before,after));

            var history=AnatomyTransportReceipts.history(body,body.getUUID());
            h.assertTrue(history.size()==1 && history.getFirst().transportSequence()==1,
                "Receipt storage must remain authoritative even when no S2C token can be published");
        } finally {
            AnatomyTransportReceipts.clear(server);
            support.discard();body.discard();
        }
        h.succeed();
    }
}
