package io.github.r3neer.scalebrews.collision.api;

import io.github.r3neer.scalebrews.collision.api.spi.BodyAdapter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/** Public init-time registry for body adapters; registration never transfers physics ownership. */
public final class CollisionAdapters {
    private CollisionAdapters() {}

    private static final Map<Identifier, BodyAdapter> BODIES = new LinkedHashMap<>();

    public static synchronized void registerBody(Identifier entityType, BodyAdapter adapter) {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(adapter, "adapter");
        var category = Objects.requireNonNull(adapter.category(), "adapter category");
        if (!category.matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid collision body category: " + category);
        if (BODIES.putIfAbsent(entityType, adapter) != null)
            throw new IllegalArgumentException("Duplicate collision body adapter: " + entityType);
    }

    public static synchronized Optional<BodyAdapter> body(Identifier entityType) {
        return Optional.ofNullable(BODIES.get(entityType));
    }

    public static synchronized Map<Identifier, BodyAdapter> bodySnapshot() {
        var sorted = new TreeMap<Identifier, BodyAdapter>(Comparator.comparing(Identifier::toString));
        sorted.putAll(BODIES);
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }
}
