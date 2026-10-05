package io.github.r3neer.scalebrews.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import io.github.r3neer.scalebrews.mount.TinyMountInventory;
import io.github.r3neer.scalebrews.mount.TinyMountMenu;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Adversarial proof that Tiny Mount behavior comes from family data rather than species branches. */
public final class TinyMountFamilyTests {
    private static final String SADDLE_VISUAL = "\"saddle_visual\":{\"texture\":\"scalebrews:textures/entity/saddle/chicken.png\",\"anchor\":\"body\"}";

    @GameTest public void unlistedCowChangesBehaviorOnlyFromFamilyJson(GameTestHelper h) {
        var rider = h.makeMockPlayer(GameType.SURVIVAL);
        rider.getAttribute(Attributes.SCALE).setBaseValue(.28);
        var cow = h.spawn(EntityTypes.COW, 2, 2, 2);
        cow.setNoAi(true);

        try (var ignored = new TinyDefinitionTestScope("{\"entity\":\"minecraft:cow\",\"family\":\"direct\",\"movement\":\"ground\",\"speed\":0.2," + SADDLE_VISUAL + "}")) {
            var definition = TinyMounts.definition(cow);
            h.assertTrue(definition != null && definition.family() == TinyMountDefinition.Family.DIRECT,
                    "Cow becomes a direct Tiny Mount from decoded family data alone");
            h.assertTrue(TinyMountInventory.canOpen(rider, cow),
                    "Unridden direct-family species exposes its generic mount inventory");
            rider.setShiftKeyDown(true);
            var inventoryResult = cow.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(inventoryResult.consumesAction() && !rider.isPassenger(),
                    "Direct family secondary use is inventory intent rather than mounting");
            rider.setShiftKeyDown(false);
            cow.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(rider.getVehicle() == cow && TinyMounts.controller(cow) == null,
                    "Direct family accepts unsaddled passive passenger");
            h.assertTrue(TinyMountInventory.canOpen(rider, cow),
                    "Mounted direct-family species exposes the same generic inventory seam");
            var menu = new TinyMountMenu(0, rider.getInventory(), cow);
            h.assertTrue(menu.getSlot(0).isActive() && !menu.getSlot(1).isActive(),
                    "Data-only direct mount gets saddle-only menu without species code");
            cow.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            h.assertTrue(TinyMounts.controller(cow) == rider,
                    "Adding the family saddle grants direct control without changing species code");
            rider.stopRiding();
        }

        try (var ignored = new TinyDefinitionTestScope("{\"entity\":\"minecraft:cow\",\"family\":\"item_steered\",\"movement\":\"ground\",\"speed\":0.2,\"steering_item\":\"minecraft:stick\"," + SADDLE_VISUAL + "}")) {
            cow.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
            rider.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            cow.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertFalse(rider.isPassenger(), "Same cow rejects unsaddled riding when family changes to item_steered");
            h.assertFalse(TinyMountInventory.canOpen(rider, cow), "Item-steered family never exposes a mount inventory");
            cow.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            cow.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(rider.getVehicle() == cow && TinyMounts.controller(cow) == null,
                    "Saddle permits passenger but not steering");
            rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            h.assertTrue(TinyMounts.controller(cow) == rider,
                    "Configured steering item grants control without a species branch");
            cow.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
            TinyMounts.enforceRider(rider);
            h.assertFalse(rider.isPassenger(), "Item-steered family ejects rider when its saddle disappears");
        }
        cow.discard(); h.succeed();
    }

    @GameTest public void dataOnlyMountAcceptsSaddleFromRealDispenserPath(GameTestHelper h) {
        var sourcePos = h.absolutePos(new BlockPos(1, 2, 2));
        var state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST);
        var blockEntity = new DispenserBlockEntity(sourcePos, state);
        var source = new BlockSource(h.getLevel(), sourcePos, state, blockEntity);
        var targetPos = sourcePos.east();
        var cow = h.spawn(EntityTypes.COW, 2, 2, 2);
        cow.setNoAi(true);
        cow.setPos(targetPos.getX() + .5, targetPos.getY(), targetPos.getZ() + .5);

