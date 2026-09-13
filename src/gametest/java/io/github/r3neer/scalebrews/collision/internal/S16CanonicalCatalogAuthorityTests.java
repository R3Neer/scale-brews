package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.JsonParser;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.migration.LegacyAnatomyCatalogMigration;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Red-first G3/S16 holdouts for canonical catalog authority. */
public final class S16CanonicalCatalogAuthorityTests {
    @GameTest
    public void incompatibleCanonicalBundleSchemaOwnsProtocolV4(GameTestHelper h) {
        h.assertTrue(AnatomyApi.PROTOCOL_VERSION == 4,
            "Replacing legacy profiles with canonical bindings inside the authoritative bundle is an incompatible wire change and must own protocol v4");
        h.succeed();
    }

    @GameTest
    public void acceptedCatalogRecordsContainNoPlatformDefinitionAuthority(GameTestHelper h) {
        for (var type : List.of(WorldAnatomyCatalog.Binding.class, WorldAnatomyCatalog.Snapshot.class)) {
            for (var component : type.getRecordComponents()) {
                h.assertTrue(component.getType() != PlatformDefinition.class && !component.getName().equals("profiles"),
                    type.getSimpleName() + "." + component.getName() + " still exposes PlatformDefinition/profile authority");
            }
        }
        h.succeed();
    }

