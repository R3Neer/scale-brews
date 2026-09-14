package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.test.fixture.ExternalCollisionFixture;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * S19 pre-implementation contract for the AdvancedModelBox geometry family.
 *
 * <p>This class intentionally uses only common/server-safe APIs and the literal family id so the
 * red baseline compiles before the production family exists. Client/tooling extractor holdouts are
 * frozen in the sprint contract and are added only once there is a real implementation surface to
 * attack; they must not redefine the contract.</p>
 */
public final class S19AdvancedModelBoxGeometryContractTests {
    private static final Identifier ADVANCED_MODEL_BOX = Identifier.parse("scalebrews:advanced_model_box");
    private static final Identifier GRIZZLY = Identifier.parse("alexsmobs:grizzly_bear");

    @GameTest
    public void builtInAdvancedModelBoxEngineExistsAndFailsClosedWithoutClientDelegate(GameTestHelper h) {
        var engine = CollisionEngines.geometry(ADVANCED_MODEL_BOX).orElse(null);
        h.assertTrue(engine != null,
            "Dedicated/common bootstrap must register scalebrews:advanced_model_box before catalog validation");
        h.assertTrue(engine.prepare(new GeometryEngine.Request(GRIZZLY)).isEmpty(),
            "Common AdvancedModelBox dispatcher without client/tooling preparation must fail closed");
        h.succeed();
    }

    @GameTest
    public void canonicalCatalogAcceptsAdvancedModelBoxFamilyIdOnServer(GameTestHelper h) {
        var binding = new CollisionBinding(CollisionBinding.SCHEMA_VERSION, GRIZZLY, Map.of(),
            new CollisionBinding.Geometry(ADVANCED_MODEL_BOX, GRIZZLY, Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(ExternalCollisionFixture.POSE, Map.of(), Set.of()),
            ExternalCollisionFixture.ROOT, CollisionPolicy.Patch.EMPTY, Set.of());
        var catalog = new CollisionBindingCatalog(List.of(binding));
        h.assertTrue(catalog.resolve(binding.entity(), Map.of()).orElse(null) == binding,
            "Dedicated/server catalog must recognize the AdvancedModelBox family without loading Alex/Citadel models");
        h.succeed();
    }

    @GameTest
    public void builtInFamilyIdCannotBeStolenAfterBootstrap(GameTestHelper h) {
        var owner = CollisionEngines.geometry(ADVANCED_MODEL_BOX).orElse(null);
        h.assertTrue(owner != null, "AdvancedModelBox built-in owner must exist before ownership can be tested");

        boolean rejected = false;
        try {
            CollisionEngines.registerGeometry(ADVANCED_MODEL_BOX, request -> java.util.Optional.empty());
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "External registration must not steal the built-in AdvancedModelBox family id");
        h.assertTrue(CollisionEngines.geometry(ADVANCED_MODEL_BOX).orElse(null) == owner,
            "Rejected id theft must retain the exact original AdvancedModelBox engine owner");
        h.succeed();
    }

    @GameTest
    public void commonBridgePublicSignaturesContainNoClientOrExternalModelTypes(GameTestHelper h) {
        for (var field : BuiltInGeometryEngines.class.getDeclaredFields())
            h.assertTrue(serverSafe(field.getType()), "Common built-in geometry field leaks a client/external type: " + field);
        for (var method : BuiltInGeometryEngines.class.getDeclaredMethods()) {
            h.assertTrue(serverSafe(method.getReturnType()), "Common built-in geometry return type leaks client/external code: " + method);
            for (var parameter : method.getParameterTypes())
                h.assertTrue(serverSafe(parameter), "Common built-in geometry parameter leaks client/external code: " + method);
        }
        h.succeed();
    }

    @GameTest
    public void commonBridgeBytecodeContainsNoClientAlexOrCitadelReferences(GameTestHelper h) {
        var input = BuiltInGeometryEngines.class.getResourceAsStream("BuiltInGeometryEngines.class");
        if (input == null) throw new AssertionError("Could not inspect BuiltInGeometryEngines class bytes");
        try (input) {
            String constantPool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            h.assertTrue(!constantPool.contains("net/minecraft/client/"),
                "Common built-in geometry bytecode references net.minecraft.client");
            h.assertTrue(!constantPool.contains("com/mojang/blaze3d/"),
                "Common built-in geometry bytecode references renderer classes");
            h.assertTrue(!constantPool.contains("io/github/r3neer/scalebrews/client/collision/preparation/"),
                "Common built-in geometry bytecode references the client preparation implementation");
            h.assertTrue(!constantPool.contains("com/github/alexthe666/"),
                "Common built-in geometry bytecode references Alex/Citadel classes");
            h.assertTrue(!constantPool.contains("/alexsmobs/") && !constantPool.contains("/citadel/"),
                "Common built-in geometry bytecode embeds an external model-family class instead of a neutral SPI");
        } catch (IOException unreadable) {
            throw new AssertionError("Could not inspect BuiltInGeometryEngines bytecode", unreadable);
        }
        h.succeed();
    }

    private static boolean serverSafe(Class<?> type) {
        String name = type.getName();
        return !name.startsWith("net.minecraft.client.")
            && !name.startsWith("com.mojang.blaze3d.")
            && !name.startsWith("com.github.alexthe666.")
            && !name.contains("AdvancedModelBox")
            && !name.contains("alexsmobs")
            && !name.contains("citadel")
            && !name.contains("Renderer");
    }
}
