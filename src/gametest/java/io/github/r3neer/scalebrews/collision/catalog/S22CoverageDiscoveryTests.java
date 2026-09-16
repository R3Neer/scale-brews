package io.github.r3neer.scalebrews.collision.catalog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Implementation tests for S22 automatic target discovery and target identity. */
public final class S22CoverageDiscoveryTests {
    @GameTest
    public void minecraftDiscoveryUsesDefaultAttributeAuthorityNotSpeciesList(GameTestHelper h) {
        var discovery = CollisionCoverageDiscovery.discover(target("26.2", Map.of("minecraft", "26.2")));
        var ids = discovery.livingEntityTypes();
        h.assertTrue(ids.contains(id("minecraft:cow")), "Automatic discovery must include ordinary LivingEntity types");
        h.assertTrue(ids.contains(id("minecraft:player")), "Automatic discovery must include Player without constructing it");
        h.assertTrue(!ids.contains(id("minecraft:item")), "Non-living entity types must not enter the coverage target");
        h.assertTrue(!ids.contains(id("minecraft:boat")), "Vehicles must not be misclassified as LivingEntity coverage targets");
        h.assertTrue(DefaultAttributes.hasSupplier(BuiltInRegistries.ENTITY_TYPE.getValue(id("minecraft:cow"))),
            "The discovery predicate must be backed by Minecraft's registered default-attribute authority");
        h.assertTrue(!DefaultAttributes.hasSupplier(BuiltInRegistries.ENTITY_TYPE.getValue(id("minecraft:item"))),
            "The attribute authority must distinguish non-living registry types in the real target");
        h.assertTrue(ids.stream().allMatch(value -> value.getNamespace().equals("minecraft")),
            "Namespace target must be enforced by discovery rather than filtered after classification");
        h.assertTrue(ids.equals(ids.stream().sorted(java.util.Comparator.comparing(Identifier::toString)).toList()),
            "Discovery output must be sorted reproducibly by registry id");
        h.assertTrue(ids.size() == ids.stream().distinct().count(), "Discovery must never emit duplicate entity ids");
        h.succeed();
    }

    @GameTest
    public void targetInputIdentityParticipatesInArtifactDigest(GameTestHelper h) {
        var firstTarget = target("26.2", Map.of("minecraft", "26.2", "fabric-loader", "0.19.5"));
        var sameTarget = target("26.2", Map.of("fabric-loader", "0.19.5", "minecraft", "26.2"));
        var changedTarget = target("26.2", Map.of("minecraft", "26.2", "fabric-loader", "0.19.6"));

        var first = unresolvedArtifact(firstTarget);
        var same = unresolvedArtifact(sameTarget);
        var changed = unresolvedArtifact(changedTarget);

        h.assertTrue(first.digest().equals(same.digest()),
            "Target input insertion order must not affect the reproducible artifact digest");
        h.assertTrue(!first.digest().equals(changed.digest()),
            "Changing any declared target input identity must change the acceptance artifact digest");
        h.succeed();
    }

    @GameTest
    public void artifactRejectsCoverageThatOmitsDiscoveredTargets(GameTestHelper h) {
        var target = target("26.2", Map.of("minecraft", "26.2"));
        boolean rejected = false;
        try {
            new CollisionCoverageDiscovery.Artifact(target, new CollisionCoverageScanner.Report(List.of()));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        h.assertTrue(rejected,
            "Acceptance Artifact must reject incomplete row membership even when the incomplete report has no UNRESOLVED rows");
        h.succeed();
    }

    @GameTest
    public void scanConnectsAutomaticDiscoveryToCoverageKernelWithoutOmissions(GameTestHelper h) {
        var target = target("26.2", Map.of("minecraft", "26.2"));
        var discovered = CollisionCoverageDiscovery.discover(target);
        var artifact = CollisionCoverageDiscovery.scan(target, new CollisionBindingCatalog(List.of()), Map.of());
        h.assertTrue(artifact.coverage().rows().size() == discovered.livingEntityTypes().size(),
            "Every automatically discovered LivingEntity type must receive exactly one coverage row");
        h.assertTrue(artifact.coverage().count(CollisionCoverageScanner.Status.UNRESOLVED) == discovered.livingEntityTypes().size(),
            "With an empty fixture catalog, discovery must expose every gap as UNRESOLVED instead of omitting it");
        h.assertTrue(artifact.coverage().rows().stream().allMatch(row -> row.status() == CollisionCoverageScanner.Status.UNRESOLVED),
            "Discovery must not invent support from registry presence alone");
        h.succeed();
    }

    @GameTest
    public void targetMetadataFailsClosedWhenVersionInputsAreMissing(GameTestHelper h) {
        boolean emptyInputs = false;
        try {
            new CollisionCoverageDiscovery.Target(id("scalebrews:bad-target"), "26.2", Set.of("minecraft"), Map.of());
        } catch (IllegalArgumentException expected) {
            emptyInputs = true;
        }
        h.assertTrue(emptyInputs, "A reproducible target must declare its versioned input identities");

        boolean badNamespace = false;
        try {
            new CollisionCoverageDiscovery.Target(id("scalebrews:bad-target"), "26.2", Set.of("Minecraft"), Map.of("minecraft", "26.2"));
        } catch (IllegalArgumentException expected) {
            badNamespace = true;
        }
        h.assertTrue(badNamespace, "Coverage target namespaces must use canonical registry namespace syntax");
        h.succeed();
    }

    private static CollisionCoverageDiscovery.Artifact unresolvedArtifact(CollisionCoverageDiscovery.Target target) {
        var discovered = CollisionCoverageDiscovery.discover(target);
        var report = CollisionCoverageScanner.scan(discovered.livingEntityTypes(), new CollisionBindingCatalog(List.of()), Map.of());
        return new CollisionCoverageDiscovery.Artifact(target, report);
    }

    private static CollisionCoverageDiscovery.Target target(String version, Map<String, String> inputs) {
        return new CollisionCoverageDiscovery.Target(id("scalebrews:minecraft-coverage"), version, Set.of("minecraft"), inputs);
    }

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }
}
