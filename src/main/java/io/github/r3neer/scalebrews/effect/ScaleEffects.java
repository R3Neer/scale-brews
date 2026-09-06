package io.github.r3neer.scalebrews.effect;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.scale.ScaleTransition;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ScaleEffects {

    public static final Holder<MobEffect> GROWTH = register(
        "growth",
        new ScaleMobEffect(MobEffectCategory.NEUTRAL, 0xD98C3F, ScaleTransition.GROWTH_PER_LEVEL)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, ScaleBrews.id("effect.growth.movement_speed"),
                0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(
                Attributes.BLOCK_INTERACTION_RANGE,
                ScaleBrews.id("effect.growth.block_interaction_range"),
                0.20,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
            .addAttributeModifier(
                Attributes.ENTITY_INTERACTION_RANGE,
                ScaleBrews.id("effect.growth.entity_interaction_range"),
                0.50,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
            .addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ScaleBrews.id("effect.growth.attack_damage"),
                0.20,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
            .addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                ScaleBrews.id("effect.growth.knockback_resistance"),
                0.20,
                AttributeModifier.Operation.ADD_VALUE
            )
            .addAttributeModifier(
                Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
                ScaleBrews.id("effect.growth.explosion_knockback_resistance"),
                0.23,
                AttributeModifier.Operation.ADD_VALUE
            )
            .addAttributeModifier(
                Attributes.STEP_HEIGHT,
                ScaleBrews.id("effect.growth.step_height"),
                0.50,
                AttributeModifier.Operation.ADD_VALUE
            )
            .addAttributeModifier(
                Attributes.MAX_HEALTH,
                ScaleBrews.id("effect.growth.max_health"),
                6.0,
                AttributeModifier.Operation.ADD_VALUE
            )
    );

    public static final Holder<MobEffect> SHRINKING = register(
            "shrinking",
        new ScaleMobEffect(MobEffectCategory.NEUTRAL, 0x7657C8, ScaleTransition.SHRINKING_PER_LEVEL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, ScaleBrews.id("effect.shrinking.jump_strength"),
                0.025, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, ScaleBrews.id("effect.shrinking.movement_speed"),
                -0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(
                Attributes.BLOCK_INTERACTION_RANGE,
                ScaleBrews.id("effect.shrinking.block_interaction_range"),
                -0.12,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
            .addAttributeModifier(
                Attributes.ENTITY_INTERACTION_RANGE,
                ScaleBrews.id("effect.shrinking.entity_interaction_range"),
                -0.12,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
            .addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ScaleBrews.id("effect.shrinking.attack_damage"),
                -0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
            .addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                ScaleBrews.id("effect.shrinking.knockback_resistance"),
                -0.25,
                AttributeModifier.Operation.ADD_VALUE
            )
            .addAttributeModifier(
                Attributes.MAX_HEALTH,
                ScaleBrews.id("effect.shrinking.max_health"),
                -4.0,
                AttributeModifier.Operation.ADD_VALUE
            )
    );

    private ScaleEffects() {
    }

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        ResourceKey<MobEffect> key = ResourceKey.create(
                Registries.MOB_EFFECT,
                ScaleBrews.id(name)
        );

        return Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                key,
                effect
        );
    }

    public static void initialize() {
        ScaleBrews.LOGGER.info("Registering Scale Brews effects");
    }
}
