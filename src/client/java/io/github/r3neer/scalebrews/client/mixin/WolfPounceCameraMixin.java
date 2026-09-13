package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.r3neer.scalebrews.client.WolfPounceFeedback;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class WolfPounceCameraMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float scalebrews$pounceFov(float original) {
        return WolfPounceFeedback.modifyFov(original);
    }

    @ModifyReturnValue(method = "getViewRotationMatrix", at = @At("RETURN"))
    private Matrix4f scalebrews$pounceShake(Matrix4f matrix) {
        return WolfPounceFeedback.applyViewShake(matrix);
    }
}
