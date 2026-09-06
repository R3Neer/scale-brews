package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.scale.ScaleSprintHandler;
import net.minecraft.world.entity.LivingEntity;

/** Explicit effect balance, separate from physical SCALE and vanilla charged power. */
public final class ScaleExplosions {
    private ScaleExplosions() {}

    public static float power(LivingEntity creeper, float vanillaPower) {
        int growth = ScaleSprintHandler.level(creeper.getEffect(ScaleEffects.GROWTH));
        int shrink = ScaleSprintHandler.level(creeper.getEffect(ScaleEffects.SHRINKING));
        float larger = switch (growth) { case 1 -> 1.15F; case 2 -> 1.30F; case 3 -> 1.50F; default -> 1; };
        float smaller = switch (shrink) { case 1 -> .90F; case 2 -> .80F; case 3 -> .65F; default -> 1; };
        return vanillaPower * larger * smaller;
    }
}
