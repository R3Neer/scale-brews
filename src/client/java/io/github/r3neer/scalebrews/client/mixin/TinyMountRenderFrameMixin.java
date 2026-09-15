package io.github.r3neer.scalebrews.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.render.MountRenderFrame;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LevelRenderer.class)
public class TinyMountRenderFrameMixin {
    @WrapMethod(method = "submitEntities")
    private void scalebrews$frame(PoseStack poses, LevelRenderState state, SubmitNodeCollector collector, Operation<Void> original) {
        MountRenderFrame.begin(state.entityRenderStates);
        try { original.call(poses, state, collector); }
        finally { MountRenderFrame.end(); }
    }
}
