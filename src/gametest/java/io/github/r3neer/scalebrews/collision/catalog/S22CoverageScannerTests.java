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

/** Implementation-facing S22 coverage-kernel tests. Adversarial holdouts remain independently owned. */
public final class S22CoverageScannerTests {
    @GameTest
    public void defaultBindingDerivesFullWithoutManualSpeciesStatus(GameTestHelper h) {
        var cow = id("minecraft:cow");
        var report = CollisionCoverageScanner.scan(List.of(cow), catalog(binding(cow, Map.of(), Set.of())), Map.of());
        var row = report.rows().getFirst();
        h.assertTrue(row.entity().equals(cow), "Coverage row must preserve the discovered entity id");
        h.assertTrue(row.status() == CollisionCoverageScanner.Status.FULL,
            "A default canonical binding with no known excluded states should derive FULL automatically");
        h.assertTrue(report.resolved(), "A FULL-only report must satisfy the unresolved gate");
        report.requireResolved();
        h.succeed();
    }

    @GameTest
    public void variantOnlyAndExcludedStatesRemainSafePartial(GameTestHelper h) {
        var cat = id("minecraft:cat");
        var cow = id("minecraft:cow");
        var report = CollisionCoverageScanner.scan(List.of(cow, cat), catalog(
            binding(cow, Map.of(), Set.of("sleeping")),
            binding(cat, Map.of("coat", "tabby"), Set.of())), Map.of());
        h.assertTrue(report.count(CollisionCoverageScanner.Status.SAFE_PARTIAL) == 2,
            "Known state gaps and variant-only bindings must not be promoted to FULL");
        h.assertTrue(report.rows().stream().anyMatch(row -> row.entity().equals(cow) && row.reason().contains("sleeping")),
            "SAFE_PARTIAL state evidence must identify the excluded state");
        h.assertTrue(report.rows().stream().anyMatch(row -> row.entity().equals(cat) && row.reason().contains("variants")),
            "Variant-only coverage must remain explicit in the report");
        h.succeed();
    }

    @GameTest
    public void variantStateGapDowngradesCleanDefaultDeterministically(GameTestHelper h) {
        var cow = id("minecraft:cow");
        var report = CollisionCoverageScanner.scan(List.of(cow), catalog(
            binding(cow, Map.of(), Set.of()),
            binding(cow, Map.of("coat", "brown"), Set.of("sleeping", "angry"))), Map.of());
        var row = report.rows().getFirst();
        h.assertTrue(row.status() == CollisionCoverageScanner.Status.SAFE_PARTIAL,
            "A clean default binding must not hide an excluded state on an explicit variant");
        h.assertTrue(row.reason().equals("canonical bindings exclude states: angry,sleeping"),
            "Aggregated excluded-state evidence must be sorted so report identity is reproducible");
        h.succeed();
    }

    @GameTest
    public void unresolvedTargetIsNeverSilentlyOmittedAndFailsGate(GameTestHelper h) {
        var cow = id("minecraft:cow");
        var pig = id("minecraft:pig");
        var report = CollisionCoverageScanner.scan(List.of(pig, cow, pig), catalog(binding(cow, Map.of(), Set.of())), Map.of());
        h.assertTrue(report.rows().size() == 2, "Every discovered id must appear exactly once even if discovery emits duplicates");
        h.assertTrue(report.rows().get(0).entity().equals(cow) && report.rows().get(1).entity().equals(pig),
            "Coverage rows must use deterministic entity-id order rather than discovery order");
        h.assertTrue(report.rows().get(1).status() == CollisionCoverageScanner.Status.UNRESOLVED,
            "A discovered entity with no binding or technical exclusion must be UNRESOLVED");
        boolean failed = false;
        try { report.requireResolved(); }
        catch (IllegalStateException expected) { failed = true; }
        h.assertTrue(failed, "NFR-032 gate must fail whenever UNRESOLVED is non-zero");
        h.succeed();
    }

