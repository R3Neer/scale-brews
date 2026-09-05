package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.GrowthImpact;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class GrowthImpactMixin {
    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void scalebrews$landing(double movementY, boolean onGround, BlockState ground, BlockPos pos, CallbackInfo ci) {
        if (onGround && (Object) this instanceof Player player && !player.level().isClientSide()) {
            // Vanilla adds the final downward movement before processing the landing.
            GrowthImpact.land(player, player.fallDistance + Math.max(0, -movementY), ground);
        }
    }
}
