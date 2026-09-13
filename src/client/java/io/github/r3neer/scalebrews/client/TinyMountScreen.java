package io.github.r3neer.scalebrews.client;

import io.github.r3neer.scalebrews.mount.TinyMountMenu;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** Vanilla mount layout without a storage grid; BODY appears only when declared by family data. */
public final class TinyMountScreen extends AbstractMountInventoryScreen<TinyMountMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/horse.png");

    public TinyMountScreen(TinyMountMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 0, menu.mount());
    }

    @Override protected Identifier getBackgroundTextureLocation() { return BACKGROUND; }
    @Override protected Identifier getSlotSpriteLocation() { return SLOT_SPRITE; }
    @Override protected @Nullable Identifier getChestSlotsSpriteLocation() { return null; }
    @Override protected boolean shouldRenderSaddleSlot() { return true; }
    @Override protected boolean shouldRenderArmorSlot() { return menu.hasBodyEquipment(); }
}
