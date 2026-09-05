package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.r3neer.scalebrews.physics.ScalePhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FarmlandBlock.class)
public abstract class ScaleFarmlandMixin {
    @WrapWithCondition(method = "fallOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private boolean scalebrews$canTrample(Entity entity, BlockState state, Level level, BlockPos pos) {
        return !ScalePhysics.weightless(entity);
    }
}
