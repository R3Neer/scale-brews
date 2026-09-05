package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BasePressurePlateBlock.class)
public interface TestPlateAccess {
    @Invoker("getSignalStrength") int test$signal(Level level, BlockPos pos);
}
