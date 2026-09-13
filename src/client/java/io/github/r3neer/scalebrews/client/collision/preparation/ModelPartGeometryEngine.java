package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Client/tooling implementation of the generic {@code scalebrews:model_part}
 * geometry family. Sources materialize fresh original ModelPart trees; the
 * common engine request remains renderer-independent.
 */
public final class ModelPartGeometryEngine implements GeometryEngine {
    private static final int MAX_SOURCES = 4096;
    private static final ModelPartGeometryEngine INSTANCE = new ModelPartGeometryEngine();

    private final Map<Identifier, Source> sources = new TreeMap<>(Comparator.comparing(Identifier::toString));

    private ModelPartGeometryEngine() {}

    /** Installs the singleton delegate into the common server-safe dispatcher. */
    public static void initialize() {
        BuiltInGeometryEngines.installModelPartPreparation(INSTANCE);
    }

    /**
     * Preparation metadata owned by tooling, not by runtime physics.
     * Both suppliers must return fresh/copyable state for deterministic export.
     */
    public record Source(String version, Supplier<ModelPart> root, Supplier<Matrix4f> modelTransform) {
        public Source {
            if (version == null || version.isBlank() || version.length() > 128 || root == null || modelTransform == null)
                throw new IllegalArgumentException("Invalid ModelPart geometry source");
        }

        /** Standard vanilla living-model transform; callers may provide an exact renderer-derived transform instead. */
        public Source(String version, Supplier<ModelPart> root) {
            this(version, root, () -> new Matrix4f().scaling(-1, -1, 1).translate(0, -1.501f, 0));
        }
    }

    public static synchronized void registerSource(Identifier model, Source source) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(source, "source");
        if (INSTANCE.sources.size() >= MAX_SOURCES && !INSTANCE.sources.containsKey(model))
            throw new IllegalArgumentException("Too many ModelPart geometry sources");
        if (INSTANCE.sources.putIfAbsent(model, source) != null)
            throw new IllegalArgumentException("Duplicate ModelPart geometry source: " + model);
    }

    /** Deterministic tooling view; values are immutable source descriptors, not prepared geometry caches. */
    public static synchronized Map<Identifier, Source> sources() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(INSTANCE.sources));
    }

    @Override
    public Optional<ModelGeometry> prepare(Request request) {
        Objects.requireNonNull(request, "request");
        // S17 deliberately defines no procedural request parameters. Unknown
        // parameters therefore mean this family cannot prepare the request.
        if (!request.parameters().isEmpty()) return Optional.empty();

        final Source source;
        synchronized (ModelPartGeometryEngine.class) {
            source = sources.get(request.model());
        }
        if (source == null) return Optional.empty();

        try {
            var root = source.root().get();
            var transform = source.modelTransform().get();
            if (root == null || transform == null)
                throw new IllegalArgumentException("ModelPart source returned null material");
            var geometry = GeometryExtractor.vanilla(request.model().toString(), source.version(), root, java.util.Set.of());
            return Optional.of(geometry.withModelTransform(new Matrix4f(transform)));
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Failed to prepare ModelPart geometry " + request.model(), failure);
        }
    }
}
