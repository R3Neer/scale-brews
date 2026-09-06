package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.platform.Platforms;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PlatformServerPlayerMixin {
    @Inject(method="teleport",at=@At("HEAD"))
    private void scalebrews$transition(TeleportTransition transition,CallbackInfoReturnable<ServerPlayer> cir) {
        Platforms.state((ServerPlayer)(Object)this).clear();
    }
    @Inject(method={"teleportTo(DDD)V","teleportRelative"},at=@At("HEAD"))
    private void scalebrews$teleport(double x,double y,double z,CallbackInfo ci) {
        Platforms.state((ServerPlayer)(Object)this).clear();
    }
}