        try (var ignored = new TinyDefinitionTestScope("{\"entity\":\"minecraft:cow\",\"family\":\"direct\",\"movement\":\"ground\",\"speed\":0.2," + SADDLE_VISUAL + "}")) {
            var stack = new ItemStack(Items.SADDLE);
            h.assertTrue(cow.canEquipWithDispenser(stack),
                    "Family data extends vanilla dispenser eligibility to a normally unsaddleable species");
            h.assertTrue(EquipmentDispenseItemBehavior.dispenseEquipment(source, stack),
                    "Real vanilla equipment dispenser path selects the data-only Tiny Mount");
            h.assertTrue(stack.isEmpty() && cow.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE),
                    "Dispenser consumes one saddle and writes the native SADDLE equipment slot");
        }
        cow.discard(); h.succeed();
    }

    @GameTest public void familyCodecRejectsContradictoryGrammars(GameTestHelper h) {
        h.assertTrue(rejects("{\"entity\":\"minecraft:cow\",\"family\":\"direct\",\"movement\":\"ground\",\"speed\":0.2,\"steering_item\":\"minecraft:stick\"," + SADDLE_VISUAL + "}"),
                "Direct family rejects a steering item");
        h.assertTrue(rejects("{\"entity\":\"minecraft:cow\",\"family\":\"item_steered\",\"movement\":\"ground\",\"speed\":0.2," + SADDLE_VISUAL + "}"),
                "Item-steered family requires a steering item");
        h.assertTrue(rejects("{\"entity\":\"minecraft:cow\",\"family\":\"item_steered\",\"movement\":\"ground\",\"speed\":0.2,\"steering_item\":\"minecraft:stick\",\"body_equipment\":{\"item\":\"minecraft:wolf_armor\",\"slot_icon\":\"scalebrews:container/slot/wolf_armor\"}," + SADDLE_VISUAL + "}"),
                "Item-steered family rejects mount-inventory BODY equipment");
        var minimal = TinyMountDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"entity\":\"minecraft:cow\"}")).getOrThrow();
        h.assertTrue(minimal.family() == TinyMountDefinition.Family.DIRECT
                        && minimal.movement() == TinyMountDefinition.Movement.GROUND
                        && Math.abs(minimal.speed() - .18F) < .0001F
                        && Math.abs(minimal.maxRiderScaleRatio() - .53) < .0001
                        && minimal.ability() == TinyMountDefinition.Ability.NONE
                        && minimal.enabled() && minimal.saddleVisual().isEmpty(),
                "Entity-only Tiny Mount definition receives every safe default");
        h.succeed();
    }

    @GameTest public void wolfUsesNativeShearingOrderAndOwnership(GameTestHelper h) {
        var owner = h.makeMockServerPlayer(GameType.SURVIVAL);
        var stranger = h.makeMockServerPlayer(GameType.SURVIVAL);
        var wolf = h.spawn(EntityTypes.WOLF, 2, 2, 2);
        wolf.tame(owner);
        var definition = TinyMounts.definition(wolf);
        h.assertTrue(definition != null && definition.family() == TinyMountDefinition.Family.TAMEABLE_DIRECT,
                "Wolf data selects tameable_direct family");
        h.assertTrue(definition.bodyEquipment().isPresent()
                        && definition.bodyEquipment().get().item().equals(Identifier.withDefaultNamespace("wolf_armor")),
                "Wolf BODY item comes from data");

        wolf.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        wolf.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        wolf.interact(stranger, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR)
                        && wolf.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE),
                "Non-owner cannot shear tame wolf equipment");

        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        wolf.interact(owner, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wolf.getItemBySlot(EquipmentSlot.BODY).isEmpty()
                        && wolf.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE),
                "Vanilla EquipmentSlot order shears BODY before SADDLE");
        wolf.interact(owner, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wolf.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(),
                "Second vanilla shear removes the saddle");
        h.succeed();
    }

    private static boolean rejects(String json) {
        try {
            TinyMountDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
            return false;
        } catch (IllegalStateException expected) {
            return true;
        }
    }
}
