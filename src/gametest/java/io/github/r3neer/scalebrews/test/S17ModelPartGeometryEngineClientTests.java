package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final Identifier THROWING = Identifier.parse("scalebrews_test:s17_throwing");
    private static final Identifier INVALID_TRANSFORM = Identifier.parse("scalebrews_test:s17_invalid_transform");
    private static final Identifier HIDDEN = Identifier.parse("scalebrews_test:s17_hidden_head");
    private static final Identifier SKIP_DRAW = Identifier.parse("scalebrews_test:s17_skip_draw_head");
    private static final Identifier VARIANT_A = Identifier.parse("scalebrews_test:s17_variant_a");
    private static final Identifier VARIANT_B = Identifier.parse("scalebrews_test:s17_variant_b");

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
            check(broken, "A null-producing source must fail preparation explicitly");
            check(engine.prepare(new GeometryEngine.Request(COW)).orElseThrow().equals(cow),
                "Failure of one ModelPart source must not contaminate another source or leave stale prepared geometry");

            ModelPartGeometryEngine.registerSource(THROWING,
                new ModelPartGeometryEngine.Source("26.2", () -> { throw new IllegalStateException("directed source failure"); },
                    S17ModelPartGeometryEngineClientTests::defaultTransform));
            boolean throwing = false;
            try {
                engine.prepare(new GeometryEngine.Request(THROWING));
            } catch (IllegalArgumentException expected) {
                throwing = true;
            }
            check(throwing, "A source exception must cross the preparation boundary as an explicit failure");

            ModelPartGeometryEngine.registerSource(INVALID_TRANSFORM,
                new ModelPartGeometryEngine.Source("26.2", () -> CowModel.createBodyLayer().bakeRoot(), () -> new Matrix4f().scaling(0f)));
            boolean invalidTransform = false;
            try {
                engine.prepare(new GeometryEngine.Request(INVALID_TRANSFORM));
            } catch (IllegalArgumentException expected) {
                invalidTransform = true;
            }
            check(invalidTransform, "A degenerate source model transform must fail before publication");
            check(engine.prepare(new GeometryEngine.Request(COW)).orElseThrow().equals(cow),
                "Invalid output from another source must not replace or poison valid geometry");

            ModelPartGeometryEngine.registerSource(VARIANT_A,
                new ModelPartGeometryEngine.Source("26.2-a", () -> CowModel.createBodyLayer().bakeRoot(), S17ModelPartGeometryEngineClientTests::defaultTransform));
            ModelPartGeometryEngine.registerSource(VARIANT_B,
                new ModelPartGeometryEngine.Source("26.2-b", () -> CowModel.createBodyLayer().bakeRoot(),
                    () -> new Matrix4f(defaultTransform()).translate(.25f, 0, 0)));
            var variantA = engine.prepare(new GeometryEngine.Request(VARIANT_A)).orElseThrow();
            var variantB = engine.prepare(new GeometryEngine.Request(VARIANT_B)).orElseThrow();
            check(!variantA.equals(variantB) && variantA.version().equals("26.2-a") && variantB.version().equals("26.2-b")
                    && !variantA.modelTransform().equals(variantB.modelTransform()),
                "Source identity/version/model transform must contribute to the prepared artifact instead of reusing stale geometry");
            check(engine.prepare(new GeometryEngine.Request(VARIANT_A)).orElseThrow().equals(variantA),
                "Preparing another source must not alter a previously reproducible source");

            ModelPartGeometryEngine.registerSource(HIDDEN,
                new ModelPartGeometryEngine.Source("26.2", S17ModelPartGeometryEngineClientTests::hiddenHeadCow,
                    S17ModelPartGeometryEngineClientTests::defaultTransform));
            var hidden = engine.prepare(new GeometryEngine.Request(HIDDEN)).orElseThrow();
            var hiddenHead = hidden.pieces().stream().filter(piece -> piece.part().equals("root/head")).toList();
            check(!hiddenHead.isEmpty() && hiddenHead.stream().allMatch(piece -> "hidden_or_cosmetic".equals(piece.excluded())),
                "visible=false ModelPart cubes must remain structurally non-material");
            var forcedInclude = new AnatomyFilter(0, 0, 0, Set.of("root/head"), Set.of());
            var hiddenReport = hidden.filterReport(forcedInclude);
            check(hiddenHead.stream().allMatch(piece -> "hidden_or_cosmetic".equals(hiddenReport.get(piece.id()))),
                "A binding include must not revive structurally hidden/cosmetic ModelPart geometry");

            ModelPartGeometryEngine.registerSource(SKIP_DRAW,
                new ModelPartGeometryEngine.Source("26.2", S17ModelPartGeometryEngineClientTests::skipDrawHeadCow,
                    S17ModelPartGeometryEngineClientTests::defaultTransform));
            var skip = engine.prepare(new GeometryEngine.Request(SKIP_DRAW)).orElseThrow();
            var skipHead = skip.pieces().stream().filter(piece -> piece.part().equals("root/head")).toList();
            check(!skipHead.isEmpty() && skipHead.stream().allMatch(piece -> "skip_draw".equals(piece.excluded())),
                "skipDraw ModelPart cubes must remain structurally non-material while preserving the hierarchy");

            var sourceIds = new ArrayList<>(ModelPartGeometryEngine.sources().keySet());
            var sortedIds = new ArrayList<>(sourceIds);
            sortedIds.sort(Comparator.comparing(Identifier::toString));
            check(sourceIds.equals(sortedIds), "ModelPart source tooling snapshot must preserve deterministic identifier order");
            boolean immutableSnapshot = false;
            try {
                ModelPartGeometryEngine.sources().clear();
            } catch (UnsupportedOperationException expected) {
                immutableSnapshot = true;
            }
            check(immutableSnapshot && engine.prepare(new GeometryEngine.Request(COW)).orElseThrow().equals(cow),
                "Tooling source snapshots must be immutable views and must not mutate the live registry");

            System.out.println("S17_MODEL_PART_ENGINE PASS reusable preparation, structural exclusions, isolation and reproducibility holdouts");
        });
    }

    private static net.minecraft.client.model.geom.ModelPart hiddenHeadCow() {
        var root = CowModel.createBodyLayer().bakeRoot();
        root.getChild("head").visible = false;
        return root;
    }

    private static net.minecraft.client.model.geom.ModelPart skipDrawHeadCow() {
        var root = CowModel.createBodyLayer().bakeRoot();
        root.getChild("head").skipDraw = true;
        return root;
    }

    private static Matrix4f defaultTransform() {
        return new Matrix4f().scaling(-1, -1, 1).translate(0, -1.501f, 0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
