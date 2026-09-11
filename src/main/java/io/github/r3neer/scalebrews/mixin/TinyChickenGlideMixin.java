package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import io.github.r3neer.scalebrews.mount.TinyMountGravity;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class TinyChickenGlideMixin {
    private static final double VANILLA_FALL_DAMPING = 0.6;

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 scalebrews$gravityAwareGlide(Vec3 movement, double x, double y, double z,
            Operation<Vec3> original) {
        var chicken = (Chicken)(Object)this;
        var rider = TinyMounts.controller(chicken);
        if (rider == null) return original.call(movement, x, y, z);

        var frame = GravityFrames.frame(chicken);
        if (frame.direction() != Direction.DOWN) {
            // Vanilla's branch is guarded by world-Y fall state. Under rotated gravity
            // both the guard and this multiplier are the wrong frame, so TAIL applies
            // the equivalent local rule exactly once.
            return movement;
        }

        var definition = TinyMounts.definition(chicken);
        boolean suppressFallDamping = definition != null
                && definition.ability() == TinyMountDefinition.Ability.CHICKEN_GLIDE
                && !TinyMounts.input(rider).jump();
        return original.call(movement, x, suppressFallDamping ? 1.0 : y, z);
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void scalebrews$applyLocalFallDamping(CallbackInfo ci) {
        var chicken = (Chicken)(Object)this;
        var rider = TinyMounts.controller(chicken);
        if (rider == null || chicken.onGround()) return;

        var frame = GravityFrames.frame(chicken);
        if (frame.direction() == Direction.DOWN) return;

        var definition = TinyMounts.definition(chicken);
        boolean suppressFallDamping = definition != null
                && definition.ability() == TinyMountDefinition.Ability.CHICKEN_GLIDE
                && !TinyMounts.input(rider).jump();
        chicken.setDeltaMovement(TinyMountGravity.dampLocalFall(chicken.getDeltaMovement(),frame,VANILLA_FALL_DAMPING,suppressFallDamping));
    }
}
