package io.github.r3neer.scalebrews.mount;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;

/** Horse-style equipment menu with no storage grid: saddle, plus wolf armor when applicable. */
public final class TinyMountMenu extends AbstractMountInventoryMenu {
    private static final Identifier SADDLE_ICON = Identifier.withDefaultNamespace("container/slot/saddle");
    private static final Identifier WOLF_ARMOR_ICON = ScaleBrews.id("container/slot/wolf_armor");
    private final Mob tinyMount;
    private final Player menuPlayer;
    private final boolean wolfArmor;

    public TinyMountMenu(int containerId, Inventory playerInventory, Mob mount) {
        super(containerId, playerInventory, new SimpleContainer(0), mount);
        this.tinyMount = mount;
        this.menuPlayer = playerInventory.player;
        this.wolfArmor = mount instanceof Wolf;

        this.addSlot(new EquipmentSlotView(mount, EquipmentSlot.SADDLE,
                8, 18, SADDLE_ICON, true));
        this.addSlot(new EquipmentSlotView(mount, EquipmentSlot.BODY,
                8, 36, WOLF_ARMOR_ICON, wolfArmor));
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    public static TinyMountMenu client(int containerId, Inventory inventory, int entityId) {
        var entity = inventory.player.level().getEntity(entityId);
        if (!(entity instanceof Mob mob))
            throw new IllegalStateException("Tiny mount " + entityId + " is not present on the client");
        return new TinyMountMenu(containerId, inventory, mob);
    }

    public Mob mount() { return tinyMount; }
    public boolean hasWolfArmor() { return wolfArmor; }

    /** AbstractMountInventoryMenu is vanilla's special-packet base and therefore stores a null type. */
    @Override public net.minecraft.world.inventory.MenuType<?> getType() { return TinyMountInventory.MENU; }
    @Override protected boolean hasInventoryChanged(net.minecraft.world.Container container) { return false; }

    private final class EquipmentSlotView extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final Identifier emptyIcon;
        private final boolean active;

        EquipmentSlotView(Mob owner, EquipmentSlot equipmentSlot,
                          int x, int y, Identifier emptyIcon, boolean active) {
            super(owner.createEquipmentSlotContainer(equipmentSlot), 0, x, y);
            this.equipmentSlot = equipmentSlot;
            this.emptyIcon = emptyIcon;
            this.active = active;
        }

        @Override public boolean isActive() {
            if (!active) return false;
            if (equipmentSlot == EquipmentSlot.SADDLE && tinyMount instanceof Wolf wolf) return wolf.isTame();
            return true;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            if (!isActive()) return false;
            if (equipmentSlot == EquipmentSlot.SADDLE) return stack.is(Items.SADDLE);
            return tinyMount instanceof Wolf wolf && wolf.isOwnedBy(menuPlayer) && stack.is(Items.WOLF_ARMOR);
        }

        @Override public boolean mayPickup(Player player) {
            if (!isActive()) return false;
            if (equipmentSlot == EquipmentSlot.BODY && tinyMount instanceof Wolf wolf && !wolf.isOwnedBy(player)) return false;
            ItemStack stack = getItem();
            return (stack.isEmpty() || player.isCreative()
                    || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
                    && super.mayPickup(player);
        }

        @Override public void setByPlayer(ItemStack stack, ItemStack previous) {
            tinyMount.onEquipItem(equipmentSlot, previous, stack);
            super.setByPlayer(stack, previous);
            if (!stack.isEmpty()) {
                tinyMount.setGuaranteedDrop(equipmentSlot);
                tinyMount.setPersistenceRequired();
            }
        }

        @Override public int getMaxStackSize() { return 1; }
        @Override public @Nullable Identifier getNoItemIcon() { return isActive() ? emptyIcon : null; }
    }
}
