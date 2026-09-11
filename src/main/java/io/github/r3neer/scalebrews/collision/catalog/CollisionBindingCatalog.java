package io.github.r3neer.scalebrews.collision.catalog;

import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Immutable canonical binding index. G1 owns data selection; G3 will own the
 * prepared geometry/pose catalog lifecycle that consumes the selected binding.
 */
public final class CollisionBindingCatalog {
    public static final String DIRECTORY = "scalebrews/entity_collision";
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final Map<Identifier, List<CollisionBinding>> byEntity;

    public CollisionBindingCatalog(Collection<CollisionBinding> bindings) {
        if (bindings == null || bindings.size() > 4096) throw new IllegalArgumentException("Invalid collision binding catalog");
        Map<Identifier, List<CollisionBinding>> index = new TreeMap<>(Comparator.comparing(Identifier::toString));
        var selectors = new HashSet<String>();
        for (var binding : bindings) {
            if (binding == null) throw new IllegalArgumentException("Null collision binding");
            String selector = binding.entity() + "|" + binding.variant();
            if (!selectors.add(selector)) throw new IllegalArgumentException("Duplicate collision binding selector: " + selector);
            index.computeIfAbsent(binding.entity(), ignored -> new ArrayList<>()).add(binding);
        }
        index.replaceAll((entity, values) -> values.stream()
            .sorted(Comparator.<CollisionBinding>comparingInt(value -> value.variant().size()).reversed()
                .thenComparing(value -> value.variant().toString()))
            .toList());
        byEntity = Map.copyOf(index);
    }

    public static CollisionBindingCatalog load(ResourceManager resources) {
        var files = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        if (files.size() > 4096) throw new IllegalArgumentException("Too many canonical collision bindings");
        List<CollisionBinding> bindings = new ArrayList<>();
        long total = 0;
        for (var entry : files.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var text = new StringBuilder();
                char[] buffer = new char[8192];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_BYTES) throw new IllegalArgumentException("Collision binding catalog exceeds size limit");
                    text.append(buffer, 0, count);
                }
                var json = com.google.gson.JsonParser.parseString(text.toString());
                bindings.add(CollisionCodecs.BINDING.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (IOException | RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid canonical collision binding " + entry.getKey(), invalid);
            }
        }
        return new CollisionBindingCatalog(bindings);
    }

    /**
     * Variant selectors are declarative subsets. Most-specific match wins; an
     * equally-specific ambiguous match fails closed instead of depending on file order.
     */
    public Optional<CollisionBinding> resolve(Identifier entity, Map<String, String> variant) {
        if (entity == null || variant == null) return Optional.empty();
        var candidates = byEntity.getOrDefault(entity, List.of());
        CollisionBinding selected = null;
        int specificity = -1;
        for (var candidate : candidates) {
            if (!matches(candidate.variant(), variant)) continue;
            int next = candidate.variant().size();
            if (next < specificity) break;
            if (selected != null && next == specificity) return Optional.empty();
            selected = candidate;
            specificity = next;
        }
        return Optional.ofNullable(selected);
    }

    public Map<Identifier, List<CollisionBinding>> snapshot() { return byEntity; }

    private static boolean matches(Map<String, String> selector, Map<String, String> runtime) {
        for (var entry : selector.entrySet()) if (!entry.getValue().equals(runtime.get(entry.getKey()))) return false;
        return true;
    }
}
