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
                menu.setData(0, 4);
                menu.setData(1, BeaconMenu.encodeEffect(ScaleEffects.GROWTH));
                menu.getSlot(0).set(new ItemStack(Items.EMERALD));
                return new BeaconScreen(menu, client.player.getInventory(), Component.literal("Scale Brews"));
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
        }
    }
}
