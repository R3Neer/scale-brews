package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.integration.CollisionRules;
import io.github.r3neer.scalebrews.test.fixture.ExternalCollisionFixture;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Final G1 integration/fixture acceptance without crossing into G2/G3 runtime lifecycle. */
public final class S04G1IntegrationTests {
    @GameTest
    public void externalFixtureRegistersApiBehaviorSelectedByDatapackJson(GameTestHelper h) {
        ExternalCollisionFixture.register();
        var catalog = CollisionBindingCatalog.load(h.getLevel().getServer().getResourceManager());
        var binding = catalog.resolve(Identifier.parse("minecraft:armor_stand"), Map.of("fixture", "external")).orElseThrow();
        h.assertTrue(binding.geometry().engine().equals(ExternalCollisionFixture.GEOMETRY)
                && CollisionEngines.geometry(binding.geometry().engine()).isPresent(),
            "JSON geometry engine id must resolve through the public registry");
        h.assertTrue(binding.pose().engine().equals(ExternalCollisionFixture.POSE)
                && CollisionEngines.pose(binding.pose().engine()).isPresent(),
            "JSON pose engine id must resolve through the public registry");
        h.assertTrue(binding.rootTransform().equals(ExternalCollisionFixture.ROOT)
                && CollisionEngines.rootTransform(binding.rootTransform()).isPresent(),
            "JSON root provider id must resolve through the public registry");
        h.assertTrue(CollisionAdapters.body(ExternalCollisionFixture.BODY).orElseThrow().category().equals("fixture_bodies"),
            "Fixture body adapter must register through the public API");
        h.succeed();
    }

    @GameTest
    public void canonicalCatalogFailsClosedOnAmbiguousVariantSelection(GameTestHelper h) {
        var entity = Identifier.parse("minecraft:pig");
        var first = fixtureBinding(entity, Map.of("coat", "brown"));
        var second = fixtureBinding(entity, Map.of("age", "adult"));
        var catalog = new CollisionBindingCatalog(java.util.List.of(first, second));
        h.assertTrue(catalog.resolve(entity, Map.of("coat", "brown", "age", "adult")).isEmpty(),
            "Equally-specific variant matches must fail closed instead of depending on resource order");
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
        h.assertTrue(CollisionRules.allows(policy, profile, "players", support, .75), "Exact ratio boundary is eligible");
        h.assertTrue(!CollisionRules.allows(policy, profile, "players", support, .750001), "Above profile ratio is rejected");
        h.assertTrue(Math.abs(CollisionRules.resolve(policy, profile, "players", support).friction() - .5) < 1e-9,
            "Friction resolves in the same canonical precedence layer");
        h.succeed();
    }

    private static io.github.r3neer.scalebrews.collision.data.CollisionBinding fixtureBinding(Identifier entity, Map<String,String> variant) {
        return new io.github.r3neer.scalebrews.collision.data.CollisionBinding(1, entity, variant,
            new io.github.r3neer.scalebrews.collision.data.CollisionBinding.Geometry(ExternalCollisionFixture.GEOMETRY,
                Identifier.parse("scalebrews_test:model"), Map.of(), io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter.DEFAULT),
            new io.github.r3neer.scalebrews.collision.data.CollisionBinding.Pose(ExternalCollisionFixture.POSE, Map.of(), java.util.Set.of()),
            ExternalCollisionFixture.ROOT, CollisionPolicy.Patch.EMPTY, java.util.Set.of());
    }
}
