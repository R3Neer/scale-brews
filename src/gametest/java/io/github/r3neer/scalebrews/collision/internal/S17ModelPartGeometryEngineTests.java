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
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Common/dedicated S17 boundary: the family id exists without loading client preparation classes. */
public final class S17ModelPartGeometryEngineTests {
    @GameTest
    public void builtInModelPartEngineExistsAndFailsClosedWithoutClientDelegate(GameTestHelper h) {
        var engine = CollisionEngines.geometry(BuiltInGeometryEngines.MODEL_PART).orElse(null);
        h.assertTrue(engine != null, "Dedicated/common bootstrap must register scalebrews:model_part before catalog validation");
        h.assertTrue(engine.prepare(new GeometryEngine.Request(Identifier.parse("proof:missing_model_part"))).isEmpty(),
            "Common ModelPart dispatcher without client tooling must fail closed instead of inventing geometry");
        h.succeed();
    }

    @GameTest
    public void canonicalCatalogAcceptsBuiltInModelPartEngineIdOnServer(GameTestHelper h) {
        var binding = new CollisionBinding(CollisionBinding.SCHEMA_VERSION, Identifier.parse("minecraft:cow"), Map.of(),
            new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART, Identifier.parse("minecraft:cow"), Map.of(), AnatomyFilter.DEFAULT),
            new CollisionBinding.Pose(ExternalCollisionFixture.POSE, Map.of(), Set.of()), ExternalCollisionFixture.ROOT,
            CollisionPolicy.Patch.EMPTY, Set.of());
        var catalog = new CollisionBindingCatalog(List.of(binding));
        h.assertTrue(catalog.resolve(binding.entity(), Map.of()).orElse(null) == binding,
            "A dedicated server must validate a canonical ModelPart binding without loading a renderer/model tree");
        h.succeed();
    }

    @GameTest
    public void commonBridgePublicSignaturesContainNoClientTypes(GameTestHelper h) {
        for (var field : BuiltInGeometryEngines.class.getDeclaredFields())
            h.assertTrue(serverSafe(field.getType()), "Common built-in geometry field leaks a client type: " + field);
        for (var method : BuiltInGeometryEngines.class.getDeclaredMethods()) {
            h.assertTrue(serverSafe(method.getReturnType()), "Common built-in geometry return type leaks client code: " + method);
            for (var parameter : method.getParameterTypes())
                h.assertTrue(serverSafe(parameter), "Common built-in geometry parameter leaks client code: " + method);
        }
        h.succeed();
    }

    @GameTest
    public void commonBridgeBytecodeContainsNoClientPreparationReferences(GameTestHelper h) {
        var input = BuiltInGeometryEngines.class.getResourceAsStream("BuiltInGeometryEngines.class");
        if (input == null) throw new AssertionError("Could not inspect BuiltInGeometryEngines class bytes");
        try (input) {
            String constantPool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            h.assertTrue(!constantPool.contains("net/minecraft/client/"),
                "Common built-in geometry bytecode references net.minecraft.client outside its public signatures");
            h.assertTrue(!constantPool.contains("com/mojang/blaze3d/"),
                "Common built-in geometry bytecode references renderer classes outside its public signatures");
            h.assertTrue(!constantPool.contains("io/github/r3neer/scalebrews/client/collision/preparation/"),
                "Common built-in geometry bytecode references the client preparation implementation");
        } catch (IOException unreadable) {
            throw new AssertionError("Could not inspect BuiltInGeometryEngines bytecode", unreadable);
        }
        h.succeed();
    }

    @GameTest
    public void preparationDelegateHasOneOwnerAndSameOwnerReinstallIsIdempotent(GameTestHelper h) {
        try {
            var slot = BuiltInGeometryEngines.class.getDeclaredField("modelPartPreparation");
            slot.setAccessible(true);
            GeometryEngine first = request -> Optional.empty();
            GeometryEngine second = request -> Optional.empty();
            synchronized (BuiltInGeometryEngines.class) {
                Object previous = slot.get(null);
                try {
                    slot.set(null, null);
                    BuiltInGeometryEngines.installModelPartPreparation(first);
                    BuiltInGeometryEngines.installModelPartPreparation(first);
                    boolean rejected = false;
                    try {
                        BuiltInGeometryEngines.installModelPartPreparation(second);
                    } catch (IllegalStateException expected) {
                        rejected = true;
                    }
                    h.assertTrue(rejected, "A distinct second ModelPart preparation delegate must not steal the built-in family id");
                    h.assertTrue(slot.get(null) == first, "Rejected delegate replacement must retain the exact first owner");
                } finally {
                    slot.set(null, previous);
                }
            }
        } catch (ReflectiveOperationException inaccessible) {
            throw new AssertionError("Could not verify ModelPart preparation delegate ownership", inaccessible);
        }
        h.succeed();
    }

    private static boolean serverSafe(Class<?> type) {
        String name = type.getName();
        return !name.startsWith("net.minecraft.client.") && !name.startsWith("com.mojang.blaze3d.")
            && !name.contains("ModelPart") && !name.contains("Renderer");
    }
}
