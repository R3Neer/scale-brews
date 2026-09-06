package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ScaleBrewsClientTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(ScaleCameraChecks::run);
            context.setScreen(() -> {
                var client = net.minecraft.client.Minecraft.getInstance();
                for (String name : new String[]{"growth", "shrinking"}) {
                    if (client.getResourceManager().getResource(ScaleBrews.id(
                            "textures/gui/sprites/mob_effect/" + name + ".png")).isEmpty()) {
                        throw new AssertionError("Missing effect icon " + name);
                    }
                    String key = "item.minecraft.potion.effect.scalebrews." + name;
                    if (Component.translatable(key).getString().equals(key)) {
                        throw new AssertionError("Missing potion translation " + key);
                    }
                }
                var menu = new BeaconMenu(0, client.player.getInventory());
                var screen = new BeaconScreen(menu, client.player.getInventory(), Component.literal("Scale Brews"));
                menu.setData(0, 4);
                menu.setData(1, BeaconMenu.encodeEffect(ScaleEffects.GROWTH));
                menu.getSlot(0).set(new ItemStack(Items.EMERALD));
                return screen;
            });
            context.waitTicks(10);
            context.takeScreenshot("scale-brews-beacon");
            context.setScreen(() -> null);
            world.getServer().runCommand("effect give @a scalebrews:shrinking 30 2 true");
            context.waitTicks(30);
            context.runOnClient(client -> {
                if (Math.abs(client.player.getScale() - 0.28) > 0.001) {
                    throw new AssertionError("Scale was not synchronized to the client: " + client.player.getScale());
                }
            });
            context.takeScreenshot("scale-brews-shrinking");
            // A tight inside corner: a .28-scale player's half-width is .084 blocks.
            // Build only in this disposable test world, never in a user's save.
            world.getServer().runCommand("fill 3 -61 3 8 -61 8 minecraft:stone");
            world.getServer().runCommand("fill 5 -60 3 5 -57 7 minecraft:stone");
            world.getServer().runCommand("fill 3 -60 5 7 -57 5 minecraft:stone");
            world.getServer().runCommand("tp @a 4.915 -60 4.915 -45 0");
            context.waitTicks(20);
            world.getConnection().waitForChunksRender();
            context.runOnClient(client -> {
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                client.options.bobView().set(true);
                client.options.fov().set(90);
                if (!client.level.getBlockState(new net.minecraft.core.BlockPos(5, -60, 4))
                        .is(net.minecraft.world.level.block.Blocks.STONE)) {
                    throw new AssertionError("Corner fixture missing");
                }
                client.player.setSprinting(true);
                client.player.avatarState().updateBob(.1F);
            });
            context.takeScreenshot("scale-brews-small-corner");
            world.getServer().runCommand("effect clear @a");
            world.getServer().runCommand("fill 8 -61 12 14 -61 19 minecraft:stone");
            world.getServer().runCommand("summon minecraft:chicken 10 -60 16 {Tags:[\"saddle_chicken\"],NoAI:1b,Rotation:[180f,0f]}");
            world.getServer().runCommand("summon minecraft:bee 12 -59.7 16 {Tags:[\"saddle_bee\"],NoAI:1b,Rotation:[180f,0f]}");
            world.getServer().runCommand("item replace entity @e[tag=saddle_chicken,limit=1] saddle with minecraft:saddle");
            world.getServer().runCommand("item replace entity @e[tag=saddle_bee,limit=1] saddle with minecraft:saddle");
            world.getServer().runCommand("tp @a 11 -60 13 0 20");
            context.waitTicks(30);
            context.runOnClient(client -> {
                if (client.level.registryAccess().lookupOrThrow(io.github.r3neer.scalebrews.mount.TinyMounts.REGISTRY).size() != 2)
                    throw new AssertionError("Mount definitions were not synchronized");
                var policy = io.github.r3neer.scalebrews.mount.MountSizePolicy.get(client.level);
                if (policy.ratios().get(net.minecraft.resources.Identifier.withDefaultNamespace("happy_ghast")) != 2.0)
                    throw new AssertionError("Living-mount size policy was not synchronized");
                if (!io.github.r3neer.scalebrews.config.ScaleRules.get(client.level).tinyMounts())
                    throw new AssertionError("Default world rules not synchronized");
                int saddles = 0;
                for (var entity : client.level.entitiesForRendering()) {
                    if (!(entity instanceof net.minecraft.world.entity.Mob mob)) continue;
                    var definition = io.github.r3neer.scalebrews.mount.TinyMounts.definition(mob);
                    if (definition == null || !mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE).is(Items.SADDLE)) continue;
                    var visual = definition.saddleVisual().orElseThrow();
                    if (client.getResourceManager().getResource(visual.texture()).isEmpty())
                        throw new AssertionError("Missing saddle texture " + visual.texture());
                    saddles++;
                }
                if (saddles != 2) throw new AssertionError("Expected two synchronized saddles, got " + saddles);
            });
            context.takeScreenshot("scale-brews-tiny-saddles");
            world.getServer().runCommand("item replace entity @e[tag=saddle_chicken,limit=1] saddle with minecraft:air");
            world.getServer().runCommand("item replace entity @e[tag=saddle_bee,limit=1] saddle with minecraft:air");
            context.waitTicks(10);
            context.takeScreenshot("scale-brews-tiny-unsaddled");
            world.getServer().runCommand("effect give @a scalebrews:shrinking 120 1 true");
            context.waitTicks(25); // Riding follows actual SCALE, not immediate effect presence.
            world.getServer().runCommand("item replace entity @e[tag=saddle_chicken,limit=1] saddle with minecraft:saddle");
            world.getServer().runCommand("data merge entity @e[tag=saddle_chicken,limit=1] {NoAI:0b}");
            world.getServer().runCommand("ride @a[limit=1] mount @e[tag=saddle_chicken,limit=1]");
            context.waitTicks(10);
            context.getInput().lookAt(0, 0);
            var chickenStart = context.computeOnClient(client -> {
                if (!(client.player.getVehicle() instanceof net.minecraft.world.entity.animal.chicken.Chicken))
                    throw new AssertionError("Client did not mount chicken");
                return client.player.getVehicle().position();
            });
            context.getInput().holdKeyFor(options -> options.keyUp, 12);
            context.runOnClient(client -> {
                var delta = client.player.getVehicle().position().subtract(chickenStart);
                if (delta.horizontalDistance() < .25) throw new AssertionError("WASD did not move chicken: " + delta);
            });
            context.takeScreenshot("scale-brews-riding-chicken");
            world.getServer().runCommand("execute as @e[tag=saddle_chicken,limit=1] at @s run tp @s ~ ~10 ~");
            context.getInput().holdKeyFor(options -> options.keyJump, 10);
            double glidingY = context.computeOnClient(client -> client.player.getVehicle().getDeltaMovement().y);
            context.waitTicks(8);
            context.runOnClient(client -> {
                double fallingY = client.player.getVehicle().getDeltaMovement().y;
                if (glidingY >= 0 || fallingY >= glidingY - .1)
                    throw new AssertionError("Releasing Space must restore faster chicken fall: glide=" + glidingY + ", fall=" + fallingY);
            });
            world.getServer().runCommand("ride @a[limit=1] dismount");
            world.getServer().runCommand("item replace entity @e[tag=saddle_bee,limit=1] saddle with minecraft:saddle");
            world.getServer().runCommand("data merge entity @e[tag=saddle_bee,limit=1] {NoAI:0b}");
            world.getServer().runCommand("item replace entity @a weapon.mainhand with scalebrews:flower_on_a_stick");
            world.getServer().runCommand("ride @a[limit=1] mount @e[tag=saddle_bee,limit=1]");
            context.waitTicks(10);
            context.getInput().lookAt(0, -30);
            var beeStart = context.computeOnClient(client -> {
                if (!(client.player.getVehicle() instanceof net.minecraft.world.entity.animal.bee.Bee))
                    throw new AssertionError("Client did not mount bee");
                return client.player.getVehicle().position();
            });
            context.waitTicks(15);
            context.runOnClient(client -> {
                var delta = client.player.getVehicle().position().subtract(beeStart);
                if (delta.y < .3 || delta.horizontalDistance() < .5)
                    throw new AssertionError("Look-directed bee flight failed: " + delta);
            });
            context.takeScreenshot("scale-brews-riding-bee");
            world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            context.waitTicks(5);
            context.runOnClient(client -> {
                if (!(client.player.getVehicle() instanceof net.minecraft.world.entity.animal.bee.Bee bee)
                        || io.github.r3neer.scalebrews.mount.TinyMounts.controller(bee) != null)
                    throw new AssertionError("Removing steering item should retain rider and release manual control");
            });
            world.getServer().runCommand("ride @a[limit=1] dismount");
        }
    }
}
