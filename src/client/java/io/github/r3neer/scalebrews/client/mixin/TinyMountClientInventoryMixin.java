package io.github.r3neer.scalebrews.client.mixin;

import io.github.r3neer.scalebrews.mount.TinyMountInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes E follow the configured Tiny Mount family without modifying every target entity class. */
@Mixin(MultiPlayerGameMode.class)
public abstract class TinyMountClientInventoryMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "isServerControlledInventory", at = @At("RETURN"), cancellable = true)
    private void scalebrews$tinyMountInventory(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        var player = minecraft.player;
        if (player != null && player.getVehicle() instanceof Mob mob && TinyMountInventory.canOpen(player, mob))
            cir.setReturnValue(true);
    }
}
