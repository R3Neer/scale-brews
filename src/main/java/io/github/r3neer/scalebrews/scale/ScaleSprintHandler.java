package io.github.r3neer.scalebrews.scale;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public final class ScaleSprintHandler {

    private static final double[] GROWTH_SPRINT_BONUSES = {0.20, 0.10, 0.0};

    private static final double[] SHRINKING_SPRINT_BONUSES = {
        0.60, 0.90, 1.20
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

    public static int level(MobEffectInstance effect) {
        return effect == null ? 0 : Math.min(3, effect.getAmplifier() + 1);
    }

    public static int state(LivingEntity entity) {
        int growth = level(entity.getEffect(ScaleEffects.GROWTH));
        return growth > 0 ? growth : -level(entity.getEffect(ScaleEffects.SHRINKING));
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
