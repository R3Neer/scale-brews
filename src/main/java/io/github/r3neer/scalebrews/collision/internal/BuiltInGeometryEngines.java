package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Common-side identities for built-in geometry families.
 *
 * <p>The ModelPart implementation itself is client/tooling-only. Dedicated
 * servers still need to know the engine id while validating canonical bindings,
 * so common registers a neutral dispatcher that is empty until preparation
 * tooling installs a delegate. This class intentionally has no client imports.</p>
 */
public final class BuiltInGeometryEngines {
    public static final Identifier MODEL_PART = ScaleBrews.id("model_part");

    private static volatile GeometryEngine modelPartPreparation;
    private static final GeometryEngine MODEL_PART_DISPATCH = request -> {
        var delegate = modelPartPreparation;
        return delegate == null ? Optional.empty() : delegate.prepare(request);
    };

    private BuiltInGeometryEngines() {}

    /** Repeat-safe common bootstrap. The built-in id may not be stolen by another implementation. */
    public static synchronized void initialize() {
        var existing = CollisionEngines.geometry(MODEL_PART);
        if (existing.isEmpty()) CollisionEngines.registerGeometry(MODEL_PART, MODEL_PART_DISPATCH);
        else if (existing.orElseThrow() != MODEL_PART_DISPATCH)
            throw new IllegalStateException("Built-in ModelPart geometry engine id is already owned: " + MODEL_PART);
    }

    /**
     * Client/tooling installation seam expressed only in the neutral SPI type.
     * Installing the same singleton twice is harmless; replacing it is not.
     */
    public static synchronized void installModelPartPreparation(GeometryEngine engine) {
        Objects.requireNonNull(engine, "engine");
        if (modelPartPreparation != null && modelPartPreparation != engine)
            throw new IllegalStateException("ModelPart geometry preparation is already installed");
        modelPartPreparation = engine;
    }
}
