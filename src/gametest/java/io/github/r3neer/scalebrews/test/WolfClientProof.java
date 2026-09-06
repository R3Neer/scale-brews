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
        var previousBindings=context.computeOnClient(client -> new String[]{client.options.keyShift.saveString(), client.options.keyUse.saveString()});
        context.runOnClient(client -> {
            client.options.keyShift.setKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.c"));
            client.options.keyUse.setKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.r"));
            net.minecraft.client.KeyMapping.resetMapping();
        });
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
                player.getAttribute(Attributes.SCALE).setBaseValue(.76);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.SADDLE,2));
            });
            server.runCommand("tp @a 0 -60 2.5");
            context.waitTicks(25);
            context.runOnClient(client->client.options.keyShift.setDown(true));
            context.waitTicks(5);
            server.runCommand("execute as @a at @s anchored eyes run tp @s ~ ~ ~ facing entity @e[tag=wolf_proof,limit=1] eyes");
            context.waitTicks(5);
            context.runOnClient(client->{
                if (!(client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) || hit.getEntity().getId()!=wolfId[0])
                    throw new AssertionError("Actual use input is not pointing at the wolf");
            });
            context.getInput().holdKeyFor(options -> options.keyUse, 1);
            context.waitTicks(8);
            server.runOnServer(s->{if(s.getPlayerList().getPlayers().getFirst().getVehicle()==null)
                throw new AssertionError("Actual crouch + use did not mount/stay mounted on sitting wolf");});
            server.runOnServer(s->{
                var player=s.getPlayerList().getPlayers().getFirst();
                var wolf=(Wolf)s.overworld().getEntity(wolfId[0]);
                if(player.getMainHandItem().getCount()!=2 || !wolf.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE))
                    throw new AssertionError("Riding with a spare saddle consumed/replaced equipment");
            });
            context.runOnClient(client->client.options.keyShift.setDown(false));
            context.waitTicks(25);
            context.runOnClient(client->client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
            context.getInput().holdKeyFor(options -> options.keyJump, 2);
            boolean animated=false;
            for(int tick=0;tick<15;tick++) {
                context.waitTicks(1);
                if(context.computeOnClient(client->{
                    var wolf=(Wolf)client.player.getVehicle();
                    System.out.println("WOLF_ATTACK_CLIENT swing="+wolf.swinging+" progress="+wolf.getAttackAnim(1)+" aggressive="+wolf.isAggressive());
                    return wolf.swinging && wolf.getAttackAnim(1)>0 && wolf.isAggressive();
                })) { animated=true;break; }
            }
            if(!animated) throw new AssertionError("Real Space bite did not synchronize native attack animation and pose");
            context.takeScreenshot("scale-brews-wolf-commanded-attack");
            context.waitTicks(25);
            context.runOnClient(client->{
                var wolf=(Wolf)client.player.getVehicle();
                if(wolf.swinging || wolf.isAggressive() || wolf.isAngry())
                    throw new AssertionError("Commanded attack left a stale animation or anger state");
            });
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
            server.runOnServer(s->{
                var wolf=(Wolf)s.overworld().getEntity(wolfId[0]);
                wolf.setItemSlot(EquipmentSlot.SADDLE,ItemStack.EMPTY);
            });
            context.waitTicks(5);
            server.runOnServer(s->{
                var wolf=(Wolf)s.overworld().getEntity(wolfId[0]);
                if(s.getPlayerList().getPlayers().getFirst().getVehicle()!=wolf
                    || io.github.r3neer.scalebrews.mount.TinyMounts.controller(wolf)!=null)
                    throw new AssertionError("Unsaddled rider must remain without control");
            });
            context.runOnClient(client->client.options.keyShift.setDown(true));
            context.waitTicks(5);
            context.runOnClient(client->{
                client.options.keyShift.setDown(false);
                if(client.player.isPassenger())throw new AssertionError("New Shift press did not dismount");
            });
            context.runOnClient(client->client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
            for (var held : java.util.List.of(Items.STONE, Items.BEEF, Items.SADDLE)) {
                server.runOnServer(s->{
                    var player=s.getPlayerList().getPlayers().getFirst();
                    var wolf=(Wolf)s.overworld().getEntity(wolfId[0]);
                    wolf.setNoAi(true);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(held,2));
                });
                server.runCommand("tp @e[tag=wolf_proof,limit=1] 0 -60 4");
                server.runCommand("tp @a 0 -60 2.5");
                context.waitTicks(10);
                context.runOnClient(client->client.options.keyShift.setDown(true));
                context.waitTicks(5);
                server.runCommand("execute as @a at @s anchored eyes run tp @s ~ ~ ~ facing entity @e[tag=wolf_proof,limit=1] eyes");
                context.waitTicks(5);
                context.getInput().holdKeyFor(options -> options.keyUse, 1);
                context.waitTicks(8);
                server.runOnServer(s->{
                    var player=s.getPlayerList().getPlayers().getFirst();
                    var wolf=(Wolf)s.overworld().getEntity(wolfId[0]);
                    if(player.getVehicle()!=wolf || player.getMainHandItem().getCount()!=2 || wolf.isInLove()
                        || !wolf.getItemBySlot(EquipmentSlot.SADDLE).isEmpty())
                        throw new AssertionError("Repeated real Shift + click with "+held+" failed or used item");
                });
                context.runOnClient(client->client.options.keyShift.setDown(false));context.waitTicks(5);
                context.runOnClient(client->client.options.keyShift.setDown(true));context.waitTicks(5);
                server.runOnServer(s->{if(s.getPlayerList().getPlayers().getFirst().isPassenger())
                    throw new AssertionError("Repeated ride must still allow explicit dismount");});
                context.runOnClient(client->client.options.keyShift.setDown(false));context.waitTicks(5);
            }
        } finally {
            context.runOnClient(client -> {
                client.options.keyShift.setDown(false);
                client.options.keyUse.setDown(false);
                client.options.keyShift.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(previousBindings[0]));
                client.options.keyUse.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(previousBindings[1]));
                net.minecraft.client.KeyMapping.resetMapping();
            });
        }
    }
}
