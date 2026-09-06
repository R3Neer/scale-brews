package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.scale.ScaleSize;
import net.minecraft.world.entity.player.Player;

/** Called only at physical activity sites, never from the global food-data sink. */
public final class ScaleExhaustion {
    private ScaleExhaustion() {}

    public static float physical(Player player, float original) {
        double growth = ScaleSize.growth(player);
        double shrinking = ScaleSize.shrinking(player);
        return (float)(original * (1 + .10 * growth - .05 * shrinking));
    }
}
