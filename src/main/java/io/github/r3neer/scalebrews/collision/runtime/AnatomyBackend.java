package io.github.r3neer.scalebrews.collision.runtime;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Scale-owned service contract below the public collision facade. Its boundary
 * deliberately uses only runtime-neutral/JDK/Minecraft types so the runtime
 * layer never depends back on collision.api.
 */
public interface AnatomyBackend {
    enum Mode { DISABLED, BINDING, READY }

    record ContactData(UUID support, long revision, String piece, int face, Vec3 localPoint, Vec3 normal, long tick) {}
    record RayHit(LivingEntity support, ContactData contact, Vec3 position, double fraction) {}

    default Mode mode(Entity entity) { return Mode.DISABLED; }
    default void clearContact(Entity entity) {}
    default boolean supported(Entity entity) { return false; }
    default Optional<LivingEntity> support(Entity entity) { return Optional.empty(); }
    default boolean spaceClear(Entity entity, AABB box) { return false; }
    default Optional<RayHit> raycast(Entity entity, Vec3 start, Vec3 end) { return Optional.empty(); }
    default boolean attachAtContact(Entity body, RayHit hit) { return false; }
    default Direction gravity(Entity entity) { return Direction.DOWN; }

    default void installGravityAdapter(String owner, Function<Entity, Direction> resolver) {
        throw new IllegalStateException("Scale Brews collision backend is unavailable");
    }

    static AnatomyBackend unavailable() { return new AnatomyBackend() {}; }
}
