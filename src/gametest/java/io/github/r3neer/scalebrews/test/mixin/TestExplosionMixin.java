package io.github.r3neer.scalebrews.test.mixin;
import io.github.r3neer.scalebrews.test.ExplosionObservation;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerExplosion.class)
public abstract class TestExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"))
    private void test$observe(CallbackInfoReturnable<Integer> ci) {
        ExplosionObservation.radius = ((ServerExplosion)(Object)this).radius();
    }
}
