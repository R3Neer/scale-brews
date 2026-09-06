package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.render.BeeRiderPose;
import io.github.r3neer.scalebrews.client.render.RiderPoseState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class AnimatedRiderRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void scalebrews$pose(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        BeeRiderPose.extract(entity, state, partialTick);
    }

    @WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    private void scalebrews$animate(LivingEntityRenderState state, PoseStack poses, SubmitNodeCollector collector,
                                   CameraRenderState camera, Operation<Void> original) {
        var animation = ((RiderPoseState)state).scalebrews$riderPose();
        if (animation == null) { original.call(state, poses, collector, camera); return; }
        poses.pushPose();
        try {
            poses.mulPose(animation);
            original.call(state, poses, collector, camera);
        } finally {
            poses.popPose();
        }
    }
}
