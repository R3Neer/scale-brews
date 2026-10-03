package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelRender;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class CausalRenderReloadMixin {
    @Inject(method="onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V",at=@At("RETURN"))
    private void scalebrews$reload(ResourceManager manager,CallbackInfo ci){CausalModelRender.resourceReload();}
}