    @GameTest
    public void technicalExclusionRequiresReasonAndCannotMaskCanonicalBinding(GameTestHelper h) {
        var dragon = id("minecraft:ender_dragon");
        var report = CollisionCoverageScanner.scan(List.of(dragon), catalog(), Map.of(dragon,
            new CollisionCoverageScanner.ExceptionRule(CollisionCoverageScanner.Status.EXCLUDED,
                "multipart boss requires a dedicated physical design")));
        h.assertTrue(report.rows().getFirst().status() == CollisionCoverageScanner.Status.EXCLUDED,
            "An explicit technical exclusion should be represented rather than omitted");
        report.requireResolved();

        boolean masked = false;
        try {
            CollisionCoverageScanner.scan(List.of(dragon), catalog(binding(dragon, Map.of(), Set.of())), Map.of(dragon,
                new CollisionCoverageScanner.ExceptionRule(CollisionCoverageScanner.Status.EXCLUDED, "try to hide binding")));
        } catch (IllegalArgumentException expected) {
            masked = true;
        }
        h.assertTrue(masked, "EXCLUDED must not hide a canonical binding that claims executable coverage");

        boolean blank = false;
        try { new CollisionCoverageScanner.ExceptionRule(CollisionCoverageScanner.Status.EXCLUDED, "   "); }
        catch (IllegalArgumentException expected) { blank = true; }
        h.assertTrue(blank, "Technical exclusions require a non-blank auditable reason");
        h.succeed();
    }

    @GameTest
    public void explicitSafePartialOnlyDowngradesRealCoverage(GameTestHelper h) {
        var cow = id("minecraft:cow");
        var pig = id("minecraft:pig");
        var rule = new CollisionCoverageScanner.ExceptionRule(CollisionCoverageScanner.Status.SAFE_PARTIAL,
            "renderer-specific state still awaiting verification");
        var report = CollisionCoverageScanner.scan(List.of(cow), catalog(binding(cow, Map.of(), Set.of())), Map.of(cow, rule));
        h.assertTrue(report.rows().getFirst().status() == CollisionCoverageScanner.Status.SAFE_PARTIAL,
            "Explicit known limitations may conservatively downgrade otherwise FULL coverage");

        boolean invented = false;
        try { CollisionCoverageScanner.scan(List.of(pig), catalog(), Map.of(pig, rule)); }
        catch (IllegalArgumentException expected) { invented = true; }
        h.assertTrue(invented, "SAFE_PARTIAL must never manufacture coverage when no canonical binding exists");
        h.succeed();
    }

    @GameTest
    public void digestIsStableAcrossDiscoveryAndBindingOrder(GameTestHelper h) {
        var cow = id("minecraft:cow");
        var cat = id("minecraft:cat");
        var cowBinding = binding(cow, Map.of(), Set.of());
        var catBinding = binding(cat, Map.of("coat", "tabby"), Set.of());
        var first = CollisionCoverageScanner.scan(List.of(cow, cat), catalog(cowBinding, catBinding), Map.of());
        var second = CollisionCoverageScanner.scan(List.of(cat, cow), catalog(catBinding, cowBinding), Map.of());
        h.assertTrue(first.canonicalText().equals(second.canonicalText()),
            "Coverage serialization must not depend on discovery or binding insertion order");
        h.assertTrue(first.digest().equals(second.digest()),
            "Identical target/catalog inputs must regenerate the same coverage digest");

        var changed = CollisionCoverageScanner.scan(List.of(cow, cat), catalog(cowBinding, catBinding), Map.of(cow,
            new CollisionCoverageScanner.ExceptionRule(CollisionCoverageScanner.Status.SAFE_PARTIAL, "newly observed limitation")));
        h.assertTrue(!first.digest().equals(changed.digest()),
            "A coverage classification change must change the reproducible report digest");
        h.succeed();
    }

    private static CollisionBindingCatalog catalog(CollisionBinding... bindings) {
        return new CollisionBindingCatalog(List.of(bindings));
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
