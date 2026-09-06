package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.WolfTaming;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WolfRiderLifecycleMixin {
    @Inject(method = "removePassenger", at = @At("TAIL"))
    private void scalebrews$wildDismount(Entity passenger, CallbackInfo ci) {
        if ((Object)this instanceof Wolf wolf) WolfTaming.dismounted(wolf, passenger);
    }
}
