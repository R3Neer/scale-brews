package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.render.SaddleState;
import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeeModel.class)
public abstract class MountedBeeModelMixin {
    @Shadow @Final protected ModelPart bone;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/BeeRenderState;)V", at = @At("RETURN"))
    private void scalebrews$stableSeat(BeeRenderState state, CallbackInfo ci) {
        if (!((SaddleState)state).scalebrews$occupied()) return;
        // The passenger anchor follows the entity, whereas vanilla bob/roll moves only the model.
        // The saddle layer copies this same pose; wing, leg, antenna and stinger animation remains native.
        var rest = bone.getInitialPose();
        bone.y = rest.y();
        bone.xRot = rest.xRot();
    }
}
