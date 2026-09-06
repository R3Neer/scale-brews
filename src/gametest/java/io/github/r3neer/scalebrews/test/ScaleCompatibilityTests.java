package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.potion.ScalePotions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;

public class ScaleCompatibilityTests {
    @GameTest public void moddedMobLanding(GameTestHelper h) {
        if (!FabricLoader.getInstance().isModLoaded("alexsmobs")) { h.succeed(); return; }
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("alexsmobs", "grizzly_bear"));
        h.assertTrue(type != null, "Installed modded mob exists");
        var mob = (net.minecraft.world.entity.LivingEntity) h.spawn(type, 1, 2, 1);
        mob.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, 2)); TestScale.settle(mob);
        var target = h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, 2.5F, 2, 1);
        float before = mob.getHealth();
        mob.fallDistance = 12;
        ((io.github.r3neer.scalebrews.test.mixin.TestEntityAccess)mob).test$land(0, true,
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)));
        h.assertTrue(mob.getHealth() < before, "Modded mob retains own fall damage");
        h.assertTrue(target.getHealth() < target.getMaxHealth() && target.getMaxHealth() - target.getHealth() <= 4, "Generic Growth wave works for modded mob without adapter");
        mob.discard(); target.discard(); h.succeed();
    }

    @GameTest public void smallCornerCollision(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var origin = h.absolutePos(new net.minecraft.core.BlockPos(1, 30, 1));
        for (int a = -2; a <= 4; a++) for (int y = 0; y <= 3; y++) {
            h.getLevel().setBlockAndUpdate(origin.offset(3, y, a), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(origin.offset(a, y, 3), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        }
        for (int tier = 1; tier <= 3; tier++) {
            p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 1200, tier - 1)); TestScale.settle(p);
            ScaleMountTests.settle(p);
            p.setPos(origin.getX() + 2, origin.getY(), origin.getZ() + 2);
            p.setSprinting(true);
            p.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(2, 0, 2));
            h.assertTrue(p.getBoundingBox().maxX <= origin.getX() + 3.00001
                && p.getBoundingBox().maxZ <= origin.getZ() + 3.00001, "Small sprint collision stays outside both corner walls");
        }
        h.succeed();
    }

    @GameTest public void scaledElytraEligibility(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(1, 30, 1))));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        for (var effect : java.util.List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
            for (int tier = 1; tier <= 3; tier++) {
                p.addEffect(new MobEffectInstance(effect, 1200, tier - 1)); TestScale.settle(p);
                ScaleMountTests.settle(p);
                p.setOnGround(false);
                p.setDeltaMovement(0, -.1, 0);
                h.assertTrue(p.tryToStartFallFlying(), "Scale does not disable vanilla elytra eligibility");
                p.stopFallFlying(); p.removeEffect(effect); TestScale.settle(p);
            }
        }
        h.succeed();
    }

    @GameTest public void optionalAlexIngredient(GameTestHelper h) {
        var tendon = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("alexsmobs", "elastic_tendon"));
        if (FabricLoader.getInstance().isModLoaded("alexsmobs")) h.assertTrue(tendon.isPresent(), "Installed Alex's Mobs provides the actual ingredient");
        var source = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        var result = h.getLevel().potionBrewing().mix(new ItemStack(tendon.orElse(Items.SLIME_BALL)), source);
        h.assertTrue(result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().orElseThrow().equals(ScalePotions.GROWTH), "Optional ingredient really brews Growth");
        h.succeed();
    }

    @GameTest public void combatifyWeaponReach(GameTestHelper h) throws Exception {
        if (!FabricLoader.getInstance().isModLoaded("combatify")) { h.succeed(); return; }
        // Reflection keeps the third-party dependency out of the default build and production JAR.
        var reachMethod = Class.forName("net.atlas.combatify.util.MethodHandler")
            .getMethod("getCurrentAttackReachWithoutChargedReach", Player.class);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        for (var item : java.util.List.of(Items.AIR, Items.IRON_SWORD, Items.IRON_AXE, Items.TRIDENT)) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            p.tick(); p.tick();
            double baseline = ((Number)reachMethod.invoke(null, p)).doubleValue();
            double attributeBaseline = p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            for (var effect : java.util.List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
                for (int tier = 1; tier <= 3; tier++) {
                    p.addEffect(new MobEffectInstance(effect, 1200, tier - 1)); TestScale.settle(p);
                    double factor = effect == ScaleEffects.GROWTH ? 1 + .2 * tier : 1 - .12 * tier;
                    double actual = ((Number)reachMethod.invoke(null, p)).doubleValue();
                    ScaleMountTests.near(h, p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), attributeBaseline * factor, "Weapon attribute and scale modifier compose");
                    h.assertTrue(effect == ScaleEffects.GROWTH ? actual > baseline : actual < baseline, "Combatify actual reach responds to scale for " + item);
                    p.removeEffect(effect); TestScale.settle(p);
                    ScaleMountTests.near(h, ((Number)reachMethod.invoke(null, p)).doubleValue(), baseline, "Removing effect restores weapon-specific reach");
                }
            }
        }
        h.succeed();
    }
}
