package io.github.r3neer.scalebrews.collision.api;

import io.github.r3neer.scalebrews.collision.runtime.AnatomyBackend;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Stable, intentionally small public API for entity-collision consumers.
 * Geometry, pose, collision response, transport and networking remain Scale-owned.
 */
public final class AnatomyApi {
    private AnatomyApi() {}

    /** Increment only for an incompatible public API/wire contract change. */
    public static final int PROTOCOL_VERSION = 3;
    /** Canonical collision binding/policy data schema understood by this API generation. */
    public static final int DATA_SCHEMA_VERSION = 1;

    public enum Capability {
        CONTACTS,
        CLEARANCE,
        RAYCAST,
        GRAVITY_FRAME,
        ROOT_TRANSPORT,
        SERVER_AUTHORITY,
        ENGINE_REGISTRY,
        VERSIONED_BINDINGS,
        BODY_ADAPTERS
    }

    private static final long CAPABILITIES = mask(Capability.CONTACTS, Capability.CLEARANCE, Capability.RAYCAST,
        Capability.GRAVITY_FRAME, Capability.ROOT_TRANSPORT, Capability.SERVER_AUTHORITY,
        Capability.ENGINE_REGISTRY, Capability.VERSIONED_BINDINGS, Capability.BODY_ADAPTERS);
    private static final AnatomyBackend BACKEND = loadBackend();

    private static AnatomyBackend loadBackend() {
        var providers = ServiceLoader.load(AnatomyBackend.class, AnatomyApi.class.getClassLoader()).stream().toList();
        if (providers.isEmpty()) return AnatomyBackend.unavailable();
        if (providers.size() != 1)
            throw new IllegalStateException("Scale Brews requires exactly one collision backend, found " + providers.size());
        return providers.getFirst().get();
    }

    public static long capabilities() { return CAPABILITIES; }

    public static long mask(Capability... requested) {
        long result = 0;
        for (var capability : requested) result |= 1L << capability.ordinal();
        return result;
    }

    public static boolean compatible(int peerVersion, long requiredCapabilities) {
        return peerVersion == PROTOCOL_VERSION && (CAPABILITIES & requiredCapabilities) == requiredCapabilities;
    }

    public static AnatomyMode mode(Entity entity) { return BACKEND.mode(entity); }
    /** The shared path owns this level/session, including fail-closed BINDING. */
    public static boolean ownsSharedPhysics(Entity entity) { return BACKEND.ownsSharedPhysics(entity); }
    /** Catalog/session data is usable for material queries. */
    public static boolean ready(Entity entity) { return BACKEND.ready(entity); }
    /** Releases only Scale's transient physical contact and transport anchor. */
    public static void clearContact(Entity entity) { BACKEND.clearContact(entity); }
    public static boolean supported(Entity entity) { return BACKEND.supported(entity); }
    /** Support identity is exposed; piece, anchor and response remain internal. */
    public static Optional<LivingEntity> support(Entity entity) { return BACKEND.support(entity); }
    /** Tests configured eligible convex anatomy only; it is not a global noCollision query. */
    public static boolean spaceClear(Entity entity, AABB box) { return BACKEND.spaceClear(entity, box); }
    /** Scoped material ray query for an active body/root; callers own block raycasts and placement rules. */
    public static Optional<RayHit> raycast(Entity entity, Vec3 start, Vec3 end) { return BACKEND.raycast(entity, start, end); }
    /**
     * Establishes a Scale-owned transient anchor after the caller has created and
     * placed the body. This validates the ray support, current material face,
     * eligibility and physical support; it never bypasses block permissions or
     * invents an AABB fallback.
     */
    public static boolean attachAtContact(Entity body, RayHit hit) { return BACKEND.attachAtContact(body, hit); }
    /** The gravity owner supplies this cardinal frame; Scale does not mutate it. */
    public static GravityFrame gravity(Entity entity) { return BACKEND.gravity(entity); }
    /** Installs the sole external cardinal gravity reader; same-owner repeats are idempotent. */
    public static void installGravityAdapter(String owner, Function<Entity, Direction> resolver) {
        BACKEND.installGravityAdapter(owner, resolver);
    }

    public record RayHit(LivingEntity support, SurfaceContact contact, Vec3 position, double fraction) {
        public RayHit {
            if (support == null || contact == null || !support.getUUID().equals(contact.support()) || position == null
                    || !Double.isFinite(position.lengthSqr()) || !Double.isFinite(fraction) || fraction < 0 || fraction > 1)
                throw new IllegalArgumentException("Invalid anatomical ray hit");
        }
    }
}
