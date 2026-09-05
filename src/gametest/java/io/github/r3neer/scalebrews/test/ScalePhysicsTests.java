package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.physics.GrowthImpact;
import io.github.r3neer.scalebrews.physics.ScalePhysics;
import io.github.r3neer.scalebrews.physics.ScaleSneaking;
import io.github.r3neer.scalebrews.test.mixin.TestEntityAccess;
import io.github.r3neer.scalebrews.test.mixin.TestPlateAccess;
import io.github.r3neer.scalebrews.test.mixin.TestEggAccess;
import io.github.r3neer.scalebrews.test.mixin.TestFoodAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

public class ScalePhysicsTests {
    @GameTest
    public void exhaustionOnlyChangesPhysicalActivity(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        var food = (TestFoodAccess) p.getFoodData();
        for (var effect : java.util.List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
            for (int level = 1; level <= 3; level++) {
                p.addEffect(new MobEffectInstance(effect, 200, level - 1));
                double multiplier = effect == ScaleEffects.GROWTH ? 1 + .10 * level : 1 - .05 * level;
                p.setSprinting(false);
                food.test$exhaustion(0);
                p.jumpFromGround();
                near(h, food.test$exhaustion(), .05 * multiplier, "Jump exhaustion");
                p.setSprinting(true);
                food.test$exhaustion(0);
                p.jumpFromGround();
                near(h, food.test$exhaustion(), .2 * multiplier, "Sprint jump exhaustion");
                p.setOnGround(true);
                food.test$exhaustion(0);
                p.checkMovementStatistics(1, 0, 0);
                near(h, food.test$exhaustion(), .1 * multiplier, "Sprint movement exhaustion");
                p.setSwimming(true);
                food.test$exhaustion(0);
                p.checkMovementStatistics(1, 0, 0);
                near(h, food.test$exhaustion(), .01 * multiplier, "Swimming exhaustion");
                p.setSwimming(false);
                p.setSprinting(false);
                food.test$exhaustion(0);
                p.causeFoodExhaustion(.4F);
                near(h, food.test$exhaustion(), .4, "Global exhaustion sink is unchanged");
                // Connected mocks haven't sent the client-loaded packet and are damage-immune.
                // A plain survival Player exercises the same damage/food code without that guard.
                var hurtPlayer = h.makeMockPlayer(GameType.SURVIVAL);
                hurtPlayer.addEffect(new MobEffectInstance(effect, 200, level - 1));
                var source = hurtPlayer.damageSources().generic();
                h.assertTrue(hurtPlayer.hurtServer(h.getLevel(), source, 1), "Damage test really hurt player");
                var hurtFood = (TestFoodAccess) hurtPlayer.getFoodData();
                near(h, hurtFood.test$exhaustion(), source.getFoodExhaustion(), "Received damage exhaustion unchanged");
                hurtFood.test$exhaustion(0);
                net.minecraft.world.effect.MobEffects.HUNGER.value().applyEffectTick(h.getLevel(), hurtPlayer, 1);
                near(h, hurtFood.test$exhaustion(), .01, "Hunger exhaustion unchanged");
                var target = h.spawnWithNoFreeWill(EntityTypes.PIG, 1, 2, 1);
                food.test$exhaustion(0);
                p.attack(target);
                near(h, food.test$exhaustion(), .1 * multiplier, "Attack exhaustion");
                target.discard();
                p.removeEffect(effect);
            }
        }
        // Natural regeneration calls the unmodified global sink, even while Growth III is active.
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, 2));
        p.setHealth(p.getMaxHealth() - 2);
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(6);
        food.test$exhaustion(0);
        for (int i = 0; i < 10; i++) p.getFoodData().tick(p);
        near(h, food.test$exhaustion(), 6, "Natural regeneration exhaustion unchanged");
        p.discard();
        h.succeed();
    }

    private static void near(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.0001, message + ": " + actual + " expected " + expected);
    }

    @GameTest
    public void fallProtectionComposesWithFeatherFalling(GameTestHelper h) {
        for (int level = 0; level <= 3; level++) {
            for (boolean feather : new boolean[]{false, true}) {
                var p = h.makeMockPlayer(GameType.SURVIVAL);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                if (level > 0) p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, level - 1));
                if (feather) {
                    var boots = new ItemStack(Items.DIAMOND_BOOTS);
                    boots.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.FEATHER_FALLING), 4);
                    p.setItemSlot(EquipmentSlot.FEET, boots);
                }
                p.setHealth(p.getMaxHealth());
                float before = p.getHealth();
                p.hurtServer(h.getLevel(), h.getLevel().damageSources().fall(), 10);
                near(h, before - p.getHealth(), 10 * (1 - 0.08 * level) * (feather ? 0.52 : 1), "Multiplicative fall protection");
                h.assertTrue(p.getHealth() < before, "Fall damage is never erased by the synergy");
            }
        }
        h.succeed();
    }

    @GameTest
    public void pressurePlateFiltering(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        p.setPos(Vec3.atBottomCenterOf(pos));
        var plates = new net.minecraft.world.level.block.Block[]{Blocks.OAK_PRESSURE_PLATE,
                Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE};
        for (int level = 0; level <= 3; level++) {
            p.removeEffect(ScaleEffects.SHRINKING);
            if (level > 0) p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, level - 1));
            for (int i = 0; i < plates.length; i++) {
                h.getLevel().setBlockAndUpdate(pos, plates[i].defaultBlockState());
                int signal = ((TestPlateAccess) plates[i]).test$signal(h.getLevel(), pos);
                boolean expected = level < 2 || (level == 2 && i < 2);
                h.assertTrue((signal > 0) == expected, "Plate " + i + " at shrink " + level + ": " + signal);
            }
        }
        var pig = h.spawnWithNoFreeWill(EntityTypes.PIG, 1.5F, 2, 1.5F);
        h.getLevel().setBlockAndUpdate(pos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        h.assertTrue(((TestPlateAccess) Blocks.STONE_PRESSURE_PLATE).test$signal(h.getLevel(), pos) == 15, "Other entities still activate plates");
        p.discard();
        pig.discard();
        h.succeed();
    }

    @GameTest
    public void fragileBlocksAndFallDamage(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, 2));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        p.setHealth(p.getMaxHealth());
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, Blocks.FARMLAND.defaultBlockState());
        float before = p.getHealth();
        Blocks.FARMLAND.fallOn(h.getLevel(), Blocks.FARMLAND.defaultBlockState(), pos, p, 10);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.FARMLAND), "Tiny player preserves farmland");
        h.assertTrue(p.getHealth() < before, "Farmland protection does not cancel fall damage");
        h.assertFalse(((TestEggAccess) Blocks.TURTLE_EGG).test$canCrush(h.getLevel(), p), "Tiny player cannot crush eggs");
        p.removeEffect(ScaleEffects.SHRINKING);
        h.assertTrue(((TestEggAccess) Blocks.TURTLE_EGG).test$canCrush(h.getLevel(), p), "Normal egg behavior restored");
        Blocks.FARMLAND.fallOn(h.getLevel(), Blocks.FARMLAND.defaultBlockState(), pos, p, 10);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.DIRT), "Normal player tramples farmland");
        h.succeed();
    }

    @GameTest
    public void terrainSlowdownsAreScoped(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var access = (TestEntityAccess) p;
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, Blocks.SOUL_SAND.defaultBlockState());
        p.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        double[] soul = {0.4, 0.64, 0.85, 1};
        for (int tier = 0; tier <= 3; tier++) {
            p.removeEffect(ScaleEffects.GROWTH);
            if (tier > 0) p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, tier - 1));
            near(h, access.test$blockSpeed(), soul[tier], "Soul sand progression");
            access.test$setStuckSpeed(Vec3.ZERO);
            p.makeStuckInBlock(Blocks.SWEET_BERRY_BUSH.defaultBlockState(), new Vec3(0.8, 0.75, 0.8));
            near(h, access.test$stuckSpeed().x, tier == 3 ? 0 : 1 - 0.2 * (1 - tier / 3.0), "Berry slowdown progression");
            p.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.25, 0.05, 0.25));
            near(h, access.test$stuckSpeed().x, 0.25, "Cobweb remains unchanged");
        }
        h.getLevel().setBlockAndUpdate(pos, Blocks.HONEY_BLOCK.defaultBlockState());
        near(h, access.test$blockSpeed(), 0.4, "Honey slowdown remains unchanged");
        h.succeed();
    }

    @GameTest
    public void swiftSneakRequiresEnchantment(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, 2));
        ScaleSneaking.tick(p);
        near(h, p.getAttributeValue(Attributes.SNEAKING_SPEED), 0.3, "No enchantment means no bonus");
        var leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
        leggings.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWIFT_SNEAK), 3);
        p.setItemSlot(EquipmentSlot.LEGS, leggings);
        p.tick();
        p.tick();
        near(h, p.getAttributeValue(Attributes.SNEAKING_SPEED), 1, "Shrink III plus Swift Sneak III");
        p.removeEffect(ScaleEffects.SHRINKING);
        p.tick();
        near(h, p.getAttributeValue(Attributes.SNEAKING_SPEED), 0.75, "Vanilla enchantment restored");
        p.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        p.tick();
        near(h, p.getAttributeValue(Attributes.SNEAKING_SPEED), 0.3, "Unequipping restores vanilla sneaking");
        h.succeed();
    }

    private static VibrationSystem.Listener listener(BlockPos pos) {
        return new VibrationSystem.Listener(new VibrationSystem() {
            final Data data = new Data();
            final User user = new User() {
                public int getListenerRadius() { return 16; }
                public PositionSource getPositionSource() { return new BlockPositionSource(pos); }
                public boolean canReceiveVibration(ServerLevel l, BlockPos p, Holder<GameEvent> e, GameEvent.Context c) { return true; }
                public void onReceiveVibration(ServerLevel l, BlockPos p, Holder<GameEvent> e, Entity s, Entity projectile, float d) {}
            };
            public Data getVibrationData() { return data; }
            public User getVibrationUser() { return user; }
        });
    }

    @GameTest
    public void vibrationRangePreservesInteractions(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos dest = h.absolutePos(new BlockPos(1, 3, 1));
        Vec3 center = Vec3.atCenterOf(dest);
        for (int tier = 1; tier <= 3; tier++) {
            p.removeEffect(ScaleEffects.SHRINKING);
            p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 200, tier - 1));
            for (boolean sprint : new boolean[]{false, true}) {
                p.setSprinting(sprint);
                double radius = 16 * ScalePhysics.vibrationRange(tier, sprint);
                h.assertTrue(listener(dest).handleGameEvent(h.getLevel(), GameEvent.STEP, GameEvent.Context.of(p), center.add(radius - 0.2, 0, 0)), "Step inside adjusted range");
                h.assertFalse(listener(dest).handleGameEvent(h.getLevel(), GameEvent.STEP, GameEvent.Context.of(p), center.add(radius + 0.2, 0, 0)), "Step outside adjusted range");
            }
        }
        h.assertTrue(listener(dest).handleGameEvent(h.getLevel(), GameEvent.BLOCK_DESTROY, GameEvent.Context.of(p), center.add(12, 0, 0)), "Block breaking remains audible");
        p.setShiftKeyDown(true);
        h.assertFalse(listener(dest).handleGameEvent(h.getLevel(), GameEvent.STEP, GameEvent.Context.of(p), center.add(1, 0, 0)), "Vanilla crouching stays silent");
        h.succeed();
    }

    @GameTest
    public void growthLandingWave(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        p.setHealth(p.getMaxHealth());
        BlockPos origin = h.absolutePos(new BlockPos(1, 2, 1));
        p.setPos(Vec3.atBottomCenterOf(origin));
        var pig = h.spawnWithNoFreeWill(EntityTypes.PIG, 2.5F, 2, 1.5F);
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, 0));
        GrowthImpact.land(p, 2, Blocks.STONE.defaultBlockState());
        near(h, pig.getDeltaMovement().length(), 0, "Normal jump has no wave");
        GrowthImpact.land(p, 5, Blocks.STONE.defaultBlockState());
        h.assertTrue(pig.getDeltaMovement().x > 0, "Wave pushes outwards");
        near(h, pig.getHealth(), pig.getMaxHealth(), "Growth I has no wave damage");
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 200, 2));
        float ownHealth = p.getHealth();
        p.fallDistance = 10;
        ((TestEntityAccess) p).test$land(0, true, Blocks.STONE.defaultBlockState(), origin.below());
        h.assertTrue(p.getHealth() < ownHealth, "Landing still hurts the giant");
        h.assertTrue(pig.getHealth() < pig.getMaxHealth(), "Growth III large fall damages nearby targets");
        h.assertTrue(pig.getMaxHealth() - pig.getHealth() <= 4, "Damage stays below two hearts");
        near(h, GrowthImpact.damage(3, 10000, 0), 4, "Extreme height damage cap");
        near(h, GrowthImpact.damage(3, 10000, 1), 0, "Damage vanishes at radius edge");
        h.assertTrue(GrowthImpact.radius(3, 10000) <= 6 && GrowthImpact.strength(3, 10000) <= 2, "Bounded radius and impulse");
        h.succeed();
    }
}
