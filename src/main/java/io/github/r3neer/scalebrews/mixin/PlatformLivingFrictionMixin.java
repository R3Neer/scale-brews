package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.r3neer.scalebrews.platform.Platforms;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class PlatformLivingFrictionMixin {
    @ModifyExpressionValue(method="travelInAir",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float scalebrews$friction(float original) {
        return (float)Platforms.friction((LivingEntity)(Object)this,original);
    }
}
