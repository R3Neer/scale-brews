package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.scale.ScaleSize;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ScaleReachTests {
    @GameTest public void giantCanAttackAtFeet(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(2, 20, 2)));
        var chicken = h.spawnWithNoFreeWill(EntityTypes.CHICKEN, 2, 20, 2);
        for (int tier = 1; tier <= 3; tier++) {
            player.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, tier - 1));
            TestScale.settle(player);
            // Beside the giant's feet, not inside its body. Test an actual small mob.
            chicken.setPos(player.position().add(player.getBbWidth() / 2 + .3, 0, 0));
            ScaleMountTests.near(h, player.entityInteractionRange(), 3 * (1 + .5 * tier), "Entity reach tier " + tier);
            ScaleMountTests.near(h, player.blockInteractionRange(), 4.5 * (1 + .2 * tier), "Block reach unchanged");
            for (var weapon : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.IRON_SWORD)}) {
                h.assertTrue(player.isWithinAttackRange(weapon, chicken.getBoundingBox(), 0), "Small mob at feet in actual weapon attack range");
                var far = AABB.ofSize(player.getEyePosition().add(0, 0, 12), .1, .1, .1);
                h.assertFalse(player.isWithinAttackRange(weapon, far, 0), "No excessive twelve-block reach");
            }
            chicken.setHealth(chicken.getMaxHealth());
            chicken.invulnerableTime = 0;
            player.attack(chicken);
            h.assertTrue(chicken.getHealth() < chicken.getMaxHealth(), "In-range melee causes real damage");
        }
        chicken.discard();
        player.removeAllEffects(); TestScale.settle(player);
        ScaleMountTests.near(h, player.entityInteractionRange(), 3, "Normal reach restored");
        h.succeed();
    }

    @GameTest public void reachFollowsActualSize(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (int growth = 1; growth <= 3; growth++) for (int shrink = 1; shrink <= 3; shrink++) {
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, growth - 1));
            player.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 1200, shrink - 1));
            TestScale.settle(player);
            checkSize(h, player);
        }
        player.removeAllEffects(); TestScale.settle(player);
        for (double size : new double[]{.28, .76, 1, 1.4, 1.96, 2.92, 3.88, 6, 1}) {
            player.getAttribute(Attributes.SCALE).setBaseValue(size);
            ScaleSize.tick(player);
            checkSize(h, player);
        }
        h.succeed();
    }

    private static void checkSize(GameTestHelper h, net.minecraft.world.entity.player.Player player) {
        double size = player.getAttributeValue(Attributes.SCALE);
        double factor = size >= 1 ? 1 + .5 * (size - 1) / .96 : 1 - .12 * Math.min(3, (1 - size) / .242);
        ScaleMountTests.near(h, player.entityInteractionRange(), 3 * factor, "Reach follows actual size, not potion presence");
    }
}
