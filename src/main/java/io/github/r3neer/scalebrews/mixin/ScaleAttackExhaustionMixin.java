package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.ScaleExhaustion;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Player.class)
public abstract class ScaleAttackExhaustionMixin {
    @ModifyArg(method = {"attack", "stabAttack"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), index = 0)
    private float scalebrews$physicalAttack(float amount) {
        return ScaleExhaustion.physical((Player) (Object) this, amount);
    }
}
