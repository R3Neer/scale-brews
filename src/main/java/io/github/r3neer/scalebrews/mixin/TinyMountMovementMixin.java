package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class TinyMountMovementMixin {
    @Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
    private void scalebrews$input(Player player, Vec3 original, CallbackInfoReturnable<Vec3> cir) {
        if ((Object)this instanceof Mob mob && TinyMounts.controller(mob) == player)
            cir.setReturnValue(TinyMounts.groundInput(player));
    }
    @Inject(method = "getRiddenSpeed", at = @At("HEAD"), cancellable = true)
    private void scalebrews$speed(Player player, CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof Mob mob && TinyMounts.controller(mob) == player)
            cir.setReturnValue(TinyMounts.definition(mob).speed());
    }
    @Inject(method = "tickRidden", at = @At("HEAD"))
    private void scalebrews$orient(Player player, Vec3 input, CallbackInfo ci) {
        if ((Object)this instanceof Mob mob && TinyMounts.controller(mob) == player) {
            mob.setYRot(player.getYRot());
            mob.yRotO = mob.getYRot();
            mob.yBodyRot = mob.getYRot();
            mob.yHeadRot = mob.getYRot();
            mob.setJumping(false);
            if (!mob.level().isClientSide()) mob.getNavigation().stop();
        }
    }
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void scalebrews$noJump(CallbackInfo ci) {
        if ((Object)this instanceof Mob mob && TinyMounts.controller(mob) != null) ci.cancel();
    }
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void scalebrews$fly(Vec3 input, CallbackInfo ci) {
        if (!((Object)this instanceof Mob mob)) return;
        var player = TinyMounts.controller(mob);
        if (player == null) return;
        var definition = TinyMounts.definition(mob);
        if (definition.movement() != TinyMountDefinition.Movement.FLYING_LOOK_DIRECTION) return;
        if (mob.canSimulateMovement()) {
            mob.setDeltaMovement(TinyMounts.flightVelocity(player, definition));
            mob.move(MoverType.SELF, mob.getDeltaMovement());
            mob.resetFallDistance();
        }
        ci.cancel();
    }
}
