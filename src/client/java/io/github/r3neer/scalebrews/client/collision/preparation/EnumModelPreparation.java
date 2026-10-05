package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import io.github.r3neer.scalebrews.collision.internal.BuiltInVariantSources;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.variant.ModelAndTexture;

/**
 * Explicit PREPARATION of the original enum-to-single-model selector technology, not an executable collider.
 * Only original 26.2 class resources and the exact declared actor class are certified.
 * Transformed code, poses, renderer root transforms and physical geometry remain unproven.
 */
public final class EnumModelPreparation {
    public static final String TOOL_VERSION = "original-enum-selector-v1";
    public static final int MAX_KEYS = 256, MAX_ROWS = 256, MAX_UNIQUE_MODELS = 256;
    private EnumModelPreparation() {}

    public record Row(String sourceKey, String modelValue, String slot, String modelClass,
                      ModelPartStructure.Fingerprint initialStructure) {}
    public record Recipe(String tool, String rendererClass, String exactActorClass, String stateClass,
                         String sourceKey, String componentId, String variantGetter, String modelField,
                         Map<String, String> originalResources, List<Row> rows, int uniqueModels,
                         String canonicalText, String digest) {
        public Recipe {
            originalResources = Collections.unmodifiableMap(new TreeMap<>(originalResources));
            rows = List.copyOf(rows);
        }
    }
    public record Result(Recipe recipe, String absence) {
        public boolean admitted() { return recipe != null; }
    }

    /** Client-thread preparation only. A failed proof never returns partially admitted rows. */
    public static Result inspect(EntityRenderer<?, ?> renderer) {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("selector preparation requires client thread");
        if (renderer == null) return new Result(null, "original_selector:missing_renderer");
        try { return new Result(prepare(renderer), null); }
        catch (RuntimeException | ReflectiveOperationException failure) {
            String reason = failure.getMessage();
            return new Result(null, reason == null ? "original_selector:unavailable" : reason.substring(0, Math.min(256, reason.length())));
        }
    }

