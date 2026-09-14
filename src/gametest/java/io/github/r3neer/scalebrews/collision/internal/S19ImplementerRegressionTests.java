package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Implementer-owned common-side regressions for S19 family bootstrap and fail-closed dispatch. */
public final class S19ImplementerRegressionTests {
    @GameTest
    public void builtInGeometryBootstrapIsIdempotent(GameTestHelper h) {
        var modelPart = CollisionEngines.geometry(BuiltInGeometryEngines.MODEL_PART).orElseThrow();
        var advanced = CollisionEngines.geometry(BuiltInGeometryEngines.ADVANCED_MODEL_BOX).orElseThrow();
        BuiltInGeometryEngines.initialize();
        h.assertTrue(CollisionEngines.geometry(BuiltInGeometryEngines.MODEL_PART).orElseThrow() == modelPart,
            "Repeated built-in bootstrap must preserve the exact ModelPart owner");
        h.assertTrue(CollisionEngines.geometry(BuiltInGeometryEngines.ADVANCED_MODEL_BOX).orElseThrow() == advanced,
            "Repeated built-in bootstrap must preserve the exact AdvancedModelBox owner");
        h.succeed();
    }

    @GameTest
    public void serverDispatcherRejectsUnknownSourcesAndParametersWithoutClientMaterial(GameTestHelper h) {
        var engine = CollisionEngines.geometry(BuiltInGeometryEngines.ADVANCED_MODEL_BOX).orElseThrow();
        var unknown = Identifier.parse("example:not_registered");
        h.assertTrue(engine.prepare(new GeometryEngine.Request(unknown)).isEmpty(),
            "Dedicated/common AdvancedModelBox dispatch must not invent geometry for an unknown source");
        h.assertTrue(engine.prepare(new GeometryEngine.Request(unknown, Map.of("species", "grizzly"))).isEmpty(),
            "Technology-family dispatch must not reinterpret species parameters as an implicit model registry");
        h.succeed();
    }
}
