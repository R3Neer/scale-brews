package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Counts actual vanilla interpolation steps, including their target adjustment barrier. */
@Mixin(InterpolationHandler.class)
public abstract class AnatomyObserverInterpolationMixin {
    @Shadow @Final private Entity entity;

    @WrapMethod(method="interpolate")
    private void scalebrews$interpolate(Operation<Void> original) {
        var handler=(InterpolationHandler)(Object)this;
        var beforeTarget=handler.position();
        boolean beforeActive=handler.hasActiveInterpolation();
        original.call();
        CausalRemoteResidual.interpolated(entity,handler,beforeTarget,beforeActive);
    }

    @WrapMethod(method="cancel")
    private void scalebrews$cancel(Operation<Void> original) {
        original.call();
        CausalRemoteResidual.retire(entity);
    }
}
