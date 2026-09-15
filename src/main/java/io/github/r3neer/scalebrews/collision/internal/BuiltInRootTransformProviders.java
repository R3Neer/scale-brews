package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** Common-safe built-in root authorities. */
public final class BuiltInRootTransformProviders {
    private BuiltInRootTransformProviders() {}

    public static final Identifier ENTITY_ROOT = Identifier.parse("scalebrews:entity_root");

    /**
     * Exact compatibility root: gravity basis * yaw convention used by the pre-S21 evaluator,
     * plus the entity world origin and uniform Scale attribute.
     */
    private static final RootTransformProvider ENTITY = entity -> {
        if (entity == null) return Optional.empty();
        float scale = entity.getScale();
        if (!Float.isFinite(scale) || scale <= 0) return Optional.empty();
        try {
            Quaternionf rotation = new Quaternionf()
                .setFromNormalized(new Matrix3f(AnatomyMovement.gravity(entity).matrix()))
                .rotateY((float)Math.toRadians(180.0 - entity.yBodyRot));
            return Optional.of(new RootTransformProvider.RootTransform(entity.position(), rotation, scale));
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    };

    /** Source/fixture compatibility seam; canonical runtime bindings resolve their provider from the accepted catalog. */
    static RootTransformProvider entityRoot() { return ENTITY; }

    public static synchronized void initialize() {
        var existing = CollisionEngines.rootTransform(ENTITY_ROOT);
        if (existing.isEmpty()) {
            CollisionEngines.registerRootTransform(ENTITY_ROOT, ENTITY);
            return;
        }
        if (existing.orElseThrow() != ENTITY)
            throw new IllegalStateException("Scale built-in root id was pre-claimed by another owner: " + ENTITY_ROOT);
    }
}
