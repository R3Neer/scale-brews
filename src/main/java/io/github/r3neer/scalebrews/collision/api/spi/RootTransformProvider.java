package io.github.r3neer.scalebrews.collision.api.spi;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Supplies a support root transform independently from local joint pose evaluation. */
@FunctionalInterface
public interface RootTransformProvider {
    Optional<RootTransform> sample(LivingEntity entity);

    /** Immutable quaternion components avoid exposing a mutable JOML object through the public DTO. */
    record RootTransform(Vec3 origin, float qx, float qy, float qz, float qw, float scale) {
        /** Shared authority/wire bound; a transform valid in authority must always be serializable. */
        public static final float MAX_SCALE = 1024f;
        public RootTransform {
            if (origin == null || !Double.isFinite(origin.lengthSqr()) || !Float.isFinite(qx + qy + qz + qw + scale)
                    || scale <= 0 || scale > MAX_SCALE)
                throw new IllegalArgumentException("Invalid root transform");
            float length = (float)Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
            if (!Float.isFinite(length) || length < 1e-6f) throw new IllegalArgumentException("Invalid root rotation");
            qx /= length; qy /= length; qz /= length; qw /= length;
            // q and -q encode the same orientation. Canonicalize the sign at the DTO boundary so
            // caches/causal comparisons cannot manufacture a root change from an equivalent quaternion.
            boolean flip = qw < 0f
                || (qw == 0f && qz < 0f)
                || (qw == 0f && qz == 0f && qy < 0f)
                || (qw == 0f && qz == 0f && qy == 0f && qx < 0f);
            if (flip) { qx = -qx; qy = -qy; qz = -qz; qw = -qw; }
        }

        public RootTransform(Vec3 origin, Quaternionf rotation, float scale) {
            this(origin, require(rotation).x, require(rotation).y, require(rotation).z, require(rotation).w, scale);
        }

        private static Quaternionf require(Quaternionf rotation) {
            if (rotation == null) throw new IllegalArgumentException("Missing root rotation");
            return rotation;
        }

        public Quaternionf quaternion() { return new Quaternionf(qx, qy, qz, qw); }
    }
}
