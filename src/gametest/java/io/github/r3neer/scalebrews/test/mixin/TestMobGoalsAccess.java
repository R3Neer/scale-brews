package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface TestMobGoalsAccess {
    @Accessor("goalSelector") GoalSelector scalebrews$goals();
    @Invoker("serverAiStep") void scalebrews$aiStep();
}
