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

/** Adversarial S22 holdout: semantically distinct coverage evidence must have distinct artifact identity. */
public final class S22AdversarialCoverageDigestTests {
    private static final Identifier ENTITY = Identifier.parse("minecraft:cow");

    @GameTest
    public void distinctVariantSelectorsMustNotAliasCanonicalCoverageDigest(GameTestHelper h) {
        // CollisionBinding deliberately permits arbitrary selector values. These two selectors are therefore
        // both valid but semantically distinct. A delimiter-based digest encoding must keep that distinction.
        var packedValue = Map.of("a", "b,c=d");
        var splitPairs = Map.of("a", "b", "c", "d");
        h.assertTrue(!packedValue.equals(splitPairs), "S22 digest fixture requires genuinely different selectors");

        var first = CollisionCoverageScanner.scan(
            List.of(ENTITY), catalog(binding(packedValue)), Map.of());
        var second = CollisionCoverageScanner.scan(
            List.of(ENTITY), catalog(binding(splitPairs)), Map.of());

        h.assertTrue(first.rows().getFirst().status() == CollisionCoverageScanner.Status.SAFE_PARTIAL
                && second.rows().getFirst().status() == CollisionCoverageScanner.Status.SAFE_PARTIAL,
            "S22 digest fixture must compare two valid variant-only SAFE_PARTIAL reports");
        h.assertTrue(!first.rows().equals(second.rows()),
            "S22 digest fixture must retain distinct selector evidence before serialization");
        h.assertTrue(!first.canonicalText().equals(second.canonicalText()),
            "NFR-028/NFR-036: distinct selector evidence must not alias in canonical coverage serialization; "
                + "escape or length-frame variant key/value boundaries before hashing");
        h.assertTrue(!first.digest().equals(second.digest()),
            "Distinct canonical coverage evidence must produce distinct report digests");
        h.succeed();
    }

    private static CollisionBindingCatalog catalog(CollisionBinding binding) {
        return new CollisionBindingCatalog(List.of(binding));
    }

    private static CollisionBinding binding(Map<String, String> variant) {
        return new CollisionBinding(CollisionBinding.SCHEMA_VERSION, ENTITY, variant,
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,
                Identifier.parse("proof:s22_digest_model"), Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(Identifier.parse("scalebrews:static"), Map.of(), Set.of()),
            BuiltInRootTransformProviders.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }
}
