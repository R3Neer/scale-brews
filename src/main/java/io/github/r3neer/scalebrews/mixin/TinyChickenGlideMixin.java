package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Chicken.class)
public abstract class TinyChickenGlideMixin {
    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"), index = 1)
    private double scalebrews$glide(double vanilla) {
        var chicken = (Chicken)(Object)this;
        var definition = TinyMounts.definition(chicken);
        var player = TinyMounts.controller(chicken);
        if (player == null || definition.ability() != TinyMountDefinition.Ability.CHICKEN_GLIDE) return vanilla;
        return TinyMounts.input(player).jump() ? vanilla : 1;
    }
}
