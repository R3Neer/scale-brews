package io.github.r3neer.scalebrews.loot;

import com.google.gson.JsonParser;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Consumer;

/** Reloadable entity-scoped material rules. Never touches equipment or repeats a loot roll. */
public final class ScaleLoot {
    private record Rule(Set<Identifier> items, Set<Identifier> tags, String entityTag) {}
    private static final Map<MinecraftServer, Map<Identifier, List<Rule>>> RULES = new WeakHashMap<>();
    private static final ThreadLocal<LivingEntity> HARVEST = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> EMITTING = ThreadLocal.withInitial(() -> false);
    private ScaleLoot() {}
    public static <T> T harvesting(LivingEntity entity, java.util.function.Supplier<T> action) {
        var previous = HARVEST.get();
        HARVEST.set(entity);
        try { return action.get(); }
        finally { if (previous == null) HARVEST.remove(); else HARVEST.set(previous); }
    }
    public static boolean isHarvesting(LivingEntity entity) { return HARVEST.get() == entity && !EMITTING.get(); }


    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(ScaleLoot::reload);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> { if (success) reload(server); });
        ServerLifecycleEvents.SERVER_STOPPED.register(RULES::remove);
    }

    private static void reload(MinecraftServer server) {
        Map<Identifier, List<Rule>> result = new HashMap<>();
        server.getResourceManager().listResources("scalebrews/scale_loot", id -> id.getPath().endsWith(".json"))
            .forEach((file, resource) -> {
                try (var reader = resource.openAsReader()) {
                    var json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("enabled") && !json.get("enabled").getAsBoolean()) return;
                    var entity = Identifier.parse(json.get("entity").getAsString());
                    // Missing optional mods are inert; typos in installed namespaces are actionable.
                    if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entity)) {
                        if (entity.getNamespace().equals("minecraft") || net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(entity.getNamespace()))
                            ScaleBrews.LOGGER.warn("Scale loot {}: unknown entity {}", file, entity);
                        return;
                    }
                    Set<Identifier> items = new HashSet<>(), tags = new HashSet<>();
                    for (var drop : json.getAsJsonArray("drops")) {
                        String value = drop.getAsString();
                        boolean tag = value.startsWith("#");
                        var id = Identifier.parse(tag ? value.substring(1) : value);
                        if (tag) tags.add(id);
                        else if (BuiltInRegistries.ITEM.containsKey(id)) items.add(id);
                        else ScaleBrews.LOGGER.warn("Scale loot {}: unknown item {} for {}", file, id, entity);
                    }
                    result.computeIfAbsent(entity, ignored -> new ArrayList<>()).add(new Rule(Set.copyOf(items), Set.copyOf(tags), json.has("entity_tag") ? json.get("entity_tag").getAsString() : null));
                } catch (Exception error) {
                    ScaleBrews.LOGGER.warn("Invalid scale loot {}: {}", file, error.getMessage());
                }
            });
        RULES.put(server, Map.copyOf(result));
    }

    public static boolean eligible(LivingEntity entity, ItemStack stack) {
        var server = entity.level().getServer();
        var rules = RULES.getOrDefault(server, Map.of()).get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if (rules == null) return false;
        var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return rules.stream().anyMatch(rule -> (rule.entityTag == null || entity.entityTags().contains(rule.entityTag))
            && (rule.items.contains(item) || rule.tags.stream().anyMatch(tag -> stack.is(TagKey.create(Registries.ITEM, tag)))));
    }

    public static void emit(LivingEntity entity, ItemStack stack, Consumer<ItemStack> output) {
        boolean previous = EMITTING.get();
        EMITTING.set(true);
        try { emitMaterial(entity, stack, output); }
        finally { if (previous) EMITTING.set(true); else EMITTING.remove(); }
    }
    private static void emitMaterial(LivingEntity entity, ItemStack stack, Consumer<ItemStack> output) {
        if (stack.isEmpty() || !eligible(entity, stack)) { output.accept(stack); return; }
        double scale = entity.getAttributeValue(Attributes.SCALE);
        if (!Double.isFinite(scale) || scale <= 0 || scale == 1) { output.accept(stack); return; }
        double expected = stack.getCount() * Math.pow(scale, 1.6);
        long count = (long)Math.floor(expected);
        if (entity.getRandom().nextDouble() < expected - count) count++;
        // Split only after scaling, preserving components and legal stack sizes.
        while (count > 0) {
            int size = (int)Math.min(count, stack.getMaxStackSize());
            output.accept(stack.copyWithCount(size));
            count -= size;
        }
    }
}
