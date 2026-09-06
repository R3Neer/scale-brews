package io.github.r3neer.scalebrews.test.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Modded atlases can tick during loading, before the first frame creates the Globals buffer. */
@Mixin(TextureAtlas.class)
public abstract class TestAtlasStartupMixin {
    @Inject(method="uploadAnimationFrames",at=@At("HEAD"),cancellable=true)
    private void scalebrews$waitForFirstFrame(CallbackInfo ci) {
        if(RenderSystem.getGlobalSettingsUniform()==null) ci.cancel();
    }
}
