package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Diagnostic coverage for the untouched vanilla horse taming pipeline under Scale Brews hooks. */
public class HorseTamingRegressionTests {
    @GameTest public void feedingThroughRealInteractionRaisesTemper(GameTestHelper h) {
        ServerPlayer player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var horse = h.spawn(EntityTypes.HORSE, 1, 2, 1);
        player.setPos(horse.position());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_CARROT, 20));

        for (int i = 0; i < 20; i++) {
            var result = horse.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(result.consumesAction(), "Wild horse must consume accepted golden-carrot interaction " + i);
        }

        h.assertTrue(horse.getTemper() == horse.getMaxTemper(),
                "Twenty golden carrots must raise wild horse temper to 100; got " + horse.getTemper());
        h.assertTrue(player.getMainHandItem().isEmpty(), "Accepted horse food must be consumed");
        h.succeed();
    }

    @GameTest public void maxTemperHorseTamesAfterRealMountAtEveryAllowedSmallScale(GameTestHelper h) {
        double[] scales = {1.0, 0.758, 0.516, 0.274};
        for (int index = 0; index < scales.length; index++) {
            ServerPlayer player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
            player.getAttribute(Attributes.SCALE).setBaseValue(scales[index]);
            var horse = h.spawn(EntityTypes.HORSE, 1 + index, 2, 2);
            horse.setTemper(horse.getMaxTemper());
            player.setPos(horse.position());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            var result = horse.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(result.consumesAction(), "Vanilla wild-horse interaction must be accepted at scale " + scales[index]);
            h.assertTrue(player.getVehicle() == horse, "Scale riding gate blocked vanilla wild-horse mount at scale " + scales[index]);

            var goal = new RunAroundLikeCrazyGoal(horse, 1.2);
            horse.getRandom().setSeed(0x5CA1EBEEFL + index);
            for (int tick = 0; tick < 10_000 && !horse.isTamed(); tick++) goal.tick();

            h.assertTrue(horse.isTamed(), "Max-temper vanilla goal never tamed horse at scale " + scales[index]);
            player.stopRiding();
            horse.discard();
        }
        h.succeed();
    }
}
