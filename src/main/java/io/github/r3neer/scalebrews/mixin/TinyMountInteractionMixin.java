package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class TinyMountInteractionMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void scalebrews$interact(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        var result = TinyMounts.interact((Mob)(Object)this, player, hand);
        if (result != InteractionResult.PASS) cir.setReturnValue(result);
    }
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void scalebrews$controller(CallbackInfoReturnable<LivingEntity> cir) {
        var player = TinyMounts.controller((Mob)(Object)this);
        if (player != null) cir.setReturnValue(player);
    }
}
