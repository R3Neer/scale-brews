package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.runtime.AnatomyBackend;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The sole Scale-owned implementation behind the public collision facade. */
public final class ScaleAnatomyBackend implements AnatomyBackend {
    @Override public AnatomyBackend.Mode mode(Entity entity) {
        if (entity == null) return AnatomyBackend.Mode.DISABLED;
        return switch (AnatomySession.mode(entity)) {
            case DISABLED -> AnatomyBackend.Mode.DISABLED;
            case BINDING -> AnatomyBackend.Mode.BINDING;
            case READY -> AnatomyBackend.Mode.READY;
        };
    }

    private boolean ready(Entity entity) { return mode(entity) == AnatomyBackend.Mode.READY; }

    @Override public void clearContact(Entity entity) { if (entity != null) AnatomyMovement.clear(entity); }
    @Override public boolean supported(Entity entity) { return ready(entity) && AnatomyMovement.supported(entity); }

    @Override public Optional<LivingEntity> support(Entity entity) {
        if (!ready(entity)) return Optional.empty();
        var contact = AnatomyMovement.contact(entity);
        return contact == null ? Optional.empty() : Optional.of(contact.support());
    }

    @Override public boolean spaceClear(Entity entity, AABB box) {
        return valid(box) && ready(entity) && AnatomyMovement.spaceClear(entity, box);
    }

    @Override public Optional<AnatomyBackend.RayHit> raycast(Entity entity, Vec3 start, Vec3 end) {
        if (!finite(start) || !finite(end) || !ready(entity)) return Optional.empty();
        var hit = AnatomyMovement.raycast(entity, start, end);
        if (hit == null) return Optional.empty();
        var contact = hit.contact();
        return Optional.of(new AnatomyBackend.RayHit(hit.support(),
            new AnatomyBackend.ContactData(contact.support(), contact.revision(), contact.piece(), contact.face(),
                contact.localPoint(), contact.normal(), contact.tick()),
            hit.position(), hit.fraction()));
    }

    @Override public boolean attachAtContact(Entity body, AnatomyBackend.RayHit hit) {
        SurfaceContact surface = hit == null || hit.contact() == null ? null : new SurfaceContact(
            hit.contact().support(), hit.contact().revision(), hit.contact().piece(), hit.contact().face(),
            hit.contact().localPoint(), hit.contact().normal(), hit.contact().tick());
        if (body == null || hit == null || surface == null || !ready(body) || hit.support() == null
                || hit.support().level() != body.level()
                || !hit.support().getUUID().equals(surface.support())
                || !AnatomyMovement.confirm(body, hit.support(), surface)
                || !AnatomyMovement.supported(body)) {
            if (body != null) AnatomyMovement.clear(body);
            return false;
        }
        return true;
    }

    @Override public Direction gravity(Entity entity) {
        return entity == null ? Direction.DOWN : GravityFrames.direction(entity);
    }

    @Override public void installGravityAdapter(String owner, Function<Entity, Direction> resolver) {
        GravityFrames.install(owner, resolver);
    }

    private static boolean finite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.lengthSqr());
    }

    private static boolean valid(AABB box) {
        return box != null && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
            && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }
}
