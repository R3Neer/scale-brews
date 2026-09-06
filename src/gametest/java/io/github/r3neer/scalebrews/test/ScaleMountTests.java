package io.github.r3neer.scalebrews.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.config.ScaleRules;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.item.ScaleItems;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.physics.ScaleCombat;
import io.github.r3neer.scalebrews.test.mixin.TestMountAccess;
import io.github.r3neer.scalebrews.test.mixin.TestBeeAccess;
import io.github.r3neer.scalebrews.test.mixin.TestVillagerSensorAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;

public class ScaleMountTests {
    static void settle(net.minecraft.world.entity.LivingEntity entity) {
        var transition = new io.github.r3neer.scalebrews.scale.ScaleTransition();
        for (int i = 0; i < 20; i++) transition.tick(entity);
        TinyMounts.enforceRider(entity);
    }
    @GameTest public void flowerRecipeUsesSmallFlowerTag(GameTestHelper h) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                io.github.r3neer.scalebrews.ScaleBrews.id("flower_on_a_stick"));
        var recipe = (net.minecraft.world.item.crafting.ShapelessRecipe)h.getLevel().recipeAccess().byKey(key).orElseThrow().value();
        for (var item : java.util.List.of(Items.POPPY, Items.DANDELION, Items.ALLIUM, Items.SUNFLOWER, Items.ROSE_BUSH)) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                    java.util.List.of(new ItemStack(Items.FISHING_ROD), new ItemStack(item)));
            boolean small = new ItemStack(item).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    Identifier.withDefaultNamespace("small_flowers")));
            h.assertTrue(small == (item != Items.SUNFLOWER && item != Items.ROSE_BUSH), "Small-flower tag contains the expected examples");
            h.assertTrue(recipe.matches(input, h.getLevel()) == small, "Recipe follows small-flower tag, excluding tall flowers");
            if (small) h.assertTrue(ItemStack.isSameItemSameComponents(recipe.assemble(input), new ItemStack(ScaleItems.FLOWER_ON_A_STICK)),
                    "Every flower produces the same item without flower metadata");
        }
        try (var ignored = new RuleTestScope("{\"tiny_mounts\":false}")) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                    java.util.List.of(new ItemStack(Items.FISHING_ROD), new ItemStack(Items.POPPY)));
            h.assertFalse(recipe.matches(input, h.getLevel()), "Tiny Mounts master switch disables steering-item crafting");
        }
        h.succeed();
    }
    @GameTest public void disabledMechanicsRestoreVanilla(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var chicken = h.spawn(EntityTypes.CHICKEN, 1, 2, 1);
        var villager = h.spawn(EntityTypes.VILLAGER, 1, 2, 2);
        p.setPos(chicken.position());
        var stone = net.minecraft.world.level.block.Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
        try (var ignored = new RuleTestScope("{\"tiny_mounts\":false,\"environment_interactions\":false,\"villager_fear\":false,\"growth_landing_impact\":false}")) {
            h.assertTrue(TinyMounts.definition(chicken) == null, "Disabled tiny mounts do not intercept vanilla interaction");
            p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, 2)); TestScale.settle(p);
            settle(p);
            h.assertTrue(io.github.r3neer.scalebrews.physics.ScalePhysics.triggersPlate(p, stone), "Disabled bypass restores vanilla plate filtering");
            h.assertTrue(((io.github.r3neer.scalebrews.test.mixin.TestEggAccess)net.minecraft.world.level.block.Blocks.TURTLE_EGG).test$canCrush(h.getLevel(), p), "Disabled egg protection restores vanilla egg rule");
            p.removeEffect(ScaleEffects.SHRINKING); TestScale.settle(p);
            settle(p);
            p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, 2)); TestScale.settle(p);
            settle(p);
            h.assertFalse(p.startRiding(chicken, true, true), "Size restriction remains when Tiny Mounts disabled");
            h.assertFalse(((TestVillagerSensorAccess)new VillagerHostilesSensor()).test$matches(h.getLevel(), villager, p), "Disabled fear does not mark giant threat");
            var target = h.spawn(EntityTypes.PIG, 2, 2, 1);
            target.setDeltaMovement(Vec3.ZERO);
            io.github.r3neer.scalebrews.physics.GrowthImpact.land(p, 12, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            near(h, target.getHealth(), target.getMaxHealth(), "Disabled landing causes no damage");
            near(h, target.getDeltaMovement().length(), 0, "Disabled landing causes no knockback");
            near(h, io.github.r3neer.scalebrews.physics.ScalePhysics.blockSpeed(p,
                net.minecraft.world.level.block.Blocks.SOUL_SAND.defaultBlockState(), .4F), .4F, "Disabled terrain resistance returns vanilla factor");
        }
        h.succeed();
    }
    @GameTest public void projectilesKeepOwnDamage(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        for (var effect : java.util.List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
            for (int tier = 0; tier <= 3; tier++) {
                if (tier > 0) p.addEffect(new MobEffectInstance(effect, 200, tier - 1)); TestScale.settle(p);
                for (boolean trident : new boolean[]{false, true}) {
                    var target = h.spawn(EntityTypes.PIG, 1, 2, 1);
                    net.minecraft.world.entity.projectile.arrow.AbstractArrow projectile = trident
                        ? h.spawn(EntityTypes.TRIDENT, 1, 2, 2) : h.spawn(EntityTypes.ARROW, 1, 2, 2);
                    projectile.setOwner(p);
                    projectile.setDeltaMovement(0, 0, 3);
                    projectile.setCritArrow(false);
                    ((io.github.r3neer.scalebrews.test.mixin.TestArrowAccess)projectile).test$hit(new net.minecraft.world.phys.EntityHitResult(target));
                    near(h, target.getMaxHealth() - target.getHealth(), trident ? 8 : 6, "Projectile damage independent of scale at tier " + tier);
                    target.discard();
                    projectile.discard();
                }
                p.removeEffect(effect); TestScale.settle(p);
                settle(p);
            }
        }
        h.succeed();
    }
    static void near(GameTestHelper h, double a, double b, String message) {
        h.assertTrue(Math.abs(a - b) < .00001, message + ": " + a + " != " + b);
    }
    @GameTest public void configurationAndDefaults(GameTestHelper h) {
        var level = h.getLevel();
        h.assertTrue(level.registryAccess().lookupOrThrow(ScaleRules.REGISTRY).size() == 1, "World rules JSON loaded");
        h.assertTrue(level.registryAccess().lookupOrThrow(TinyMounts.REGISTRY).size() == 3, "Chicken, bee and wolf mount definitions");
        var bee = Identifier.withDefaultNamespace("bee");
        h.assertTrue(ScaleRules.get(level).mountEnabled(bee), "Default bee enabled");
        var off = ScaleRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"tiny_mounts\":false,\"environment_interactions\":false,\"villager_fear\":false,\"growth_landing_impact\":false}")).getOrThrow();
        h.assertFalse(off.mountEnabled(bee), "Master mount switch overrides individual defaults");
        h.assertFalse(off.protectsFarmland() || off.protectsEggs() || off.bypassesPlates() || off.quietensMovement() || off.resistsTerrain(), "Environment master disables all its mechanics");
        h.assertFalse(off.villagerFear() || off.growthImpact(), "Independent fear/landing switches");
        var selective = ScaleRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"mounts\":{\"minecraft:bee\":false},\"farmland_protection\":false}")).getOrThrow();
        h.assertFalse(selective.mountEnabled(bee) || selective.protectsFarmland(), "Individual switches disabled");
        h.assertTrue(selective.mountEnabled(Identifier.withDefaultNamespace("chicken")) && selective.protectsEggs(), "Other defaults retained");
        h.succeed();
    }
    @GameTest public void saddleMountAndGrowthGuards(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        var chicken = h.spawn(EntityTypes.CHICKEN, 1, 2, 1);
        chicken.setNoAi(true);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE, 2));
        h.assertTrue(chicken.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.FAIL, "Normal size saddle denied");
        h.assertTrue(p.getMainHandItem().getCount() == 2, "Failed interaction consumes nothing");
        p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, 1)); TestScale.settle(p);
        settle(p);
        chicken.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(chicken.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE), "Vanilla saddle equipped");
        h.assertTrue(p.getMainHandItem().getCount() == 1, "Exactly one saddle consumed");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        chicken.setNoAi(false);
        chicken.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(p.getVehicle() == chicken && chicken.getControllingPassenger() == p, "Direct chicken controller");
        p.setLastClientInput(new Input(true, false, false, false, true, false, false));
        near(h, ((TestMountAccess)chicken).test$input(p, Vec3.ZERO).z, 1, "Native packet WASD");
        chicken.setDeltaMovement(Vec3.ZERO);
        ((TestMountAccess)chicken).test$jump();
        near(h, chicken.getDeltaMovement().y, 0, "Space never normal-jumps");
        p.removeEffect(ScaleEffects.SHRINKING); TestScale.settle(p);
        settle(p);
        h.assertFalse(p.isPassenger(), "Losing size safely dismounts");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        chicken.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(chicken.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "Vanilla shears can recover the saddle after dismount");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var pig = h.spawn(EntityTypes.PIG, 1, 2, 2);
        h.assertTrue(TinyMounts.definition(pig) == null, "Native pig controller not replaced");
        h.assertTrue(p.startRiding(pig, true, true), "Baseline living mount works");
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, 0)); TestScale.settle(p);
        settle(p);
        h.assertFalse(p.isPassenger() || p.startRiding(pig, true, true), "Oversized rider dismounts and cannot force-mount a normal pig");
        var cart = h.spawn(EntityTypes.MINECART, 1, 2, 3);
        h.assertTrue(p.startRiding(cart, true, true), "Growth still permits minecarts");
        p.stopRiding();
        h.succeed();
    }
    @GameTest public void beeSteeringAndHive(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, 1)); TestScale.settle(p);
        settle(p);
        var bee = h.spawn(EntityTypes.BEE, 1, 2, 1);
        bee.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        h.assertTrue(p.startRiding(bee), "Bee mounts without steering item");
        h.assertTrue(TinyMounts.controller(bee) == null, "No item leaves vanilla AI controller");
        h.assertFalse(((TestBeeAccess)bee).test$wantsHive(), "Mounted bee cannot enter hive");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ScaleItems.FLOWER_ON_A_STICK));
        h.assertTrue(bee.getControllingPassenger() == p, "Flower enables steering");
        var def = TinyMounts.definition(bee);
        p.setYRot(0);
        p.setXRot(-90);
        var velocity = TinyMounts.flightVelocity(p, def);
        h.assertTrue(velocity.y > 0 && velocity.y <= def.maxVerticalSpeed() && velocity.z > 0, "Upward look remains bounded and forward");
        p.setXRot(90);
        h.assertTrue(TinyMounts.flightVelocity(p, def).y < 0, "Downward look descends");
        p.setXRot(0);
        near(h, TinyMounts.flightVelocity(p, def).y, 0, "Horizontal look horizontal flight");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(p.isPassenger() && TinyMounts.controller(bee) == null, "Item removal restores AI without dismount");
        p.stopRiding();
        h.assertTrue(p.startRiding(bee), "Can remount before external storage");
        var hivePos = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(hivePos, net.minecraft.world.level.block.Blocks.BEEHIVE.defaultBlockState());
        var hive = (net.minecraft.world.level.block.entity.BeehiveBlockEntity)h.getLevel().getBlockEntity(hivePos);
        p.fallDistance = 20;
        hive.addOccupant(bee);
        h.assertFalse(p.isPassenger(), "External hive storage dismounts first");
        near(h, p.fallDistance, 0, "Storage clears accumulated rider fall distance");
        h.assertTrue(hive.getOccupantCount() == 1 && bee.isRemoved(), "Bee stored normally after dismount");
        h.succeed();
    }
    @GameTest public void meleeKnockbackAndJumpBoost(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var target = h.spawn(EntityTypes.PIG, 1, 2, 1);
        var access = (TestMountAccess)p;
        float baseline = access.test$jumpPower();
        for (int tier = 1; tier <= 3; tier++) {
            p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, tier - 1)); TestScale.settle(p);
            settle(p);
            float smallJump = access.test$jumpPower();
            near(h, smallJump, baseline * (1 + .025 * tier), "Shrinking jump tier");
            p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, tier - 1)); TestScale.settle(p);
            settle(p);
            float combined = access.test$jumpPower();
            p.removeEffect(ScaleEffects.SHRINKING); TestScale.settle(p);
            settle(p);
            h.assertTrue(access.test$jumpPower() > smallJump && combined > access.test$jumpPower(), "Jump Boost stronger alone and synergistic combined");
            p.removeEffect(MobEffects.JUMP_BOOST); TestScale.settle(p);
            settle(p);
            for (var effect : java.util.List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
                p.addEffect(new MobEffectInstance(effect, 200, tier - 1)); TestScale.settle(p);
                settle(p);
                double multiplier = effect == ScaleEffects.GROWTH ? 1 + .1 * tier : 1 - .1 * tier;
                for (double power : new double[]{.4, 1, 2}) {
                    target.setDeltaMovement(Vec3.ZERO);
                    target.knockback(power, 1, 0, p.damageSources().playerAttack(p), 0, true);
                    near(h, -target.getDeltaMovement().x, power * multiplier, "Final knockback preserves arbitrary sprint/enchantment contributions");
                }
                var arrow = h.spawn(EntityTypes.ARROW, 1, 2, 1);
                near(h, ScaleCombat.knockback(1, p.damageSources().arrow(arrow, p)), 1, "Projectile knockback unchanged");
                p.removeEffect(effect); TestScale.settle(p);
                settle(p);
            }
        }
        h.succeed();
    }
    @GameTest public void localVillagerFear(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var villager = h.spawn(EntityTypes.VILLAGER, 1, 2, 1);
        p.setPos(villager.position().add(2, 0, 0));
        var sensor = (TestVillagerSensorAccess)new VillagerHostilesSensor();
        for (int tier = 1; tier <= 3; tier++) {
            p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, tier - 1)); TestScale.settle(p);
            settle(p);
            h.assertTrue(sensor.test$matches(h.getLevel(), villager, p) == (tier >= 2), "Only Growth II+ scares villagers");
            h.assertTrue(villager.getLastHurtByMob() == null, "Fear does not mark an attacker");
            p.removeEffect(ScaleEffects.GROWTH); TestScale.settle(p);
            settle(p);
        }
        h.succeed();
    }
}
