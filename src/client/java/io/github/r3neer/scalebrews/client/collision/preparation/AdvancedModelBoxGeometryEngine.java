package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.BuiltInGeometryEngines;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Client/tooling implementation of the version-pinned AdvancedModelBox geometry family.
 *
 * <p>No Alex/Citadel class is linked at compile time. Sources are model-id keyed tooling
 * descriptors whose factories materialize fresh original model state only when explicitly
 * requested. This keeps the ordinary mod classpath independent from the external proof jars.</p>
 */
public final class AdvancedModelBoxGeometryEngine implements GeometryEngine {
    public static final String ALEX_2_1_9_DIALECT = AdvancedModelBoxGeometryExtractor.DIALECT;
    private static final int MAX_SOURCES = 4096;
    private static final AdvancedModelBoxGeometryEngine INSTANCE = new AdvancedModelBoxGeometryEngine();

    private final Map<Identifier, Source> sources = new TreeMap<>(Comparator.comparing(Identifier::toString));

    private AdvancedModelBoxGeometryEngine() {}

    /** Installs the singleton delegate into the common server-safe dispatcher. */
    public static void initialize() {
        BuiltInGeometryEngines.installAdvancedModelBoxPreparation(INSTANCE);
    }

    /**
     * Tooling-owned source descriptor. The exact supported dialect is deliberately pinned instead
     * of inferred from class names or whatever Citadel happens to be installed at runtime.
     */
    public record Source(String version, String dialect, Supplier<?> model, Supplier<Matrix4f> modelTransform) {
        public Source {
            if (!"2.1.9".equals(version) || !ALEX_2_1_9_DIALECT.equals(dialect) || model == null || modelTransform == null)
                throw new IllegalArgumentException("Unsupported/invalid AdvancedModelBox geometry source");
        }

        public Source(Supplier<?> model, Supplier<Matrix4f> modelTransform) {
            this("2.1.9", ALEX_2_1_9_DIALECT, model, modelTransform);
        }
    }

    /** Explicit evidence for primitives that exist in the renderer but intentionally have no collider. */
    public record Omission(String pieceId, String partId, String reason) {
        public Omission {
            if (pieceId == null || pieceId.isBlank() || partId == null || partId.isBlank()
                    || !"render_only_non_volumetric".equals(reason))
                throw new IllegalArgumentException("Invalid AdvancedModelBox omission diagnostic");
        }
    }

    /** Detailed tooling result; the ordinary GeometryEngine SPI exposes only {@link #geometry()}. */
    public record Preparation(ModelGeometry geometry, List<Omission> omissions) {
        public Preparation {
            Objects.requireNonNull(geometry, "geometry");
            omissions = List.copyOf(omissions);
        }
    }

    public static synchronized void registerSource(Identifier model, Source source) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(source, "source");
        if (INSTANCE.sources.containsKey(model))
            throw new IllegalArgumentException("Duplicate AdvancedModelBox geometry source: " + model);
        if (INSTANCE.sources.size() >= MAX_SOURCES)
            throw new IllegalArgumentException("Too many AdvancedModelBox geometry sources");
        INSTANCE.sources.put(model, source);
    }

    /** Deterministic read-only tooling view. No prepared geometry or external model instance is cached. */
    public static synchronized Map<Identifier, Source> sources() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(INSTANCE.sources));
    }

    @Override
    public Optional<ModelGeometry> prepare(Request request) {
        return prepareDetailed(request).map(Preparation::geometry);
    }

    /**
     * Prepares twice from fresh material and requires byte-for-byte DTO equality. This deliberately
     * turns a reused/mutated model supplier or unstable source transform into a preparation failure
     * instead of a hidden cache/history dependency.
     */
    public Optional<Preparation> prepareDetailed(Request request) {
        Objects.requireNonNull(request, "request");
        if (!request.parameters().isEmpty()) return Optional.empty();

        final Source source;
        synchronized (AdvancedModelBoxGeometryEngine.class) {
            source = sources.get(request.model());
        }
        if (source == null) return Optional.empty();

        try {
            Object firstModel = source.model().get();
            Object secondModel = source.model().get();
            if (firstModel == null || secondModel == null)
                throw new IllegalArgumentException("AdvancedModelBox source returned null model material");
            if (firstModel == secondModel)
                throw new IllegalArgumentException("AdvancedModelBox source factory reused mutable model identity");

            Matrix4f firstTransform = materializeTransform(source.modelTransform());
            Matrix4f secondTransform = materializeTransform(source.modelTransform());
            if (!matrixEquals(firstTransform, secondTransform))
                throw new IllegalArgumentException("AdvancedModelBox source transform is not reproducible");

            var first = AdvancedModelBoxGeometryExtractor.extract(request.model().toString(), source.version(), source.dialect(), firstModel);
            var second = AdvancedModelBoxGeometryExtractor.extract(request.model().toString(), source.version(), source.dialect(), secondModel);
            ModelGeometry firstGeometry = first.geometry().withModelTransform(firstTransform);
            ModelGeometry secondGeometry = second.geometry().withModelTransform(secondTransform);
            if (!firstGeometry.equals(secondGeometry) || !first.omissions().equals(second.omissions()))
                throw new IllegalArgumentException("AdvancedModelBox source factory is not deterministic");
            return Optional.of(new Preparation(firstGeometry, first.omissions()));
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Failed to prepare AdvancedModelBox geometry " + request.model(), failure);
        }
    }

    private static Matrix4f materializeTransform(Supplier<Matrix4f> supplier) {
        Matrix4f supplied = supplier.get();
        if (supplied == null) throw new IllegalArgumentException("AdvancedModelBox source returned null model transform");
        Matrix4f copy = new Matrix4f(supplied);
        float[] values = copy.get(new float[16]);
        for (float value : values) if (!Float.isFinite(value))
            throw new IllegalArgumentException("AdvancedModelBox model transform is non-finite");
        if (Math.abs(copy.m03()) > 1e-6f || Math.abs(copy.m13()) > 1e-6f || Math.abs(copy.m23()) > 1e-6f
                || Math.abs(copy.m33() - 1f) > 1e-6f || !Float.isFinite(copy.determinant()) || Math.abs(copy.determinant()) < 1e-12f)
            throw new IllegalArgumentException("AdvancedModelBox model transform is non-affine or singular");
        return copy;
    }

    private static boolean matrixEquals(Matrix4f left, Matrix4f right) {
        float[] a = left.get(new float[16]);
        float[] b = right.get(new float[16]);
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++)
            if (Float.floatToIntBits(a[i]) != Float.floatToIntBits(b[i])) return false;
        return true;
    }
}
