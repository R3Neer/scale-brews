package io.github.r3neer.scalebrews.client.mixin;

import net.minecraft.client.model.EntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Transitional accessor for vanilla's adult model while Mojang migrates
 * age-aware renderers away from the deprecated AgeableMobRenderer type.
 *
 * <p>The target is named rather than imported so Scale Brews does not compile
 * against the deprecated class itself. Vanilla 26.2 still uses that class for
 * several renderers, so the field bridge remains necessary until the upstream
 * renderer migration exposes a non-deprecated adult-model API.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.AgeableMobRenderer")
public interface AgeableModelAccess {
    @Accessor("adultModel") EntityModel<?> scalebrews$adultModel();
}
