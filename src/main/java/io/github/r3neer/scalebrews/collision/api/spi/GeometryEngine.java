package io.github.r3neer.scalebrews.collision.api.spi;

import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/**
 * Reusable geometry-family extension point. An engine converts one technology's
 * version-pinned model input into Scale's renderer-independent ModelGeometry.
 */
@FunctionalInterface
public interface GeometryEngine {
    Optional<ModelGeometry> prepare(Request request);

    record Request(Identifier model, Map<String, String> parameters) {
        public Request {
            if (model == null || parameters == null || parameters.size() > 128)
                throw new IllegalArgumentException("Invalid geometry engine request");
            var copy = new TreeMap<String, String>();
            parameters.forEach((key, value) -> {
                if (key == null || !key.matches("[a-z0-9_.-]{1,64}") || value == null || value.length() > 1024)
                    throw new IllegalArgumentException("Invalid geometry engine parameter");
                copy.put(key, value);
            });
            parameters = Map.copyOf(copy);
        }

        public Request(Identifier model) { this(model, Map.of()); }
    }
}
