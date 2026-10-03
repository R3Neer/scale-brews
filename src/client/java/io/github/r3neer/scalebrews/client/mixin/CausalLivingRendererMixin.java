package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelRender;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalPresentationState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class CausalLivingRendererMixin {
    @Shadow protected EntityModel<?> model;

    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("RETURN"))
    private void scalebrews$capture(LivingEntity entity,LivingEntityRenderState state,float partialTick,CallbackInfo ci){
        CausalModelRender.capture(entity,state);
    }

    @WrapMethod(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    private void scalebrews$scope(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector collector,
            CameraRenderState camera,Operation<Void> original){
        if(((CausalPresentationState)state).scalebrews$causalToken()==null){
            original.call(state,poses,collector,camera);
            return;
        }
        try(var scope=CausalModelRender.enterSubmit(state,poses)){
            original.call(state,poses,collector,camera);
        }
    }

    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;isBodyVisible(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Z"))
    private void scalebrews$root(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector collector,
            CameraRenderState camera,CallbackInfo ci){
        CausalModelRender.applyRoot(state,model,poses);
    }

    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;setupAnim(Ljava/lang/Object;)V",shift=At.Shift.AFTER))
    private void scalebrews$layers(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector collector,
            CameraRenderState camera,CallbackInfo ci){
        CausalModelRender.applyLayers(state,model);
    }
}
