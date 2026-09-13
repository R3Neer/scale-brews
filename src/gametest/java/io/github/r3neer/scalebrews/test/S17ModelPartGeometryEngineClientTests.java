package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Client/tooling acceptance for the reusable ModelPart geometry family. */
public final class S17ModelPartGeometryEngineClientTests implements FabricClientGameTest {
    private static final Identifier COW = Identifier.parse("scalebrews_test:s17_cow");
    private static final Identifier CHICKEN = Identifier.parse("scalebrews_test:s17_chicken");
    private static final Identifier BROKEN = Identifier.parse("scalebrews_test:s17_broken");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var engine = CollisionEngines.geometry(BuiltInGeometryEngines.MODEL_PART).orElseThrow();
            var transform = defaultTransform();

            ModelPartGeometryEngine.registerSource(COW,
                new ModelPartGeometryEngine.Source("26.2", () -> CowModel.createBodyLayer().bakeRoot(), S17ModelPartGeometryEngineClientTests::defaultTransform));
            ModelPartGeometryEngine.registerSource(CHICKEN,
                new ModelPartGeometryEngine.Source("26.2", () -> AdultChickenModel.createBodyLayer().bakeRoot(), S17ModelPartGeometryEngineClientTests::defaultTransform));

            var cow = engine.prepare(new GeometryEngine.Request(COW)).orElseThrow();
            var chicken = engine.prepare(new GeometryEngine.Request(CHICKEN)).orElseThrow();
            check(cow.format() == 2 && chicken.format() == 2, "ModelPart engine must publish explicit format-2 model transforms");
            check(cow.source().equals(COW.toString()) && chicken.source().equals(CHICKEN.toString()), "Prepared source identity must follow the request model id");
            check(!cow.parts().isEmpty() && !cow.pieces().isEmpty() && !chicken.parts().isEmpty() && !chicken.pieces().isEmpty(),
                "Two distinct vanilla ModelPart sources must produce material hierarchy through one engine");

            var cowAgain = engine.prepare(new GeometryEngine.Request(COW)).orElseThrow();
            check(cow.equals(cowAgain), "Fresh repeated ModelPart preparation must be deterministic without a geometry cache");

            var expectedCow = GeometryExtractor.vanilla(COW.toString(), "26.2", CowModel.createBodyLayer().bakeRoot(), Set.of()).withModelTransform(transform);
            var expectedChicken = GeometryExtractor.vanilla(CHICKEN.toString(), "26.2", AdultChickenModel.createBodyLayer().bakeRoot(), Set.of()).withModelTransform(transform);
            check(cow.equals(expectedCow) && chicken.equals(expectedChicken),
                "GeometryEngine output must match the existing original-tree extractor exactly for the same sources");

            check(engine.prepare(new GeometryEngine.Request(Identifier.parse("scalebrews_test:s17_unknown"))).isEmpty(),
                "Unknown ModelPart source must fail closed");
            check(engine.prepare(new GeometryEngine.Request(COW, Map.of("invented", "language"))).isEmpty(),
                "Unsupported engine parameters must not become an accidental procedural language");

            boolean duplicate = false;
            try {
                ModelPartGeometryEngine.registerSource(COW,
                    new ModelPartGeometryEngine.Source("26.2", () -> CowModel.createBodyLayer().bakeRoot()));
            } catch (IllegalArgumentException expected) {
                duplicate = true;
            }
            check(duplicate, "Duplicate ModelPart source ids must be rejected");

            ModelPartGeometryEngine.registerSource(BROKEN,
                new ModelPartGeometryEngine.Source("26.2", () -> null, S17ModelPartGeometryEngineClientTests::defaultTransform));
            boolean broken = false;
            try {
                engine.prepare(new GeometryEngine.Request(BROKEN));
            } catch (IllegalArgumentException expected) {
                broken = true;
            }
            check(broken, "A broken source must fail preparation explicitly");
            check(engine.prepare(new GeometryEngine.Request(COW)).orElseThrow().equals(cow),
                "Failure of one ModelPart source must not contaminate another source or leave stale prepared geometry");

            System.out.println("S17_MODEL_PART_ENGINE PASS reusable cow/chicken preparation, determinism and fail-closed sources");
        });
    }

    private static Matrix4f defaultTransform() {
        return new Matrix4f().scaling(-1, -1, 1).translate(0, -1.501f, 0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
