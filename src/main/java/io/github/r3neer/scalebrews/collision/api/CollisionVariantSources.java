package io.github.r3neer.scalebrews.collision.api;

import io.github.r3neer.scalebrews.collision.api.spi.VariantSource;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Init-time trait-family registry. JSON selectors choose keys and values, never mob classes. */
public final class CollisionVariantSources {
    public static final int MAX_SOURCES = 1024, MAX_VALUES = 256, MAX_VALUE_LENGTH = 256;
    private static final Map<String, Source> SOURCES = new TreeMap<>();
    private CollisionVariantSources() {}

    public record Source(Set<String> values, VariantSource reader) {
        public Source {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(reader, "reader");
            if (values.isEmpty() || values.size() > MAX_VALUES) throw new IllegalArgumentException("Invalid variant source domain");
            var copy = new TreeSet<String>();
            for (var value : values) {
                if (!validValue(value)) throw new IllegalArgumentException("Invalid variant source value");
                copy.add(value);
            }
            values = Collections.unmodifiableSet(copy);
        }
    }

    public static synchronized void register(String key, Set<String> values, VariantSource reader) {
        if (key == null || !key.matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid variant source key");
        if (SOURCES.containsKey(key)) throw new IllegalArgumentException("Duplicate variant source " + key);
        if (SOURCES.size() >= MAX_SOURCES) throw new IllegalArgumentException("Too many variant sources");
        SOURCES.put(key, new Source(values, reader));
    }

    /** PREPARATION only. Readers are captured by a revision; registration cannot mutate a live plan. */
    public static synchronized Map<String, Source> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(SOURCES));
    }

    private static boolean validValue(String value) {
        if (value == null || value.length() > MAX_VALUE_LENGTH) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }
}
