package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Observes real vanilla old-position replacements, including value round trips. */
@Mixin(Entity.class)
public abstract class AnatomyObserverBaselineMixin {
    @WrapMethod(method="setOldPosAndRot")
    private void scalebrews$oldPosition(Operation<Void> original) {
        var body=(Entity)(Object)this;
        var before=body.oldPosition();
        original.call();
        if(!before.equals(body.oldPosition()))CausalRemoteResidual.nativeBaselineChanged(body);
    }
}
