package io.github.r3neer.scalebrews.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.mount.MountSizePolicy;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.test.mixin.TestCreeperAccess;
import io.github.r3neer.scalebrews.test.mixin.TestEntityAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ScaleEntityTests {
    @GameTest public void splashAndLingeringCreeperPower(GameTestHelper h) {
        for (boolean lingering : new boolean[]{false, true}) {
            var c = h.spawnWithNoFreeWill(EntityTypes.CREEPER, 1, 50, 1);
            c.setNoGravity(true);
            var potion = new ItemStack(lingering ? Items.LINGERING_POTION : Items.SPLASH_POTION);
            var contents = net.minecraft.world.item.alchemy.PotionContents.EMPTY.withEffectAdded(
                new MobEffectInstance(ScaleEffects.GROWTH, 1200, 2));
            potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, contents);
            if (lingering) {
                var thrown = h.spawn(EntityTypes.LINGERING_POTION, 1, 50, 1);
                thrown.onHitAsPotion(h.getLevel(), potion, new net.minecraft.world.phys.EntityHitResult(c));
                thrown.discard();
                for (var cloud : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, c.getBoundingBox().inflate(4))) {
                    // ServerLevel increments tickCount before calling Entity.tick().
                    for (int tick = 0; tick < 25; tick++) { cloud.tickCount++; cloud.tick(); }
                    cloud.discard();
                }
            } else {
                var thrown = h.spawn(EntityTypes.SPLASH_POTION, 1, 50, 1);
                thrown.onHitAsPotion(h.getLevel(), potion, new net.minecraft.world.phys.EntityHitResult(c));
                thrown.discard();
            }
            h.assertTrue(c.hasEffect(ScaleEffects.GROWTH), "Vanilla thrown potion applies Growth, lingering=" + lingering);
            ((TestCreeperAccess)c).test$explode();
            ScaleMountTests.near(h, ExplosionObservation.radius, 4.5, "Thrown effect reaches real explosion");
            for (var cloud : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, c.getBoundingBox().inflate(10))) cloud.discard();
        }
        h.succeed();
    }

    @GameTest public void mobWaveConfiguration(GameTestHelper h) {
        for (String json : java.util.List.of("{}", "{\"growth_landing_impact\":false}",
                "{\"growth_landing_knockback\":false}", "{\"growth_landing_damage\":false}")) {
            var mob = h.spawnWithNoFreeWill(EntityTypes.PIG, 1, 2, 1);
            mob.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, 2));
            var target = h.spawnWithNoFreeWill(EntityTypes.PIG, 2.5F, 2, 1);
            try (var ignored = new RuleTestScope(json)) {
                target.setDeltaMovement(Vec3.ZERO);
                io.github.r3neer.scalebrews.physics.GrowthImpact.land(mob, 12, Blocks.STONE.defaultBlockState());
                var rules = io.github.r3neer.scalebrews.config.ScaleRules.get(h.getLevel());
                h.assertTrue((target.getDeltaMovement().length() > 0) == rules.growthImpact(), "Combined impact switch controls radial push");
                h.assertTrue((target.getHealth() < target.getMaxHealth()) == rules.growthImpact(), "Combined impact switch controls Growth III damage");
                var encoded = io.github.r3neer.scalebrews.config.ScaleRules.CODEC.encodeStart(JsonOps.INSTANCE, rules).getOrThrow().getAsJsonObject();
                h.assertFalse(encoded.has("growth_landing_knockback") || encoded.has("growth_landing_damage"), "Only combined switch is encoded");
                if (!rules.growthImpact()) {
                    var source = new net.minecraft.world.damagesource.DamageSource(h.getLevel().registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getOrThrow(io.github.r3neer.scalebrews.physics.ScaleCombat.LANDING), mob);
                    target.hurtServer(h.getLevel(), source, 2);
                    h.assertTrue(target.getDeltaMovement().length() > 0, "Switch never cancels normal received-damage recoil");
                    float health = mob.getHealth();
                    mob.fallDistance = 10;
                    try (var observation = new LandingObservation(mob)) {
                        ((TestEntityAccess)mob).test$land(0, true, Blocks.STONE.defaultBlockState(), h.absolutePos(new BlockPos(1, 1, 1)));
                        h.assertTrue(observation.gusts == 0 && observation.landings == 1, "Disabled impact has no gust but retains native landing event");
                    }
                    h.assertTrue(mob.getHealth() < health, "Combined impact switch does not cancel self fall damage");
                }
            }
            mob.discard(); target.discard();
        }
        h.succeed();
    }

    @GameTest(maxTicks = 100) public void naturalMobFall(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(1, 21, 1));
        for (int x = -3; x <= 5; x++) for (int z = -3; z <= 3; z++)
            h.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        var mob = h.spawnWithNoFreeWill(EntityTypes.PIG, 1, 36, 1);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        mob.setHealth(200);
        mob.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, 2));
        float health = mob.getHealth();
        var target = h.spawnWithNoFreeWill(EntityTypes.PIG, 3.5F, 21, 1);
        h.succeedWhen(() -> {
            h.assertTrue(mob.onGround() && mob.getHealth() < health, "Gravity produces a real landing with self damage");
            h.assertTrue(target.getHealth() < target.getMaxHealth(), "Natural mob landing produces Growth III wave: source=" + mob.position() + " target=" + target.position() + " health=" + target.getHealth());
            h.assertTrue(target.getMaxHealth() - target.getHealth() <= 4, "Natural wave remains bounded");
        });
    }

    @GameTest public void relativeMountMatrix(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var horse = h.spawnWithNoFreeWill(EntityTypes.HORSE, 1, 2, 1);
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, 1));
        ScaleMountTests.settle(p);
        for (int tier = 1; tier <= 3; tier++) {
            horse.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, tier - 1));
            ScaleMountTests.settle(horse);
            h.assertTrue(p.startRiding(horse, true, true) == (tier >= 2), "Growth II / horse Growth " + tier);
            p.stopRiding();
        }
        horse.removeEffect(ScaleEffects.GROWTH);
        p.removeEffect(ScaleEffects.GROWTH);
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, 0));
        ScaleMountTests.settle(horse); ScaleMountTests.settle(p);
        h.assertFalse(p.startRiding(horse, true, true), "Growth I cannot ride normal horse");
        p.removeEffect(ScaleEffects.GROWTH);
        for (var type : java.util.List.of(EntityTypes.BEE, EntityTypes.CHICKEN)) {
            var mount = h.spawn(type, 2, 2, 1);
            mount.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            int[][] cases = {{2, 0, 1}, {2, 1, 0}, {3, 1, 1}, {3, 2, 0}};
            for (int[] row : cases) {
                p.removeEffect(ScaleEffects.SHRINKING); mount.removeEffect(ScaleEffects.SHRINKING);
                p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 1200, row[0] - 1));
                if (row[1] > 0) mount.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 1200, row[1] - 1));
                ScaleMountTests.settle(p); ScaleMountTests.settle(mount);
                h.assertTrue(p.startRiding(mount, true, true) == (row[2] == 1), "Tiny ratio matrix " + java.util.Arrays.toString(row));
                p.stopRiding();
            }
            mount.discard();
        }
        horse.discard(); h.succeed();
    }

    @GameTest public void effectiveScaleAndTransitionEnforcement(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var horse = h.spawnWithNoFreeWill(EntityTypes.HORSE, 1, 2, 1);
        var external = ScaleBrews.id("test_external_scale");
        p.getAttribute(Attributes.SCALE).addTransientModifier(new AttributeModifier(external, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        h.assertFalse(p.startRiding(horse, true, true), "External rider SCALE participates without our effects");
        horse.getAttribute(Attributes.SCALE).addTransientModifier(new AttributeModifier(external, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        h.assertTrue(p.startRiding(horse, true, true), "External mount SCALE participates");
        horse.getAttribute(Attributes.SCALE).removeModifier(external);
        TinyMounts.enforceRider(p);
        h.assertFalse(p.isPassenger(), "Mount size change dismounts existing rider");
        p.getAttribute(Attributes.SCALE).removeModifier(external);
        h.assertTrue(p.startRiding(horse, true, true), "Equal baseline size");
        p.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, 0));
        h.assertTrue(p.isPassenger(), "Adding effect before SCALE changes does not prematurely dismount");
        new io.github.r3neer.scalebrews.scale.ScaleTransition().tick(p);
        TinyMounts.enforceRider(p);
        h.assertFalse(p.isPassenger(), "First actual oversize transition step dismounts");
        var cart = h.spawn(EntityTypes.MINECART, 1, 2, 2);
        var boat = h.spawn(EntityTypes.OAK_BOAT, 1, 2, 3);
        h.assertTrue(p.startRiding(cart, true, true), "Minecart exempt"); p.stopRiding();
        h.assertTrue(p.startRiding(boat, true, true), "Boat exempt"); p.stopRiding();
        var chicken = h.spawn(EntityTypes.CHICKEN, 2, 2, 2);
        p.removeEffect(ScaleEffects.GROWTH);
        p.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 1200, 2));
        ScaleMountTests.settle(p);
        h.assertFalse(p.startRiding(chicken, true, true), "Ratio never bypasses saddle requirement");
        chicken.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE)); chicken.setBaby(true);
        h.assertFalse(p.startRiding(chicken, true, true), "Ratio never bypasses tiny baby restriction");
        chicken.setBaby(false);
        h.assertTrue(p.startRiding(chicken, true, true), "Adult saddled tiny mount");
        var second = h.makeMockPlayer(GameType.SURVIVAL);
        second.getAttribute(Attributes.SCALE).setBaseValue(.28);
        h.assertFalse(second.startRiding(chicken, true, true), "No additional tiny passenger");
        p.stopRiding();
        horse.discard(); cart.discard(); boat.discard(); chicken.discard(); h.succeed();
    }

    @GameTest public void ratioConfigurationAndMigration(GameTestHelper h) {
        var horse = h.spawn(EntityTypes.HORSE, 1, 2, 1);
        var camel = h.spawn(EntityTypes.CAMEL, 1, 2, 2);
        var ghast = h.spawn(EntityTypes.HAPPY_GHAST, 1, 2, 3);
        var policy = MountSizePolicy.get(h.getLevel());
        ScaleMountTests.near(h, policy.limit(horse), 1, "Horse default");
        ScaleMountTests.near(h, policy.limit(camel), 1.1, "Camel default");
        ScaleMountTests.near(h, policy.limit(ghast), 2, "Happy ghast default");
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getAttribute(Attributes.SCALE).setBaseValue(1.1);
        h.assertTrue(MountSizePolicy.permits(p, camel), "Exact JSON ratio boundary is inclusive");
        p.getAttribute(Attributes.SCALE).setBaseValue(1.1001);
        h.assertFalse(MountSizePolicy.permits(p, camel), "Values above ratio boundary are rejected");
        var chicken = h.spawn(EntityTypes.CHICKEN, 2, 2, 1);
        var def = TinyMounts.definition(chicken);
        var json = TinyMountDefinition.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow().getAsJsonObject();
        json.remove("max_rider_scale_ratio"); json.addProperty("minimum_shrinking", 3);
        var migrated = TinyMountDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        ScaleMountTests.near(h, migrated.maxRiderScaleRatio(), .53, "Legacy level field adopts new ratio");
        h.assertTrue(migrated.saddleVisual().equals(def.saddleVisual()) && migrated.control() == def.control(), "Migration retains artwork and controls");
        var custom = MountSizePolicy.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{\"default_max_rider_scale_ratio\":0.8,\"mounts\":{\"minecraft:chicken\":0.7}}" )).getOrThrow();
        ScaleMountTests.near(h, custom.limit(chicken), .7, "General override wins over tiny ratio");
        ScaleMountTests.near(h, custom.limit(horse), .8, "Configurable fallback");
        try (var ignored = new RuleTestScope("{\"tiny_mounts\":false}")) {
            ScaleMountTests.near(h, policy.limit(chicken), .53, "Size policy independent of optional controls");
        }
        horse.discard(); camel.discard(); ghast.discard(); chicken.discard(); h.succeed();
    }

    @GameTest public void actualCreeperExplosions(GameTestHelper h) {
        float[][] multipliers = {{1, 1.15F, 1.30F, 1.50F}, {1, .90F, .80F, .65F}};
        int family = 0;
        for (var effect : java.util.List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
            for (boolean charged : new boolean[]{false, true}) {
                float previousDamage = -1;
                for (int tier = 0; tier <= 3; tier++) {
                    // Above other fixtures: real blasts must not destroy neighbouring tests.
                    var c = h.spawn(EntityTypes.CREEPER, 1, 50, 1);
                    c.setNoAi(true); c.setNoGravity(true);
                    if (tier > 0) c.addEffect(new MobEffectInstance(effect, 1200, tier - 1));
                    if (charged) {
                        var lightning = h.spawn(EntityTypes.LIGHTNING_BOLT, 1, 50, 1);
                        c.thunderHit(h.getLevel(), lightning); lightning.discard();
                    }
                    var target = h.spawnWithNoFreeWill(EntityTypes.PIG, 3, 50, 1);
                    target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
                    var distant = h.spawnWithNoFreeWill(EntityTypes.PIG, 15, 50, 1);
                    var glass = c.blockPosition().below();
                    h.getLevel().setBlockAndUpdate(glass, Blocks.GLASS.defaultBlockState());
                    ExplosionObservation.radius = Float.NaN;
                    ((TestCreeperAccess)c).test$explode();
                    ScaleMountTests.near(h, ExplosionObservation.radius, (charged ? 6 : 3) * multipliers[family][tier], "Actual vanilla explosion power");
                    h.assertTrue(c.isRemoved() && target.getHealth() < 1000, "Vanilla explosion removes creeper and deals damage");
                    float dealt = 1000 - target.getHealth();
                    if (tier > 0) h.assertTrue(family == 0 ? dealt > previousDamage : dealt < previousDamage, "Explosion damage follows power progression");
                    previousDamage = dealt;
                    h.assertTrue((distant.getHealth() < distant.getMaxHealth()) == (ExplosionObservation.radius * 2 > 14), "Actual blast reach expands beyond charged baseline");
                    h.assertTrue(h.getLevel().getBlockState(glass).isAir(), "Actual explosion destroys nearby glass");
                    target.discard(); distant.discard();
                    for (var cloud : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, c.getBoundingBox().inflate(10))) cloud.discard();
                }
            }
            family++;
        }
        h.succeed();
    }

    @GameTest public void mobLandingRetainsVanillaDamage(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(1, 2, 1));
        for (int tier = 1; tier <= 3; tier++) {
            var mob = h.spawnWithNoFreeWill(EntityTypes.PIG, 1, 2, 1);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); mob.setHealth(200);
            mob.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, tier - 1));
            float ownHealth = mob.getHealth();
            var target = h.spawnWithNoFreeWill(EntityTypes.PIG, 2.5F, 2, 1);
            target.setDeltaMovement(Vec3.ZERO);
            mob.fallDistance = 2;
            ((TestEntityAccess)mob).test$land(0, true, Blocks.STONE.defaultBlockState(), origin.below());
            ScaleMountTests.near(h, target.getDeltaMovement().length(), 0, "Ordinary mob jump has no wave");
            mob.fallDistance = 10;
            ((TestEntityAccess)mob).test$land(0, false, Blocks.STONE.defaultBlockState(), origin.below());
            ScaleMountTests.near(h, target.getDeltaMovement().length(), 0, "Falling without landing has no wave");
            try (var observation = new LandingObservation(mob)) {
                ((TestEntityAccess)mob).test$land(0, true, Blocks.STONE.defaultBlockState(), origin.below());
                h.assertTrue(observation.landings == 1, "Exactly one native landing event retained");
                h.assertTrue(observation.gusts == 1 && observation.terrain >= 12 * tier, "Mob gust and ground particles dispatched");
            }
            h.assertTrue(target.getDeltaMovement().x > 0, "Mob landing pushes outwards");
            h.assertTrue(mob.getHealth() < ownHealth && mob.fallDistance == 0, "Vanilla self fall damage and distance reset survive");
            h.assertTrue((target.getHealth() < target.getMaxHealth()) == (tier == 3), "Only Growth III damages neighbours");
            h.assertTrue(target.getMaxHealth() - target.getHealth() <= 4, "Mob wave damage bounded");
            mob.discard(); target.discard();
        }
        var bee = h.spawn(EntityTypes.BEE, 1, 2, 1);
        bee.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, 2));
        var target = h.spawnWithNoFreeWill(EntityTypes.PIG, 2.5F, 2, 1);
        bee.fallDistance = 0;
        ((TestEntityAccess)bee).test$land(0, true, Blocks.STONE.defaultBlockState(), origin.below());
        ScaleMountTests.near(h, target.getDeltaMovement().length(), 0, "Flying mob touching ground without accumulated fall has no wave");
        bee.discard(); target.discard(); h.succeed();
    }
}
