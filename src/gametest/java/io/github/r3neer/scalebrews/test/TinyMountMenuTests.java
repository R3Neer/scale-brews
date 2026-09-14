package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.mount.TinyMountMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class TinyMountMenuTests {
    @GameTest public void chickenExposesOnlySaddleEquipment(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var chicken = h.spawn(EntityTypes.CHICKEN, 1, 2, 1);
        var menu = new TinyMountMenu(0, player.getInventory(), chicken);
        h.assertTrue(menu.getSlot(0).isActive() && menu.getSlot(0).mayPlace(new ItemStack(Items.SADDLE)),
                "Chicken saddle slot is available");
        h.assertFalse(menu.hasBodyEquipment() || menu.getSlot(1).isActive(), "Chicken BODY slot is absent");
        menu.getSlot(0).setByPlayer(new ItemStack(Items.SADDLE));
        h.assertTrue(chicken.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE),
                "Chicken menu writes the same saddle equipment slot used by riding/rendering");
        menu.getSlot(0).setByPlayer(ItemStack.EMPTY);
        h.assertTrue(chicken.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "Chicken saddle can be removed through the menu");
        chicken.discard();
        h.succeed();
    }

    @GameTest public void itemSteeredBeeEquipsSaddleByDirectInteraction(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var bee = h.spawn(EntityTypes.BEE, 1, 2, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        var saddleResult = bee.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(saddleResult.consumesAction(), "Using a saddle on a bee consumes the interaction");
        h.assertTrue(bee.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE),
                "Item-steered bee equips saddle by direct interaction like pig/strider");
        bee.discard();
        h.succeed();
    }

    @GameTest public void wolfMenuUsesConfiguredOwnerProtectedBodyEquipment(GameTestHelper h) {
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        var stranger = h.makeMockPlayer(GameType.SURVIVAL);
        var wolf = h.spawn(EntityTypes.WOLF, 1, 2, 1);
        wolf.tame(owner);
        var ownerMenu = new TinyMountMenu(0, owner.getInventory(), wolf);
        h.assertTrue(ownerMenu.hasBodyEquipment() && ownerMenu.getSlot(1).isActive(), "Wolf data exposes BODY slot");
        h.assertTrue(ownerMenu.getSlot(1).mayPlace(new ItemStack(Items.WOLF_ARMOR)), "Owner can equip configured wolf armor");
        h.assertFalse(ownerMenu.getSlot(1).mayPlace(new ItemStack(Items.IRON_HORSE_ARMOR)), "Unconfigured horse armor is rejected");
        ownerMenu.getSlot(1).setByPlayer(new ItemStack(Items.WOLF_ARMOR));
        h.assertTrue(wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR), "Configured body item uses native BODY equipment");
        var strangerMenu = new TinyMountMenu(1, stranger.getInventory(), wolf);
        h.assertFalse(strangerMenu.getSlot(1).mayPlace(new ItemStack(Items.WOLF_ARMOR))
                || strangerMenu.getSlot(1).mayPickup(stranger), "Borrowed tameable BODY equipment remains owner-protected");
        h.succeed();
    }
}
