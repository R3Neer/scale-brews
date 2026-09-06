package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.item.ScaleItems;
import io.github.r3neer.scalebrews.mount.TinyMountTemptation;
import io.github.r3neer.scalebrews.test.mixin.TestMobGoalsAccess;
import io.github.r3neer.scalebrews.test.mixin.TestTemptGoalAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;

public class ScaleTemptationTests {
    private static boolean accepts(Mob mob, Player player) {
        return ((TestMobGoalsAccess)mob).scalebrews$goals().getAvailableGoals().stream()
            .map(g -> g.getGoal()).filter(g -> g instanceof TemptGoal)
            .anyMatch(g -> ((TestTemptGoalAccess)g).scalebrews$follows(player));
    }

    @GameTest public void vanillaSteeringItemsAttractWithoutRiding(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var pig = h.spawn(EntityTypes.PIG, 1, 2, 1);
        var strider = h.spawn(EntityTypes.STRIDER, 2, 2, 1);
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(hand, new ItemStack(Items.CARROT_ON_A_STICK));
            h.assertTrue(accepts(pig, player), "Native pig follows carrot stick in either hand, without saddle");
            h.assertFalse(accepts(strider, player), "Strider does not follow carrot stick");
            player.setItemInHand(hand, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
            h.assertTrue(accepts(strider, player), "Native strider temptation tag includes fungus stick");
            h.assertFalse(accepts(pig, player), "Pig does not follow fungus stick");
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        pig.discard(); strider.discard(); h.succeed();
    }

    @GameTest public void beeSteeringAttractionPreservesFoodAndToggles(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var bee = h.spawn(EntityTypes.BEE, 1, 2, 1);
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(hand, new ItemStack(ScaleItems.FLOWER_ON_A_STICK));
            h.assertTrue(accepts(bee, player), "Unsaddled bee follows unshrunk player in either hand");
            h.assertFalse(bee.isFood(player.getItemInHand(hand)), "Steering item is not breeding food");
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        for (String json : new String[]{"{\"tiny_mounts\":false}", "{\"mounts\":{\"minecraft:bee\":false}}"}) {
            try (var ignored = new RuleTestScope(json)) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ScaleItems.FLOWER_ON_A_STICK));
                h.assertFalse(accepts(bee, player), "Disabled mounts do not follow steering items");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DANDELION));
                h.assertTrue(accepts(bee, player) && bee.isFood(player.getMainHandItem()), "Native flower attraction and breeding preserved");
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ScaleItems.FLOWER_ON_A_STICK));
        bee.setTarget(player);
        h.assertFalse(accepts(bee, player), "Stick does not distract an attacking bee");
        bee.setTarget(null);
        bee.setNoAi(true);
        h.assertFalse(accepts(bee, player), "NoAI is respected");
        bee.discard(); h.succeed();
    }

    @GameTest public void jsonSteeringItemsAndFallbackGoals(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var chicken = h.spawn(EntityTypes.CHICKEN, 1, 2, 1);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        h.assertFalse(accepts(chicken, player), "Direct-control default chicken does not follow stick");
        try (var ignored = new TinyDefinitionTestScope(definition("chicken", "minecraft:stick", true))) {
            h.assertTrue(accepts(chicken, player), "New item-steered JSON extends existing native goal");
            var goals = ((TestMobGoalsAccess)chicken).scalebrews$goals();
            int count = goals.getAvailableGoals().size();
            TinyMountTemptation.install(chicken, goals);
            h.assertTrue(count == goals.getAvailableGoals().size(), "Existing native temptation is not duplicated");
        }
        for (String json : new String[]{definition("chicken", "minecraft:stick", false), definition("chicken", "missing:unknown", true)}) {
            try (var ignored = new TinyDefinitionTestScope(json)) {
                h.assertFalse(accepts(chicken, player), "Disabled or missing item definition does not attract");
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                h.assertFalse(accepts(chicken, player), "Missing item never resolves to empty hand");
            }
        }
        var golem = h.spawn(EntityTypes.SNOW_GOLEM, 2, 2, 1);
        try (var ignored = new TinyDefinitionTestScope(definition("snow_golem", "minecraft:stick", true))) {
            var access = (TestMobGoalsAccess)golem;
            access.scalebrews$aiStep();
            access.scalebrews$aiStep();
            long count = access.scalebrews$goals().getAvailableGoals().stream()
                .filter(g -> g.getGoal() instanceof TinyMountTemptation.SteeringGoal).count();
            h.assertTrue(count == 1, "Mob without native temptation receives one fallback through actual AI hook");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
            h.assertTrue(TinyMountTemptation.follows(golem, player), "Fallback uses configured item in either hand");
        }
        chicken.discard(); golem.discard(); h.succeed();
    }

    private static String definition(String entity, String item, boolean enabled) {
        return "{\"entity\":\"minecraft:" + entity + "\",\"control\":\"item_steered\",\"movement\":\"ground\","
            + "\"speed\":0.18,\"saddle\":false,\"steering_item\":\"" + item + "\",\"enabled\":" + enabled + "}";
    }
}
