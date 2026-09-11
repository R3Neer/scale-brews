package io.github.r3neer.scalebrews.collision.runtime;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Scale-owned service contract behind {@link AnatomyApi}. This type is runtime
 * wiring, not supported consumer API; external consumers use AnatomyApi and the
 * explicit public extension SPIs.
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

    static AnatomyBackend unavailable() { return new AnatomyBackend() {}; }
}
