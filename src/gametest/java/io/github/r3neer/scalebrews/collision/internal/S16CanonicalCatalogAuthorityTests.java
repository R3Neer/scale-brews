package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.JsonParser;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static ModelGeometry fixtureModel() {
        return new ModelGeometry(1, "minecraft:cow", "1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body", "root", List.of(0d, 0d, 0d), List.of(1d, 1d, 1d), null)),
            ModelGeometry.values(new Matrix4f()));
    }
}
