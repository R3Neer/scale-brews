package io.github.r3neer.scalebrews.collision.catalog;

import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import io.github.r3neer.scalebrews.collision.internal.BuiltInRootTransformProviders;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Remaining S22 classification holdout: mixed default/variant state gaps must aggregate deterministically. */
public final class S22AdversarialCoverageResidualTests {
    @GameTest
    public void multipleVariantStateGapsDowngradeDefaultWithoutOrderOrDuplicateNoise(GameTestHelper h) {
        var cow = id("minecraft:cow");
        var cleanDefault = binding(cow, Map.of(), Set.of());
        var brown = binding(cow, Map.of("coat", "brown"), Set.of("sleeping", "angry"));
        var spotted = binding(cow, Map.of("coat", "spotted"), Set.of("sleeping", "grazing"));
        var baby = binding(cow, Map.of("age", "baby"), Set.of("grazing"));

        var first = CollisionCoverageScanner.scan(List.of(cow),
            new CollisionBindingCatalog(List.of(cleanDefault, brown, spotted, baby)), Map.of());
        var second = CollisionCoverageScanner.scan(List.of(cow),
            new CollisionBindingCatalog(List.of(baby, spotted, cleanDefault, brown)), Map.of());

        var row = first.rows().getFirst();
        h.assertTrue(row.status() == CollisionCoverageScanner.Status.SAFE_PARTIAL,
            "A clean default must not hide state gaps from any applicable variant binding");
        h.assertTrue(row.reason().equals("canonical bindings exclude states: angry,grazing,sleeping"),
            "State-gap union must deduplicate and sort all excluded states across default and multiple variants");
        h.assertTrue(first.canonicalText().equals(second.canonicalText()) && first.digest().equals(second.digest()),
            "Equivalent mixed default/variant evidence must be independent of binding insertion order");
        h.assertTrue(row.bindings().size() == 4,
            "Coverage evidence must retain every applicable canonical binding while aggregating state gaps");
        h.succeed();
    }

    private static CollisionBinding binding(Identifier entity, Map<String, String> variant, Set<String> excludedStates) {
        return new CollisionBinding(CollisionBinding.SCHEMA_VERSION, entity, variant,
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, id("proof:model"), Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(id("scalebrews:static"), Map.of(), Set.of()),
            BuiltInRootTransformProviders.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, excludedStates);
    }

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }
}
