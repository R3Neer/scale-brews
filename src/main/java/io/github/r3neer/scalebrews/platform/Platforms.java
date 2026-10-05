package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.CollisionAdapters;
import io.github.r3neer.scalebrews.collision.api.spi.BodyAdapter;
import io.github.r3neer.scalebrews.collision.integration.BodyClassification;
import io.github.r3neer.scalebrews.collision.integration.CollisionRules;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Released data registry boundary and canonical body-policy facade; no platform runtime. */
public final class Platforms {
    public static final ResourceKey<Registry<PlatformDefinition>> DEFINITIONS = ResourceKey.createRegistryKey(ScaleBrews.id("entity_platform"));
    public static final ResourceKey<Registry<PlatformPolicy>> POLICIES = ResourceKey.createRegistryKey(ScaleBrews.id("platform_policy"));
    public static final ResourceKey<Registry<io.github.r3neer.scalebrews.collision.geometry.ModelGeometry>> GEOMETRIES = ResourceKey.createRegistryKey(ScaleBrews.id("entity_geometry"));
    public static final ResourceKey<PlatformPolicy> DEFAULT = ResourceKey.create(POLICIES, ScaleBrews.id("default"));
    /** @deprecated Register {@link BodyAdapter} through {@link CollisionAdapters}. */
    @Deprecated
    public interface PhysicalAdapter extends BodyAdapter {}

    /** @deprecated Use {@link CollisionAdapters#registerBody(Identifier, BodyAdapter)}. */
    @Deprecated
    public static void registerAdapter(Identifier type, PhysicalAdapter adapter) { CollisionAdapters.registerBody(type, adapter); }
    private Platforms() {}
    public static void initialize() {
        io.github.r3neer.scalebrews.collision.internal.AnatomyNetworking.initialize();
        io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime.initialize();
        DynamicRegistries.registerSynced(DEFINITIONS, PlatformDefinition.CODEC);
        DynamicRegistries.registerSynced(POLICIES, PlatformPolicy.CODEC);
        DynamicRegistries.registerSynced(GEOMETRIES, io.github.r3neer.scalebrews.collision.internal.AnatomyCodecs.GEOMETRY);
        ServerTickEvents.END_LEVEL_TICK.register(Platforms::tick);
    }
    public static boolean simulates(Entity e) { return AnatomyMovement.simulates(e); }
    /** Conservative padding of the canonical spatial index, never material derived from an entity AABB. */
    public static double searchMargin(Level level) { return .5; }
    public static PlatformPolicy policy(Level level) {
        return level.registryAccess().lookup(POLICIES).flatMap(r -> r.get(DEFAULT)).map(h -> h.value()).orElse(PlatformPolicy.DEFAULT);
    }
    public static net.minecraft.world.phys.Vec3 adaptTransport(Entity body,net.minecraft.world.phys.Vec3 requested) {
        return BodyClassification.adaptTransport(body,requested);
    }
    public static String category(Entity e) { return BodyClassification.category(e); }
    public static boolean ordinary(Entity e) { return BodyClassification.ordinary(e); }
    public static boolean eligible(Entity body, LivingEntity support) {
        if (body == support || body.level() != support.level() || !ordinary(body) || !ordinary(support)) return false;
        var category=category(body);
        double ratio=body.getBbWidth()/(double)support.getBbWidth();
        var canonical=AnatomyMovement.canonicalBinding(support);
        if(canonical==null || !CollisionRules.allows(LegacyCollisionData.policy(policy(body.level())),
                canonical.policy(),category,canonical.entity(),ratio))return false;
        Entity ancestor=support;
        Set<Entity> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        while(ancestor!=null) {
            if(ancestor==body || !seen.add(ancestor))return false;
            var anatomical=AnatomyMovement.contact(ancestor);
            ancestor=anatomical==null?null:anatomical.support();
        }
        return true;
    }
    public static boolean supported(Entity e) { return AnatomyApi.supported(e); }
    /** Read-only support from the sole canonical owner. */
    public static LivingEntity support(Entity e) { return AnatomyApi.support(e).orElse(null); }
    public static double friction(Entity e, double original) {
        if(!AnatomyApi.ready(e) || !AnatomyMovement.predictsBody(e))return original;
        var support=support(e);var category=category(e);
        if(support==null || category==null)return original;
        var binding=AnatomyMovement.canonicalBinding(support);
        return binding==null?original:CollisionRules.resolve(LegacyCollisionData.policy(policy(e.level())),
            binding.policy(),category,binding.entity()).friction();
    }
    public static void tick(ServerLevel level) {
        io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime.prepare(level);
        io.github.r3neer.scalebrews.collision.internal.AnatomyMovement.tick(level);
        io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime.publish(level);
        // Commit the simultaneous canonical joint batch before later world work can observe it stale.
        io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime.drain(level);
    }
}
