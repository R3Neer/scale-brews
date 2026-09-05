package io.github.r3neer.scalebrews.scale;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public final class ScaleSprintHandler {

    private static final double[] GROWTH_SPRINT_BONUSES = {
        0.27, 0.216
    };

    private static final double[] SHRINKING_SPRINT_BONUSES = {
        0.354, 0.414, 0.51
    };

    private ScaleSprintHandler() {}

    public static void initialize() {
        ServerMobEffectEvents.AFTER_ADD.register((effect, entity, context) -> {
            if (isScaleEffect(effect)) {
                refreshSprint(entity);
            }
        });

        ServerMobEffectEvents.AFTER_REMOVE.register((effect, entity, context) -> {
            if (isScaleEffect(effect)) {
                refreshSprint(entity);
            }
        });
    }

    public static boolean blocksSprinting(LivingEntity entity) {
        MobEffectInstance growth = entity.getEffect(ScaleEffects.GROWTH);
        return entity instanceof Player && growth != null && growth.getAmplifier() >= 2;
    }

    public static AttributeModifier sprintModifierFor (
        LivingEntity entity,
        AttributeModifier vanillaModifier
    ) {
        if (!(entity instanceof Player)) {
            return vanillaModifier;
        }

        MobEffectInstance growth = entity.getEffect(ScaleEffects.GROWTH);
        if (growth != null) {
            return withBonus(
                    vanillaModifier,
                    bonusFor(growth, GROWTH_SPRINT_BONUSES)
            );
        }

        MobEffectInstance shrinking = entity.getEffect(ScaleEffects.SHRINKING);
        if (shrinking != null) {
            return withBonus(
                    vanillaModifier,
                    bonusFor(shrinking, SHRINKING_SPRINT_BONUSES)
            );
        }

        return vanillaModifier;
    }

    private static boolean isScaleEffect(MobEffectInstance effect) {
        return effect.is(ScaleEffects.GROWTH) || effect.is(ScaleEffects.SHRINKING);
    }

    private static void refreshSprint(LivingEntity entity) {
        if (entity instanceof Player) {
            entity.setSprinting(entity.isSprinting());
        }
    }

    private static double bonusFor(MobEffectInstance effect, double[] bonuses) {
        int levelIndex = Math.min(effect.getAmplifier(), bonuses.length - 1);
        return bonuses[levelIndex];
    }

    private static AttributeModifier withBonus(AttributeModifier vanillaModifier, double bonus) {
        return new AttributeModifier(
            vanillaModifier.id(),
            bonus,
            vanillaModifier.operation()
        );
    }
}