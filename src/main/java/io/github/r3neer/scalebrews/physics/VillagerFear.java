package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.scale.ScaleTransition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Additional local threat rule; vanilla's sensor still owns visibility and panic memory. */
public final class VillagerFear {
    private VillagerFear() {}

    public static boolean isSizeThreat(LivingEntity villager, LivingEntity candidate) {
        return candidate != villager && candidate.isAlive() && !candidate.isSpectator()
                && candidate.distanceToSqr(villager) <= 64
                && position(candidate) - position(villager) >= 2 - 1e-6;
    }

    private static double position(LivingEntity entity) {
        double scale = entity.getAttributeValue(Attributes.SCALE);
        if (!Double.isFinite(scale)) return Double.NaN;
        // Do not cap at tier III: external scales must remain comparable to each other.
        return (scale - 1) / (scale >= 1
                ? ScaleTransition.GROWTH_PER_LEVEL : -ScaleTransition.SHRINKING_PER_LEVEL);
    }
}
