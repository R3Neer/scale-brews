package io.github.r3neer.scalebrews.collision.api;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Runtime backend used by {@link AnatomyApi}. Consumers call {@code AnatomyApi};
 * Scale supplies exactly one backend implementation through Java services.
 *
 * <p>This interface exists to invert the dependency between the stable API and
 * Scale's runtime implementation. It is not a second physics engine and must
 * not be used by consumers as an alternative entry point.</p>
 */
public interface AnatomyBackend {
    default AnatomyMode mode(Entity entity) { return AnatomyMode.DISABLED; }
    default boolean ownsSharedPhysics(Entity entity) { return false; }
    default boolean ready(Entity entity) { return false; }
    default void clearContact(Entity entity) {}
    default boolean supported(Entity entity) { return false; }
    default Optional<LivingEntity> support(Entity entity) { return Optional.empty(); }
    default boolean spaceClear(Entity entity, AABB box) { return false; }
    default Optional<AnatomyApi.RayHit> raycast(Entity entity, Vec3 start, Vec3 end) { return Optional.empty(); }
    default boolean attachAtContact(Entity body, AnatomyApi.RayHit hit) { return false; }
    default GravityFrame gravity(Entity entity) { return GravityFrame.VANILLA; }

    default void installGravityAdapter(String owner, Function<Entity, Direction> resolver) {
        throw new IllegalStateException("Scale Brews collision backend is unavailable");
    }

    /** A deliberately inert backend for API-only consumers and absent runtime services. */
    static AnatomyBackend unavailable() { return new AnatomyBackend() {}; }
}
