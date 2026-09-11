package io.github.r3neer.scalebrews.integration.gravity;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Cardinal gravity-local frame used by Scale-generated mechanics.
 *
 * <p>Coordinates are expressed in the same local convention used by Gravity Changer:
 * +Y is away from gravity, +Z is local forward and +X is local right. The frame only
 * converts newly generated local contributions; it never reinterprets existing world
 * momentum.</p>
 */
public record GravityFrame(Direction direction) {
    public static final GravityFrame VANILLA = new GravityFrame(Direction.DOWN);

    public GravityFrame {
        Objects.requireNonNull(direction, "direction");
    }

    public Vec3 toWorld(Vec3 local) {
        Objects.requireNonNull(local, "local");
        return toWorld(local.x, local.y, local.z);
    }

    public Vec3 toWorld(double x, double y, double z) {
        return switch (direction) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, -z, y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(y, -z, -x);
            case EAST -> new Vec3(-y, -z, x);
        };
    }

    public Vec3 toLocal(Vec3 world) {
        Objects.requireNonNull(world, "world");
        return toLocal(world.x, world.y, world.z);
    }

    public Vec3 toLocal(double x, double y, double z) {
        return switch (direction) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, z, -y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(-z, x, -y);
            case EAST -> new Vec3(z, -x, -y);
        };
    }

    /** Keeps only the current local vertical component of an existing world vector. */
    public Vec3 keepLocalVertical(Vec3 world) {
        Vec3 local = toLocal(world);
        return toWorld(0, local.y, 0);
    }

    /** Applies a multiplier only to the current local vertical component. */
    public Vec3 multiplyLocalVertical(Vec3 world, double multiplier) {
        Vec3 local = toLocal(world);
        return toWorld(local.x, local.y * multiplier, local.z);
    }
}
