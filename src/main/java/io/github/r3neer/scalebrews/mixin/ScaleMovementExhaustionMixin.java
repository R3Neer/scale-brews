package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.ScaleExhaustion;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerPlayer.class)
public abstract class ScaleMovementExhaustionMixin {
    @ModifyArg(method = {"checkMovementStatistics", "jumpFromGround"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"), index = 0)
    private float scalebrews$physicalMovement(float amount) {
        return ScaleExhaustion.physical((ServerPlayer) (Object) this, amount);
    }
}
