package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.integration.gravity.GravityFrame;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

public class GravityFrameTests {
    private static final double EPS = 1.0E-9;

    @GameTest public void everyCardinalFrameRoundTrips(GameTestHelper h) {
        Vec3 value = new Vec3(1.25, -2.5, 3.75);
        for (Direction direction : Direction.values()) {
            GravityFrame frame = new GravityFrame(direction);
            near(h, frame.toLocal(frame.toWorld(value)), value, "local/world round trip " + direction);
            near(h, frame.toWorld(frame.toLocal(value)), value, "world/local round trip " + direction);
            Vec3 worldUp = frame.toWorld(0, 1, 0);
            near(h, worldUp, new Vec3(-direction.getStepX(), -direction.getStepY(), -direction.getStepZ()),
                    "local up opposes gravity " + direction);
        }
        h.succeed();
    }

    @GameTest public void localVerticalOperationsPreserveFrameSemantics(GameTestHelper h) {
        Vec3 local = new Vec3(4, -3, 2);
        for (Direction direction : Direction.values()) {
            GravityFrame frame = new GravityFrame(direction);
            Vec3 world = frame.toWorld(local);
            near(h, frame.toLocal(frame.keepLocalVertical(world)), new Vec3(0, -3, 0),
                    "keep vertical " + direction);
            near(h, frame.toLocal(frame.multiplyLocalVertical(world, .6)), new Vec3(4, -1.8, 2),
                    "multiply vertical " + direction);
        }
        h.succeed();
    }

    @GameTest public void providerOwnershipIsSingleAndDefaultIsDown(GameTestHelper h) {
        String owner = GravityFrames.owner();
        var pig = h.spawn(EntityTypes.PIG, 1, 2, 1);
        if (owner == null) {
            h.assertTrue(GravityFrames.direction(pig) == Direction.DOWN, "No provider defaults to DOWN");
            GravityFrames.install("scalebrews-test", entity -> Direction.DOWN);
            h.assertTrue("scalebrews-test".equals(GravityFrames.owner()), "First provider owns gravity service");
            GravityFrames.install("scalebrews-test", entity -> Direction.UP);
            h.assertTrue(GravityFrames.direction(pig) == Direction.DOWN,
                    "Same-owner reinstall is idempotent and does not replace resolver");
        } else {
            GravityFrames.install(owner, entity -> Direction.UP);
            h.assertTrue(owner.equals(GravityFrames.owner()), "Existing owner remains installed");
        }
        boolean rejected = false;
        try {
            GravityFrames.install("conflicting-test-owner", entity -> Direction.UP);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "Different provider owner is rejected explicitly");
        h.succeed();
    }

    private static void near(GameTestHelper h, Vec3 actual, Vec3 expected, String message) {
        h.assertTrue(actual.distanceToSqr(expected) <= EPS * EPS,
                message + ": " + actual + " != " + expected);
    }
}
