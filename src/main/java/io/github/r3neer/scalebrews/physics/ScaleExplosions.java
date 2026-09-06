package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.scale.ScaleSize;
import net.minecraft.world.entity.LivingEntity;

/** Size-interpolated balance, multiplied by the existing (possibly charged) explosion power. */
public final class ScaleExplosions {
    private ScaleExplosions() {}

    public static float power(LivingEntity creeper, float vanillaPower) {
        double growth = ScaleSize.growth(creeper);
        double shrink = ScaleSize.shrinking(creeper);
        return (float)(vanillaPower * (growth > 0
                ? ScaleSize.interpolate(growth, 1, 1.15, 1.30, 1.50)
                : ScaleSize.interpolate(shrink, 1, .90, .80, .65)));
    }
}
