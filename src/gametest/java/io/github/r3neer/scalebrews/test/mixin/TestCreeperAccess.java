package io.github.r3neer.scalebrews.test.mixin;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Creeper.class)
public interface TestCreeperAccess {
    @Invoker("explodeCreeper") void test$explode();
}
