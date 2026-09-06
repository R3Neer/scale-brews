package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.client.platform.PlatformCamera;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class PlatformClientTeleportMixin {
    @Inject(method="handleMovePlayer",at=@At("RETURN"))
    private void scalebrews$resetCamera(ClientboundPlayerPositionPacket packet,CallbackInfo ci) {
        PlatformCamera.reset();
    }
}
