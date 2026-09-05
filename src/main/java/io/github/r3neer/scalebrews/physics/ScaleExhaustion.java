package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.scale.ScaleSprintHandler;
import net.minecraft.world.entity.player.Player;

/** Called only at physical activity sites, never from the global food-data sink. */
public final class ScaleExhaustion {
    private ScaleExhaustion() {}

    public static float physical(Player player, float original) {
        int growth = ScaleSprintHandler.level(player.getEffect(ScaleEffects.GROWTH));
        int shrinking = ScaleSprintHandler.level(player.getEffect(ScaleEffects.SHRINKING));
        return original * (1.0F + 0.10F * growth) * (1.0F - 0.05F * shrinking);
    }
}
