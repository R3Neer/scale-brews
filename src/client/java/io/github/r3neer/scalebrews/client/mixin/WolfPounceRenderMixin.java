package io.github.r3neer.scalebrews.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.WolfPounceFeedback;
import io.github.r3neer.scalebrews.client.render.WolfPounceVisualState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class WolfPounceRenderMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void scalebrews$wolfVisuals(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof Wolf wolf && state instanceof WolfRenderState) WolfPounceFeedback.extract(wolf, state);
        else ((WolfPounceVisualState)state).scalebrews$wolfScale(1, 1, 1);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", shift = At.Shift.AFTER))
    private void scalebrews$stretch(LivingEntityRenderState state, PoseStack poses, SubmitNodeCollector collector,
                                    CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof WolfRenderState)) return;
        var visual = (WolfPounceVisualState) state;
        poses.scale(visual.scalebrews$wolfScaleX(), visual.scalebrews$wolfScaleY(), visual.scalebrews$wolfScaleZ());
    }
}
