package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.test.CollisionPerformanceProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test-only instrumentation. Production remains untouched. */
@Mixin(ServerLevel.class)
public abstract class TestCollisionEntityScanMixin {
    @Inject(method = "getAllEntities", at = @At("RETURN"), cancellable = true)
    private void scalebrewsTest$countCollisionGlobalEnumeration(CallbackInfoReturnable<Iterable<Entity>> cir) {
        if (!CollisionPerformanceProbe.active() || !CollisionPerformanceProbe.collisionCaller()) return;
        cir.setReturnValue(CollisionPerformanceProbe.wrapFullEnumeration(cir.getReturnValue()));
    }
}
