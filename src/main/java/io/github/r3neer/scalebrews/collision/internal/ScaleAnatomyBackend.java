package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyBackend;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The sole Scale-owned implementation behind the public collision facade. */
public final class ScaleAnatomyBackend implements AnatomyBackend {
    @Override public AnatomyMode mode(Entity entity) { return AnatomySession.mode(entity); }
    @Override public boolean ownsSharedPhysics(Entity entity) { return AnatomySession.owns(entity); }
    @Override public boolean ready(Entity entity) { return AnatomySession.ready(entity); }
    @Override public void clearContact(Entity entity) { AnatomyMovement.clear(entity); }
    @Override public boolean supported(Entity entity) { return ready(entity) && AnatomyMovement.supported(entity); }

    @Override public Optional<LivingEntity> support(Entity entity) {
        if (!ready(entity)) return Optional.empty();
        var contact = AnatomyMovement.contact(entity);
        return contact == null ? Optional.empty() : Optional.of(contact.support());
    }

    @Override public boolean spaceClear(Entity entity, AABB box) {
        return ready(entity) && AnatomyMovement.spaceClear(entity, box);
    }

    @Override public Optional<AnatomyApi.RayHit> raycast(Entity entity, Vec3 start, Vec3 end) {
        if (!ready(entity)) return Optional.empty();
        var hit = AnatomyMovement.raycast(entity, start, end);
        return hit == null ? Optional.empty()
            : Optional.of(new AnatomyApi.RayHit(hit.support(), hit.contact(), hit.position(), hit.fraction()));
    }

    @Override public boolean attachAtContact(Entity body, AnatomyApi.RayHit hit) {
        if (!ready(body) || hit.support().level() != body.level()
                || !hit.support().getUUID().equals(hit.contact().support())
                || !AnatomyMovement.confirm(body, hit.support(), hit.contact())
                || !AnatomyMovement.supported(body)) {
            AnatomyMovement.clear(body);
            return false;
        }
        return true;
    }

    @Override public GravityFrame gravity(Entity entity) { return AnatomyMovement.gravity(entity); }

    @Override public void installGravityAdapter(String owner, Function<Entity, Direction> resolver) {
        java.util.Objects.requireNonNull(resolver, "resolver");
        GravityFrames.install(owner, entity -> new GravityFrame(
            java.util.Objects.requireNonNull(resolver.apply(entity), "gravity direction")));
    }
}
