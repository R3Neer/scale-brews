package io.github.r3neer.scalebrews.scale;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public final class ScaleSprintHandler {
    private static final double[] GROWTH_SPRINT = {1.20, 1.12, 1.05};
    private static final double[] SHRINKING_SPRINT = {1.50, 1.90, 2.50};
    private ScaleSprintHandler() {}

    public static void initialize() {
        ServerMobEffectEvents.AFTER_ADD.register((effect, entity, context) -> refresh(effect, entity));
        ServerMobEffectEvents.AFTER_REMOVE.register((effect, entity, context) -> refresh(effect, entity));
    }

    public static int level(MobEffectInstance effect) {
        return effect == null ? 0 : Math.min(3, effect.getAmplifier() + 1);
    }

    /** Discrete ability tier, based on physical size rather than effect presence. */
    public static int state(LivingEntity entity) {
        double size = ScaleSize.signedLevel(entity);
        return size >= 0 ? ScaleSize.tier(size) : -ScaleSize.tier(-size);
    }

    public static AttributeModifier sprintModifierFor(LivingEntity entity, AttributeModifier original) {
        if (!(entity instanceof Player)) return original;
        double state = ScaleSize.signedLevel(entity);
        if (state == 0) return original;
        // Sprint is a multiplier of the already modified walking speed, not a second
        // multiplication of vanilla's 1.3. Other movement modifiers (e.g. Speed) survive.
        double[] anchors = state > 0 ? GROWTH_SPRINT : SHRINKING_SPRINT;
        double multiplier = ScaleSize.interpolate(Math.abs(state), 1 + original.amount(), anchors[0], anchors[1], anchors[2]);
        return new AttributeModifier(original.id(), multiplier - 1.0, original.operation());
    }

    private static void refresh(MobEffectInstance effect, LivingEntity entity) {
        if (entity instanceof Player && (effect.is(ScaleEffects.GROWTH) || effect.is(ScaleEffects.SHRINKING))) {
            entity.setSprinting(entity.isSprinting());
        }
    }
}
