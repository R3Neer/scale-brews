package io.github.r3neer.scalebrews.client.mixin;

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
    @SuppressWarnings("unchecked")
    private void scalebrews$layer(CallbackInfo ci) {
        addLayer(new TinySaddleLayer<>((RenderLayerParent<S, M>)(Object)this));
    }
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void scalebrews$extract(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        var definition = TinyMounts.definition(entity);
        // Snapshot occupancy separately from equipment and steering: an idle rider still needs a stable seat.
        ((SaddleState)state).scalebrews$occupied(definition != null && entity.isVehicle());
        ((SaddleState)state).scalebrews$saddle(definition != null && entity.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)
            ? definition.saddleVisual().orElse(null) : null);
    }
}
