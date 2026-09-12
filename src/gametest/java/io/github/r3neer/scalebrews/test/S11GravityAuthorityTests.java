package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import java.util.Arrays;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Adversarial ownership holdouts for the G2 shared gravity authority reconciliation. */
public final class S11GravityAuthorityTests {
    private static final String SHARED_AUTHORITY = "io.github.r3neer.scalebrews.integration.gravity.GravityFrames";
    private static final String COLLISION_AUTHORITY = "io.github.r3neer.scalebrews.collision.internal.GravityFrames";

    @GameTest
    public void sharedScaleGravityAuthorityMustExist(GameTestHelper h) {
        h.assertTrue(classExists(SHARED_AUTHORITY),
            "G2 requires Scale Brews to expose one shared integration.gravity.GravityFrames authority");
        h.succeed();
    }

    @GameTest
    public void anatomyMovementMustNotOwnPerEntityGravityState(GameTestHelper h) {
        boolean ownsGravity = Arrays.stream(AnatomyMovement.class.getDeclaredFields())
            .anyMatch(field -> field.getName().equals("GRAVITY"));
        h.assertTrue(!ownsGravity,
            "AnatomyMovement must consume shared gravity rather than shadow it with a per-entity GRAVITY store");
        h.succeed();
    }

    @GameTest
    public void collisionInternalGravityAuthorityMustBeGone(GameTestHelper h) {
        h.assertTrue(!classExists(COLLISION_AUTHORITY),
            "collision.internal.GravityFrames is a competing gravity owner and must disappear after S11");
        h.succeed();
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, S11GravityAuthorityTests.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }
}
