package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Matrix4f;

/** Implementer regression for the first S21 root-authority increment. */
public final class S21RootTransformProviderTests {
    private static final float EPS = 3e-5f;

    @GameTest
    public void executableEntityRootMatchesPreS21Composition(GameTestHelper h) {
        var entity = h.spawn(EntityTypes.COW, 2, 3, 4);
        try {
            entity.setYBodyRot(37.5f);
            var provider = CollisionEngines.rootTransform(BuiltInRootTransformProviders.ENTITY_ROOT).orElseThrow();
            var sampled = provider.sample(entity).orElseThrow();

            Matrix4f expected = new Matrix4f().translation(entity.position())
                .mul(new Matrix4f(AnatomyMovement.gravity(entity).matrix())
                    .rotateY((float)Math.toRadians(180.0 - entity.yBodyRot))
                    .scale(entity.getScale()));
            assertNear(h, expected, sampled.matrix(),
                "scalebrews:entity_root must preserve the exact pre-S21 root matrix");
        } finally {
            entity.discard();
        }
        h.succeed();
    }

    @GameTest
    public void rootProviderRejectsInvalidNullSampleWithoutFallback(GameTestHelper h) {
        var provider = CollisionEngines.rootTransform(BuiltInRootTransformProviders.ENTITY_ROOT).orElseThrow();
        h.assertTrue(provider.sample(null).equals(Optional.empty()),
            "Built-in root authority must fail closed for a missing entity");
        h.succeed();
    }

    private static void assertNear(GameTestHelper h, Matrix4f expected, Matrix4f actual, String message) {
        float[] left = expected.get(new float[16]);
        float[] right = actual.get(new float[16]);
        for (int index = 0; index < left.length; index++) {
            h.assertTrue(Math.abs(left[index] - right[index]) <= EPS,
                message + " at matrix index " + index + ": expected=" + left[index] + " actual=" + right[index]);
        }
    }
}
