package io.github.r3neer.scalebrews.scale;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class ScaleHealthHandler {

    private ScaleHealthHandler() {}

    public static boolean isScaleEffect(MobEffectInstance effect) {
        return effect.is(ScaleEffects.GROWTH) || effect.is(ScaleEffects.SHRINKING);
    }

    public static Snapshot capture(LivingEntity entity, boolean relevant) {
        float max = entity.getMaxHealth();
        return new Snapshot(relevant && !entity.level().isClientSide() && max > 0,
                max, entity.getHealth() / max);
    }

    public record Snapshot(boolean active, float oldMax, float ratio) {
        public void restore(LivingEntity entity) {
            if (active && entity.getMaxHealth() != oldMax) {
                entity.setHealth(Math.clamp(ratio, 0.0F, 1.0F) * entity.getMaxHealth());
            }
        }
    }
}
