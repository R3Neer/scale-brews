package io.github.r3neer.scalebrews.test.mixin;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AbstractArrow.class)
public interface TestArrowAccess {
    @Invoker("onHitEntity") void test$hit(EntityHitResult hit);
}
