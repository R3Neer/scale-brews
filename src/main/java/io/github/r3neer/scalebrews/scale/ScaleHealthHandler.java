package io.github.r3neer.scalebrews.scale;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class ScaleHealthHandler {

    private static final Map<LivingEntity, Float> PENDING_HEALTH_RATIOS =
            new IdentityHashMap<>();

    private ScaleHealthHandler() {}

    public static void initialize() {
        ServerMobEffectEvents.BEFORE_ADD.register((effect, entity, context) -> {
            if (isScaleEffect(effect)) {
                saveHealthRatio(entity);
            }
        });

        ServerMobEffectEvents.AFTER_ADD.register((effect, entity, context) -> {
            if (isScaleEffect(effect)) {
                restoreHealthRatio(entity);
            }
        });

        ServerMobEffectEvents.BEFORE_REMOVE.register((effect, entity, context) -> {
            if (isScaleEffect(effect)) {
                saveHealthRatio(entity);
            }
        });

        ServerMobEffectEvents.AFTER_REMOVE.register((effect, entity, context) -> {
            if (isScaleEffect(effect)) {
                restoreHealthRatio(entity);
            }
        });
    }

    private static boolean isScaleEffect(MobEffectInstance effect) {
        return effect.is(ScaleEffects.GROWTH) || effect.is(ScaleEffects.SHRINKING);
    }

    private static void saveHealthRatio(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();

        if (maxHealth > 0.0F) {
            PENDING_HEALTH_RATIOS.put(
                entity,
                entity.getHealth() / maxHealth
            );
        }
    }

    private static void restoreHealthRatio(LivingEntity entity) {
        Float ratio = PENDING_HEALTH_RATIOS.remove(entity);

        if (ratio != null) {
            float newHealth = entity.getMaxHealth() * ratio;
            entity.setHealth(Math.max(0.0F, newHealth));
        }
    }
}