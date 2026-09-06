package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.r3neer.scalebrews.platform.Platforms;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public abstract class PlatformItemMixin {
    @ModifyExpressionValue(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float scalebrews$friction(float original) { return (float)Platforms.friction((ItemEntity)(Object)this,original); }
}
