package io.github.r3neer.scalebrews.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.render.MountRenderFrame;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class TinyMountRenderFrameMixin {
    @Inject(method = "submitEntities", at = @At("HEAD"))
    private void scalebrews$begin(PoseStack poses, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        MountRenderFrame.begin(state.entityRenderStates);
    }

    @Inject(method = "submitEntities", at = @At("RETURN"))
    private void scalebrews$end(PoseStack poses, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        MountRenderFrame.end();
    }
}