    private static Recipe prepare(EntityRenderer<?, ?> renderer) throws ReflectiveOperationException {
        Class<?> owner = renderer.getClass();
        OriginalEnumSelectorProof.originalOrigin(owner, LivingEntityRenderer.class);
        var programs = new OriginalModelPrograms(owner.getClassLoader());
        // Original platform dependencies, not supported entity types. The merged and
        // Loom split/access-widened resources were audited for whole-program equivalence;
        // resource hashes remain exact and every other resource version is unavailable.
        programs.pinned(LivingEntityRenderer.class, "cf44b4e0b89d04e5ea4f56049443b4304edda55b4ebad6378cf486738251629b",
            "d3c5792a8ea3c6ebc74bfdaee0f81cb994755ba86a23e9f3778b9b5f574550c9");
        programs.pinned(EntityRenderer.class, "99c26f6552f5524144ba34093d241cecb818a745531d7592657d99a23ef97011",
            "a37ef501fb2ffe5386fdfbe7b3c929aca8d4dacda4cb66db17b8843aa3cdaad1");
        programs.pinned(ModelAndTexture.class, "a3a9be7dd6a78a3de2bb33fe5a64de3293e5b8e5bc2f5e97d90c71da38fc4832");
        var selector = OriginalEnumSelectorProof.selector(programs, owner, OriginalEnumSelectorProof.Family.ENUM_ONLY);
        var inputs = OriginalEnumSelectorProof.inputs(programs, owner, selector);
        OriginalEnumSelectorProof.bridges(programs, owner, inputs.actor(), selector.state());
        String sourceKey = CollisionVariantSources.modelComponentKey(BuiltInRegistries.DATA_COMPONENT_TYPE
            .getKey((DataComponentType<?>) inputs.component().get(null)));
        BuiltInVariantSources.initialize(); // Idempotent ownership check, PREPARATION only.
        var source = CollisionVariantSources.snapshot().get(sourceKey);
        if (source == null) throw OriginalModelPrograms.absent("source_missing");
        var enumType = OriginalEnumSelectorProof.modelEnum(selector.variant(), selector.accessor());
        programs.model(enumType);
        programs.model(selector.variant());
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0 || constants.length > MAX_KEYS)
            throw OriginalModelPrograms.absent("enum_domain_limit");
        var names = new TreeSet<String>();
        var capturedNames = new IdentityHashMap<Object, String>();
        for (Object constant : constants) {
            String name = ((StringRepresentable)constant).getSerializedName();
            if (name == null || name.isEmpty() || name.length() > CollisionVariantSources.MAX_VALUE_LENGTH || !names.add(name))
                throw OriginalModelPrograms.absent("enum_domain_invalid");
            capturedNames.put(constant, name);
        }
        if (!source.values().equals(names)) throw OriginalModelPrograms.absent("source_domain_mismatch");
        if (!selector.map().trySetAccessible()) throw OriginalModelPrograms.absent("map_inaccessible");
        Object observed = selector.map().get(renderer);
        if (!(observed instanceof Map<?, ?> map) || map.size() != constants.length || map.size() > MAX_KEYS)
            throw OriginalModelPrograms.absent("map_domain");
        int keys = 0;
        for (Object key : map.keySet()) {
            if (++keys > MAX_KEYS || !(key instanceof Enum<?> value) || value.getDeclaringClass() != enumType)
                throw OriginalModelPrograms.absent("map_enum_owner");
        }
        if (keys != constants.length) throw OriginalModelPrograms.absent("map_domain");
        var structures = new IdentityHashMap<Model<?>, ModelPartStructure.Fingerprint>();
        var rows = new ArrayList<Row>();
        String mapSlot = selector.map().getDeclaringClass().getName() + "#" + selector.map().getName();
        for (Object constant : constants) {
            if (!(map.get(constant) instanceof EntityModel<?> model))
                throw OriginalModelPrograms.absent("model_missing");
            String value = capturedNames.get(constant);
            append(rows, structures, sourceKey, value, mapSlot, model);
        }
        rows.sort(java.util.Comparator.comparing(Row::modelValue));
        String componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey((DataComponentType<?>)inputs.component().get(null)).toString();
        StringBuilder canonical = new StringBuilder(TOOL_VERSION + "|original-resources-only|");
        for (String text : List.of(owner.getName(), inputs.actor().getName(), selector.state().getName(), sourceKey,
            componentId, inputs.getter(), mapSlot)) ModelPartStructure.frame(canonical, text);
        new TreeMap<>(programs.digests()).forEach((resource, digest) -> {
            ModelPartStructure.frame(canonical, resource); ModelPartStructure.frame(canonical, digest);
        });
        for (var row : rows) for (String text : List.of(row.modelValue, row.slot, row.modelClass,
            row.initialStructure.digest(), Integer.toString(row.initialStructure.parts()),
            Integer.toString(row.initialStructure.cubes()), Integer.toString(row.initialStructure.solidPieces())))
            ModelPartStructure.frame(canonical, text);
        String serialized = canonical.toString();
        return new Recipe(TOOL_VERSION, owner.getName(), inputs.actor().getName(), selector.state().getName(), sourceKey,
            componentId, inputs.getter(), mapSlot, programs.digests(), rows, structures.size(), serialized, ModelPartStructure.hash(serialized));
    }

    private static void append(List<Row> rows, IdentityHashMap<Model<?>, ModelPartStructure.Fingerprint> structures,
                               String key, String value, String slot, Model<?> model) {
        if (!(model instanceof EntityModel<?>) || rows.size() >= MAX_ROWS)
            throw OriginalModelPrograms.absent("model_missing_or_row_limit");
        var fingerprint = structures.get(model);
        if (fingerprint == null) {
            if (structures.size() >= MAX_UNIQUE_MODELS) throw OriginalModelPrograms.absent("model_limit");
            fingerprint = ModelPartStructure.inspect(model.root());
            structures.put(model, fingerprint);
        }
        rows.add(new Row(key, value, slot + "[" + value + "]", model.getClass().getName(), fingerprint));
    }

}
