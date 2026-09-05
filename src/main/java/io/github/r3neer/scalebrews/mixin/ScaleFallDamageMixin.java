package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.r3neer.scalebrews.physics.ScalePhysics;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ScaleFallDamageMixin {
    @ModifyReturnValue(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"))
    private float scalebrews$reduceFallDamage(float original, DamageSource source, float damage) {
        return source.is(DamageTypeTags.IS_FALL)
                ? ScalePhysics.fallDamage(original, ScalePhysics.shrinking((LivingEntity) (Object) this)) : original;
    }
}
