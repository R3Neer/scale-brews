package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Data-family mutation barrier; no concrete mob or renderer knowledge. */
@Mixin(SynchedEntityData.class)
public abstract class AnatomyVariantDataMixin {
    @Shadow @Final private SyncedDataHolder entity;

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", at = @At("TAIL"))
    private <T> void scalebrews$variantChanged(EntityDataAccessor<T> key, T value, boolean force, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) AnatomyRuntime.variantChanged(living);
    }
}
