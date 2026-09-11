package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.integration.CollisionRules;
import io.github.r3neer.scalebrews.test.fixture.ExternalCollisionFixture;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Final G1 integration/fixture acceptance without crossing into G2/G3 runtime lifecycle. */
public final class S04G1IntegrationTests {
    @GameTest
    public void externalFixtureRegistersApiBehaviorSelectedByDatapackJson(GameTestHelper h) {
        h.assertTrue(CollisionEngines.geometry(ExternalCollisionFixture.GEOMETRY).isPresent()
                && CollisionEngines.pose(ExternalCollisionFixture.POSE).isPresent()
                && CollisionEngines.rootTransform(ExternalCollisionFixture.ROOT).isPresent()
                && CollisionAdapters.body(ExternalCollisionFixture.BODY).isPresent(),
            "The separate test mod must register all G1 extension behavior during initialization, before GameTests run");
        var catalog = CollisionBindingCatalog.load(h.getLevel().getServer().getResourceManager());
        var binding = catalog.resolve(Identifier.parse("minecraft:armor_stand"), Map.of("fixture", "external")).orElseThrow();
        h.assertTrue(binding.geometry().engine().equals(ExternalCollisionFixture.GEOMETRY),
            "JSON geometry engine id must select the registered public engine");
        h.assertTrue(binding.pose().engine().equals(ExternalCollisionFixture.POSE),
            "JSON pose engine id must select the registered public engine");
        h.assertTrue(binding.rootTransform().equals(ExternalCollisionFixture.ROOT),
            "JSON root provider id must select the registered public provider");
        h.assertTrue(CollisionAdapters.body(ExternalCollisionFixture.BODY).orElseThrow().category().equals("fixture_bodies"),
            "Fixture body adapter must be visible through the public API");
        h.succeed();
    }

    @GameTest
    public void canonicalCatalogRejectsUnregisteredEngineReferences(GameTestHelper h) {
        var entity = Identifier.parse("minecraft:pig");
        var base = fixtureBinding(entity, Map.of());
        var missingGeometry = new CollisionBinding(1, entity, Map.of(),
            new CollisionBinding.Geometry(Identifier.parse("scalebrews_test:missing_geometry"), base.geometry().model(), Map.of(), AnatomyFilter.DEFAULT),
            base.pose(), base.rootTransform(), CollisionPolicy.Patch.EMPTY, Set.of());
        var missingPose = new CollisionBinding(1, entity, Map.of(), base.geometry(),
            new CollisionBinding.Pose(Identifier.parse("scalebrews_test:missing_pose"), Map.of(), Set.of()),
            base.rootTransform(), CollisionPolicy.Patch.EMPTY, Set.of());
        var missingRoot = new CollisionBinding(1, entity, Map.of(), base.geometry(), base.pose(),
            Identifier.parse("scalebrews_test:missing_root"), CollisionPolicy.Patch.EMPTY, Set.of());
        h.assertTrue(rejected(missingGeometry), "Catalog rejects an unregistered geometry engine");
        h.assertTrue(rejected(missingPose), "Catalog rejects an unregistered pose engine");
        h.assertTrue(rejected(missingRoot), "Catalog rejects an unregistered root transform provider");
        h.succeed();
    }

    @GameTest
    public void canonicalCatalogFailsClosedOnAmbiguousVariantSelection(GameTestHelper h) {
        var entity = Identifier.parse("minecraft:pig");
        var first = fixtureBinding(entity, Map.of("coat", "brown"));
        var second = fixtureBinding(entity, Map.of("age", "adult"));
        var catalog = new CollisionBindingCatalog(List.of(first, second));
        h.assertTrue(catalog.resolve(entity, Map.of("coat", "brown", "age", "adult")).isEmpty(),
            "Equally-specific variant matches must fail closed instead of depending on resource order");
        h.succeed();
    }

