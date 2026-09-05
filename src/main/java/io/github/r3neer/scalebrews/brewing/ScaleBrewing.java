package io.github.r3neer.scalebrews.brewing;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.potion.ScalePotions;
import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class ScaleBrewing {

    private static final Identifier ELASTIC_TENDON_ID = Identifier.fromNamespaceAndPath("alexsmobs", "elastic_tendon");

    private ScaleBrewing() {}

    private static Item growthStarterIngredient() {
        return BuiltInRegistries.ITEM.getOptional(ELASTIC_TENDON_ID).orElse(Items.SLIME_BALL);
    }

    public static void initialize() {
        FabricPotionBrewingBuilder.BUILD.register(builder -> {
            builder.addMix(
                Potions.AWKWARD,
                growthStarterIngredient(),
                ScalePotions.GROWTH
            );

            builder.addMix(
                ScalePotions.GROWTH,
                Items.GLOWSTONE_DUST,
                ScalePotions.STRONG_GROWTH
            );

            builder.addMix(
                ScalePotions.STRONG_GROWTH,
                Items.GLOWSTONE_DUST,
                ScalePotions.VERY_STRONG_GROWTH
            );

            builder.addMix(
                ScalePotions.GROWTH,
                Items.FERMENTED_SPIDER_EYE,
                ScalePotions.SHRINKING
            );

            builder.addMix(
                ScalePotions.STRONG_GROWTH,
                Items.FERMENTED_SPIDER_EYE,
                ScalePotions.STRONG_SHRINKING
            );

            builder.addMix(
                ScalePotions.VERY_STRONG_GROWTH,
                Items.FERMENTED_SPIDER_EYE,
                ScalePotions.VERY_STRONG_SHRINKING
            );

            builder.addMix(
                ScalePotions.SHRINKING,
                Items.GLOWSTONE_DUST,
                ScalePotions.STRONG_SHRINKING
            );

            builder.addMix(
                ScalePotions.STRONG_SHRINKING,
                Items.GLOWSTONE_DUST,
                ScalePotions.VERY_STRONG_SHRINKING
            );
        });

        ScaleBrews.LOGGER.info("Registering Scale Brews brewing recipes");
    }
}