package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.potion.ScalePotions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public class ScaleBrewingTests {
    @GameTest public void extendedPotionRecipes(GameTestHelper h) {
        var brewing = h.getLevel().potionBrewing();
        var redstone = new ItemStack(Items.REDSTONE);
        var glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        var eye = new ItemStack(Items.FERMENTED_SPIDER_EYE);
        for (Item container : new Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
            for (var family : java.util.List.of(
                    java.util.List.of(ScalePotions.GROWTH, ScalePotions.LONG_GROWTH, ScalePotions.STRONG_GROWTH, ScalePotions.VERY_STRONG_GROWTH),
                    java.util.List.of(ScalePotions.SHRINKING, ScalePotions.LONG_SHRINKING, ScalePotions.STRONG_SHRINKING, ScalePotions.VERY_STRONG_SHRINKING))) {
                var basic = PotionContents.createItemStack(container, family.get(0));
                h.assertTrue(brewing.hasMix(basic, redstone), "Level I accepts redstone in every container");
                var extended = brewing.mix(redstone, basic);
                expect(h, extended, container, family.get(1), 9600, 0);
                h.assertFalse(brewing.hasMix(extended, glowstone), "Extended potions cannot gain potency");
                h.assertFalse(brewing.hasMix(extended, redstone), "Extended potions cannot extend again");
                for (int tier = 2; tier <= 3; tier++) {
                    var stronger = PotionContents.createItemStack(container, family.get(tier));
                    h.assertFalse(brewing.hasMix(stronger, redstone), "Levels II/III cannot extend");
                }
                var strong = brewing.mix(glowstone, basic);
                expect(h, strong, container, family.get(2), 1800, 1);
                expect(h, brewing.mix(glowstone, strong), container, family.get(3), 900, 2);
                expect(h, brewing.mix(glowstone, extended), container, family.get(1), 9600, 0);
            }
            var growth = PotionContents.createItemStack(container, ScalePotions.LONG_GROWTH);
            h.assertTrue(brewing.hasMix(growth, eye), "Extended Growth can be corrupted");
            expect(h, brewing.mix(eye, growth), container, ScalePotions.LONG_SHRINKING, 9600, 0);
            var basic = PotionContents.createItemStack(container, ScalePotions.GROWTH);
            expect(h, brewing.mix(redstone, brewing.mix(eye, basic)), container, ScalePotions.LONG_SHRINKING, 9600, 0);
        }
        for (var potion : java.util.List.of(ScalePotions.LONG_GROWTH, ScalePotions.LONG_SHRINKING)) {
            var drink = PotionContents.createItemStack(Items.POTION, potion);
            var splash = brewing.mix(new ItemStack(Items.GUNPOWDER), drink);
            expect(h, splash, Items.SPLASH_POTION, potion, 9600, 0);
            var lingering = brewing.mix(new ItemStack(Items.DRAGON_BREATH), splash);
            expect(h, lingering, Items.LINGERING_POTION, potion, 9600, 0);
            // The stored duration is eight minutes; native lingering delivery uses its own duration factor.
            int[] duration = {0};
            lingering.get(DataComponents.POTION_CONTENTS).forEachEffect(e -> duration[0] = e.getDuration(),
                    lingering.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F));
            h.assertTrue(duration[0] == 2400, "Lingering retains vanilla quarter-duration behavior");
        }
        h.succeed();
    }

    private static void expect(GameTestHelper h, ItemStack result, Item container, Holder<Potion> potion, int duration, int amplifier) {
        h.assertTrue(result.is(container), "Brewing preserves the expected container");
        var contents = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        h.assertTrue(contents.is(potion), "Brewing resolves the expected registered potion");
        var effects = contents.getAllEffects().iterator();
        h.assertTrue(effects.hasNext(), "Potion has an effect");
        var effect = effects.next();
        h.assertTrue(effect.getDuration() == duration && effect.getAmplifier() == amplifier && !effects.hasNext(),
                "Potion contains exactly one effect with the expected duration and potency");
    }
}
