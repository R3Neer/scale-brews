package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;

/**
 * Retroactive adversarial performance holdouts for steady-state orchestration locality.
 *
 * <p>These tests deliberately measure structural work rather than CI wall-clock noise. A stable
 * tick with no relevant participants must not enumerate every entity in the level merely to
 * discover that there is no work.</p>
 */
public final class RetroPerformanceLocalityTests {
    private static final int IRRELEVANT_ENTITIES = 64;

    @GameTest
    public void inactiveSteadyTickMustNotEnumerateWholeLevel(GameTestHelper h) {
        var level = h.getLevel();
        AnatomyRuntime.stop(level.getServer());
        CollisionPerformanceProbe.begin();
        CollisionPerformanceProbe.Snapshot snapshot;
        try {
            Platforms.tick(level);
        } finally {
            snapshot = CollisionPerformanceProbe.end();
        }
        h.assertTrue(snapshot.globalEnumerations() == 0,
            "Inactive collision runtime performed full-level entity enumeration during one stable tick: calls="
                + snapshot.globalEnumerations() + " visited=" + snapshot.entitiesVisited());
        h.succeed();
    }

    @GameTest
    public void activeEmptyRuntimeSteadyTickMustNotEnumerateWholeLevel(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        AnatomyRuntime.stop(server);
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());
        CollisionPerformanceProbe.Snapshot snapshot;
        try {
            // startPrepared performs the explicit bootstrap before measurement. Only the following
            // stable tick is observed.
            CollisionPerformanceProbe.begin();
            try {
                Platforms.tick(level);
            } finally {
                snapshot = CollisionPerformanceProbe.end();
            }
            h.assertTrue(snapshot.globalEnumerations() == 0,
                "Active empty anatomy runtime performed full-level entity enumeration during one stable tick: calls="
                    + snapshot.globalEnumerations() + " visited=" + snapshot.entitiesVisited());
        } finally {
            CollisionPerformanceProbe.end();
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }

    @GameTest
    public void inactiveRuntimeWorkMustNotScaleWithIrrelevantWorldPopulation(GameTestHelper h) {
        var level = h.getLevel();
        AnatomyRuntime.stop(level.getServer());
        spawnIrrelevantLivingEntities(h);

        CollisionPerformanceProbe.begin();
        CollisionPerformanceProbe.Snapshot snapshot;
        try {
            Platforms.tick(level);
        } finally {
            snapshot = CollisionPerformanceProbe.end();
        }

        long minimumLinearVisits = (long) snapshot.globalEnumerations() * IRRELEVANT_ENTITIES;
        h.assertTrue(snapshot.entitiesVisited() < IRRELEVANT_ENTITIES,
            "Inactive runtime work scales with irrelevant world population: calls=" + snapshot.globalEnumerations()
                + " visited=" + snapshot.entitiesVisited() + " expected locality independent of " + IRRELEVANT_ENTITIES
                + " irrelevant living entities; observed full-scan lower bound=" + minimumLinearVisits);
        h.succeed();
    }

    @GameTest
    public void activeEmptyRuntimeWorkMustNotScaleWithIrrelevantWorldPopulation(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        AnatomyRuntime.stop(server);
        spawnIrrelevantLivingEntities(h);
        AnatomyRuntime.startPrepared(server, Map.of(), Map.of());

        CollisionPerformanceProbe.Snapshot snapshot;
        try {
            CollisionPerformanceProbe.begin();
            try {
                Platforms.tick(level);
            } finally {
                snapshot = CollisionPerformanceProbe.end();
            }
            long minimumLinearVisits = (long) snapshot.globalEnumerations() * IRRELEVANT_ENTITIES;
            h.assertTrue(snapshot.entitiesVisited() < IRRELEVANT_ENTITIES,
                "Active empty runtime work scales with irrelevant world population: calls=" + snapshot.globalEnumerations()
                    + " visited=" + snapshot.entitiesVisited() + " expected locality independent of " + IRRELEVANT_ENTITIES
                    + " irrelevant living entities; observed full-scan lower bound=" + minimumLinearVisits);
        } finally {
            CollisionPerformanceProbe.end();
            AnatomyRuntime.stop(server);
        }
        h.succeed();
    }

    private static void spawnIrrelevantLivingEntities(GameTestHelper h) {
        for (int i = 0; i < IRRELEVANT_ENTITIES; i++) {
            var pig = h.spawn(EntityTypes.PIG, 1 + (i % 4), 2, 1 + ((i / 4) % 4));
            pig.setNoAi(true);
            pig.setNoGravity(true);
        }
    }
}
