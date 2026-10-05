package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.collision.integration.MovingBodyContext;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class PlatformEntityMixin {
    @Inject(method="teleport",at=@At("HEAD"))
    private void scalebrews$transition(net.minecraft.world.level.portal.TeleportTransition transition,CallbackInfoReturnable<Entity> cir) {
        scalebrews$invalidateLifecycle();
    }
    @Inject(method="teleportTo(DDD)V",at=@At("HEAD"))
    private void scalebrews$teleport(double x,double y,double z,CallbackInfo ci) {
        scalebrews$invalidateLifecycle();
    }
    @Inject(method="remove",at=@At("HEAD"))
    private void scalebrews$remove(Entity.RemovalReason reason,CallbackInfo ci) {
        scalebrews$invalidateLifecycle();
    }
    @Unique
    private void scalebrews$invalidateLifecycle() {
        Entity self=(Entity)(Object)this;
        if(self instanceof LivingEntity living){AnatomyMovement.invalidateRoot(living);MaterialIntervalRuntime.invalidate(living);}
        else AnatomyMovement.invalidateBodyLifecycle(self);
    }
    @Inject(method="setPos(DDD)V",at=@At("RETURN"))
    private void scalebrews$spatialSetPos(double x,double y,double z,CallbackInfo ci) {
        if((Object)this instanceof LivingEntity living)AnatomyMovement.spatialMutation(living);
    }
    @Inject(method="refreshDimensions",at=@At("RETURN"))
    private void scalebrews$spatialDimensions(CallbackInfo ci) {
        AnatomyMovement.revalidateDimensions((Entity)(Object)this);
        if((Object)this instanceof LivingEntity living) {
            AnatomyMovement.spatialMutation(living);
        }
    }
    @WrapMethod(method="move")
    private void scalebrews$move(MoverType type, Vec3 delta, Operation<Void> original) {
        Entity self=(Entity)(Object)this;
        var root=self instanceof LivingEntity living && AnatomyApi.ready(living)?MaterialIntervalRuntime.captureRoot(living):null;
        if(AnatomyApi.ready(self) && AnatomyMovement.predictsBody(self))AnatomyMovement.carry(self);
        Entity previous=MovingBodyContext.enter(self);
        try {
            original.call(type,delta);
            if(self instanceof LivingEntity living && root!=null) {
                MaterialIntervalRuntime.commitRoot(living,root);
                // Root mutations are causally ordered by the move call itself. Drain now, before a
                // later body can enter the swept region and be affected retrospectively.
                if(living.level() instanceof ServerLevel serverLevel)MaterialPhysicsRuntime.drain(serverLevel);
            }
            if(AnatomyApi.ready(self) && AnatomyMovement.predictsBody(self))AnatomyMovement.afterMove(self);
        }
        finally { MovingBodyContext.exit(previous); }
    }
    @Inject(method="collide",at=@At("RETURN"),cancellable=true)
    private void scalebrews$collide(Vec3 delta, CallbackInfoReturnable<Vec3> cir) {
        Entity self=(Entity)(Object)this;
        if(AnatomyApi.ready(self) && AnatomyMovement.predictsBody(self))
            cir.setReturnValue(AnatomyMovement.collide(self,cir.getReturnValue()));
    }
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void scalebrews$pair(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (MovingBodyContext.suppressPair((Entity)(Object)this,other)) cir.setReturnValue(false);
    }
}
