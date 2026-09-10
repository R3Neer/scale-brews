package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.AnatomyMode;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * One P2-owned ownership/readiness decision for anatomy consumers.
 *
 * <p>The server side means the world catalog session is active; it deliberately
 * does not require the transported root to be a geometry support. The client
 * side is installed only after it has accepted the matching epoch/revision.
 * P1 separately decides whether a body has an eligible configured support.
 * BINDING owns the route but fails closed: it never revives legacy
 * AABB/carry/placement merely because data is still synchronizing.</p>
 */
public final class AnatomySession {
    private AnatomySession() {}

    private static volatile Function<Entity,AnatomyMode> clientMode=entity->AnatomyMode.DISABLED;

    public static AnatomyMode mode(Entity entity) {
        Objects.requireNonNull(entity,"entity");
        if(entity.level().isClientSide())return clientMode.apply(entity);
        if(!AnatomyRuntime.owns(entity))return AnatomyMode.DISABLED;
        return AnatomyRuntime.ready(entity)?AnatomyMode.READY:AnatomyMode.BINDING;
    }
    /** Shared route ownership; true in BINDING even though material queries are unavailable. */
    public static boolean owns(Entity entity){return mode(entity)!=AnatomyMode.DISABLED;}
    /** Material data is usable only after catalog/session acceptance. */
    public static boolean ready(Entity entity) {
        return mode(entity)==AnatomyMode.READY;
    }

    /** Client bootstrap installs the level/catalog resolver; no client data reaches the server. */
    public static void installClientMode(Function<Entity,AnatomyMode> resolver) {
        clientMode=Objects.requireNonNull(resolver,"resolver");
    }
}
