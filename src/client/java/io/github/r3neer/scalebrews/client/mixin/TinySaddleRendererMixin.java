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
import net.minecraft.core.registries.BuiltInRegistries;
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
        boolean equipped = definition != null && entity.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE);
        var legacy = definition == null ? null : definition.saddleVisual().orElse(null);
        var entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        saddle.scalebrews$seatFrame(null);
        saddle.scalebrews$hasSaddle(equipped);
        saddle.scalebrews$saddle(equipped ? legacy : null);
        saddle.scalebrews$visualProfile(definition == null ? null : io.github.r3neer.scalebrews.client.render.TinyMountVisualProfile.resolve(entityType, legacy));
        var mount = (MountPoseState) state;
        mount.scalebrews$entityId(entity.getId());
        mount.scalebrews$tinyMount(definition != null);
        mount.scalebrews$mountType(definition == null ? null : entityType);
    }
}
