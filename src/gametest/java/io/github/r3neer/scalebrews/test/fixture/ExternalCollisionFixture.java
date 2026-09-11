package io.github.r3neer.scalebrews.test.fixture;

import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.BodyAdapter;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Test-mod stand-in: registration uses only the public G1 extension surface. */
public final class ExternalCollisionFixture {
    private ExternalCollisionFixture() {}

    public static final Identifier GEOMETRY = Identifier.parse("scalebrews_test:s04_geometry");
    public static final Identifier POSE = Identifier.parse("scalebrews_test:s04_pose");
    public static final Identifier ROOT = Identifier.parse("scalebrews_test:s04_root");
    public static final Identifier BODY = Identifier.parse("scalebrews_test:s04_virtual_body");

    /** GameTests share one JVM registry; make the fixture bootstrap repeat-safe without weakening registry duplicate rejection. */
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
