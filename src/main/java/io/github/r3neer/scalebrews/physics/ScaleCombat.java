package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;

public final class ScaleCombat {
    public static final ResourceKey<DamageType> LANDING = ResourceKey.create(Registries.DAMAGE_TYPE, ScaleBrews.id("growth_landing"));
    private ScaleCombat() {}
    public static double knockback(double original, DamageSource source) {
        if (source == null || source.is(LANDING) || source.is(DamageTypes.THORNS)
                || source.getDirectEntity() != source.getEntity() || !(source.getEntity() instanceof Player p)) return original;
        return original * (1 + .1 * ScalePhysics.growth(p)) * (1 - .1 * ScalePhysics.shrinking(p));
    }
}
