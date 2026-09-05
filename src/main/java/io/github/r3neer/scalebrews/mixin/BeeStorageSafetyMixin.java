package io.github.r3neer.scalebrews.mixin;

import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeehiveBlockEntity.class)
public abstract class BeeStorageSafetyMixin {
    @Inject(method = "addOccupant", at = @At("HEAD"))
    private void scalebrews$dismountBeforeStorage(Bee bee, CallbackInfo ci) {
        if (io.github.r3neer.scalebrews.mount.TinyMounts.definition(bee) == null) return;
        if (((BeehiveBlockEntity)(Object)this).isFull()) return;
        for (var passenger : java.util.List.copyOf(bee.getPassengers())) {
            if (passenger instanceof Player player) {
                player.stopRiding();
                player.resetFallDistance();
            }
        }
    }
}
