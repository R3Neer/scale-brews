package io.github.r3neer.scalebrews.collision.catalog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Adversarial S22 holdout: exact membership alone must not authenticate invented coverage classifications. */
public final class S22AdversarialCoverageProvenanceTests {
    @GameTest
    public void completeMembershipCannotForgeAnyResolvedCoverageClassification(GameTestHelper h) {
        var target = new CollisionCoverageDiscovery.Target(
            Identifier.parse("scalebrews_test:s22_minecraft_provenance"),
            "26.2", Set.of("minecraft"), Map.of("minecraft", "26.2"));
        var discovered = CollisionCoverageDiscovery.discover(target).livingEntityTypes();
        h.assertTrue(discovered.contains(Identifier.parse("minecraft:cow")) && discovered.size() > 1,
            "S22 provenance fixture requires a non-empty automatically discovered Minecraft LivingEntity target");

        // Preserve exact automatic membership and locally plausible row shape. None of this evidence came
        // from the canonical binding catalog/scanner or an explicit exclusion set, so each resolved status
        // must still fail closed. This rejects status-specific shallow repairs.
        var inventedFullEvidence = new CollisionCoverageScanner.BindingEvidence(
            Map.of(),
            Identifier.parse("proof:forged_geometry"),
            Identifier.parse("proof:forged_pose"),
            Identifier.parse("proof:forged_root"),
            Set.of());
        var inventedPartialEvidence = new CollisionCoverageScanner.BindingEvidence(
            Map.of("variant", "forged"),
            Identifier.parse("proof:forged_geometry"),
            Identifier.parse("proof:forged_pose"),
            Identifier.parse("proof:forged_root"),
            Set.of("forged_state_gap"));

        assertForgedResolvedClassificationRejected(h, target, discovered,
            CollisionCoverageScanner.Status.FULL,
            "forged full coverage with plausible but unauthenticated binding evidence",
            List.of(inventedFullEvidence));
        assertForgedResolvedClassificationRejected(h, target, discovered,
            CollisionCoverageScanner.Status.SAFE_PARTIAL,
            "forged safe-partial coverage with plausible but unauthenticated state-gap evidence",
            List.of(inventedPartialEvidence));
        assertForgedResolvedClassificationRejected(h, target, discovered,
            CollisionCoverageScanner.Status.EXCLUDED,
            "forged technical exclusion absent from the explicit exclusion authority",
            List.of());
        h.succeed();
    }

    private static void assertForgedResolvedClassificationRejected(
            GameTestHelper h,
            CollisionCoverageDiscovery.Target target,
            List<Identifier> discovered,
            CollisionCoverageScanner.Status status,
            String reason,
            List<CollisionCoverageScanner.BindingEvidence> evidence) {
        var forgedRows = discovered.stream()
            .map(id -> new CollisionCoverageScanner.Row(id, status, reason, evidence))
            .toList();

        boolean failedClosed = false;
        try {
            var forged = new CollisionCoverageDiscovery.Artifact(
                target, new CollisionCoverageScanner.Report(forgedRows));
            forged.requireResolved();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            failedClosed = true;
        }

        h.assertTrue(failedClosed,
            "FR-038/NFR-032/NFR-036: exact discovered membership and locally plausible row shape must not "
                + "authenticate caller-authored " + status + " coverage; resolved claims need canonical scanner provenance");
    }
}