    @GameTest
    public void structurallyDifferentVariantSelectorsCannotCollideThroughFormatting(GameTestHelper h) {
        var entity = Identifier.parse("minecraft:pig");
        var embeddedDelimiter = fixtureBinding(entity, Map.of("a", "b, c=d"));
        var twoEntries = fixtureBinding(entity, Map.of("a", "b", "c", "d"));
        h.assertTrue(embeddedDelimiter.variant().toString().equals(twoEntries.variant().toString()),
            "Fixture must reproduce the Map.toString collision that selector identity must ignore");
        var catalog = new CollisionBindingCatalog(List.of(embeddedDelimiter, twoEntries));
        h.assertTrue(catalog.resolve(entity, Map.of("a", "b, c=d")).orElseThrow().equals(embeddedDelimiter),
            "Structural selector identity preserves an embedded-delimiter value");
        h.assertTrue(catalog.resolve(entity, Map.of("a", "b", "c", "d")).orElseThrow().equals(twoEntries),
            "Structural selector identity preserves the genuinely two-entry variant");
        h.succeed();
    }

    @GameTest
    public void capabilitiesVersionTheNewG1Contract(GameTestHelper h) {
        long required = AnatomyApi.mask(AnatomyApi.Capability.ENGINE_REGISTRY,
            AnatomyApi.Capability.VERSIONED_BINDINGS, AnatomyApi.Capability.BODY_ADAPTERS);
        h.assertTrue(AnatomyApi.PROTOCOL_VERSION == 3 && AnatomyApi.DATA_SCHEMA_VERSION == 1,
            "G1 public/wire and data generations must be explicit");
        h.assertTrue(AnatomyApi.compatible(3, required), "Current peer must advertise every G1 extension capability");
        h.assertTrue(!AnatomyApi.compatible(2, required), "Pre-G1 protocol generation must fail compatibility explicitly");
        h.succeed();
    }

    @GameTest
    public void canonicalRulesKeepRatioFrictionAndEnablementInOneLayer(GameTestHelper h) {
        var support = Identifier.parse("minecraft:cow");
        var policy = new CollisionPolicy(1, new CollisionPolicy.Rule(true, .85, .6),
            Map.of("players", new CollisionPolicy.Patch(java.util.Optional.empty(), java.util.Optional.of(.8), java.util.Optional.empty())),
            Map.of(support, new CollisionPolicy.Patch(java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.of(.4))));
        var profile = new CollisionPolicy.Patch(java.util.Optional.empty(), java.util.Optional.of(.75), java.util.Optional.of(.5));
        h.assertTrue(CollisionRules.allows(policy, profile, "players", support, Math.nextDown(.75)), "Immediately below profile ratio is eligible");
        h.assertTrue(CollisionRules.allows(policy, profile, "players", support, .75), "Exact profile ratio boundary is eligible");
        h.assertTrue(!CollisionRules.allows(policy, profile, "players", support, Math.nextUp(.75)), "Immediately above profile ratio is rejected");
        h.assertTrue(Math.abs(CollisionRules.resolve(policy, profile, "players", support).friction() - .5) < 1e-9,
            "Friction resolves in the same canonical precedence layer");
        h.assertTrue(CollisionRules.allows(CollisionPolicy.DEFAULT, CollisionPolicy.Patch.EMPTY, "players", support, Math.nextDown(.85)),
            "Immediately below default 0.85 ratio is eligible");
        h.assertTrue(CollisionRules.allows(CollisionPolicy.DEFAULT, CollisionPolicy.Patch.EMPTY, "players", support, .85),
            "Exact default 0.85 ratio is eligible");
        h.assertTrue(!CollisionRules.allows(CollisionPolicy.DEFAULT, CollisionPolicy.Patch.EMPTY, "players", support, Math.nextUp(.85)),
            "Immediately above default 0.85 ratio is rejected");
        h.succeed();
    }

    private static boolean rejected(CollisionBinding binding) {
        try {
            new CollisionBindingCatalog(List.of(binding));
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static CollisionBinding fixtureBinding(Identifier entity, Map<String,String> variant) {
        return new CollisionBinding(1, entity, variant,
            new CollisionBinding.Geometry(ExternalCollisionFixture.GEOMETRY,
                Identifier.parse("scalebrews_test:model"), Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(ExternalCollisionFixture.POSE, Map.of(), Set.of()),
            ExternalCollisionFixture.ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
    }
}
