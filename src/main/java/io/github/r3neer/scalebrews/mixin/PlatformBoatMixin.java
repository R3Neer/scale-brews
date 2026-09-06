package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.platform.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBoat.class)
public abstract class PlatformBoatMixin {
    @Inject(method="canVehicleCollide",at=@At("HEAD"),cancellable=true)
    private static void scalebrews$pair(Entity boat, Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (PlatformPhysics.suppressPair(boat,other)) cir.setReturnValue(false);
    }
    @Inject(method="getGroundFriction",at=@At("RETURN"),cancellable=true)
    private void scalebrews$friction(CallbackInfoReturnable<Float> cir) {
        Entity self=(Entity)(Object)this;
        if (Platforms.supported(self)) cir.setReturnValue((float)Platforms.friction(self,.6));
    }
}
