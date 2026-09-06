package io.github.r3neer.scalebrews.scale;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Per-entity, server-authoritative multiplier; the native attribute synchronizes it. */
public final class ScaleTransition {
    public static final int DURATION = 20;
    public static final double GROWTH_PER_LEVEL = 0.96;
    public static final double SHRINKING_PER_LEVEL = -0.242;
    public static final Identifier MODIFIER = ScaleBrews.id("scale_transition");
    private static final Identifier OLD_GROWTH = ScaleBrews.id("effect.growth.scale");
    private static final Identifier OLD_SHRINKING = ScaleBrews.id("effect.shrinking.scale");
    private double current = 1;
    private double start = 1;
    private double target = 1;
    private int elapsed = DURATION;
    private boolean initialized;

    public void tick(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.SCALE);
        if (attribute == null) return;
        if (!initialized) {
            AttributeModifier running = attribute.getModifier(MODIFIER);
            if (running != null) current = 1 + running.amount();
            // Migrate old saved permanent modifiers without an abrupt size change.
            AttributeModifier oldGrowth = attribute.getModifier(OLD_GROWTH);
            AttributeModifier oldShrinking = attribute.getModifier(OLD_SHRINKING);
            if (oldGrowth != null) current *= 1 + oldGrowth.amount();
            if (oldShrinking != null) current *= 1 + oldShrinking.amount();
            attribute.removeModifier(OLD_GROWTH);
            attribute.removeModifier(OLD_SHRINKING);
            start = target = current;
            initialized = true;
        }
        int growth = ScaleSprintHandler.level(entity.getEffect(ScaleEffects.GROWTH));
        int shrinking = ScaleSprintHandler.level(entity.getEffect(ScaleEffects.SHRINKING));
        double desired = (1 + GROWTH_PER_LEVEL * growth) * (1 + SHRINKING_PER_LEVEL * shrinking);
        if (Double.compare(desired, target) != 0) {
            start = current;
            target = desired;
            elapsed = 0;
        }
        if (elapsed < DURATION) current = interpolate(start, target, ++elapsed);
        if (current == 1) {
            attribute.removeModifier(MODIFIER);
        } else {
            AttributeModifier existing = attribute.getModifier(MODIFIER);
            if (existing == null || Double.compare(existing.amount(), current - 1) != 0) {
                attribute.addOrUpdateTransientModifier(new AttributeModifier(
                        MODIFIER, current - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    public static double interpolate(double start, double target, int ticks) {
        double t = Math.clamp(ticks / (double) DURATION, 0, 1);
        return t == 1 ? target : start + (target - start) * t * t * (3 - 2 * t);
    }
}
