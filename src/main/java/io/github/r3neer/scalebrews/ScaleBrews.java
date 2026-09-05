package io.github.r3neer.scalebrews;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.potion.ScalePotions;
import io.github.r3neer.scalebrews.brewing.ScaleBrewing;
import io.github.r3neer.scalebrews.scale.ScaleHealthHandler;
import io.github.r3neer.scalebrews.scale.ScaleSprintHandler;

public class ScaleBrews implements ModInitializer {
	public static final String MOD_ID = "scalebrews";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ScaleEffects.initialize();
		ScalePotions.initialize();
		ScaleBrewing.initialize();
		ScaleHealthHandler.initialize();
		ScaleSprintHandler.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
