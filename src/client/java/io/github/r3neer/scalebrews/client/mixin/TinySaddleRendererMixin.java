package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.render.MountPoseCaptureLayer;
import io.github.r3neer.scalebrews.client.render.MountPoseState;
import io.github.r3neer.scalebrews.client.render.SaddleState;
import io.github.r3neer.scalebrews.client.render.TinySaddleLayer;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class TinySaddleRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Shadow protected abstract boolean addLayer(RenderLayer<S, M> layer);

    @Inject(method = "<init>", at = @At("RETURN"))
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void scalebrews$layer(CallbackInfo ci) {
        var parent = (RenderLayerParent<S, M>)(Object)this;
        addLayer(new MountPoseCaptureLayer((LivingEntityRenderer)(Object)this, parent));
        addLayer(new TinySaddleLayer<>(parent));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void scalebrews$extract(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        var definition = TinyMounts.definition(entity);
        var saddle = (SaddleState) state;
        saddle.scalebrews$saddlePose(null);
        saddle.scalebrews$saddle(definition != null && entity.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)
                ? definition.saddleVisual().orElse(null) : null);
        var mount = (MountPoseState) state;
        mount.scalebrews$entityId(entity.getId());
        mount.scalebrews$hasPassengers(entity.isVehicle());
        mount.scalebrews$mountAnchor(definition == null ? null : definition.saddleVisual().map(v -> v.anchor()).orElse(null));
    }
}
