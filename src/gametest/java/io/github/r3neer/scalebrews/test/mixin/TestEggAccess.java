package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.TurtleEggBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TurtleEggBlock.class)
public interface TestEggAccess {
    @Invoker("canDestroyEgg") boolean test$canCrush(ServerLevel level, Entity entity);
}
