package io.github.r3neer.scalebrews.client.mixin;

import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin({CowRenderer.class,PigRenderer.class,ChickenRenderer.class})
public interface PlatformVariantModels {
    @Accessor("models") Map<?, ? extends AdultAndBabyModelPair<? extends EntityModel<?>>> scalebrews$models();
}
