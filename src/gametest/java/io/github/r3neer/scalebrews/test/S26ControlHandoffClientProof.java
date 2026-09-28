package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.collision.internal.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Real-client control handoff: prediction authority follows the current controlled root only. */
public final class S26ControlHandoffClientProof implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        var properties=new java.util.Properties();
        properties.setProperty("allow-flight","false");
        var firstId=new AtomicInteger(-1);
        var secondId=new AtomicInteger(-1);

        try(var world=context.worldBuilder().createServer(properties);var connection=world.connect()) {
            world.runCommand("kill @e[tag=s26_boat_a]");
            world.runCommand("kill @e[tag=s26_boat_b]");
            world.runCommand("summon minecraft:oak_boat 0 70 0 {Tags:[\"s26_boat_a\"]}");
            world.runCommand("summon minecraft:oak_boat 3 70 0 {Tags:[\"s26_boat_b\"]}");
            world.runCommand("gamemode survival @a");
            world.runCommand("tp @a 0 70 0");

            world.runOnServer(server->{
                for(var entity:server.overworld().getAllEntities()) {
                    if(entity.entityTags().contains("s26_boat_a"))firstId.set(entity.getId());
                    if(entity.entityTags().contains("s26_boat_b"))secondId.set(entity.getId());
                }
                if(firstId.get()<0 || secondId.get()<0)throw new AssertionError("S26 could not create both control-handoff boats");
            });

            context.waitFor(client->client.player!=null && client.level!=null
                && client.level.getEntity(firstId.get())!=null && client.level.getEntity(secondId.get())!=null,100);

            // Player owns prediction while not mounted.
            context.runOnClient(client->{
                var player=client.player;
                var first=client.level.getEntity(firstId.get());
                var second=client.level.getEntity(secondId.get());
                if(player.getRootVehicle()!=player || !AnatomyMovement.predictsBody(player)
                        || AnatomyMovement.predictsBody(first) || AnatomyMovement.predictsBody(second))
                    throw new AssertionError("Initial full prediction authority is not player-only: player="+player.getRootVehicle()
                        +" first="+AnatomyMovement.predictsBody(first)+" second="+AnatomyMovement.predictsBody(second));
            });

            // Hand authority to boat A.
            world.runCommand("ride @a[limit=1] mount @e[tag=s26_boat_a,limit=1]");
            context.waitFor(client->client.player!=null && client.player.getRootVehicle()!=client.player
                && client.player.getRootVehicle().getId()==firstId.get(),100);
            context.waitTicks(1);
            context.runOnClient(client->{
                assertAuthority(client,firstId.get(),secondId.get(),firstId.get(),"boat A");
                var first=client.level.getEntity(firstId.get());
                seedPredictionMetadata(client,first,101);
                assertPredictionMetadataPresent(first.getUUID(),"boat A");
            });

            // Move authority directly from A to B. The old root must stop simulating immediately once
            // the client observes B as its new root; no retained contact/cursor can keep A authoritative.
            world.runCommand("ride @a[limit=1] dismount");
            world.runCommand("ride @a[limit=1] mount @e[tag=s26_boat_b,limit=1]");
            context.waitFor(client->client.player!=null && client.player.getRootVehicle()!=client.player
                && client.player.getRootVehicle().getId()==secondId.get(),100);
            context.waitTicks(1);
            context.runOnClient(client->{
                assertAuthority(client,firstId.get(),secondId.get(),secondId.get(),"boat B");
                var first=client.level.getEntity(firstId.get());
                var second=client.level.getEntity(secondId.get());
                assertPredictionOwner(second.getUUID(),"boat B");
                assertPredictionMetadataAbsent(first.getUUID(),"retired boat A");
                seedPredictionMetadata(client,second,202);
                assertPredictionMetadataPresent(second.getUUID(),"boat B");
            });

            // Dismount returns prediction to the player and retires both vehicle roots.
            world.runCommand("ride @a[limit=1] dismount");
            context.waitFor(client->client.player!=null && client.player.getRootVehicle()==client.player,100);
            context.waitTicks(1);
            context.runOnClient(client->{
                var player=client.player;
                var first=client.level.getEntity(firstId.get());
                var second=client.level.getEntity(secondId.get());
                if(!AnatomyMovement.predictsBody(player) || AnatomyMovement.predictsBody(first) || AnatomyMovement.predictsBody(second))
                    throw new AssertionError("Dismount left stale vehicle full-prediction authority: player="
                        +AnatomyMovement.predictsBody(player)+" first="+AnatomyMovement.predictsBody(first)
                        +" second="+AnatomyMovement.predictsBody(second));
                assertPredictionOwner(player.getUUID(),"dismounted player");
                assertPredictionMetadataAbsent(first.getUUID(),"retired boat A after dismount");
                assertPredictionMetadataAbsent(second.getUUID(),"retired boat B after dismount");
            });

            System.out.println("S26_CONTROL_HANDOFF PASS player -> boatA -> boatB -> player prediction authority");
        }
    }

    @SuppressWarnings("unchecked")
    private static void seedPredictionMetadata(net.minecraft.client.Minecraft client,net.minecraft.world.entity.Entity body,long sequence) {
        try {
            var referenceClass=java.util.Arrays.stream(AnatomyClientNetworking.class.getDeclaredClasses())
                .filter(type->type.getSimpleName().equals("PredictedMovementReference"))
                .findFirst().orElseThrow();
            var constructor=referenceClass.getDeclaredConstructor(long.class,long.class);constructor.setAccessible(true);
            var referencesField=AnatomyClientNetworking.class.getDeclaredField("predictedMovementReferences");referencesField.setAccessible(true);
            var references=(Map<UUID,Object>)referencesField.get(null);
            references.put(body.getUUID(),constructor.newInstance(sequence,sequence));

            var inboxField=AnatomyClientNetworking.class.getDeclaredField("transportReceiptTokens");inboxField.setAccessible(true);
            var inbox=(AnatomyReceiptTokenInbox)inboxField.get(null);
            var token=new AnatomyTransportReceiptPayload(UUID.randomUUID(),0,client.level.dimension().identifier(),
                body.getId(),body.getUUID(),1,UUID.randomUUID(),1,sequence,client.level.getGameTime());
            if(!inbox.accept(token,client.level.getGameTime()))
                throw new AssertionError("Could not seed test-only receipt token for "+body.getUUID());
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not seed S26 prediction metadata",failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertPredictionMetadataPresent(UUID body,String phase) {
        try {
            var referencesField=AnatomyClientNetworking.class.getDeclaredField("predictedMovementReferences");referencesField.setAccessible(true);
            var references=(Map<UUID,Object>)referencesField.get(null);
            var inboxField=AnatomyClientNetworking.class.getDeclaredField("transportReceiptTokens");inboxField.setAccessible(true);
            var inbox=(AnatomyReceiptTokenInbox)inboxField.get(null);
            var tokensField=AnatomyReceiptTokenInbox.class.getDeclaredField("tokens");tokensField.setAccessible(true);
            var tokens=(Map<UUID,?>)tokensField.get(inbox);
            if(!references.containsKey(body) || !tokens.containsKey(body))
                throw new AssertionError("Fixture failed to retain prediction metadata for "+phase+": refs="+references.keySet()+" tokens="+tokens.keySet());
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect S26 prediction metadata",failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertPredictionMetadataAbsent(UUID body,String phase) {
        try {
            var referencesField=AnatomyClientNetworking.class.getDeclaredField("predictedMovementReferences");referencesField.setAccessible(true);
            var references=(Map<UUID,Object>)referencesField.get(null);
            var inboxField=AnatomyClientNetworking.class.getDeclaredField("transportReceiptTokens");inboxField.setAccessible(true);
            var inbox=(AnatomyReceiptTokenInbox)inboxField.get(null);
            var tokensField=AnatomyReceiptTokenInbox.class.getDeclaredField("tokens");tokensField.setAccessible(true);
            var tokens=(Map<UUID,?>)tokensField.get(inbox);
            if(references.containsKey(body) || tokens.containsKey(body))
                throw new AssertionError("Control handoff retained prediction metadata for "+phase+": refs="+references.keySet()+" tokens="+tokens.keySet());
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect S26 prediction metadata",failure);
        }
    }

    private static void assertPredictionOwner(UUID expected,String phase) {
        try {
            var ownerField=AnatomyClientNetworking.class.getDeclaredField("predictionOwner");ownerField.setAccessible(true);
            var observed=(UUID)ownerField.get(null);
            if(!java.util.Objects.equals(expected,observed))
                throw new AssertionError("Prediction owner mismatch during "+phase+": expected="+expected+" observed="+observed);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError("Could not inspect S26 prediction owner",failure);
        }
    }

    private static void assertAuthority(net.minecraft.client.Minecraft client,int firstId,int secondId,int expectedRoot,String phase) {
        var player=client.player;
        var first=client.level.getEntity(firstId);
        var second=client.level.getEntity(secondId);
        var root=player.getRootVehicle();
        if(root==player || root.getId()!=expectedRoot)
            throw new AssertionError("Unexpected local root during "+phase+": "+root);
        boolean playerPredicts=AnatomyMovement.predictsBody(player);
        boolean firstPredicts=AnatomyMovement.predictsBody(first);
        boolean secondPredicts=AnatomyMovement.predictsBody(second);
        if(playerPredicts || (expectedRoot==firstId)!=firstPredicts || (expectedRoot==secondId)!=secondPredicts)
            throw new AssertionError("Full prediction authority did not follow the unique root actor during "+phase
                +": player="+playerPredicts+" first="+firstPredicts+" second="+secondPredicts+" root="+root);
    }
}
