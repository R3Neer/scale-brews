package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** G3.7 / FR-018: future display-composite families stay behind the generic geometry SPI. */
public final class S23DisplayRigSpiTests {
    @GameTest
    public void syntheticDisplayCompositeUsesExistingGeometryAndSolver(GameTestHelper h) {
        var engineId = Identifier.parse("scalebrews_test:s23_display_composite");
        GeometryEngine displayComposite = request -> Optional.of(new ModelGeometry(
            2,
            "synthetic_display_composite",
            "proof-v1",
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece(
                "root/display_panel",
                "root",
                List.of(0.0, 0.0, 0.0),
                List.of(1.0, 1.0, 1.0),
                null)),
            ModelGeometry.values(new Matrix4f())));

        CollisionEngines.registerGeometry(engineId, displayComposite);
        var prepared = CollisionEngines.geometry(engineId).orElseThrow()
            .prepare(new GeometryEngine.Request(Identifier.parse("scalebrews_test:display_fixture"), Map.of("profile", "single_panel")))
            .orElseThrow();

        var pieces = prepared.evaluate(new Matrix4f(), Map.of(), AnatomyFilter.DEFAULT);
        h.assertTrue(pieces.size() == 1 && pieces.containsKey("root/display_panel"),
            "A future display family must publish ordinary ModelGeometry pieces through GeometryEngine");

        var panel = pieces.get("root/display_panel");
        var hit = panel.raycast(new Vec3(-1.0, 0.5, 0.5), new Vec3(2.0, 0.5, 0.5));
        h.assertTrue(hit != null && Math.abs(hit.fraction() - (1.0 / 3.0)) < 1.0e-6,
            "The prepared display piece must use the existing convex raycast without a display-specific solver");
        h.assertTrue(panel.overlaps(new AABB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75)),
            "The prepared display piece must use the existing convex collision test");
        h.assertTrue(!panel.overlaps(new AABB(2.0, 2.0, 2.0, 3.0, 3.0, 3.0)),
            "The generic solver must preserve empty space around the display piece");
        h.succeed();
    }
}
