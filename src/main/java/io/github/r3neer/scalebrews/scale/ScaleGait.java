package io.github.r3neer.scalebrews.scale;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Size-derived presentation factors for footsteps and locomotion animation. */
public final class ScaleGait {
    private static final float MIN_STEP_PITCH = 0.5F;
    private static final float MAX_STEP_PITCH = 2.0F;
    private static final float MIN_WALK_TIME_SCALE = 0.25F;
    private static final float MAX_WALK_TIME_SCALE = 4.0F;

    private ScaleGait() {}

    public static float stepPitch(LivingEntity entity, float vanillaPitch) {
        return vanillaPitch * stepPitchMultiplier(entity.getAttributeValue(Attributes.SCALE));
    }

    public static float walkTimeScale(LivingEntity entity, float vanillaTimeScale) {
        return vanillaTimeScale * walkTimeMultiplier(entity.getAttributeValue(Attributes.SCALE));
    }

    public static float stepPitchMultiplier(double scale) {
        double safeScale = sanitize(scale);
        return (float) Math.clamp(1.0 / Math.sqrt(safeScale), MIN_STEP_PITCH, MAX_STEP_PITCH);
    }

    public static float walkTimeMultiplier(double scale) {
        double safeScale = sanitize(scale);
        return (float) Math.clamp(1.0 / safeScale, MIN_WALK_TIME_SCALE, MAX_WALK_TIME_SCALE);
    }

    private static double sanitize(double scale) {
        return Double.isFinite(scale) && scale > 0.0 ? scale : 1.0;
    }
}
