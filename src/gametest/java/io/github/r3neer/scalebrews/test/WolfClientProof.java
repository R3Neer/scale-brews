package io.github.r3neer.scalebrews.test;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
public class WolfClientProof implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context) {
        var properties=new java.util.Properties();
        properties.setProperty("allow-flight","false");
        try(var server=context.worldBuilder().createServer(properties); var connection=server.connect()) {
            server.runCommand("fill -10 -61 -10 20 -61 30 minecraft:stone");
            server.runCommand("summon minecraft:wolf 0 -60 4 {Tags:[\"wolf_proof\"],Silent:1b}");
            server.runCommand("tp @a 0 -60 0 0 20");
            final int[] wolfId={0};
            server.runOnServer(s->{
                var player=s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                for(var e:s.overworld().getAllEntities()) if(e instanceof Wolf wolf && e.entityTags().contains("wolf_proof")) {
                    wolfId[0]=wolf.getId();wolf.tame(player);wolf.setOrderedToSit(true);
                    wolf.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
                    wolf.setItemSlot(EquipmentSlot.BODY,new ItemStack(Items.WOLF_ARMOR));
                }
            });
            context.waitTicks(30);connection.waitForChunksRender();
            server.runCommand("tp @a 2 -60 1 35 20");
            context.waitTicks(10);
            context.takeScreenshot("scale-brews-wolf-saddle-armor");
            server.runOnServer(s->{
                var player=s.getPlayerList().getPlayers().getFirst();
                var wolf=(Wolf)s.overworld().getEntity(wolfId[0]);
                player.getAttribute(Attributes.SCALE).setBaseValue(.76);
                wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
                if(!player.startRiding(wolf))throw new AssertionError("Wolf mount rejected");
            });
            context.waitTicks(25);
            context.runOnClient(client->{
                if(!(client.player.getVehicle() instanceof Wolf wolf) || wolf.isClientAuthoritative())throw new AssertionError("Server-authoritative wolf not synchronized");
                client.player.setYRot(0);client.player.setXRot(0);
                client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                client.options.keyUp.setDown(true);
            });
            context.waitTicks(12);
            context.runOnClient(client->client.options.keyUp.setDown(false));
            context.waitTicks(20);
            final Vec3[] start={Vec3.ZERO};
            server.runOnServer(s->start[0]=s.overworld().getEntity(wolfId[0]).position());
            context.runOnClient(client->client.options.keyJump.setDown(true));
            context.waitTicks(9);
            context.takeScreenshot("scale-brews-wolf-pounce-charge");
            context.runOnClient(client->client.options.keyJump.setDown(false));
            context.waitTicks(25);
            server.runOnServer(s->{
                var wolf=(Wolf)s.overworld().getEntity(wolfId[0]);
                var player=s.getPlayerList().getPlayers().getFirst();
                double distance=wolf.position().subtract(start[0]).horizontalDistance();
                System.out.println("WOLF_CLIENT_POUNCE distance="+distance);
                if(distance<3.5 || distance>5.5)throw new AssertionError("Real packet pounce distance: "+distance);
                if(player.getVehicle()!=wolf)throw new AssertionError("Rider desynchronized");
            });
            context.takeScreenshot("scale-brews-wolf-pounce-landed");
            context.runOnClient(client->client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        }
    }
}
