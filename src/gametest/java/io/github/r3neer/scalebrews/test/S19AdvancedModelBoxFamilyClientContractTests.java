package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * Pre-implementation S19 acceptance for the real Alex 2.1.9 model family.
 *
 * <p>The class deliberately imports no Alex/Citadel types. The exact external jars are supplied
 * only to the focal client run. Production must expose both real models through the same neutral
 * GeometryEngine id; later holdouts compare the prepared geometry against the original renderer
 * models without changing this contract.</p>
 */
public final class S19AdvancedModelBoxFamilyClientContractTests implements FabricClientGameTest {
    private static final Identifier ADVANCED_MODEL_BOX = Identifier.parse("scalebrews:advanced_model_box");
    private static final Identifier GRIZZLY = Identifier.parse("alexsmobs:grizzly_bear");
    private static final Identifier GAZELLE = Identifier.parse("alexsmobs:gazelle");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            check(FabricLoader.getInstance().isModLoaded("alexsmobs"),
                "S19 real-family lane must actually load the pinned Alex's Mobs Continued jar");
            check(FabricLoader.getInstance().isModLoaded("codxlib"),
                "S19 real-family lane must actually load the pinned CodxLib jar");
            check(FabricLoader.getInstance().isModLoaded("cloth-config"),
                "S19 real-family lane must actually load the pinned Cloth Config jar");

            var engine = CollisionEngines.geometry(ADVANCED_MODEL_BOX).orElseThrow(() ->
                new AssertionError("Client bootstrap must expose scalebrews:advanced_model_box when the exact family jars are present"));

            var grizzly = engine.prepare(new GeometryEngine.Request(GRIZZLY)).orElseThrow(() ->
                new AssertionError("The reusable AdvancedModelBox engine must prepare the real Grizzly 2.1.9 model"));
            var gazelle = engine.prepare(new GeometryEngine.Request(GAZELLE)).orElseThrow(() ->
                new AssertionError("The same AdvancedModelBox engine must prepare the real Gazelle 2.1.9 model"));

            check(grizzly.format() == 2 && gazelle.format() == 2,
                "External family preparation must publish explicit format-2 model transforms");
            check(GRIZZLY.toString().equals(grizzly.source()) && GAZELLE.toString().equals(gazelle.source()),
                "Prepared source identity must follow the requested external model id");
            check("2.1.9".equals(grizzly.version()) && "2.1.9".equals(gazelle.version()),
                "The S19 acceptance lane is version-locked to the demonstrated Alex 2.1.9 dialect");
            check(!grizzly.parts().isEmpty() && !grizzly.pieces().isEmpty(),
                "Real Grizzly preparation must contain material hierarchy and pieces");
            check(!gazelle.parts().isEmpty() && !gazelle.pieces().isEmpty(),
                "Real Gazelle preparation must contain material hierarchy and pieces despite its private model fields/render-only primitive");
            check(!grizzly.equals(gazelle),
                "Two structurally distinct AdvancedModelBox models must not collapse to one cached/stale artifact");

            check(engine.prepare(new GeometryEngine.Request(GRIZZLY, Map.of("species", "grizzly"))).isEmpty(),
                "Unknown parameters must not become a species-specific escape hatch in the family engine");
            check(engine.prepare(new GeometryEngine.Request(Identifier.parse("alexsmobs:not_a_real_model"))).isEmpty(),
                "Unknown external models must fail closed rather than falling back to a generic collider");

            System.out.println("S19_ADVANCED_MODEL_BOX_FAMILY PASS grizzly+gazelle through one pinned 2.1.9 engine contract");
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
