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

        // Preserve exact automatic membership while fabricating every semantic result as FULL with no
        // binding evidence. A membership-only Artifact check must not turn this into trusted acceptance.
        var forgedRows = discovered.stream()
            .map(id -> new CollisionCoverageScanner.Row(id, CollisionCoverageScanner.Status.FULL,
                "forged full coverage without canonical binding evidence", List.of()))
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
                + "its row ids match discovery; FULL/SAFE_PARTIAL/EXCLUDED classifications need canonical scanner provenance");
        h.succeed();
    }
}
