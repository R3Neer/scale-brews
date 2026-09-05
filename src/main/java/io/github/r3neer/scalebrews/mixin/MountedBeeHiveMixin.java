package io.github.r3neer.scalebrews.mixin;

import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.class)
public abstract class MountedBeeHiveMixin {
    @Inject(method = "wantsToEnterHive", at = @At("HEAD"), cancellable = true)
    private void scalebrews$occupied(CallbackInfoReturnable<Boolean> cir) {
        var bee = (Bee)(Object)this;
        if (bee.isVehicle() && io.github.r3neer.scalebrews.mount.TinyMounts.definition(bee) != null)
            cir.setReturnValue(false);
    }
}
