package io.github.r3neer.scalebrews.potion;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public final class ScalePotions {

    public static final Holder<Potion> GROWTH = register(
        "growth",
        "growth",
        ScaleEffects.GROWTH,
        3600,
        0
    );

    public static final Holder<Potion> STRONG_GROWTH = register(
        "strong_growth",
        "growth",
        ScaleEffects.GROWTH,
        1800,
        1
    );

    public static final Holder<Potion> VERY_STRONG_GROWTH = register(
        "very_strong_growth",
        "growth",
        ScaleEffects.GROWTH,
        900,
        2
    );

    public static final Holder<Potion> SHRINKING = register(
        "shrinking",
        "shrinking",
        ScaleEffects.SHRINKING,
        3600,
        0
    );

    public static final Holder<Potion> STRONG_SHRINKING = register(
        "strong_shrinking",
        "shrinking",
        ScaleEffects.SHRINKING,
        1800,
        1
    );

    public static final Holder<Potion> VERY_STRONG_SHRINKING = register(
        "very_strong_shrinking",
        "shrinking",
        ScaleEffects.SHRINKING,
        900,
        2
    );

    private ScalePotions() {}

    private static Holder<Potion> register(
        String id,
        String name,
        Holder<MobEffect> effect,
        int durationTicks,
        int amplifier
    ) {
        ResourceKey<Potion> key = ResourceKey.create(
            Registries.POTION,
            ScaleBrews.id(id)
        );

        Potion potion = new Potion(
            ScaleBrews.MOD_ID + "." + name,
            new MobEffectInstance(effect, durationTicks, amplifier)
        );

        return Registry.registerForHolder(
            BuiltInRegistries.POTION,
            key,
            potion
        );
    }

    public static void initialize() {
        ScaleBrews.LOGGER.info("Registering Scale Brews potions");
    }
}
