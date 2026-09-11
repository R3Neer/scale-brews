package io.github.r3neer.scalebrews.collision.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/** Versioned policy independent from the released upper-surface platform engine. */
public record CollisionPolicy(int schemaVersion, Rule defaults, Map<String, Patch> categories,
                              Map<Identifier, Patch> supports) {
    public static final int SCHEMA_VERSION = 1;
    public static final CollisionPolicy DEFAULT = new CollisionPolicy(SCHEMA_VERSION,
        new Rule(true, .85, .6), Map.of(), Map.of());

    public CollisionPolicy {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported collision policy schema " + schemaVersion);
        if (defaults == null || categories == null || supports == null || categories.size() > 256 || supports.size() > 4096)
            throw new IllegalArgumentException("Invalid collision policy");
        var categoryCopy = new TreeMap<String, Patch>();
        categories.forEach((key, value) -> {
            if (key == null || !key.matches("[a-z0-9_.-]{1,64}") || value == null)
                throw new IllegalArgumentException("Invalid collision category policy");
            categoryCopy.put(key, value);
        });
        categories = Collections.unmodifiableMap(categoryCopy);
        var supportCopy = new LinkedHashMap<Identifier, Patch>();
        supports.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString())).forEach(entry -> {
            if (entry.getKey() == null || entry.getValue() == null) throw new IllegalArgumentException("Invalid support policy");
            supportCopy.put(entry.getKey(), entry.getValue());
        });
        supports = Collections.unmodifiableMap(supportCopy);
    }

    /** Deterministic precedence: global -> body category -> support type -> profile. */
    public Rule resolve(String category, Identifier support, Patch profile) {
        Rule resolved = defaults;
        if (category != null) resolved = categories.getOrDefault(category, Patch.EMPTY).apply(resolved);
        if (support != null) resolved = supports.getOrDefault(support, Patch.EMPTY).apply(resolved);
        return (profile == null ? Patch.EMPTY : profile).apply(resolved);
    }

    public record Rule(boolean enabled, double maxWidthRatio, double friction) {
        public Rule {
            if (!Double.isFinite(maxWidthRatio + friction) || maxWidthRatio <= 0 || maxWidthRatio > 1024 || friction < 0 || friction > 1)
                throw new IllegalArgumentException("Invalid collision rule");
        }
    }

    public record Patch(Optional<Boolean> enabled, Optional<Double> maxWidthRatio, Optional<Double> friction) {
        public static final Patch EMPTY = new Patch(Optional.empty(), Optional.empty(), Optional.empty());
        public Patch {
            if (enabled == null || maxWidthRatio == null || friction == null)
                throw new IllegalArgumentException("Invalid collision policy patch");
            maxWidthRatio.ifPresent(value -> {
                if (!Double.isFinite(value) || value <= 0 || value > 1024) throw new IllegalArgumentException("Invalid max width ratio");
            });
            friction.ifPresent(value -> {
                if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("Invalid friction");
            });
        }
        public Rule apply(Rule base) {
            return new Rule(enabled.orElse(base.enabled()), maxWidthRatio.orElse(base.maxWidthRatio()), friction.orElse(base.friction()));
        }
    }
}
