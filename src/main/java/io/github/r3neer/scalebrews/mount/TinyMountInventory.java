package io.github.r3neer.scalebrews.mount;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Registration and server opening seam for inventory-bearing Tiny Mount families. */
public final class TinyMountInventory {
    public static final ExtendedMenuType<TinyMountMenu, Integer> MENU = Registry.register(
            BuiltInRegistries.MENU,
            ScaleBrews.id("tiny_mount"),
            new ExtendedMenuType<>((id, inventory, entityId) -> TinyMountMenu.client(id, inventory, entityId),
                    ByteBufCodecs.VAR_INT.cast()));

    private TinyMountInventory() {}
    public static void initialize() {}

    public static boolean canOpen(Player player, Mob mount) {
        return player.getVehicle() == mount && mount.isAlive() && TinyMounts.hasMountInventory(mount);
    }

    public static void open(ServerPlayer player, Mob mount) {
        if (!canOpen(player, mount)) return;
        player.openMenu(new ExtendedMenuProvider<Integer>() {
            @Override public Integer getScreenOpeningData(ServerPlayer ignored) { return mount.getId(); }
            @Override public Component getDisplayName() { return mount.getDisplayName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                return new TinyMountMenu(id, inventory, mount);
            }
        });
    }
}
