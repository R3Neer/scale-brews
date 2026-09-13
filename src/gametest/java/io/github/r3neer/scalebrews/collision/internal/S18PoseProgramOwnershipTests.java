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
    public void acceptedCatalogSnapshotOwnsPosePrograms(GameTestHelper h) {
        var component = Arrays.stream(WorldAnatomyCatalog.Snapshot.class.getRecordComponents())
            .filter(value -> value.getName().toLowerCase(java.util.Locale.ROOT).contains("program"))
            .findFirst().orElse(null);
        h.assertTrue(component != null && Map.class.isAssignableFrom(component.getType()),
            "Accepted WorldAnatomyCatalog snapshot must own the revision-local pose program table");
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
