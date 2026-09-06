package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.WolfMount;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class WolfConnectionMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;updateAwaitingTeleport()Z"), cancellable = true)
    private void scalebrews$ignoreClientPosition(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        if (player.getRootVehicle() instanceof Wolf wolf && WolfMount.enabled(wolf)) ci.cancel();
    }
}
