package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.ScaleCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

// Combatify 1.4 wraps at 1400. Run outside it so alternate physics receive the scaled input.
@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class ScaleAttackKnockbackMixin {
    @WrapMethod(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V")
    private void scalebrews$attack(double power, double x, double z, DamageSource source, float damage, boolean effect, Operation<Void> original) {
        original.call(ScaleCombat.knockback(power, source), x, z, source, damage, effect);
    }
}
