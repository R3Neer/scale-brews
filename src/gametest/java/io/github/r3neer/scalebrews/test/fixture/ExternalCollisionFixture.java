package io.github.r3neer.scalebrews.test.fixture;

import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.BodyAdapter;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/** Test-mod fixture: behavior is registered during mod initialization through only the public G1 API. */
public final class ExternalCollisionFixture implements ModInitializer {
    public static final Identifier GEOMETRY = Identifier.parse("scalebrews_test:s04_geometry");
    public static final Identifier POSE = Identifier.parse("scalebrews_test:s04_pose");
    public static final Identifier ROOT = Identifier.parse("scalebrews_test:s04_root");
    public static final Identifier BODY = Identifier.parse("scalebrews_test:s04_virtual_body");

    @Override
    public void onInitialize() { register(); }

    /** Repeat-safe helper for special proof lanes that may bootstrap the test fixture explicitly. */
    public static void register() {
        if (CollisionEngines.geometry(GEOMETRY).isEmpty())
            CollisionEngines.registerGeometry(GEOMETRY, request -> Optional.empty());
        if (CollisionEngines.pose(POSE).isEmpty())
            CollisionEngines.registerPose(POSE, (model, inputs, parameters) -> Optional.of(Map.of()));
        if (CollisionEngines.rootTransform(ROOT).isEmpty())
            CollisionEngines.registerRootTransform(ROOT, entity -> Optional.empty());
        if (CollisionAdapters.body(BODY).isEmpty())
            CollisionAdapters.registerBody(BODY, new BodyAdapter() {
                @Override public String category() { return "fixture_bodies"; }
                @Override public boolean permits(net.minecraft.world.entity.Entity body) { return true; }
            });
    }
}
