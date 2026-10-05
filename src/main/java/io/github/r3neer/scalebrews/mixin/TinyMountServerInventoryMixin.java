package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMountInventory;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles vanilla OPEN_INVENTORY for data-driven Tiny Mount families. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TinyMountServerInventoryMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void scalebrews$openTinyMountInventory(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) return;
        if (player.getVehicle() instanceof Mob mob && TinyMountInventory.canOpen(player, mob)) {
            TinyMountInventory.open(player, mob);
            ci.cancel();
        }
    }
}
