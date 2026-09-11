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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import java.util.*;

public final class Platforms {
    public static final ResourceKey<Registry<PlatformDefinition>> DEFINITIONS = ResourceKey.createRegistryKey(ScaleBrews.id("entity_platform"));
    public static final ResourceKey<Registry<PlatformPolicy>> POLICIES = ResourceKey.createRegistryKey(ScaleBrews.id("platform_policy"));
    public static final ResourceKey<Registry<io.github.r3neer.scalebrews.collision.geometry.ModelGeometry>> GEOMETRIES = ResourceKey.createRegistryKey(ScaleBrews.id("entity_geometry"));
    public static final ResourceKey<PlatformPolicy> DEFAULT = ResourceKey.create(POLICIES, ScaleBrews.id("default"));
    private static final Map<Registry<PlatformDefinition>, Map<Identifier, PlatformDefinition>> INDEX = Collections.synchronizedMap(new WeakHashMap<>());

    /** @deprecated Register {@link BodyAdapter} through {@link CollisionAdapters}. */
    @Deprecated
    public interface PhysicalAdapter extends BodyAdapter {}

    private static final Map<Level,Double> MARGINS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity,PlatformDefinition> AUTOMATIC=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level,Map<Identifier,PlatformDefinition>> ANATOMICAL_DEFINITIONS=Collections.synchronizedMap(new WeakHashMap<>());
    public static void anatomicalDefinitions(Level level,Collection<PlatformDefinition> profiles) {
        Map<Identifier,PlatformDefinition> indexed=new HashMap<>();
        for(var profile:profiles)if(indexed.putIfAbsent(profile.entity(),profile)!=null)throw new IllegalArgumentException("Duplicate anatomical species "+profile.entity());
        ANATOMICAL_DEFINITIONS.put(level,Map.copyOf(indexed));
    }
    public static void clearAnatomicalDefinitions(Level level){ANATOMICAL_DEFINITIONS.remove(level);}
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
        PlatformNetworking.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server->
            server.registryAccess().lookup(DEFINITIONS).ifPresent(Platforms::index));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((e,level)->noteSupport(e));
    }
    public static PlatformState state(Entity e) { return ((PlatformBody)e).scalebrews$platform(); }
    public static boolean simulates(Entity e) { return !e.level().isClientSide() || e.isLocalInstanceAuthoritative(); }
    public static void noteSupport(Entity e) {
        if(!(e instanceof LivingEntity living)) return;
        var d=definition(e);
        if(d==null) return;
        double margin=.5,scale=living.getScale();
        for(var p:d.surfaces()) {
            double radius=Math.hypot(Math.abs(p.x())+p.width()/2,Math.abs(p.z())+p.depth()/2)*scale;
            margin=Math.max(margin,Math.max(radius-e.getBbWidth()/2,p.y()*scale-e.getBbHeight())+.05);
        }
        MARGINS.merge(e.level(),margin,Math::max);
    }
    public static double searchMargin(Level level) { return MARGINS.getOrDefault(level,.5); }
    public static PlatformPolicy policy(Level level) {
        return level.registryAccess().lookup(POLICIES).flatMap(r -> r.get(DEFAULT)).map(h -> h.value()).orElse(PlatformPolicy.DEFAULT);
    }
    public static PlatformDefinition definition(Entity support) {
        // Happy Ghast owns its vanilla platform/parking mechanics. Multipart dragon physics are not ordinary bodies.
        if (!(support instanceof LivingEntity living) || support instanceof net.minecraft.world.entity.animal.happyghast.HappyGhast
                || support instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return null;
        var anatomical=ANATOMICAL_DEFINITIONS.get(support.level());
        if(anatomical!=null)return anatomical.get(BuiltInRegistries.ENTITY_TYPE.getKey(support.getType()));
        var registry = support.registryAccess().lookup(DEFINITIONS).orElse(null);
        if (registry == null) return null;
        var id=BuiltInRegistries.ENTITY_TYPE.getKey(support.getType());
        var explicit=index(registry).get(id);
        if(explicit!=null) return explicit; // Including explicit disable: never replace it with a fallback.
        if(!policy(support.level()).automaticSurfaces()) return null;
        double scale=living.getScale();
        var box=support.getBoundingBox();
        double width=box.getXsize()/scale, depth=box.getZsize()/scale, height=(box.maxY-support.getY())/scale;
        if(!Double.isFinite(width+depth+height) || width<=0 || depth<=0 || height<=0) return null;
        var cached=AUTOMATIC.get(support);
        if(cached!=null) {
            var p=cached.surfaces().getFirst();
            if(Math.abs(p.width()-width)<1e-6 && Math.abs(p.depth()-depth)<1e-6 && Math.abs(p.y()-height)<1e-6) return cached;
        }
        var generated=new PlatformDefinition(id,true,.6,Optional.empty(),List.of(
            new PlatformDefinition.Surface("automatic_top",0,height,0,width,depth,Optional.empty())));
        AUTOMATIC.put(support,generated);
        return generated;
    }
    private static Map<Identifier,PlatformDefinition> index(Registry<PlatformDefinition> registry) {
        return INDEX.computeIfAbsent(registry, r -> {
            Map<Identifier, PlatformDefinition> result = new HashMap<>();
            r.listElements().forEach(h -> {
                var d = h.value();
                if (result.putIfAbsent(d.entity(), d) != null)
                    throw new IllegalStateException("Duplicate platform species " + d.entity() + " at " + h.key());
            });
            return Map.copyOf(result);
        });
    }
    public static net.minecraft.world.phys.Vec3 adaptTransport(Entity body,net.minecraft.world.phys.Vec3 requested) {
        return BodyClassification.adaptTransport(body,requested);
    }
    public static String category(Entity e) { return BodyClassification.category(e); }
    public static boolean ordinary(Entity e) { return BodyClassification.ordinary(e); }
    public static boolean eligible(Entity body, LivingEntity support) {
        if (body == support || body.level() != support.level() || !ordinary(body) || !ordinary(support)) return false;
        var legacyPolicy = policy(body.level());
        var definition = definition(support);
        var category = category(body);
        double ratio = body.getBbWidth() / (double)support.getBbWidth();
        if (definition == null || !CollisionRules.allows(LegacyCollisionData.policy(legacyPolicy),
                LegacyCollisionData.profilePolicy(definition), category, definition.entity(), ratio)) return false;
        Entity ancestor = support;
        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (ancestor != null) {
            if (ancestor == body || !seen.add(ancestor)) return false;
            var anatomical=AnatomyMovement.contact(ancestor);
            ancestor=anatomical==null?state(ancestor).support:anatomical.support();
        }
        return true;
    }
    public static boolean supported(Entity e) {
        if(AnatomyApi.ownsSharedPhysics(e)) return AnatomyApi.ready(e) && AnatomyMovement.supported(e);
        var s=state(e);return s.support!=null && eligible(e,s.support);
    }
    /** Current physical support across the one active core mode; never creates a fallback. */
    public static LivingEntity support(Entity e) {
        if(AnatomyApi.ownsSharedPhysics(e)) return AnatomyApi.ready(e)?AnatomyApi.support(e).orElse(null):null;
        return state(e).support;
    }
    public static double friction(Entity e, double original) {
        var support=support(e);var definition=support==null?null:definition(support);var category=category(e);
        if(definition==null || category==null)return original;
        return CollisionRules.resolve(LegacyCollisionData.policy(policy(e.level())),LegacyCollisionData.profilePolicy(definition),
            category,definition.entity()).friction();
    }
    public static void tick(ServerLevel level) {
        io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime.prepare(level);
        io.github.r3neer.scalebrews.collision.internal.AnatomyMovement.tick(level);
        io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime.publish(level);
        // Provider cadence is simultaneous. Drain the canonical joint batch immediately after
        // publication, before legacy end-of-tick carry or later world work can observe it stale.
        io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime.drain(level);
        for(Entity e:level.getAllEntities()) noteSupport(e);
        for (Entity e : level.getAllEntities()) {
            if(AnatomyApi.ownsSharedPhysics(e)) {
                state(e).clear();state(e).published=false;continue;
            }
            if(state(e).support == null && !state(e).published)continue;
            PlatformPhysics.carry(e);
            PlatformNetworking.broadcast(e);
            state(e).published = state(e).support != null;
            state(e).transported = net.minecraft.world.phys.Vec3.ZERO;
        }
    }
}
