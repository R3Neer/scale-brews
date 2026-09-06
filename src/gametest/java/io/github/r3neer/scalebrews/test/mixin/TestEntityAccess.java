package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface TestEntityAccess {
    @Accessor("boardingCooldown") void test$setBoardingCooldown(int ticks);
    @Invoker("getBlockSpeedFactor") float test$blockSpeed();
    @Invoker("checkFallDamage") void test$land(double movementY, boolean onGround, BlockState ground, BlockPos pos);
    @Accessor("stuckSpeedMultiplier") Vec3 test$stuckSpeed();
    @Accessor("stuckSpeedMultiplier") void test$setStuckSpeed(Vec3 value);
}
