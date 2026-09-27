package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/**
 * G4.1 lifecycle holdout: a same-dimension teleport retires the body's transport generation.
 * Unincorporated S2C receipt tokens from the old physical life must not block a restarted
 * body-local receiptSequence in the new physical life.
 */
public final class S25AdversarialReceiptTokenTeleportTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var world=context.worldBuilder().create();var boatId=new AtomicInteger();
        try {
            world.getServer().runOnServer(server->{
                var boat=EntityTypes.OAK_BOAT.create(server.overworld(),EntitySpawnReason.COMMAND);
                check(boat!=null,"Could not create non-Living lifecycle boat fixture");
                boat.setPos(3,4,3);server.overworld().addFreshEntity(boat);boatId.set(boat.getId());
            });
            context.waitFor(client->client.level!=null && client.player!=null
                && client.level.getEntity(boatId.get())!=null,100);
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

                // Non-Living controlled vehicles share the same transport ledger and receipt-sequence
                // restart semantics. Entity lifecycle hooks must therefore retire their token cache too.
                inbox.clear();
                var boat=level.getEntity(boatId.get());
                check(boat!=null && !(boat instanceof net.minecraft.world.entity.LivingEntity),
                    "Boat lifecycle fixture is missing or unexpectedly LivingEntity");
                long boatNow=level.getGameTime();
                var oldBoatToken=new AnatomyTransportReceiptPayload(epoch,1,level.dimension().identifier(),
                    boat.getId(),boat.getUUID(),1,support,20,1,boatNow);
                check(inbox.accept(oldBoatToken,boatNow),"Fixture could not retain pre-teleport boat receipt token");
                long boatGenerationBefore=AnatomyMovement.transportGeneration(boat);
                boat.teleportTo(boat.getX()+1,boat.getY(),boat.getZ());
                long boatGenerationAfter=AnatomyMovement.transportGeneration(boat);
                check(boatGenerationAfter>boatGenerationBefore,
                    "Non-Living boat teleport did not cross the production transport lifecycle barrier: before="
                        +boatGenerationBefore+" after="+boatGenerationAfter);
                var restartedBoatToken=new AnatomyTransportReceiptPayload(epoch,1,level.dimension().identifier(),
                    boat.getId(),boat.getUUID(),1,support,21,1,boatNow+1);
                check(inbox.accept(restartedBoatToken,boatNow+1),
                    "Non-Living boat teleport retained a colliding old receipt token across transport lifecycle reset");
                inbox.clear();
            });
            System.out.println("S25_RECEIPT_TOKEN_TELEPORT PASS living and non-living physical lifecycle resets retire colliding receipt tokens");
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
