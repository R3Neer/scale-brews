package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;

/**
 * Retroactive adversarial performance holdout for steady-state orchestration locality.
 *
 * <p>The four observations run sequentially inside one GameTest so global runtime state and
 * world population cannot be contaminated by concurrent fixtures in the same GameTest batch.</p>
 */
public final class RetroPerformanceLocalityTests {
    private static final int IRRELEVANT_ENTITIES = 64;

    @GameTest
    public void steadyTickWorkMustRemainLocalToCollisionParticipants(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        AnatomyRuntime.stop(server);

        CollisionPerformanceProbe.Snapshot inactiveEmpty = measure(level);

        final CollisionPerformanceProbe.Snapshot activeEmpty;
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
        try {
            activeEmpty = measure(level);
        } finally {
            AnatomyRuntime.stop(server);
        }

        spawnIrrelevantLivingEntities(h);
        CollisionPerformanceProbe.Snapshot inactivePopulated = measure(level);

        final CollisionPerformanceProbe.Snapshot activePopulated;
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
        try {
            activePopulated = measure(level);
        } finally {
            AnatomyRuntime.stop(server);
        }

        boolean local = inactiveEmpty.globalEnumerations() == 0
            && activeEmpty.globalEnumerations() == 0
            && inactivePopulated.globalEnumerations() == 0
            && activePopulated.globalEnumerations() == 0
            && inactivePopulated.entitiesVisited() == 0
            && activePopulated.entitiesVisited() == 0;

        long inactiveFullScanFloor = (long) inactivePopulated.globalEnumerations() * IRRELEVANT_ENTITIES;
        long activeFullScanFloor = (long) activePopulated.globalEnumerations() * IRRELEVANT_ENTITIES;
        h.assertTrue(local,
            "Steady collision orchestration is not local to relevant participants. "
                + "empty[inactive=" + describe(inactiveEmpty) + ", active=" + describe(activeEmpty) + "]; "
                + IRRELEVANT_ENTITIES + " irrelevant living entities[inactive=" + describe(inactivePopulated)
                + " fullScanFloor=" + inactiveFullScanFloor + ", active=" + describe(activePopulated)
                + " fullScanFloor=" + activeFullScanFloor + "]");
        h.succeed();
    }

    private static CollisionPerformanceProbe.Snapshot measure(ServerLevel level) {
        CollisionPerformanceProbe.begin();
        try {
            Platforms.tick(level);
            return CollisionPerformanceProbe.end();
        } catch (RuntimeException | Error failure) {
            CollisionPerformanceProbe.end();
            throw failure;
        }
    }

    private static String describe(CollisionPerformanceProbe.Snapshot snapshot) {
        return "calls=" + snapshot.globalEnumerations() + ",visited=" + snapshot.entitiesVisited();
    }

    private static void spawnIrrelevantLivingEntities(GameTestHelper h) {
        for (int i = 0; i < IRRELEVANT_ENTITIES; i++) {
            var pig = h.spawn(EntityTypes.PIG, 1 + (i % 4), 2, 1 + ((i / 4) % 4));
            pig.setNoAi(true);
            pig.setNoGravity(true);
        }
    }
}
