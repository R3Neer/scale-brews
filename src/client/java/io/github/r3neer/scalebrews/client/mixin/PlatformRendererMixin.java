package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.platform.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(EntityRenderer.class)
public abstract class PlatformRendererMixin {
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void scalebrews$state(Entity entity,EntityRenderState state,float partial,CallbackInfo ci) {
        ((PlatformRenderState)state).scalebrews$platformOffset(PlatformVisuals.sampling()?Vec3.ZERO:PlatformVisuals.offset(entity,partial));
    }
    @Inject(method="getRenderOffset",at=@At("RETURN"),cancellable=true)
    private void scalebrews$offset(EntityRenderState state,CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(cir.getReturnValue().add(((PlatformRenderState)state).scalebrews$platformOffset()));
    }
}
