package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import io.github.r3neer.scalebrews.collision.api.spi.VariantSource;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.variant.ModelAndTexture;

/** Common/server-safe structural families, independent of concrete entity types. */
public final class BuiltInVariantSources {
    private static final VariantSource AGE = entity -> entity.isBaby() ? "baby" : "adult";
    private static final Map<String, CollisionVariantSources.Source> BUILT_INS = discover();
    private BuiltInVariantSources() {}

    public static synchronized void initialize() {
        var registered = CollisionVariantSources.snapshot();
        for (var entry : BUILT_INS.entrySet()) {
            var existing = registered.get(entry.getKey());
            var source = entry.getValue();
            if (existing == null) CollisionVariantSources.register(entry.getKey(), source.values(), source.reader());
            else if (existing.reader() != source.reader())
                throw new IllegalStateException("Built-in variant source was claimed by another owner: " + entry.getKey());
        }
    }

    /** Init-time introspection of component technology; no species dispatch or runtime reflection. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, CollisionVariantSources.Source> discover() {
        var sources = new TreeMap<String, CollisionVariantSources.Source>();
        sources.put("age", new CollisionVariantSources.Source(Set.of("adult", "baby"), AGE));
        for (var field : DataComponents.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != DataComponentType.class
                || !(field.getGenericType() instanceof ParameterizedType type)) continue;
            var argument = type.getActualTypeArguments()[0];
            Class<?> valueType;
            boolean modelProjection;
            if (argument instanceof Class<?> direct && direct.isEnum()
                && StringRepresentable.class.isAssignableFrom(direct)) {
                valueType = direct;
                modelProjection = false;
            } else if (argument instanceof ParameterizedType holder && holder.getRawType() == Holder.class
                && holder.getActualTypeArguments()[0] instanceof Class<?> record && record.isRecord()
                && hasModelField(record)) {
                valueType = record;
                modelProjection = true;
            } else continue;
            try {
                var component = (DataComponentType<?>) field.get(null);
                var id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component);
                if (id == null) throw new IllegalStateException("Unregistered variant component: " + field.getName());
                String key = modelProjection ? CollisionVariantSources.modelComponentKey(id) : CollisionVariantSources.componentKey(id);
                var source = modelProjection
                    ? CollisionVariantSources.holderModelComponent((DataComponentType) component, (Class) valueType)
                    : CollisionVariantSources.enumComponent((DataComponentType) component, (Class) valueType);
                if (sources.putIfAbsent(key, source) != null) throw new IllegalStateException("Duplicate component trait: " + id);
            } catch (IllegalAccessException inaccessible) {
                throw new IllegalStateException("Cannot prepare component source: " + field.getName(), inaccessible);
            }
        }
        return Collections.unmodifiableMap(sources);
    }

    private static boolean hasModelField(Class<?> record) {
        for (var field : record.getRecordComponents()) if (field.getType() == ModelAndTexture.class) return true;
        return false;
    }
}
