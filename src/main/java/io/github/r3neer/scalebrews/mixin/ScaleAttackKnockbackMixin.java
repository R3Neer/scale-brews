package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.ScaleCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class ScaleAttackKnockbackMixin {
    @ModifyVariable(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double scalebrews$attack(double original, double power, double x, double z, DamageSource source, float damage, boolean effect) {
        return ScaleCombat.knockback(original, source);
    }
}
