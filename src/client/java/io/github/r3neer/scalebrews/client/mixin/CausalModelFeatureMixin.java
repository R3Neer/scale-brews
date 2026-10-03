package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelRender;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public abstract class CausalModelFeatureMixin {
    @WrapOperation(method="prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void scalebrews$draw(Model<?> model,PoseStack poses,VertexConsumer vertices,int light,int overlay,int color,
            Operation<Void> original,ModelFeatureRenderer.Submit<?> submit){
        CausalModelRender.draw(submit.state(),model,()->original.call(model,poses,vertices,light,overlay,color));
    }
}
