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
    public void completeMembershipCannotForgeFullCoverageWithoutCanonicalEvidence(GameTestHelper h) {
        var target = new CollisionCoverageDiscovery.Target(
            Identifier.parse("scalebrews_test:s22_minecraft_provenance"),
            "26.2", Set.of("minecraft"), Map.of("minecraft", "26.2"));
        var discovered = CollisionCoverageDiscovery.discover(target).livingEntityTypes();
        h.assertTrue(discovered.contains(Identifier.parse("minecraft:cow")) && discovered.size() > 1,
            "S22 provenance fixture requires a non-empty automatically discovered Minecraft LivingEntity target");

        // Preserve exact automatic membership and provide locally plausible FULL evidence for every row.
        // This deliberately defeats a shallow repair such as "FULL requires at least one binding". None of
        // this evidence came from the canonical binding catalog or scanner, so acceptance must still fail.
        var inventedEvidence = new CollisionCoverageScanner.BindingEvidence(
            Map.of(),
            Identifier.parse("proof:forged_geometry"),
            Identifier.parse("proof:forged_pose"),
            Identifier.parse("proof:forged_root"),
            Set.of());
        var forgedRows = discovered.stream()
            .map(id -> new CollisionCoverageScanner.Row(id, CollisionCoverageScanner.Status.FULL,
                "forged full coverage with plausible but unauthenticated binding evidence", List.of(inventedEvidence)))
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
            "FR-038/NFR-032/NFR-036: an acceptance artifact must not satisfy requireResolved() merely because "
                + "its row ids and local row shape look valid; FULL/SAFE_PARTIAL/EXCLUDED claims need canonical scanner provenance");
        h.succeed();
    }
}
