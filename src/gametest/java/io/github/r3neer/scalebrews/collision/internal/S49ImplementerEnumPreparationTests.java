package io.github.r3neer.scalebrews.collision.internal;

import com.mojang.serialization.Codec;
import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

/** IMPLEMENTER development probes; these do not certify original model geometry or FULL. */
public final class S49ImplementerEnumPreparationTests {
    @GameTest
    public void oneComponentFamilyReadsSeveralNativeEnumsAndNoDefaultOnAbsentEntity(GameTestHelper h) {
        var fox = h.spawn(EntityTypes.FOX, 2, 20, 2);
        var salmon = h.spawn(EntityTypes.SALMON, 4, 20, 2);
        var rabbit = h.spawn(EntityTypes.RABBIT, 6, 20, 2);
        var cow = h.spawn(EntityTypes.COW, 8, 20, 2);
        try {
            var sources = CollisionVariantSources.snapshot();
            var foxSource = sources.get(key(DataComponents.FOX_VARIANT));
            var fishSource = sources.get(key(DataComponents.SALMON_SIZE));
            var rabbitSource = sources.get(key(DataComponents.RABBIT_VARIANT));
            h.assertTrue(foxSource != null && fishSource != null && rabbitSource != null, "Native enum component families were discovered without a mob list");
            fox.setComponent(DataComponents.FOX_VARIANT, Fox.Variant.SNOW);
            salmon.setComponent(DataComponents.SALMON_SIZE, Salmon.Variant.LARGE);
            rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.BLACK);
            h.assertTrue(foxSource.reader().value(fox).equals(Fox.Variant.SNOW.getSerializedName())
                && fishSource.reader().value(salmon).equals(Salmon.Variant.LARGE.getSerializedName())
                && rabbitSource.reader().value(rabbit).equals(Rabbit.Variant.BLACK.getSerializedName()), "Sources return original serialized component values");
            h.assertTrue(foxSource.reader().value(cow) == null && fishSource.reader().value(fox) == null, "Absent component remains unknown, never a default enum ordinal");
            var reader = foxSource.reader();
            BuiltInVariantSources.initialize();
            h.assertTrue(CollisionVariantSources.snapshot().get(key(DataComponents.FOX_VARIANT)).reader() == reader, "Built-in initialization retains the prepared owner identity");
        } finally {fox.discard();salmon.discard();rabbit.discard();cow.discard();}
        h.succeed();
    }

    @GameTest
    public void extensionFactoryPreparesSerializedNamesAndRejectsAmbiguousDomains(GameTestHelper h) {
        var component = DataComponentType.<Extension>builder().persistent(Codec.STRING.xmap(text -> Extension.JAVA_WIDE, value -> "fixture")).build();
        var source = CollisionVariantSources.enumComponent(component, Extension.class);
        h.assertTrue(source.values().equals(Set.of("external_wide", "external_slim")), "Public factory uses serialized names rather than Java enum names");
        try {
            CollisionVariantSources.enumComponent(DataComponentType.<Duplicate>builder().persistent(Codec.STRING.xmap(text -> Duplicate.A, value -> "fixture")).build(), Duplicate.class);
            throw new AssertionError("Ambiguous serialized enum domain was accepted");
        } catch (IllegalArgumentException expected) { }
        h.assertTrue(key(DataComponents.FOX_VARIANT).equals("component.minecraft.fox_2fvariant"), "The actual original slash component ID is representable");
        var a = CollisionVariantSources.componentKey(Identifier.parse("example:a/b"));
        var b = CollisionVariantSources.componentKey(Identifier.parse("example:a_2fb"));
        var c = CollisionVariantSources.componentKey(Identifier.parse("example:a.b"));
        h.assertTrue(!a.equals(b) && !a.equals(c) && !b.equals(c), "Separator and escape literals retain distinct component identities");
        h.succeed();
    }

    static String key(DataComponentType<?> component) {
        return CollisionVariantSources.componentKey(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component));
    }
    private enum Extension implements StringRepresentable {
        JAVA_WIDE("external_wide"), JAVA_SLIM("external_slim");
        private final String name;
        Extension(String name) {this.name = name;}
        public String getSerializedName() {return name;}
    }
    private enum Duplicate implements StringRepresentable {
        A, B;
        public String getSerializedName() {return "same";}
    }
}
