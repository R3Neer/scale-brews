package io.github.r3neer.scalebrews.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.r3neer.scalebrews.client.ScaleCamera;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class ScaleCameraMixin {
    @ModifyArg(method = "update", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setupPerspective(FFFFF)V"), index = 0)
    private float scalebrews$renderNear(float near) {
        return ScaleCamera.nearPlane((Camera) (Object) this, near);
    }

    @ModifyArg(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE",
        target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"), index = 2)
    private float scalebrews$cullingNear(float near) {
        return ScaleCamera.nearPlane((Camera) (Object) this, near);
    }

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/entity/ClientAvatarState;getInterpolatedBob(F)F"))
    private float scalebrews$scaledViewBobbing(float bob) {
        // The shared snapshot keeps world and held-item bobbing aligned. Frequency,
        // hurt tilt, FOV, and the user's bobbing toggle remain vanilla.
        return bob * ScaleCamera.factor((Camera) (Object) this);
    }
}
