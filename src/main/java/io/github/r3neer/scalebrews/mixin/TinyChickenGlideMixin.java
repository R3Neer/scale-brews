package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Chicken.class)
public abstract class TinyChickenGlideMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 scalebrews$gravityAwareGlide(Vec3 movement, double x, double y, double z,
            Operation<Vec3> original) {
        var chicken = (Chicken)(Object)this;
        var rider = TinyMounts.controller(chicken);
        if (rider == null) return original.call(movement, x, y, z);

        var definition = TinyMounts.definition(chicken);
        boolean suppressFallDamping = definition != null
                && definition.ability() == TinyMountDefinition.Ability.CHICKEN_GLIDE
                && !TinyMounts.input(rider).jump();
        var frame = GravityFrames.frame(chicken);

        if (frame.direction() == Direction.DOWN)
            return original.call(movement, x, suppressFallDamping ? 1.0 : y, z);

        // Vanilla's call damps world Y. Under rotated gravity that would modify a
        // tangential axis, so controlled chickens apply only the equivalent local
        // vertical damping. A released glide suppresses that damping entirely.
        if (suppressFallDamping) return movement;
        Vec3 local = frame.toLocal(movement);
        return local.y < 0 ? frame.multiplyLocalVertical(movement, y) : movement;
    }
}
