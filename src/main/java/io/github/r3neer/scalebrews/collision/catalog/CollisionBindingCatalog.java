package io.github.r3neer.scalebrews.collision.catalog;

import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.data.CollisionBinding;
import io.github.r3neer.scalebrews.collision.data.CollisionCodecs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

    private record Selector(Identifier entity, Map<String, String> variant) {}

    private final Map<Identifier, List<CollisionBinding>> byEntity;

    public CollisionBindingCatalog(Collection<CollisionBinding> bindings) {
        if (bindings == null || bindings.size() > 4096) throw new IllegalArgumentException("Invalid collision binding catalog");
        Map<Identifier, List<CollisionBinding>> index = new TreeMap<>(Comparator.comparing(Identifier::toString));
        var selectors = new HashSet<Selector>();
        for (var binding : bindings) {
            if (binding == null) throw new IllegalArgumentException("Null collision binding");
            validateRegisteredEngines(binding);
            var selector = new Selector(binding.entity(), binding.variant());
            if (!selectors.add(selector)) throw new IllegalArgumentException("Duplicate collision binding selector for " + binding.entity());
            index.computeIfAbsent(binding.entity(), ignored -> new ArrayList<>()).add(binding);
        }
        index.replaceAll((entity, values) -> values.stream()
            .sorted(Comparator.<CollisionBinding>comparingInt(value -> value.variant().size()).reversed()
                .thenComparing(CollisionBinding::variant, CollisionBindingCatalog::compareVariants))
            .toList());
        byEntity = Collections.unmodifiableMap(new LinkedHashMap<>(index));
    }

    public static CollisionBindingCatalog load(ResourceManager resources) {
        var files = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        if (files.size() > 4096) throw new IllegalArgumentException("Too many canonical collision bindings");
        List<CollisionBinding> bindings = new ArrayList<>();
        long total = 0;
        for (var entry : files.entrySet()) {
            try (var input = entry.getValue().open()) {
                byte[] bytes = readBounded(input, MAX_BYTES - total);
                total += bytes.length;
                var json = com.google.gson.JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
                bindings.add(CollisionCodecs.BINDING.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (IOException | RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid canonical collision binding " + entry.getKey(), invalid);
            }
        }
        return new CollisionBindingCatalog(bindings);
    }

    /**
     * Reads one resource against the remaining aggregate byte budget. Package
     * visibility keeps the exact byte-boundary oracle testable without widening API.
     */
    static byte[] readBounded(InputStream input, long remainingBytes) throws IOException {
        if (input == null || remainingBytes < 0) throw new IllegalArgumentException("Invalid collision binding byte budget");
        var output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > remainingBytes) throw new IllegalArgumentException("Collision binding catalog exceeds size limit");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
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

    /** Deterministic entity and per-entity binding order for tooling and later catalog serialization. */
    public Map<Identifier, List<CollisionBinding>> snapshot() { return byEntity; }

    private static void validateRegisteredEngines(CollisionBinding binding) {
        if (CollisionEngines.geometry(binding.geometry().engine()).isEmpty())
            throw new IllegalArgumentException("Missing collision geometry engine " + binding.geometry().engine() + " for " + binding.entity());
        if (CollisionEngines.pose(binding.pose().engine()).isEmpty())
            throw new IllegalArgumentException("Missing collision pose engine " + binding.pose().engine() + " for " + binding.entity());
        if (CollisionEngines.rootTransform(binding.rootTransform()).isEmpty())
            throw new IllegalArgumentException("Missing collision root transform provider " + binding.rootTransform() + " for " + binding.entity());
    }

    private static int compareVariants(Map<String, String> left, Map<String, String> right) {
        var leftEntries = new TreeMap<>(left).entrySet().iterator();
        var rightEntries = new TreeMap<>(right).entrySet().iterator();
        while (leftEntries.hasNext() && rightEntries.hasNext()) {
            var a = leftEntries.next();
            var b = rightEntries.next();
            int key = a.getKey().compareTo(b.getKey());
            if (key != 0) return key;
            int value = a.getValue().compareTo(b.getValue());
            if (value != 0) return value;
        }
        return Integer.compare(left.size(), right.size());
    }

    private static boolean matches(Map<String, String> selector, Map<String, String> runtime) {
        for (var entry : selector.entrySet()) if (!entry.getValue().equals(runtime.get(entry.getKey()))) return false;
        return true;
    }
}