    @GameTest
    public void wireBundleUsesCanonicalBindingsInsteadOfLegacyProfiles(GameTestHelper h) {
        var model = fixtureModel();
        var profile = new PlatformDefinition(Identifier.parse("minecraft:cow"), true, .6, Optional.empty(), List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(model.source()), Identifier.parse("scalebrews:static"), AnatomyFilter.DEFAULT)));
        var packets = AnatomyCatalogTransfer.encode(UUID.randomUUID(), 1, Map.of(model.source(), model), Map.of("fixture:cow", profile));
        var complete = new ByteArrayOutputStream();
        for (var packet : packets) complete.writeBytes(packet.fragment());
        var json = JsonParser.parseString(complete.toString(java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        h.assertTrue(json.has("bindings"), "Authoritative catalog bundle must carry canonical bindings");
        h.assertTrue(!json.has("profiles"), "Protocol-v4 catalog bundle must not carry legacy PlatformDefinition profiles");
        h.succeed();
    }

    @GameTest
    public void legacyPlaneProfileNeverAppearsAsAnatomicalBindingOnTheWire(GameTestHelper h) {
        var plane = new PlatformDefinition(Identifier.parse("minecraft:cow"), true, .6, Optional.empty(),
            List.of(new PlatformDefinition.Surface("back", 0, 1.25, 0, .75, 1, Optional.empty())), Optional.empty());
        var packets = AnatomyCatalogTransfer.encode(UUID.randomUUID(), 1, Map.of(), Map.of("fixture:legacy_plane", plane));
        var complete = new ByteArrayOutputStream();
        for (var packet : packets) complete.writeBytes(packet.fragment());
        var json = JsonParser.parseString(complete.toString(java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        h.assertTrue(json.has("bindings"), "Migrated wire catalog must use the canonical binding collection even when no anatomy survives migration");
        h.assertTrue(json.getAsJsonArray("bindings").isEmpty(), "Legacy one-sided planes must never be promoted into canonical anatomical bindings");
        h.succeed();
    }

    @GameTest
    public void variantBridgeReferencesMustValidateBeforeAtomicPublication(GameTestHelper h) {
        var catalog = new WorldAnatomyCatalog();
        var epoch = UUID.randomUUID();
        var accepted = catalog.snapshot();
        var prepared = catalog.preparedPackets(epoch);
        var variant = new CollisionBinding(CollisionBinding.SCHEMA_VERSION, Identifier.parse("minecraft:cow"), Map.of("coat", "brown"),
            new CollisionBinding.Geometry(LegacyCollisionData.PRECOMPUTED_GEOMETRY, Identifier.parse("proof:missing_variant_model"),
                Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(LegacyCollisionData.LEGACY_POSE_PROVIDER, Map.of("provider", "scalebrews:static"), Set.of()),
            LegacyCollisionData.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());

        boolean rejected = false;
        try {
            catalog.replace(Map.of(), List.of(variant));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        h.assertTrue(rejected,
            "A variant-only compatibility binding is still part of the accepted candidate and must validate its model/provider/filter references before publication");
        h.assertTrue(catalog.snapshot() == accepted,
            "Rejecting an invalid variant binding must retain the exact previously accepted snapshot object");
        h.assertTrue(catalog.preparedPackets(epoch) == prepared,
            "Rejecting an invalid variant binding must retain the exact S15 prepared bundle paired with the accepted snapshot");
        h.succeed();
    }

    @GameTest
    public void canonicalAndMigratedLegacyDuplicateSelectorFailsAtomically(GameTestHelper h) {
        var model = fixtureModel();
        var profile = new PlatformDefinition(Identifier.parse("minecraft:cow"), true, .6, Optional.empty(), List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse(model.source()), Identifier.parse("scalebrews:static"), AnatomyFilter.DEFAULT)));
        var migrated = LegacyAnatomyCatalogMigration.bindings(List.of(profile)).get(0);
        var canonical = new CollisionBinding(CollisionBinding.SCHEMA_VERSION, migrated.entity(), Map.of(), migrated.geometry(), migrated.pose(),
            migrated.rootTransform(), new CollisionPolicy.Patch(Optional.empty(), Optional.empty(), Optional.of(.91)), Set.of());
        var candidate = new ArrayList<CollisionBinding>();
        candidate.add(canonical);
        candidate.add(migrated);

        var catalog = new WorldAnatomyCatalog();
        var epoch = UUID.randomUUID();
        var accepted = catalog.snapshot();
        var prepared = catalog.preparedPackets(epoch);
        boolean rejected = false;
        try {
            catalog.replace(Map.of(model.source(), model), candidate);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        h.assertTrue(rejected,
            "Canonical and migrated legacy bindings for the same selector must conflict instead of gaining precedence from merge/file order");
        h.assertTrue(catalog.snapshot() == accepted,
            "A canonical/legacy selector conflict must leave the exact previously accepted snapshot published");
        h.assertTrue(catalog.preparedPackets(epoch) == prepared,
            "A canonical/legacy selector conflict must leave the exact previously prepared bundle published");
        h.succeed();
    }

    @GameTest
    public void variantSelectorRoundTripsWithoutInventingDefaultExecution(GameTestHelper h) {
        var model = fixtureModel();
        var entity = Identifier.parse("minecraft:cow");
        var selector = Map.of("coat", "brown");
        var variant = new CollisionBinding(CollisionBinding.SCHEMA_VERSION, entity, selector,
            new CollisionBinding.Geometry(LegacyCollisionData.PRECOMPUTED_GEOMETRY, Identifier.parse(model.source()),
                Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(LegacyCollisionData.LEGACY_POSE_PROVIDER, Map.of("provider", "scalebrews:static"), Set.of()),
            LegacyCollisionData.ENTITY_ROOT, CollisionPolicy.Patch.EMPTY, Set.of());

        var packets = AnatomyCatalogTransfer.encode(UUID.randomUUID(), 7, Map.of(model.source(), model), List.of(variant));
        var receiver = new AnatomyCatalogTransfer();
        boolean completed = false;
        for (var packet : packets) completed |= receiver.accept(packet);
        var snapshot = receiver.snapshot();

        h.assertTrue(completed && receiver.ready() && receiver.revision() == 7,
            "A complete protocol-v4 variant catalog must publish the announced revision atomically");
        h.assertTrue(snapshot.catalog().resolve(entity, selector).orElse(null).equals(variant),
            "Variant selector and binding identity must survive canonical wire round-trip");
        h.assertTrue(snapshot.catalog().resolve(entity, Map.of()).isEmpty(),
            "A variant-only binding must not resolve for the empty/default selector after transfer");
        h.assertTrue(!snapshot.bindings().containsKey(entity),
            "S16 must retain a variant canonically without inventing default bridge execution before runtime variant authority exists");
        h.succeed();
    }

    private static ModelGeometry fixtureModel() {
        return new ModelGeometry(1, "minecraft:cow", "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
