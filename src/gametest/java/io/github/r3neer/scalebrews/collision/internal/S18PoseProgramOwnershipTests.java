package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Pose programs are revision-local catalog data; CollisionEngines owns behavior only. */
public final class S18PoseProgramOwnershipTests {
    @GameTest
    public void acceptedCatalogSnapshotOwnsThirdRevisionLocalDataMap(GameTestHelper h) {
        long mapComponents = Arrays.stream(WorldAnatomyCatalog.Snapshot.class.getRecordComponents())
            .filter(value -> Map.class.isAssignableFrom(value.getType()))
            .count();
        h.assertTrue(mapComponents >= 3,
            "Accepted catalog snapshot must own models, executable bindings and a third revision-local pose-program map; component naming is not prescribed");
        h.succeed();
    }

    @GameTest
    public void collisionEngineRegistryDoesNotBecomePoseProgramStorage(GameTestHelper h) {
        boolean publicProgramRegistryApi = Arrays.stream(CollisionEngines.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
            .map(method -> method.getName().toLowerCase(java.util.Locale.ROOT))
            .anyMatch(name -> name.contains("program"));
        h.assertTrue(!publicProgramRegistryApi,
            "CollisionEngines registers reusable behavior; pose programs must remain revision-local catalog data");
        h.succeed();
    }
}
