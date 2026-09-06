package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.platform.PlatformCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class PlatformCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Inject(method="alignWithEntity",at=@At("RETURN"))
    private void scalebrews$platformCamera(float partial,CallbackInfo ci) {
        Camera self=(Camera)(Object)this;
        setPosition(self.position().add(PlatformCamera.offset(self,partial)));
    }
}
