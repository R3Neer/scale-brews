package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import java.util.Arrays;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

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

    @GameTest
    public void vanillaFallbackIsSharedByApiAndPipeline(GameTestHelper h) {
        var body = h.spawn(EntityTypes.ARMOR_STAND, 1, 2, 1);
        try {
            AnatomyMovement.gravity(body, GravityFrame.VANILLA);
            h.assertTrue(GravityFrames.frame(body).equals(GravityFrame.VANILLA),
                "Without a per-entity test override, the shared authority must resolve vanilla DOWN in this fixture");
            h.assertTrue(AnatomyMovement.gravity(body).equals(GravityFrames.frame(body)),
                "The collision pipeline must read the shared gravity authority");
            h.assertTrue(AnatomyApi.gravity(body).equals(GravityFrames.frame(body)),
                "The public API and collision pipeline must expose the same effective gravity frame");
        } finally {
            AnatomyMovement.gravity(body, GravityFrame.VANILLA);
            body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void allCardinalFramesRoundTripThroughOneAuthority(GameTestHelper h) {
        var body = h.spawn(EntityTypes.ARMOR_STAND, 2, 2, 2);
        var local = new Vec3(.25, -.5, .75);
        try {
            for (Direction direction : Direction.values()) {
                var frame = new GravityFrame(direction);
                AnatomyMovement.gravity(body, frame);
                var shared = GravityFrames.frame(body);
                h.assertTrue(shared.down() == direction,
                    "Shared authority lost cardinal gravity direction " + direction);
                h.assertTrue(AnatomyMovement.gravity(body).equals(shared) && AnatomyApi.gravity(body).equals(shared),
                    "API and pipeline diverged from shared gravity for " + direction);
                h.assertTrue(shared.toLocal(shared.toWorld(local)).distanceToSqr(local) < 1e-12,
                    "Gravity frame local/world transform did not round-trip for " + direction);
                h.assertTrue(shared.supports(shared.up()),
                    "Gravity frame must recognize its own up normal as supporting for " + direction);
            }
        } finally {
            AnatomyMovement.gravity(body, GravityFrame.VANILLA);
            body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void publicAdapterInstallationHasOneOwner(GameTestHelper h) {
        String installed = GravityFrames.owner();
        String owner = installed == null ? "scalebrews_test:s11" : installed;
        AnatomyApi.installGravityAdapter(owner, ignored -> Direction.DOWN);
        h.assertTrue(owner.equals(GravityFrames.owner()),
            "Public collision adapter installation must terminate at the shared Scale gravity owner");
        AnatomyApi.installGravityAdapter(owner, ignored -> Direction.UP);
        h.assertTrue(owner.equals(GravityFrames.owner()),
            "Reinstalling the same owner must be idempotent rather than replacing authority");
        boolean rejected = false;
        try {
            AnatomyApi.installGravityAdapter(owner + ":competitor", ignored -> Direction.UP);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "A competing gravity owner must fail explicitly");
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
