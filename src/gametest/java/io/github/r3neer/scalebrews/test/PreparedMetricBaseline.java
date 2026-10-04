package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import net.minecraft.server.level.ServerLevel;

/** Zeroes a previous tick's retained metric sample for a new GameTest scenario. */
final class PreparedMetricBaseline {
    private PreparedMetricBaseline() {}

    static MaterialPhysicsRuntime.Metrics current(ServerLevel level) {
        var last=MaterialPhysicsRuntime.metrics(level);
        return last.tick()==level.getGameTime()?last
            :new MaterialPhysicsRuntime.Metrics(level.getGameTime(),0,0,0,0,0);
    }
}
