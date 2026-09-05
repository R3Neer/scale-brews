package io.github.r3neer.scalebrews.client;

import net.fabricmc.api.ClientModInitializer;

public class ScaleBrewsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		io.github.r3neer.scalebrews.mount.TinyMounts.clientInput = player ->
			player instanceof net.minecraft.client.player.LocalPlayer local ? local.input.keyPresses : net.minecraft.world.entity.player.Input.EMPTY;
	}
}
