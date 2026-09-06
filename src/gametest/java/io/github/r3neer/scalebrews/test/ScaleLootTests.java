package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.loot.ScaleLoot;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;

public class ScaleLootTests {
    @GameTest public void entityRulesAndRounding(GameTestHelper h) {
        var creeper = h.spawn(EntityTypes.CREEPER, 1, 2, 1);
        var witch = h.spawn(EntityTypes.WITCH, 3, 2, 1);
        var iron = h.spawn(EntityTypes.IRON_GOLEM, 5, 2, 1);
        var blaze = h.spawn(EntityTypes.BLAZE, 7, 2, 1);
        h.assertTrue(ScaleLoot.eligible(creeper, new ItemStack(Items.GUNPOWDER)), "Creeper gunpowder eligible");
        h.assertFalse(ScaleLoot.eligible(witch, new ItemStack(Items.GUNPOWDER)), "Witch context excluded");
        h.assertTrue(ScaleLoot.eligible(iron, new ItemStack(Items.IRON_INGOT)), "Iron production eligible");
        h.assertTrue(ScaleLoot.eligible(blaze, new ItemStack(Items.BLAZE_ROD)), "Blaze production eligible");
        for (double scale : new double[]{1, .76, .52, .28, 1.96, 2.92, 3.88}) {
            creeper.getAttribute(Attributes.SCALE).setBaseValue(scale);
            long[] total = {0};
            boolean[] zero = {false};
            for (int i = 0; i < 10000; i++) {
                var drops = new ArrayList<ItemStack>();
                ScaleLoot.emit(creeper, new ItemStack(Items.GUNPOWDER, 2), drops::add);
                if (drops.isEmpty()) zero[0] = true;
                drops.forEach(stack -> { total[0] += stack.getCount(); h.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "Legal stacks"); });
            }
            h.assertTrue(Math.abs(total[0] / 10000.0 - 2 * scale * scale) < .035, "Unbiased scale squared: " + scale);
            if (scale == 1) h.assertTrue(total[0] == 20000, "Normal size exact identity");
            if (scale == .28) h.assertTrue(zero[0], "Shrinking can yield no items");
        }
        h.succeed();
    }
    @GameTest public void finalLootConsumerScalesOnce(GameTestHelper h) {
        var creeper = h.spawn(EntityTypes.CREEPER, 1, 2, 1);
        creeper.getAttribute(Attributes.SCALE).setBaseValue(2);
        long[] total = {0};
        for (int i = 0; i < 1000; i++) {
            creeper.dropFromLootTable(h.getLevel(), creeper.damageSources().generic(), false,
                creeper.getLootTable().orElseThrow(), stack -> {
                    if (stack.is(Items.GUNPOWDER)) {
                        h.assertTrue(stack.getCount() % 4 == 0 && stack.getCount() <= 8, "Exactly one scale pass");
                        total[0] += stack.getCount();
                    }
                });
        }
        h.assertTrue(total[0] > 3400 && total[0] < 4600, "Real vanilla loot generation remains probabilistic");
        h.succeed();
    }

    @GameTest public void lootingComposesWithScale(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var weapon=new ItemStack(Items.DIAMOND_SWORD);
        weapon.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),3);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,weapon);
        var creeper=h.spawn(EntityTypes.CREEPER,2,2,2);
        creeper.setLastHurtByPlayer(player,100);
        creeper.getAttribute(Attributes.SCALE).setBaseValue(2);
        long[] total={0};
        for(int i=0;i<1000;i++)creeper.dropFromLootTable(h.getLevel(),player.damageSources().playerAttack(player),true,
            creeper.getLootTable().orElseThrow(),stack->{if(stack.is(Items.GUNPOWDER))total[0]+=stack.getCount();});
        h.assertTrue(total[0]>8500 && total[0]<11500,"Looting III final quantities scale with physical size");
        h.succeed();
    }

    @GameTest public void optionalMurmurAndLobsterRules(GameTestHelper h) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("alexsmobs")) { h.succeed(); return; }
        var registry=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
        var murmur=(net.minecraft.world.entity.LivingEntity)h.spawn(registry.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:murmur")),2,2,2);
        var lobster=(net.minecraft.world.entity.LivingEntity)h.spawn(registry.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:lobster")),4,2,2);
        var tendon=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:elastic_tendon"));
        var tail=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:lobster_tail"));
        h.assertTrue(ScaleLoot.eligible(murmur,new ItemStack(tendon)),"Real Murmur tendon eligible");
        h.assertFalse(ScaleLoot.eligible(lobster,new ItemStack(tail)),"Real lobster tail excluded");
        murmur.getAttribute(Attributes.SCALE).setBaseValue(2.92);
        long[] count={0};
        for(int n=0;n<1000;n++)murmur.dropFromLootTable(h.getLevel(),murmur.damageSources().generic(),false,
            murmur.getLootTable().orElseThrow(),stack->{if(stack.is(tendon))count[0]+=stack.getCount();});
        h.assertTrue(count[0]>7600 && count[0]<9400,"Murmur real loot table scales tendons with scale squared");
        var pig=h.spawn(EntityTypes.PIG,6,2,2);
        h.assertFalse(ScaleLoot.eligible(pig,new ItemStack(Items.SOUL_SAND)),"Ordinary pig lacks Shockwave material rule");
        pig.entityTags().add("deeper_dark.shockwave");
        h.assertTrue(ScaleLoot.eligible(pig,new ItemStack(Items.SOUL_SAND)),"Datapack-defined Shockwave body material enabled");
        var turtle=(net.minecraft.world.entity.LivingEntity)h.spawn(registry.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:alligator_snapping_turtle")),8,2,2);
        turtle.getAttribute(Attributes.SCALE).setBaseValue(2);
        var shearer=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        shearer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
        var scute=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:spiked_scute"));
        int harvested=0;
        try {
            var moss=turtle.getClass().getMethod("setMoss",int.class);
            for(int n=0;n<200;n++) {
                moss.invoke(turtle,10);
                shearer.interactOn(turtle,net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);
                for(var item:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,turtle.getBoundingBox().inflate(3))) {
                    if(item.getItem().is(scute)) {
                        h.assertTrue(item.getItem().getCount()==4,"Sheared scute scales once");
                        harvested+=item.getItem().getCount();
                    }
                    item.discard();
                }
            }
        } catch(ReflectiveOperationException error) { throw new RuntimeException(error); }
        h.assertTrue(harvested>280 && harvested<520,"Real snapping-turtle shearing yields scaled scutes");
        h.succeed();
    }
}
