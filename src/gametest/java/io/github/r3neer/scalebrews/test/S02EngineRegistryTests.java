package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** G1/S02 pure registry and DTO acceptance tests. */
public final class S02EngineRegistryTests {
    @GameTest
    public void externalFixtureCanRegisterReusableEngines(GameTestHelper h) {
        var geometryId = Identifier.parse("scalebrews_test:s02_geometry");
        var poseId = Identifier.parse("scalebrews_test:s02_pose");
        var rootId = Identifier.parse("scalebrews_test:s02_root");
        GeometryEngine geometry = request -> Optional.empty();
        PoseEngine pose = (model, inputs, parameters) -> Optional.of(Map.of());
        RootTransformProvider root = entity -> Optional.empty();

        CollisionEngines.registerGeometry(geometryId, geometry);
        CollisionEngines.registerPose(poseId, pose);
        CollisionEngines.registerRootTransform(rootId, root);

        h.assertTrue(CollisionEngines.geometry(geometryId).orElseThrow() == geometry,
            "Registered geometry engine must be addressable by data id");
        h.assertTrue(CollisionEngines.pose(poseId).orElseThrow() == pose,
            "Registered pose engine must be addressable by data id");
        h.assertTrue(CollisionEngines.rootTransform(rootId).orElseThrow() == root,
            "Registered root provider must be addressable by data id");
        h.assertTrue(CollisionEngines.geometry(Identifier.parse("scalebrews_test:missing")).isEmpty(),
            "Unknown engine ids must remain unresolved rather than falling back");
        h.succeed();
    }

    @GameTest
    public void duplicateEngineIdsFailInsteadOfChangingBehavior(GameTestHelper h) {
        var id = Identifier.parse("scalebrews_test:s02_duplicate");
        CollisionEngines.registerGeometry(id, request -> Optional.empty());
        boolean rejected = false;
        try { CollisionEngines.registerGeometry(id, request -> Optional.empty()); }
        catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "Duplicate engine registration must fail deterministically");
        h.succeed();
    }

    @GameTest
    public void geometryRequestKeepsCanonicalParameterOrder(GameTestHelper h) {
        var source = new LinkedHashMap<String, String>();
        source.put("zeta", "2");
        source.put("alpha", "1");
        var request = new GeometryEngine.Request(Identifier.parse("minecraft:cow"), source);
        h.assertTrue(List.copyOf(request.parameters().keySet()).equals(List.of("alpha", "zeta")),
            "Geometry engine parameters must retain canonical key order independent of caller insertion order");
        boolean immutable = false;
        try { request.parameters().put("later", "3"); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        h.assertTrue(immutable, "Canonical geometry request parameters are immutable");
        h.succeed();
    }

    @GameTest
    public void spiDtosRejectUnboundedOrInvalidInputs(GameTestHelper h) {
        boolean badGeometry = false;
        try { new GeometryEngine.Request(Identifier.parse("minecraft:cow"), Map.of("BAD KEY", "value")); }
        catch (IllegalArgumentException expected) { badGeometry = true; }
        h.assertTrue(badGeometry, "Geometry parameters use bounded canonical keys");

        boolean badPose = false;
        try { new PoseEngine.Inputs(0, -1, 0, 0, 0, true); }
        catch (IllegalArgumentException expected) { badPose = true; }
        h.assertTrue(badPose, "Pose DTO rejects invalid motion input");

        boolean badRoot = false;
        try { new RootTransformProvider.RootTransform(net.minecraft.world.phys.Vec3.ZERO, 0, 0, 0, 0, 1); }
        catch (IllegalArgumentException expected) { badRoot = true; }
        h.assertTrue(badRoot, "Root DTO rejects a degenerate rotation");
        h.succeed();
    }
}
