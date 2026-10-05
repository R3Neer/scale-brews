package io.github.r3neer.scalebrews.collision.api;

import io.github.r3neer.scalebrews.collision.api.spi.VariantSource;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

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

    /** PREPARATION only: preserve component IDs without ambiguity in the bounded trait-key vocabulary. */
    public static String componentKey(Identifier id) {
        Objects.requireNonNull(id, "id");
        String key = "component." + componentSegment(id.getNamespace()) + "." + componentSegment(id.getPath());
        if (!key.matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Component trait key exceeds vocabulary: " + id);
        return key;
    }

    private static String componentSegment(String value) {
        return value.replace("_", "_5f").replace(".", "_2e").replace("/", "_2f");
    }

    /**
     * PREPARATION factory for a common enum-component family. Names are captured once;
     * the local reader returns only a prepared name from the exact declaring enum.
     * Register the returned source with the existing init-time registry.
     */
    public static <T extends Enum<T> & StringRepresentable> Source enumComponent(DataComponentType<T> component, Class<T> enumType) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(enumType, "enumType");
        if (!enumType.isEnum() || !StringRepresentable.class.isAssignableFrom(enumType))
            throw new IllegalArgumentException("Component domain must be a serialized enum");
        T[] constants = enumType.getEnumConstants();
        if (constants.length == 0 || constants.length > MAX_VALUES) throw new IllegalArgumentException("Invalid component enum domain");
        var values = new TreeSet<String>();
        var names = new String[constants.length];
        for (int i = 0; i < constants.length; i++) {
            String name = constants[i].getSerializedName();
            if (!validValue(name) || !values.add(name)) throw new IllegalArgumentException("Invalid or duplicate component enum name");
            names[i] = name;
        }
        return new Source(values, entity -> {
            Object observed = entity.get(component);
            if (!(observed instanceof Enum<?> value) || value.getDeclaringClass() != enumType) return null;
            return names[value.ordinal()];
        });
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
