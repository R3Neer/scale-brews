package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.ScaleExplosions;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Creeper.class)
public abstract class ScaleCreeperExplosionMixin {
    @ModifyArg(method = "explodeCreeper", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"), index = 4)
    private float scalebrews$power(float vanillaPower) {
        return ScaleExplosions.power((Creeper) (Object) this, vanillaPower);
    }
}
