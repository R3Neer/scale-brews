package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.collision.presentation.CausalBodyResidual;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlatformClientRidingMixin {
    @Inject(method="startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",at=@At("RETURN"))
    private void scalebrews$mounted(Entity vehicle,boolean force,boolean sendEvent,CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue())CausalBodyResidual.ridingTopologyChanged((Entity)(Object)this,vehicle);
    }

    @Inject(method="removeVehicle",at=@At("HEAD"))
    private void scalebrews$dismounted(CallbackInfo ci) {
        var passenger=(Entity)(Object)this;
        var vehicle=passenger.getVehicle();
        if(vehicle!=null)CausalBodyResidual.ridingTopologyChanged(passenger,vehicle);
    }
}
