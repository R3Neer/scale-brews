package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Retroactive adversarial performance holdouts for steady-state orchestration locality.
 *
 * <p>These tests deliberately measure structural work rather than CI wall-clock noise. A stable
 * tick with no relevant participants must not enumerate every entity in the level merely to
 * discover that there is no work.</p>
 */
public final class RetroPerformanceLocalityTests {
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
}
