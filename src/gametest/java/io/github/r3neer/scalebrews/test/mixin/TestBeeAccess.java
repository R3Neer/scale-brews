package io.github.r3neer.scalebrews.test.mixin;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Bee.class)
public interface TestBeeAccess {
    @Invoker("wantsToEnterHive") boolean test$wantsHive();
}
