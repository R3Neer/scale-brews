package io.github.r3neer.scalebrews.collision.api;

import io.github.r3neer.scalebrews.collision.api.spi.BodyAdapter;
import io.github.r3neer.scalebrews.collision.api.spi.PoseChannelAdapter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Public init-time registry for reusable body and authoritative pose-channel adapters. */
public final class CollisionAdapters {
    private CollisionAdapters() {}

    private static final Map<Identifier, BodyAdapter> BODIES = new LinkedHashMap<>();
    private static final Map<Identifier, PoseChannelAdapter> POSE_CHANNELS = new LinkedHashMap<>();

    public static synchronized void registerBody(Identifier entityType, BodyAdapter adapter) {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(adapter, "adapter");
        if (BODIES.containsKey(entityType))
            throw new IllegalArgumentException("Duplicate collision body adapter: " + entityType);
        var category = Objects.requireNonNull(adapter.category(), "adapter category");
        if (!category.matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid collision body category: " + category);
        // Category is registration metadata, not mutable runtime state. Snapshot it once
        // while leaving behavioral hooks delegated to the registered adapter.
        BodyAdapter stable = new BodyAdapter() {
            @Override public String category() { return category; }
            @Override public boolean permits(Entity body) { return adapter.permits(body); }
            @Override public Vec3 transport(Entity body, Vec3 requested) { return adapter.transport(body, requested); }
        };
        BODIES.put(entityType, stable);
    }

    /**
     * Registers the sole authoritative scalar-channel provider for one entity type.
     * The adapter runs on the authority side and must not depend on renderer/client state.
     */
    public static synchronized void registerPoseChannels(Identifier entityType, PoseChannelAdapter adapter) {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(adapter, "adapter");
        if (POSE_CHANNELS.putIfAbsent(entityType, adapter) != null)
            throw new IllegalArgumentException("Duplicate pose channel adapter: " + entityType);
    }

    public static synchronized Optional<BodyAdapter> body(Identifier entityType) {
        return Optional.ofNullable(BODIES.get(entityType));
    }

    public static synchronized Optional<PoseChannelAdapter> poseChannels(Identifier entityType) {
        return Optional.ofNullable(POSE_CHANNELS.get(entityType));
    }

    public static synchronized Map<Identifier, BodyAdapter> bodySnapshot() {
        var sorted = new TreeMap<Identifier, BodyAdapter>(Comparator.comparing(Identifier::toString));
        sorted.putAll(BODIES);
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    public static synchronized Map<Identifier, PoseChannelAdapter> poseChannelSnapshot() {
        var sorted = new TreeMap<Identifier, PoseChannelAdapter>(Comparator.comparing(Identifier::toString));
        sorted.putAll(POSE_CHANNELS);
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }
}
