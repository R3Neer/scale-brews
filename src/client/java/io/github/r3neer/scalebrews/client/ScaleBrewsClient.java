package io.github.r3neer.scalebrews.client;

import io.github.r3neer.scalebrews.mount.TinyMountInventory;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class ScaleBrewsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.initialize();
        io.github.r3neer.scalebrews.client.render.TinyMountVisualProfile.initialize();
        io.github.r3neer.scalebrews.client.platform.PlatformClient.initialize();
        WolfPounceFeedback.initialize();
        MenuScreens.register(TinyMountInventory.MENU, TinyMountScreen::new);
        io.github.r3neer.scalebrews.mount.TinyMounts.clientInput = player ->
                player instanceof net.minecraft.client.player.LocalPlayer local ? local.input.keyPresses : net.minecraft.world.entity.player.Input.EMPTY;
    }
}
