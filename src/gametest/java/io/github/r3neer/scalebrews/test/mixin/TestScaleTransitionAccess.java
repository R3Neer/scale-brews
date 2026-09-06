package io.github.r3neer.scalebrews.test.mixin;
import io.github.r3neer.scalebrews.scale.ScaleTransition;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LivingEntity.class)
public interface TestScaleTransitionAccess {
    @Accessor("scalebrews$transition") ScaleTransition test$transition();
    @Accessor("scalebrews$transition") void test$transition(ScaleTransition value);
}
