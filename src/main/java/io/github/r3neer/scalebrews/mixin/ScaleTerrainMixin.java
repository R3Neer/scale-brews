package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.scalebrews.physics.ScalePhysics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ScaleTerrainMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void scalebrews$ignoreSmallObstacle(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        // A multiplier of ONE still zeros velocity in Entity.move. Skip the stuck state entirely.
        if (ScalePhysics.growth((Entity) (Object) this) == 3 && state.is(ScalePhysics.RESISTED_SLOWDOWN)) ci.cancel();
    }

    @WrapOperation(method = "getBlockSpeedFactor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getSpeedFactor()F"))
    private float scalebrews$groundFactor(Block block, Operation<Float> original) {
        return ScalePhysics.blockSpeed((Entity) (Object) this, block.defaultBlockState(), original.call(block));
    }

    @ModifyVariable(method = "makeStuckInBlock", at = @At("HEAD"), argsOnly = true)
    private Vec3 scalebrews$stuckFactor(Vec3 multiplier, BlockState state, Vec3 original) {
        return ScalePhysics.stuckSpeed((Entity) (Object) this, state, multiplier);
    }
}
