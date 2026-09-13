package io.github.r3neer.scalebrews.mount;

import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;

/** Data-driven mount equipment menu: saddle plus an optional native BODY slot. */
public final class TinyMountMenu extends AbstractMountInventoryMenu {
    private static final Identifier SADDLE_ICON = Identifier.withDefaultNamespace("container/slot/saddle");
    private static final Identifier FALLBACK_BODY_ICON = Identifier.withDefaultNamespace("container/slot/horse_armor");
    private final Mob tinyMount;
    private final Player menuPlayer;
    private final TinyMountDefinition definition;
    private final TinyMountDefinition.BodyEquipment bodyEquipment;

    public TinyMountMenu(int containerId, Inventory playerInventory, Mob mount) {
        super(containerId, playerInventory, new SimpleContainer(0), mount);
        this.tinyMount = mount;
        this.menuPlayer = playerInventory.player;
        this.definition = TinyMounts.definition(mount);
        if (definition == null || !definition.family().hasInventory())
            throw new IllegalStateException("Entity is not an inventory-bearing Tiny Mount: " + mount.getType());
        this.bodyEquipment = definition.bodyEquipment().orElse(null);

        this.addSlot(new EquipmentSlotView(mount, EquipmentSlot.SADDLE,
                8, 18, SADDLE_ICON, true));
        this.addSlot(new EquipmentSlotView(mount, EquipmentSlot.BODY,
                8, 36, bodyEquipment == null ? FALLBACK_BODY_ICON : bodyEquipment.slotIcon(), bodyEquipment != null));
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    public static TinyMountMenu client(int containerId, Inventory inventory, int entityId) {
        var entity = inventory.player.level().getEntity(entityId);
        if (!(entity instanceof Mob mob))
            throw new IllegalStateException("Tiny mount " + entityId + " is not present on the client");
        return new TinyMountMenu(containerId, inventory, mob);
    }

    public Mob mount() { return tinyMount; }
    public boolean hasBodyEquipment() { return bodyEquipment != null; }

    /** AbstractMountInventoryMenu is vanilla's special-packet base and therefore stores a null type. */
    @Override public net.minecraft.world.inventory.MenuType<?> getType() { return TinyMountInventory.MENU; }
    @Override protected boolean hasInventoryChanged(net.minecraft.world.Container container) { return false; }

    private final class EquipmentSlotView extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final Identifier emptyIcon;
        private final boolean configured;

        EquipmentSlotView(Mob owner, EquipmentSlot equipmentSlot,
                          int x, int y, Identifier emptyIcon, boolean configured) {
            super(owner.createEquipmentSlotContainer(equipmentSlot), 0, x, y);
            this.equipmentSlot = equipmentSlot;
            this.emptyIcon = emptyIcon;
            this.configured = configured;
        }

        @Override public boolean isActive() {
            return configured && TinyMounts.equipmentAvailable(tinyMount, definition);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            if (!isActive()) return false;
            if (equipmentSlot == EquipmentSlot.SADDLE) return stack.is(Items.SADDLE);
            return TinyMounts.matchesBodyEquipment(definition, stack)
                    && TinyMounts.mayManageBodyEquipment(tinyMount, menuPlayer, definition)
                    && tinyMount.isEquippableInSlot(stack, EquipmentSlot.BODY);
        }

        @Override public boolean mayPickup(Player player) {
            if (!isActive()) return false;
            if (equipmentSlot == EquipmentSlot.BODY && !TinyMounts.mayManageBodyEquipment(tinyMount, player, definition)) return false;
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
