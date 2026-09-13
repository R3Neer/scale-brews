package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.scale.ScaleSize;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

public class ScaleReachTests {
    @GameTest public void purePotionReachValues(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (int tier = 1; tier <= 3; tier++) {
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(ScaleEffects.GROWTH, 1200, tier - 1));
            TestScale.settle(player);
            ScaleMountTests.near(h, player.blockInteractionRange(), 4.5 * (1 + .2 * tier), "Growth block reach tier " + tier);
            ScaleMountTests.near(h, player.entityInteractionRange(), 3 * (1 + .3 * tier), "Growth entity reach tier " + tier);

            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(ScaleEffects.SHRINKING, 1200, tier - 1));
            TestScale.settle(player);
            ScaleMountTests.near(h, player.blockInteractionRange(), 4.5 * (1 - .1 * tier), "Shrinking block reach tier " + tier);
            ScaleMountTests.near(h, player.entityInteractionRange(), 3 * (1 - .1 * tier), "Shrinking entity reach tier " + tier);
        }
        player.removeAllEffects(); TestScale.settle(player);
        ScaleMountTests.near(h, player.blockInteractionRange(), 4.5, "Normal block reach restored");
        ScaleMountTests.near(h, player.entityInteractionRange(), 3, "Normal entity reach restored");
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
        double equivalent = size >= 1 ? (size - 1) / .96 : (1 - size) / .242;
        double entityFactor = size >= 1 ? 1 + .3 * equivalent : 1 - .1 * Math.min(3, equivalent);
        double blockFactor = size >= 1 ? 1 + .2 * Math.min(3, equivalent) : 1 - .1 * Math.min(3, equivalent);
        ScaleMountTests.near(h, player.entityInteractionRange(), 3 * entityFactor, "Entity reach follows actual size");
        ScaleMountTests.near(h, player.blockInteractionRange(), 4.5 * blockFactor, "Block reach follows actual size");
    }
}
