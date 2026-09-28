package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
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
            context.runOnClient(client->assertAuthority(client,firstId.get(),secondId.get(),firstId.get(),"boat A"));

            // Move authority directly from A to B. The old root must stop simulating immediately once
            // the client observes B as its new root; no retained contact/cursor can keep A authoritative.
            world.runCommand("ride @a[limit=1] dismount");
            world.runCommand("ride @a[limit=1] mount @e[tag=s26_boat_b,limit=1]");
            context.waitFor(client->client.player!=null && client.player.getRootVehicle()!=client.player
                && client.player.getRootVehicle().getId()==secondId.get(),100);
            context.runOnClient(client->assertAuthority(client,firstId.get(),secondId.get(),secondId.get(),"boat B"));

            // Dismount returns prediction to the player and retires both vehicle roots.
            world.runCommand("ride @a[limit=1] dismount");
            context.waitFor(client->client.player!=null && client.player.getRootVehicle()==client.player,100);
            context.runOnClient(client->{
                var player=client.player;
                var first=client.level.getEntity(firstId.get());
                var second=client.level.getEntity(secondId.get());
                if(!AnatomyMovement.predictsBody(player) || AnatomyMovement.predictsBody(first) || AnatomyMovement.predictsBody(second))
                    throw new AssertionError("Dismount left stale vehicle full-prediction authority: player="
                        +AnatomyMovement.predictsBody(player)+" first="+AnatomyMovement.predictsBody(first)
                        +" second="+AnatomyMovement.predictsBody(second));
            });

            System.out.println("S26_CONTROL_HANDOFF PASS player -> boatA -> boatB -> player prediction authority");
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
