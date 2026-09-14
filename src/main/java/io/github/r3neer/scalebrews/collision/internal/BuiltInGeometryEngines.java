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
 * <p>Concrete model implementations are client/tooling-only. Dedicated servers
 * still need stable family ids while validating canonical bindings, so common
 * registers neutral dispatchers that remain empty until preparation tooling
 * installs their delegates. This class intentionally has no client imports.</p>
 */
public final class BuiltInGeometryEngines {
    public static final Identifier MODEL_PART = ScaleBrews.id("model_part");
    public static final Identifier ADVANCED_MODEL_BOX = ScaleBrews.id("advanced_model_box");

    private static volatile GeometryEngine modelPartPreparation;
    private static volatile GeometryEngine advancedModelBoxPreparation;

    private static final GeometryEngine MODEL_PART_DISPATCH = request -> {
        var delegate = modelPartPreparation;
        return delegate == null ? Optional.empty() : delegate.prepare(request);
    };
    private static final GeometryEngine ADVANCED_MODEL_BOX_DISPATCH = request -> {
        var delegate = advancedModelBoxPreparation;
        return delegate == null ? Optional.empty() : delegate.prepare(request);
    };

    private BuiltInGeometryEngines() {}

    /** Repeat-safe common bootstrap. Built-in ids may not be stolen by another implementation. */
    public static synchronized void initialize() {
        registerBuiltIn(MODEL_PART, MODEL_PART_DISPATCH, "ModelPart");
        registerBuiltIn(ADVANCED_MODEL_BOX, ADVANCED_MODEL_BOX_DISPATCH, "advanced model box");
    }

    private static void registerBuiltIn(Identifier id, GeometryEngine dispatcher, String label) {
        var existing = CollisionEngines.geometry(id);
        if (existing.isEmpty()) CollisionEngines.registerGeometry(id, dispatcher);
        else if (existing.orElseThrow() != dispatcher)
            throw new IllegalStateException("Built-in " + label + " geometry engine id is already owned: " + id);
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

    /** Same ownership rule as ModelPart, for the optional external-model preparation family. */
    public static synchronized void installAdvancedModelBoxPreparation(GeometryEngine engine) {
        Objects.requireNonNull(engine, "engine");
        if (advancedModelBoxPreparation != null && advancedModelBoxPreparation != engine)
            throw new IllegalStateException("Advanced model-box geometry preparation is already installed");
        advancedModelBoxPreparation = engine;
    }
}
