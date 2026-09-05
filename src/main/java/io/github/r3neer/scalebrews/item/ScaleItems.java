package io.github.r3neer.scalebrews.item;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public final class ScaleItems {
    private static final ResourceKey<Item> FLOWER_KEY = ResourceKey.create(Registries.ITEM, ScaleBrews.id("flower_on_a_stick"));
    public static final Item FLOWER_ON_A_STICK = Registry.register(BuiltInRegistries.ITEM, FLOWER_KEY,
            new Item(new Item.Properties().setId(FLOWER_KEY).stacksTo(1)));
    private ScaleItems() {}
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(FLOWER_ON_A_STICK));
    }
}
