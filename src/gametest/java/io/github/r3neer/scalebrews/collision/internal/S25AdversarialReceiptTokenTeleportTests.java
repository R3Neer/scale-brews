package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import java.lang.reflect.Field;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * G4.1 lifecycle holdout: a same-dimension teleport retires the body's transport generation.
 * Unincorporated S2C receipt tokens from the old physical life must not block a restarted
 * body-local receiptSequence in the new physical life.
 */
public final class S25AdversarialReceiptTokenTeleportTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var world=context.worldBuilder().create();
        try {
            context.waitFor(client->client.level!=null && client.player!=null,100);
            context.runOnClient(client->{
                var player=client.player;var level=client.level;var inbox=inbox();
                inbox.clear();
                long now=level.getGameTime();
                UUID epoch=UUID.randomUUID(),support=UUID.randomUUID();

                var oldToken=new AnatomyTransportReceiptPayload(epoch,1,level.dimension().identifier(),
                    player.getId(),player.getUUID(),1,support,10,1,now);
                check(inbox.accept(oldToken,now),"Fixture could not retain the pre-teleport server receipt token");

                long generationBefore=AnatomyMovement.transportGeneration(player);
                player.teleportTo(player.getX()+1,player.getY(),player.getZ());
                long generationAfter=AnatomyMovement.transportGeneration(player);
                check(generationAfter>generationBefore,
                    "Fixture teleport did not cross the production transport lifecycle barrier: before="
                        +generationBefore+" after="+generationAfter);

                var restartedToken=new AnatomyTransportReceiptPayload(epoch,1,level.dimension().identifier(),
                    player.getId(),player.getUUID(),1,support,11,1,now+1);
                check(inbox.accept(restartedToken,now+1),
                    "Same-dimension teleport must retire old client receipt-token identity immediately; "
                        +"a new server receipt sequence restarted at 1 was rejected as stale/duplicate");
                inbox.clear();
            });
            System.out.println("S25_RECEIPT_TOKEN_TELEPORT PASS physical lifecycle reset cannot retain a colliding old receipt token");
        } finally {world.close();}
    }

    private static AnatomyReceiptTokenInbox inbox() {
        try {
            Field field=AnatomyClientNetworking.class.getDeclaredField("transportReceiptTokens");
            field.setAccessible(true);
            return (AnatomyReceiptTokenInbox)field.get(null);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not access client receipt-token inbox",failure);
        }
    }

    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
