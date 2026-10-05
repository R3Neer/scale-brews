package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.cow.CowVariants;
import net.minecraft.world.entity.animal.pig.PigVariants;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

/** IMPLEMENTER probes for common structural-model sources; no original geometry claim. */
public final class S50ImplementerModelComponentTests {
    @GameTest
    public void nativeHolderReferencesAndDirectValuesUseTheOriginalModelEnum(GameTestHelper h) {
        var cow = h.spawn(EntityTypes.COW, 2, 20, 2);
        var pig = h.spawn(EntityTypes.PIG, 4, 20, 2);
        var fox = h.spawn(EntityTypes.FOX, 6, 20, 2);
        try {
            var registry = CollisionVariantSources.snapshot();
            var source = registry.get(key(DataComponents.COW_VARIANT));
            var pigSource = registry.get(key(DataComponents.PIG_VARIANT));
            h.assertTrue(source != null && pigSource != null && source.values().equals(Set.of("normal", "cold", "warm")), "Structural family discovered with complete original domains");
            cow.setComponent(DataComponents.COW_VARIANT, h.getLevel().registryAccess().lookupOrThrow(Registries.COW_VARIANT).getOrThrow(CowVariants.COLD));
            pig.setComponent(DataComponents.PIG_VARIANT, h.getLevel().registryAccess().lookupOrThrow(Registries.PIG_VARIANT).getOrThrow(PigVariants.COLD));
            h.assertTrue(source.reader().value(cow).equals("cold") && pigSource.reader().value(pig).equals("cold"), "Bound native registry references project their actual model");
            cow.setComponent(DataComponents.COW_VARIANT, variant(CowVariant.ModelType.WARM, "first"));
            var first = source.reader().value(cow);
            cow.setComponent(DataComponents.COW_VARIANT, variant(CowVariant.ModelType.WARM, "different_cosmetic"));
            h.assertTrue(first == source.reader().value(cow), "Direct holders with cosmetic-only differences reuse the same prepared model name");
            var allocation = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
            allocation.setThreadAllocatedMemoryEnabled(true);
            for (int i = 0; i < 100000; i++) source.reader().value(cow);
            long thread = Thread.currentThread().threadId(), before = allocation.getThreadAllocatedBytes(thread);
            int lengths = 0;
            for (int i = 0; i < 100000; i++) lengths += source.reader().value(cow).length();
            long bytes = allocation.getThreadAllocatedBytes(thread) - before;
            h.assertTrue(lengths == 400000 && bytes <= 4096, "Native Cow component steady reader remains bounded: " + bytes);
            System.out.println("S50 IMPLEMENTER nativeHolderReads=100000 bytes=" + bytes);
            h.assertTrue(source.reader().value(fox) == null && pigSource.reader().value(cow) == null, "Absent component remains unknown");
            BuiltInVariantSources.initialize();
            h.assertTrue(CollisionVariantSources.snapshot().get(key(DataComponents.COW_VARIANT)) == source, "Repeated initialization preserves Source and prepared reader ownership");
            h.assertTrue(!key(DataComponents.COW_VARIANT).equals(CollisionVariantSources.componentKey(Identifier.parse("minecraft:cow/variant.model"))), "Projection suffix is distinct from a literal dotted component ID");
        } finally {cow.discard();pig.discard();fox.discard();}
        h.succeed();
    }

    static Holder<CowVariant> variant(CowVariant.ModelType model, String cosmetic) {
        var texture = Identifier.fromNamespaceAndPath("scalebrews_test", cosmetic);
        return Holder.direct(new CowVariant(new ModelAndTexture<>(model, texture), new ClientAsset.ResourceTexture(texture), SpawnPrioritySelectors.EMPTY));
    }
    static String key(DataComponentType<?> component) {
        return CollisionVariantSources.modelComponentKey(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component));
    }
}
