package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TemptGoal.class)
public interface TestTemptGoalAccess {
    @Invoker("shouldFollow") boolean scalebrews$follows(LivingEntity player);
}
