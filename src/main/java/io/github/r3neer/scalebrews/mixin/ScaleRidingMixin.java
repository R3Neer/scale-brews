package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ScaleRidingMixin {
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("HEAD"), cancellable = true)
    private void scalebrews$mount(Entity vehicle, boolean force, boolean sendEvent, CallbackInfoReturnable<Boolean> cir) {
        // Equipment/effect packets may arrive after the passenger packet. Only the server
        // authorizes mounting; rechecking a stale client snapshot can strand a server-side rider.
        if ((Object)this instanceof Player p && !p.level().isClientSide()
                && !TinyMounts.mayMount(p, vehicle)) cir.setReturnValue(false);
    }
}
