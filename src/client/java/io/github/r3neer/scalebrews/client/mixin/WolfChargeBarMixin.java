package io.github.r3neer.scalebrews.client.mixin;
import io.github.r3neer.scalebrews.mount.WolfMount;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Reuse the horse HUD with a monotonic charge for wolves. */
@Mixin(LocalPlayer.class)
public abstract class WolfChargeBarMixin {
    @Shadow private int jumpRidingTicks;
    @Inject(method = "getJumpRidingScale", at = @At("HEAD"), cancellable = true)
    private void charge(CallbackInfoReturnable<Float> cir) {
        if (((LocalPlayer)(Object)this).getVehicle() instanceof Wolf wolf && WolfMount.enabled(wolf))
            cir.setReturnValue(Math.clamp(jumpRidingTicks * .1F, 0F, 1F));
    }
}
