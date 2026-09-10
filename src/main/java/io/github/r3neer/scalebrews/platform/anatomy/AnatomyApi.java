package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Stable, intentionally small API for optional anatomy consumers.
 * Geometry, pose, collision response, transport and networking remain Scale-owned.
 */
public final class AnatomyApi {
    private AnatomyApi() {}

    /** Increment only for an incompatible public API change. */
    /** Pose v4 adds non-ambiguous causal frame/root provenance. */
    public static final int PROTOCOL_VERSION=2;
    public enum Capability {
        CONTACTS,
        CLEARANCE,
        RAYCAST,
        GRAVITY_FRAME,
        ROOT_TRANSPORT,
        SERVER_AUTHORITY
    }
    private static final long CAPABILITIES=mask(Capability.CONTACTS,Capability.CLEARANCE,Capability.RAYCAST,
        Capability.GRAVITY_FRAME,Capability.ROOT_TRANSPORT,Capability.SERVER_AUTHORITY);

    public static long capabilities(){return CAPABILITIES;}
    public static long mask(Capability... requested) {
        long result=0;
        for(var capability:requested)result|=1L<<capability.ordinal();
        return result;
    }
    public static boolean compatible(int peerVersion,long requiredCapabilities) {
        return peerVersion==PROTOCOL_VERSION && (CAPABILITIES&requiredCapabilities)==requiredCapabilities;
    }
    /**
     * Deprecated ownership alias retained because older Scale hooks used it as
     * an outer guard. It includes BINDING so those hooks fail closed instead of
     * falling through into the old platform engine.
     */
    @Deprecated(forRemoval=false)
    public static boolean active(Entity entity){return ownsSharedPhysics(entity);}
    public static AnatomyMode mode(Entity entity){return AnatomySession.mode(entity);}
    /** The shared path owns this level/session, including fail-closed BINDING. */
    public static boolean ownsSharedPhysics(Entity entity){return AnatomySession.owns(entity);}
    /** Catalog/session data is usable for material queries. */
    public static boolean ready(Entity entity){return AnatomySession.ready(entity);}
    /**
     * Compatibility alias for the former ambiguous guard. It means READY, not
     * ownership; new consumers must pair ownsSharedPhysics with ready according
     * to their operation. BINDING remains fail-closed and never authorizes a
     * legacy route.
     */
    @Deprecated(forRemoval=false)
    public static boolean usesSharedPhysics(Entity entity) {
        return ready(entity);
    }
    /** Releases only Scale's transient physical contact and transport anchor. */
    public static void clearContact(Entity entity){AnatomyMovement.clear(entity);}
    public static boolean supported(Entity entity){return ready(entity) && AnatomyMovement.supported(entity);}
    /** Support identity is exposed; piece, anchor and response remain internal. */
    public static Optional<LivingEntity> support(Entity entity) {
        if(!ready(entity))return Optional.empty();
        var contact=AnatomyMovement.contact(entity);
        return contact==null?Optional.empty():Optional.of(contact.support());
    }
    /** Tests configured eligible convex anatomy only; it is not a global noCollision query. */
    public static boolean spaceClear(Entity entity,AABB box){return ready(entity) && AnatomyMovement.spaceClear(entity,box);}
    /** Scoped material ray query for an active body/root; callers own block raycasts and placement rules. */
    public static Optional<RayHit> raycast(Entity entity,net.minecraft.world.phys.Vec3 start,net.minecraft.world.phys.Vec3 end) {
        if(!ready(entity))return Optional.empty();
        var hit=AnatomyMovement.raycast(entity,start,end);
        return hit==null?Optional.empty():Optional.of(new RayHit(hit.support(),hit.contact(),hit.position(),hit.fraction()));
    }
    /**
     * Establishes a Scale-owned transient anchor after the caller has created and
     * placed the body. This validates the ray support, current material face,
     * eligibility and physical support; it never bypasses block permissions or
     * invents an AABB fallback.
     */
    public static boolean attachAtContact(Entity body,RayHit hit) {
        if(!ready(body) || hit.support().level()!=body.level() || !hit.support().getUUID().equals(hit.contact().support())
            || !AnatomyMovement.confirm(body,hit.support(),hit.contact()) || !AnatomyMovement.supported(body)) {
            AnatomyMovement.clear(body);return false;
        }
        return true;
    }
    /** The gravity owner supplies this cardinal frame; Scale does not mutate it. */
    public static GravityFrame gravity(Entity entity){return AnatomyMovement.gravity(entity);}
    /** Installs the sole external cardinal gravity reader; same-owner repeats are idempotent. */
    public static void installGravityAdapter(String owner,Function<Entity,Direction> resolver) {
        java.util.Objects.requireNonNull(resolver,"resolver");
        GravityFrames.install(owner,entity->new GravityFrame(java.util.Objects.requireNonNull(resolver.apply(entity),"gravity direction")));
    }
    public record RayHit(LivingEntity support,SurfaceContact contact,net.minecraft.world.phys.Vec3 position,double fraction) {
        public RayHit {
            if(support==null || contact==null || !support.getUUID().equals(contact.support()) || position==null || !Double.isFinite(position.lengthSqr()) || !Double.isFinite(fraction) || fraction<0 || fraction>1)
                throw new IllegalArgumentException("Invalid anatomical ray hit");
        }
    }
}
