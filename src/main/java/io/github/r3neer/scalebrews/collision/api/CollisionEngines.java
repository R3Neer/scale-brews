package io.github.r3neer.scalebrews.collision.api;

import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Public init-time registry for reusable collision engines. Species selection is
 * data-driven; registering an engine never registers a concrete mob binding.
 */
public final class CollisionEngines {
    private CollisionEngines() {}

    private static final Map<Identifier, GeometryEngine> GEOMETRY = new LinkedHashMap<>();
    private static final Map<Identifier, PoseEngine> POSE = new LinkedHashMap<>();
    private static final Map<Identifier, RootTransformProvider> ROOTS = new LinkedHashMap<>();

    public static synchronized void registerGeometry(Identifier id, GeometryEngine engine) {
        register(GEOMETRY, id, engine, "geometry");
    }

    public static synchronized void registerPose(Identifier id, PoseEngine engine) {
        register(POSE, id, engine, "pose");
    }

    public static synchronized void registerRootTransform(Identifier id, RootTransformProvider provider) {
        register(ROOTS, id, provider, "root transform");
    }

    public static synchronized Optional<GeometryEngine> geometry(Identifier id) { return Optional.ofNullable(GEOMETRY.get(id)); }
    public static synchronized Optional<PoseEngine> pose(Identifier id) { return Optional.ofNullable(POSE.get(id)); }
    public static synchronized Optional<RootTransformProvider> rootTransform(Identifier id) { return Optional.ofNullable(ROOTS.get(id)); }

    public static synchronized Map<Identifier, GeometryEngine> geometrySnapshot() { return Map.copyOf(GEOMETRY); }
    public static synchronized Map<Identifier, PoseEngine> poseSnapshot() { return Map.copyOf(POSE); }
    public static synchronized Map<Identifier, RootTransformProvider> rootTransformSnapshot() { return Map.copyOf(ROOTS); }

    private static <T> void register(Map<Identifier, T> registry, Identifier id, T value, String kind) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
        if (registry.putIfAbsent(id, value) != null)
            throw new IllegalArgumentException("Duplicate collision " + kind + " engine/provider: " + id);
    }
}
