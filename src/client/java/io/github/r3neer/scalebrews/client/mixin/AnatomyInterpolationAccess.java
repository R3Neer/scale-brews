package io.github.r3neer.scalebrews.client.mixin;

import net.minecraft.world.entity.InterpolationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InterpolationHandler.class)
public interface AnatomyInterpolationAccess {
    @Accessor("interpolationSteps") int scalebrews$interpolationLength();
}
