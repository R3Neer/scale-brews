package io.github.r3neer.scalebrews.collision.data;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;

/** Canonical entity/variant selection. No legacy plane or renderer object is part of this record. */
public record CollisionBinding(int schemaVersion, Identifier entity, Map<String, String> variant,
                               Geometry geometry, Pose pose, Identifier rootTransform,
                               CollisionPolicy.Patch policy, Set<String> excludedStates) {
    public static final int SCHEMA_VERSION = 1;

    public CollisionBinding {
        if (schemaVersion != SCHEMA_VERSION || entity == null || geometry == null || pose == null || rootTransform == null || policy == null
                || variant == null || excludedStates == null || variant.size() > 32 || excludedStates.size() > 64)
            throw new IllegalArgumentException("Invalid collision binding");
        variant = canonicalMap(variant, 64, 256, "variant");
        excludedStates = canonicalSet(excludedStates, "excluded state");
    }

    public record Geometry(Identifier engine, Identifier model, Map<String, String> parameters, AnatomyFilter filter) {
        public Geometry {
            if (engine == null || model == null || parameters == null || filter == null)
                throw new IllegalArgumentException("Invalid geometry selection");
            parameters = canonicalMap(parameters, 64, 1024, "geometry parameter");
        }
    }

    public record Pose(Identifier engine, Map<String, String> parameters, Set<String> channels) {
        public Pose {
            if (engine == null || parameters == null || channels == null || channels.size() > 64)
                throw new IllegalArgumentException("Invalid pose selection");
            parameters = canonicalMap(parameters, 64, 1024, "pose parameter");
            channels = canonicalSet(channels, "pose channel");
        }
    }

    private static Map<String, String> canonicalMap(Map<String, String> source, int keyLimit, int valueLimit, String label) {
        if (source.size() > 128) throw new IllegalArgumentException("Too many " + label + " values");
        var result = new TreeMap<String, String>();
        source.forEach((key, value) -> {
            if (key == null || !key.matches("[a-z0-9_.-]{1," + keyLimit + "}") || value == null || value.length() > valueLimit)
                throw new IllegalArgumentException("Invalid " + label);
            result.put(key, value);
        });
        return Collections.unmodifiableMap(result);
    }

    private static Set<String> canonicalSet(Set<String> source, String label) {
        var result = new TreeSet<String>();
        for (var value : source) {
            if (value == null || !value.matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid " + label);
            result.add(value);
        }
        return Collections.unmodifiableSet(result);
    }
}
