package io.github.r3neer.scalebrews.test.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface TestFoodAccess {
    @Accessor("exhaustionLevel") float test$exhaustion();
    @Accessor("exhaustionLevel") void test$exhaustion(float amount);
}
