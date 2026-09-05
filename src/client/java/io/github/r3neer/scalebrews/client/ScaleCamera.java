package io.github.r3neer.scalebrews.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;

/** Rendering-only correction; never changes the player's position or collision box. */
public final class ScaleCamera {
    private ScaleCamera() {}

    public static float factor(Camera camera) {
        if (camera.isDetached() || camera.isPanoramicMode() || !(camera.entity() instanceof Player player)) return 1;
        float scale = player.getScale();
        // Bound the near plane to at least 0.0025 blocks for extreme external scales.
        return Float.isFinite(scale) ? Math.clamp(scale, 0.05F, 1.0F) : 1;
    }

    public static float nearPlane(Camera camera, float original) {
        return original * factor(camera);
    }
}
