package io.github.r3neer.scalebrews;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.potion.ScalePotions;
import io.github.r3neer.scalebrews.brewing.ScaleBrewing;
import io.github.r3neer.scalebrews.scale.ScaleSprintHandler;
import io.github.r3neer.scalebrews.beacon.ScaleBeacon;

public class ScaleBrews implements ModInitializer {
	public static final String MOD_ID = "scalebrews";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		io.github.r3neer.scalebrews.integration.gravity.GravityFrames.initialize();
		// Built-in family ids must exist on dedicated before canonical binding validation.
		// Client-only model/animation preparation implementations are installed separately.
		io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines.initialize();
		io.github.r3neer.scalebrews.collision.pose.BuiltInPoseEngines.initialize();
		// Canonical catalogs validate engine ids during construction. Register the
		// one-way legacy migration sentinels before any runtime/resource catalog can load.
		io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData.initializeCompatibilityEngines();
		ScaleEffects.initialize();
		io.github.r3neer.scalebrews.loot.ScaleLoot.initialize();
		io.github.r3neer.scalebrews.platform.Platforms.initialize();
		io.github.r3neer.scalebrews.config.ScaleRules.initialize();
		ScalePotions.initialize();
		ScaleBrewing.initialize();
		ScaleSprintHandler.initialize();
		ScaleBeacon.initialize();
		io.github.r3neer.scalebrews.item.ScaleItems.initialize();
		io.github.r3neer.scalebrews.mount.TinyMounts.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
