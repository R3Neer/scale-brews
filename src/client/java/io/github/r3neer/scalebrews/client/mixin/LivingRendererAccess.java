package io.github.r3neer.scalebrews.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingRendererAccess {
    @Invoker("setupRotations") void scalebrews$rotations(LivingEntityRenderState state, PoseStack poses, float yaw, float scale);
    @Invoker("scale") void scalebrews$scale(LivingEntityRenderState state, PoseStack poses);
}
