package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.platform.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class PlatformEntityMixin implements PlatformBody {
    @Unique private final PlatformState scalebrews$platform = new PlatformState();
    public PlatformState scalebrews$platform() { return scalebrews$platform; }
    @Inject(method="teleport",at=@At("HEAD"))
    private void scalebrews$transition(net.minecraft.world.level.portal.TeleportTransition transition,CallbackInfoReturnable<Entity> cir) {
        scalebrews$platform.clear();
    }
    @Inject(method="teleportTo(DDD)V",at=@At("HEAD"))
    private void scalebrews$teleport(double x,double y,double z,CallbackInfo ci) {
        scalebrews$platform.clear();
    }
    @WrapMethod(method="move")
    private void scalebrews$move(MoverType type, Vec3 delta, Operation<Void> original) {
        Entity self=(Entity)(Object)this;
        PlatformPhysics.carry(self);
        var before=self instanceof LivingEntity living ? PlatformGeometry.frame(living) : null;
        Entity previous=PlatformPhysics.enter(self);
        try {
            original.call(type,delta); PlatformPhysics.afterMove(self);
            if(before!=null) PlatformPhysics.movedSupport((LivingEntity)self,before);
        }
        finally { PlatformPhysics.exit(previous); }
    }
    @Inject(method="collide",at=@At("RETURN"),cancellable=true)
    private void scalebrews$collide(Vec3 delta, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(PlatformPhysics.collide((Entity)(Object)this,delta,cir.getReturnValue()));
    }
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void scalebrews$pair(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (PlatformPhysics.suppressPair((Entity)(Object)this,other)) cir.setReturnValue(false);
    }
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void scalebrews$supportContact(Entity other,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if(Platforms.state(self).support==other || Platforms.state(other).support==self) ci.cancel();
    }
}
