package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import java.util.*;

public final class Platforms {
    public static final ResourceKey<Registry<PlatformDefinition>> DEFINITIONS = ResourceKey.createRegistryKey(ScaleBrews.id("entity_platform"));
    public static final ResourceKey<Registry<PlatformPolicy>> POLICIES = ResourceKey.createRegistryKey(ScaleBrews.id("platform_policy"));
    public static final ResourceKey<PlatformPolicy> DEFAULT = ResourceKey.create(POLICIES, ScaleBrews.id("default"));
    private static final Map<Registry<PlatformDefinition>, Map<Identifier, PlatformDefinition>> INDEX = Collections.synchronizedMap(new WeakHashMap<>());
    public interface PhysicalAdapter {
        String category();
        boolean permits(Entity body);
        /** Special motion implementations may restrict a requested carry before collision resolution. */
        default net.minecraft.world.phys.Vec3 transport(Entity body,net.minecraft.world.phys.Vec3 requested) { return requested; }
    }
    private static final Map<Identifier, PhysicalAdapter> ADAPTERS = new HashMap<>();
    private static final Map<Level,Double> MARGINS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity,PlatformDefinition> AUTOMATIC=Collections.synchronizedMap(new WeakHashMap<>());
    public static void registerAdapter(Identifier type, PhysicalAdapter adapter) { ADAPTERS.put(type, adapter); }
    private Platforms() {}
    public static void initialize() {
        DynamicRegistries.registerSynced(DEFINITIONS, PlatformDefinition.CODEC);
        DynamicRegistries.registerSynced(POLICIES, PlatformPolicy.CODEC);
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
        var adapter=ADAPTERS.get(BuiltInRegistries.ENTITY_TYPE.getKey(body.getType()));
        return adapter==null?requested:adapter.transport(body,requested);
    }
    public static String category(Entity e) {
        if(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return null;
        var adapter = ADAPTERS.get(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
        if (adapter != null) return adapter.permits(e) ? adapter.category() : null;
        if (e instanceof Player) return "players";
        if (e instanceof Mob) return "mobs";
        if (e instanceof AbstractBoat) return "boats";
        if (e instanceof AbstractMinecart cart) return cart.isOnRails() ? null : "minecarts";
        if (e instanceof ItemEntity) return "items";
        if (e instanceof FallingBlockEntity) return "falling_blocks";
        return null;
    }
    public static boolean ordinary(Entity e) {
        if (!e.isAlive() || e.noPhysics || e.isSpectator() || e.isPassenger()) return false;
        if (e instanceof LivingEntity l && (l.isSleeping() || l.isFallFlying() || l.isSwimming() || l.isBaby())) return false;
        if (e instanceof Player p && p.getAbilities().flying) return false;
        if (e instanceof Camel c && c.isCamelSitting()) return false;
        if (e instanceof net.minecraft.world.entity.animal.feline.Cat cat
                && (cat.isInSittingPose() || cat.isLying())) return false;
        return true;
    }
    public static boolean eligible(Entity body, LivingEntity support) {
        if (body == support || body.level() != support.level() || !ordinary(body) || !ordinary(support)) return false;
        var p = policy(body.level()); var category = category(body);
        var d = definition(support);
        double ratio = body.getBbWidth() / (double)support.getBbWidth();
        if (!PlatformEligibility.allows(p,d,category,ratio)) return false;
        Entity ancestor = support;
        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (ancestor != null) {
            if (ancestor == body || !seen.add(ancestor)) return false;
            ancestor = state(ancestor).support;
        }
        return true;
    }
    public static boolean supported(Entity e) { var s=state(e); return s.support!=null && eligible(e,s.support); }
    public static double friction(Entity e, double original) {
        return supported(e) ? definition(state(e).support).friction() : original;
    }
    public static void tick(ServerLevel level) {
        for(Entity e:level.getAllEntities()) noteSupport(e);
        for (Entity e : level.getAllEntities()) if (state(e).support != null || state(e).published) {
            PlatformPhysics.carry(e);
            PlatformNetworking.broadcast(e);
            state(e).published = state(e).support != null;
            state(e).transported = net.minecraft.world.phys.Vec3.ZERO;
        }
    }
}
