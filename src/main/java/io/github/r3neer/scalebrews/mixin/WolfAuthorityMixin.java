package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.WolfMount;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class WolfAuthorityMixin {
    @Inject(method = {"isClientAuthoritative", "isLocalClientAuthoritative"}, at = @At("HEAD"), cancellable = true)
    private void scalebrews$serverControl(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof Wolf wolf && WolfMount.enabled(wolf)) cir.setReturnValue(false);
    }
}
