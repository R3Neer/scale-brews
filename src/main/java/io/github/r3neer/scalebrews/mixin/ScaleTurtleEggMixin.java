package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.ScalePhysics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.TurtleEggBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TurtleEggBlock.class)
public abstract class ScaleTurtleEggMixin {
    @Inject(method = "canDestroyEgg", at = @At("HEAD"), cancellable = true)
    private void scalebrews$canCrush(ServerLevel level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ScalePhysics.weightless(entity)) cir.setReturnValue(false);
    }
}
