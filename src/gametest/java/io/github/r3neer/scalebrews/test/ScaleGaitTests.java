package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.scale.ScaleGait;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

public class ScaleGaitTests {
    @GameTest
    public void walkAnimationScalesTimeWithoutChangingAmplitude(GameTestHelper h) throws ReflectiveOperationException {
        var normal = h.makeMockPlayer(GameType.SURVIVAL);
        var giant = h.makeMockPlayer(GameType.SURVIVAL);
        var tiny = h.makeMockPlayer(GameType.SURVIVAL);
        giant.getAttribute(Attributes.SCALE).setBaseValue(2.0);
        tiny.getAttribute(Attributes.SCALE).setBaseValue(0.5);

        Method update = LivingEntity.class.getDeclaredMethod("updateWalkAnimation", float.class);
        update.setAccessible(true);
        update.invoke(normal, 0.1F);
        update.invoke(giant, 0.1F);
        update.invoke(tiny, 0.1F);

        near(h, giant.walkAnimation.speed(), normal.walkAnimation.speed(), "Growth keeps walk amplitude");
        near(h, tiny.walkAnimation.speed(), normal.walkAnimation.speed(), "Shrinking keeps walk amplitude");
        near(h, giant.walkAnimation.position(), normal.walkAnimation.position() * 0.5F, "2x body halves gait clock");
        near(h, tiny.walkAnimation.position(), normal.walkAnimation.position() * 2.0F, "0.5x body doubles gait clock");
        h.succeed();
    }

    @GameTest
    public void stepPitchTracksPhysicalScaleAndStaysBounded(GameTestHelper h) {
        near(h, ScaleGait.stepPitchMultiplier(1.0), 1.0, "Normal pitch is unchanged");
        near(h, ScaleGait.stepPitchMultiplier(3.88), 1.0 / Math.sqrt(3.88), "Growth III lowers step pitch");
        near(h, ScaleGait.stepPitchMultiplier(0.274), 1.0 / Math.sqrt(0.274), "Shrinking III raises step pitch");
        near(h, ScaleGait.stepPitchMultiplier(100.0), 0.5, "Huge external scales keep an audible floor");
        near(h, ScaleGait.stepPitchMultiplier(0.001), 2.0, "Tiny external scales keep an audible ceiling");
        near(h, ScaleGait.walkTimeMultiplier(100.0), 0.25, "Huge external scales keep gait moving");
        near(h, ScaleGait.walkTimeMultiplier(0.001), 4.0, "Tiny external scales keep gait readable");
        h.succeed();
    }

    private static void near(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 1e-4, message + ": " + actual + " != " + expected);
    }
}
