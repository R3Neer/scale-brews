package io.github.r3neer.scalebrews.collision.catalog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Adversarial S22 holdout: acceptance artifacts must not bypass automatic discovery completeness. */
public final class S22AdversarialCoverageCompletenessTests {
    @GameTest
    public void manuallyAssembledArtifactCannotResolveWhileOmittingDiscoveredTargets(GameTestHelper h) {
        var target = new CollisionCoverageDiscovery.Target(
            Identifier.parse("scalebrews_test:s22_minecraft_completeness"),
            "26.2", Set.of("minecraft"), Map.of("minecraft", "26.2"));
        var discovered = CollisionCoverageDiscovery.discover(target).livingEntityTypes();
        h.assertTrue(discovered.contains(Identifier.parse("minecraft:cow")) && discovered.size() > 1,
            "S22 completeness fixture requires a non-empty automatically discovered Minecraft LivingEntity target");

        // A Report with no rows contains no UNRESOLVED rows, but it is not a complete report for this Target.
        // Artifact is the acceptance identity/gate object, so direct assembly must not turn omission into success.
        boolean failedClosed = false;
        try {
            var incomplete = new CollisionCoverageDiscovery.Artifact(
                target, new CollisionCoverageScanner.Report(List.of()));
            incomplete.requireResolved();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            failedClosed = true;
        }

        h.assertTrue(failedClosed,
            "FR-038/FR-039/FR-040: an acceptance artifact that silently omits discovered LivingEntity rows "
                + "must not satisfy requireResolved(); completeness must be tied to automatic discovery");
        h.succeed();
    }
}
