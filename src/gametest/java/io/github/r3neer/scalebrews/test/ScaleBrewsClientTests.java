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
        }
    }
}
