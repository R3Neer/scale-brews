package io.github.r3neer.scalebrews.scale;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Physical size is the sole input; potion levels only determine the SCALE target. */
public final class ScaleSize {
    private ScaleSize() {}

    public static double signedLevel(LivingEntity entity) {
        double scale = entity.getAttributeValue(Attributes.SCALE);
        if (!Double.isFinite(scale)) return 0;
        return scale >= 1 ? Math.clamp((scale - 1) / ScaleTransition.GROWTH_PER_LEVEL, 0, 3)
                : -Math.clamp((1 - scale) / -ScaleTransition.SHRINKING_PER_LEVEL, 0, 3);
    }

    public static double growth(LivingEntity entity) { return Math.max(0, signedLevel(entity)); }
    public static double shrinking(LivingEntity entity) { return Math.max(0, -signedLevel(entity)); }
    public static int tier(double magnitude) { return Math.clamp((int)Math.floor(magnitude + 1e-6), 0, 3); }

    public static double interpolate(double magnitude, double neutral, double one, double two, double three) {
        double value = Math.clamp(magnitude, 0, 3);
        double[] anchors = {neutral, one, two, three};
        int lower = Math.min(2, (int)value);
        return anchors[lower] + (anchors[lower + 1] - anchors[lower]) * (value - lower);
    }

    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        var health = ScaleHealthHandler.capture(entity, true);
        apply(entity, ScaleEffects.GROWTH.value(), growth(entity));
        apply(entity, ScaleEffects.SHRINKING.value(), shrinking(entity));
        health.restore(entity);
    }

    private static void apply(LivingEntity entity, net.minecraft.world.effect.MobEffect effect, double magnitude) {
        effect.createModifiers(2, (type, template) -> {
            if (type.equals(Attributes.SCALE)) return;
            var attribute = entity.getAttribute(type);
            if (attribute == null) return;
            // Reach must continue following eye height even beyond the potion tiers.
            // Other mechanics retain their established tier-III cap.
            double effectiveMagnitude = magnitude;
            if (type.equals(Attributes.ENTITY_INTERACTION_RANGE) && effect == ScaleEffects.GROWTH.value()) {
                double scale = entity.getAttributeValue(Attributes.SCALE);
                effectiveMagnitude = Double.isFinite(scale)
                        ? Math.max(0, (scale - 1) / ScaleTransition.GROWTH_PER_LEVEL) : 0;
            }
            double amount = template.amount() * effectiveMagnitude / 3;
            var existing = attribute.getModifier(template.id());
            if (amount == 0) attribute.removeModifier(template.id());
            else if (existing == null || existing.amount() != amount)
                attribute.addOrUpdateTransientModifier(new AttributeModifier(template.id(), amount, template.operation()));
        });
    }
}
