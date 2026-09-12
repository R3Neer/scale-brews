package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/**
 * Test-source-only bridge for the legacy anatomy fixture that used the former
 * collision-internal gravity registry. It owns no state: every installation
 * terminates at the shared Scale Brews gravity authority.
 */
final class GravityFrames {
    private GravityFrames() {}

    static void install(String owner, Function<Entity, GravityFrame> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        String current = io.github.r3neer.scalebrews.integration.gravity.GravityFrames.owner();
        if (current != null) {
            if (current.equals(owner) || current.equals("gravity_changer")) return;
            throw new IllegalStateException("Unexpected gravity provider already installed by " + current);
        }
        io.github.r3neer.scalebrews.integration.gravity.GravityFrames.install(owner,
            entity -> Objects.requireNonNull(resolver.apply(entity), "gravity frame").down());
    }
}
