package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMountInventory;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** E while riding opens the same equipment surface players expect from vanilla mounts. */
@Mixin({Chicken.class, Bee.class, Wolf.class})
public abstract class TinyMountInventoryScreenMixin implements HasCustomInventoryScreen {
    @Override
    public void openCustomInventoryScreen(Player player) {
        Mob self = (Mob)(Object)this;
        if (player instanceof ServerPlayer serverPlayer && player.getVehicle() == self
                && TinyMounts.definition(self) != null) {
            TinyMountInventory.open(serverPlayer, self);
        }
    }
}
