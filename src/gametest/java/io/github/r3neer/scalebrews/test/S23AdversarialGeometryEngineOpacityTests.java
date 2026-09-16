package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Adversarial S23 holdout: once a GeometryEngine has produced common geometry,
 * the physical query path must be unable to distinguish the originating family.
 */
public final class S23AdversarialGeometryEngineOpacityTests {
    @GameTest
    public void commonSolverIsOpaqueToGeometryEngineFamilyAndMetadata(GameTestHelper h) {
        var displayEngineId = Identifier.parse("scalebrews_test:s23_adversarial_display_engine");
        var controlEngineId = Identifier.parse("scalebrews_test:s23_adversarial_control_engine");
        var displayModelId = Identifier.parse("scalebrews_test:s23_adversarial_display_model");
        var controlModelId = Identifier.parse("scalebrews_test:s23_adversarial_control_model");

        CollisionEngines.registerGeometry(displayEngineId,
            exactEngine(displayModelId, "display", "synthetic_display_composite", "display-v1"));
        CollisionEngines.registerGeometry(controlEngineId,
            exactEngine(controlModelId, "control", "synthetic_reference_family", "control-v9"));

        var displayModel = CollisionEngines.geometry(displayEngineId).orElseThrow()
            .prepare(new GeometryEngine.Request(displayModelId, Map.of("family", "display")))
            .orElseThrow();
        var controlModel = CollisionEngines.geometry(controlEngineId).orElseThrow()
            .prepare(new GeometryEngine.Request(controlModelId, Map.of("family", "control")))
            .orElseThrow();

        h.assertTrue(!displayModel.source().equals(controlModel.source())
                && !displayModel.version().equals(controlModel.version()),
            "Holdout setup must vary engine identity and ModelGeometry metadata, not merely replay one object");

        var displayPanel = onlyPiece(displayModel);
        var controlPanel = onlyPiece(controlModel);
        assertSameBounds(h, displayPanel.bounds(), controlPanel.bounds());

        var rayStart = new Vec3(-1.0, 0.5, 0.5);
        var rayEnd = new Vec3(2.0, 0.5, 0.5);
        var displayHit = displayPanel.raycast(rayStart, rayEnd);
        var controlHit = controlPanel.raycast(rayStart, rayEnd);
        h.assertTrue(displayHit != null && controlHit != null
                && near(displayHit.fraction(), controlHit.fraction())
                && sameVector(displayHit.normal(), controlHit.normal()),
            "FR-018: equal common geometry must raycast identically regardless of the source GeometryEngine family");

        var body = new AABB(-1.0, 0.25, 0.25, -0.5, 0.75, 0.75);
        var displacement = new Vec3(2.0, 0.0, 0.0);
        var displaySweep = ConservativeSweep.query(body, displacement,
            new ConservativeSweep.Motion(t -> displayPanel, 0.0), 32);
        var controlSweep = ConservativeSweep.query(body, displacement,
            new ConservativeSweep.Motion(t -> controlPanel, 0.0), 32);

        h.assertTrue(displaySweep.status() == controlSweep.status()
                && near(displaySweep.safeFraction(), controlSweep.safeFraction())
                && sameVector(displaySweep.normal(), controlSweep.normal())
                && displaySweep.evaluations() == controlSweep.evaluations(),
            "FR-018: the shared physical solver must not branch on engine id, model id, source or version metadata");
        h.succeed();
    }

    private static GeometryEngine exactEngine(Identifier expectedModel, String expectedFamily,
                                               String source, String version) {
        return request -> {
            if (!request.model().equals(expectedModel)
                    || !expectedFamily.equals(request.parameters().get("family"))) return Optional.empty();
            return Optional.of(unitCube(source, version));
        };
    }

    private static ModelGeometry unitCube(String source, String version) {
        return new ModelGeometry(
            2,
            source,
            version,
            List.of(new ModelGeometry.Part("root", null, ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece(
                "root/panel",
                "root",
                List.of(0.0, 0.0, 0.0),
                List.of(1.0, 1.0, 1.0),
                null)),
            ModelGeometry.values(new Matrix4f()));
    }

    private static ConvexBox onlyPiece(ModelGeometry model) {
        var pieces = model.evaluate(new Matrix4f(), Map.of(), AnatomyFilter.DEFAULT);
        if (pieces.size() != 1 || !pieces.containsKey("root/panel"))
            throw new IllegalStateException("Adversarial S23 fixture did not materialize exactly one common piece");
        return pieces.get("root/panel");
    }

    private static void assertSameBounds(GameTestHelper h, AABB left, AABB right) {
        h.assertTrue(near(left.minX, right.minX) && near(left.minY, right.minY) && near(left.minZ, right.minZ)
                && near(left.maxX, right.maxX) && near(left.maxY, right.maxY) && near(left.maxZ, right.maxZ),
            "Equal common geometry must materialize equal physical bounds regardless of family metadata");
    }

    private static boolean sameVector(Vec3 left, Vec3 right) {
        return near(left.x, right.x) && near(left.y, right.y) && near(left.z, right.z);
    }

    private static boolean near(double left, double right) {
        return Math.abs(left - right) <= 1.0e-9;
    }
}
