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
        public RootTransform {
            if (origin == null || !Double.isFinite(origin.lengthSqr()) || !Float.isFinite(qx + qy + qz + qw + scale) || scale <= 0)
                throw new IllegalArgumentException("Invalid root transform");
            float length = (float)Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
            if (!Float.isFinite(length) || length < 1e-6f) throw new IllegalArgumentException("Invalid root rotation");
            qx /= length; qy /= length; qz /= length; qw /= length;
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
