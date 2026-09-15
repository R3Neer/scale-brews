package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import java.util.ArrayList;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Independent S21 holdout for the root-authority SPI before runtime integration. */
public final class S21AdversarialRootTransformContractTests {
    private static final float EPS = 2e-6f;

    @GameTest
    public void rootDtoNormalizesAndDoesNotAliasCallerQuaternion(GameTestHelper h) {
        var source = new Quaternionf(2f, -3f, 4f, 5f);
        var expected = new Quaternionf(source).normalize();
        var root = new RootTransformProvider.RootTransform(new Vec3(1.25, -2.5, 3.75), source, 1.5f);

        source.set(0f, 0f, 0f, 1f);
        assertQuaternion(h, root, expected,
            "Accepted root DTO must keep the normalized value sampled at construction time");

        var returned = root.quaternion();
        returned.set(1f, 0f, 0f, 0f);
        assertQuaternion(h, root, expected,
            "Mutating quaternion() result must not mutate the authoritative DTO");
        h.succeed();
    }

    @GameTest
    public void quaternionSignProducesEquivalentOrientation(GameTestHelper h) {
        var q = new Quaternionf(.23f, -.41f, .17f, .86f).normalize();
        var a = new RootTransformProvider.RootTransform(Vec3.ZERO, q, 1f);
        var b = new RootTransformProvider.RootTransform(Vec3.ZERO,
            new Quaternionf(-q.x, -q.y, -q.z, -q.w), 1f);
        float dot = a.qx() * b.qx() + a.qy() * b.qy() + a.qz() * b.qz() + a.qw() * b.qw();
        h.assertTrue(Math.abs(Math.abs(dot) - 1f) <= EPS,
            "q and -q must survive DTO normalization as the same world orientation");
        h.succeed();
    }

    @GameTest
    public void invalidRootDtoStatesFailClosed(GameTestHelper h) {
        expectRejected(h, () -> new RootTransformProvider.RootTransform(Vec3.ZERO, 0f, 0f, 0f, 0f, 1f),
            "zero quaternion");
        expectRejected(h, () -> new RootTransformProvider.RootTransform(Vec3.ZERO, Float.NaN, 0f, 0f, 1f, 1f),
            "NaN quaternion");
        expectRejected(h, () -> new RootTransformProvider.RootTransform(Vec3.ZERO, 0f, 0f, 0f, 1f, Float.POSITIVE_INFINITY),
            "infinite scale");
        expectRejected(h, () -> new RootTransformProvider.RootTransform(Vec3.ZERO, 0f, 0f, 0f, 1f, 0f),
            "zero scale");
        expectRejected(h, () -> new RootTransformProvider.RootTransform(new Vec3(Double.NaN, 0, 0), 0f, 0f, 0f, 1f, 1f),
            "non-finite origin");
        h.succeed();
    }

    @GameTest
    public void rootRegistryRejectsDuplicateOwnershipAndPublishesImmutableSortedSnapshot(GameTestHelper h) {
        Identifier firstId = Identifier.parse("scalebrews_test:s21_adversarial_root_a");
        Identifier secondId = Identifier.parse("scalebrews_test:s21_adversarial_root_z");
        RootTransformProvider first = ignored -> Optional.empty();
        RootTransformProvider second = ignored -> Optional.empty();

        CollisionEngines.registerRootTransform(firstId, first);
        CollisionEngines.registerRootTransform(secondId, second);
        expectRejected(h, () -> CollisionEngines.registerRootTransform(firstId, second),
            "duplicate root-provider ownership");

        var snapshot = CollisionEngines.rootTransformSnapshot();
        h.assertTrue(snapshot.get(firstId) == first && snapshot.get(secondId) == second,
            "Root registry snapshot must retain exact provider ownership");
        var ids = new ArrayList<>(snapshot.keySet());
        var sorted = new ArrayList<>(ids);
        sorted.sort(java.util.Comparator.comparing(Identifier::toString));
        h.assertTrue(ids.equals(sorted), "Root registry snapshot must be deterministically sorted");
        try {
            snapshot.put(Identifier.parse("scalebrews_test:illegal_mutation"), first);
            h.assertTrue(false, "Root registry snapshot must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        h.succeed();
    }

    private static void assertQuaternion(GameTestHelper h, RootTransformProvider.RootTransform actual,
                                         Quaternionf expected, String message) {
        h.assertTrue(Math.abs(actual.qx() - expected.x) <= EPS, message + " [x]");
        h.assertTrue(Math.abs(actual.qy() - expected.y) <= EPS, message + " [y]");
        h.assertTrue(Math.abs(actual.qz() - expected.z) <= EPS, message + " [z]");
        h.assertTrue(Math.abs(actual.qw() - expected.w) <= EPS, message + " [w]");
    }

    private static void expectRejected(GameTestHelper h, Runnable action, String label) {
        try {
            action.run();
            h.assertTrue(false, "Expected fail-closed rejection for " + label);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
