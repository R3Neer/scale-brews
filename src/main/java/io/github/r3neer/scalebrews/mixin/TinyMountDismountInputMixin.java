package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Release the mounting Shift gesture before treating a new press as dismount. */
@Mixin(Player.class)
public abstract class TinyMountDismountInputMixin {
    @Unique private Entity scalebrews$inputVehicle;
    @Unique private boolean scalebrews$awaitShiftRelease;
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void scalebrews$dismount(CallbackInfoReturnable<Boolean> cir) {
        var player = (Player)(Object)this;
        var vehicle = player.getVehicle();
        if (vehicle != scalebrews$inputVehicle) {
            scalebrews$inputVehicle = vehicle;
            scalebrews$awaitShiftRelease = vehicle instanceof TamableAnimal && TinyMounts.definition(vehicle) != null
                    && player.isShiftKeyDown();
        }
        if (!player.isShiftKeyDown()) scalebrews$awaitShiftRelease = false;
        if (scalebrews$awaitShiftRelease) cir.setReturnValue(false);
    }
}
