package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import io.github.r3neer.scalebrews.collision.internal.BuiltInVariantSources;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.item.ItemStack;

/**
 * Explicit PREPARATION of one original selector technology, not an executable collider.
 * Only original 26.2 class resources and the exact declared actor class are certified.
 * Transformed code, poses, renderer root transforms and physical geometry remain unproven.
 */
public final class EnumAgeModelPreparation {
    public static final String TOOL_VERSION = "original-enum-age-selector-v1";
    public static final int MAX_KEYS = 256, MAX_ROWS = 512, MAX_UNIQUE_MODELS = 512;
    private static final String MT = "net/minecraft/world/entity/variant/ModelAndTexture";
    private static final String HOLDER = "net/minecraft/core/Holder";
    private static final String PAIR = "net/minecraft/client/model/AdultAndBabyModelPair";
    private static final String MOB = "net/minecraft/client/renderer/entity/MobRenderer";
    private static final String SUBMIT_TAIL = "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";
    private static final String BASE_SUBMIT = "(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;" + SUBMIT_TAIL;
    private static final String BASE_EXTRACT = "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V";
    private static final String COMPONENT_GET = "(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;";
    private static final String CAST_COMPONENT = "(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String HOLDER_GET = "()Lnet/minecraft/core/Holder;";
    private EnumAgeModelPreparation() {}

    public record Row(String sourceKey, String modelValue, String age, String slot, String modelClass,
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

    private record Selector(Class<?> state, Class<?> variant, Field map, Field stateVariant, String accessor) {}
    private record Inputs(Class<?> actor, String getter, Field component) {}

    private static Recipe prepare(EntityRenderer<?, ?> renderer) throws ReflectiveOperationException {
        Class<?> owner = renderer.getClass();
        originalOrigin(owner, LivingEntityRenderer.class);
        var programs = new OriginalModelPrograms(owner.getClassLoader());
        // Original platform dependencies, not supported entity types. The merged and
        // Loom split/access-widened resources were audited for whole-program equivalence;
        // resource hashes remain exact and every other resource version is unavailable.
        programs.pinned(LivingEntityRenderer.class, "cf44b4e0b89d04e5ea4f56049443b4304edda55b4ebad6378cf486738251629b",
            "d3c5792a8ea3c6ebc74bfdaee0f81cb994755ba86a23e9f3778b9b5f574550c9");
        programs.pinned(EntityRenderer.class, "99c26f6552f5524144ba34093d241cecb818a745531d7592657d99a23ef97011",
            "a37ef501fb2ffe5386fdfbe7b3c929aca8d4dacda4cb66db17b8843aa3cdaad1");
        programs.pinned(AdultAndBabyModelPair.class, "916247c96b3bf02b816c9ea10f36ab91c1cd956a88df1445d7bfce37f1002bfe",
            "f9c371601aecca00d9d3ab1aa1dd886b53f9922192e7baf5e55f47bdebe5ca7d");
        programs.pinned(ModelAndTexture.class, "a3a9be7dd6a78a3de2bb33fe5a64de3293e5b8e5bc2f5e97d90c71da38fc4832");
        var selector = selector(programs, owner);
        var inputs = inputs(programs, owner, selector);
        bridges(programs, owner, inputs.actor, selector.state);
        String sourceKey = CollisionVariantSources.modelComponentKey(BuiltInRegistries.DATA_COMPONENT_TYPE
            .getKey((DataComponentType<?>) inputs.component.get(null)));
        BuiltInVariantSources.initialize(); // Idempotent ownership check, PREPARATION only.
        var source = CollisionVariantSources.snapshot().get(sourceKey);
        if (source == null) throw OriginalModelPrograms.absent("source_missing");
        var enumType = modelEnum(selector.variant, selector.accessor);
        programs.model(enumType);
        programs.model(selector.variant);
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
        if (!selector.map.trySetAccessible()) throw OriginalModelPrograms.absent("map_inaccessible");
        Object observed = selector.map.get(renderer);
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
        String mapSlot = selector.map.getDeclaringClass().getName() + "#" + selector.map.getName();
        for (Object constant : constants) {
            if (!(map.get(constant) instanceof AdultAndBabyModelPair<?> pair))
                throw OriginalModelPrograms.absent("model_pair_missing");
            String value = capturedNames.get(constant);
            append(rows, structures, sourceKey, value, "adult", mapSlot, pair.adultModel());
            append(rows, structures, sourceKey, value, "baby", mapSlot, pair.babyModel());
        }
        rows.sort(java.util.Comparator.comparing(Row::modelValue).thenComparing(Row::age));
        String componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey((DataComponentType<?>)inputs.component.get(null)).toString();
        StringBuilder canonical = new StringBuilder(TOOL_VERSION + "|original-resources-only|");
        for (String text : List.of(owner.getName(), inputs.actor.getName(), selector.state.getName(), sourceKey,
            componentId, inputs.getter, mapSlot)) ModelPartStructure.frame(canonical, text);
        new TreeMap<>(programs.digests()).forEach((resource, digest) -> {
            ModelPartStructure.frame(canonical, resource); ModelPartStructure.frame(canonical, digest);
        });
        for (var row : rows) for (String text : List.of(row.modelValue, row.age, row.slot, row.modelClass,
            row.initialStructure.digest(), Integer.toString(row.initialStructure.parts()),
            Integer.toString(row.initialStructure.cubes()), Integer.toString(row.initialStructure.solidPieces())))
            ModelPartStructure.frame(canonical, text);
        String serialized = canonical.toString();
        return new Recipe(TOOL_VERSION, owner.getName(), inputs.actor.getName(), selector.state.getName(), sourceKey,
            componentId, inputs.getter, mapSlot, programs.digests(), rows, structures.size(), serialized, ModelPartStructure.hash(serialized));
    }

    private static void append(List<Row> rows, IdentityHashMap<Model<?>, ModelPartStructure.Fingerprint> structures,
                               String key, String value, String age, String slot, Model<?> model) {
        if (!(model instanceof EntityModel<?>) || rows.size() >= MAX_ROWS)
            throw OriginalModelPrograms.absent("model_missing_or_row_limit");
        var fingerprint = structures.get(model);
        if (fingerprint == null) {
            if (structures.size() >= MAX_UNIQUE_MODELS) throw OriginalModelPrograms.absent("model_limit");
            fingerprint = ModelPartStructure.inspect(model.root());
            structures.put(model, fingerprint);
        }
        rows.add(new Row(key, value, age, slot + "[" + value + "]." + age + "Model", model.getClass().getName(), fingerprint));
    }

    private static Selector selector(OriginalModelPrograms programs, Class<?> owner) {
        java.lang.classfile.MethodModel selected = null;
        Class<?> state = null;
        for (var method : programs.model(owner).methods()) {
            if (!method.methodName().stringValue().equals("submit") || method.flags().has(java.lang.reflect.AccessFlag.BRIDGE)
                || method.flags().has(java.lang.reflect.AccessFlag.SYNTHETIC) || method.flags().has(java.lang.reflect.AccessFlag.STATIC)) continue;
            var signature = MethodType.fromMethodDescriptorString(method.methodType().stringValue(), owner.getClassLoader());
            if (signature.returnType() != void.class || signature.parameterCount() != 4
                || !LivingEntityRenderState.class.isAssignableFrom(signature.parameterType(0))) continue;
            if (!method.methodType().stringValue().equals("(" + signature.parameterType(0).descriptorString() + SUBMIT_TAIL)
                || selected != null) throw OriginalModelPrograms.absent("submit_signature_ambiguous");
            selected = method; state = signature.parameterType(0);
        }
        if (selected == null) throw OriginalModelPrograms.absent("submit_family_missing");
        var code = new OriginalModelPrograms.Code(selected);
        code.load(1);
        Field stateVariant = programs.resolveField(state, code.field(Opcode.GETFIELD));
        Class<?> variant = stateVariant.getType();
        if (!variant.isRecord() || Modifier.isStatic(stateVariant.getModifiers())) throw OriginalModelPrograms.absent("variant_record_missing");
        int present = code.branch(Opcode.IFNONNULL);
        code.op(Opcode.RETURN);
        if (present != code.next) throw OriginalModelPrograms.absent("null_guard_target");
        code.load(0); code.load(0);
        Field map = programs.resolveField(owner, code.field(Opcode.GETFIELD));
        if (map.getType() != Map.class || Modifier.isStatic(map.getModifiers())) throw OriginalModelPrograms.absent("map_field");
        code.load(1);
        if (!stateVariant.equals(programs.resolveField(state, code.field(Opcode.GETFIELD)))) throw OriginalModelPrograms.absent("variant_alias");
        var accessor = code.virtual();
        if (!accessor.owner().asInternalName().equals(variant.getName().replace('.', '/'))
            || !accessor.type().stringValue().equals("()L" + MT + ";")) throw OriginalModelPrograms.absent("variant_accessor");
        var recordAccessor = new OriginalModelPrograms.Code(programs.declared(variant, accessor.name().stringValue(), accessor.type().stringValue()));
        recordAccessor.load(0);
        Field recordField = programs.resolveField(variant, recordAccessor.field(Opcode.GETFIELD));
        if (recordField.getDeclaringClass() != variant || recordField.getType() != ModelAndTexture.class
            || !recordField.getName().equals(accessor.name().stringValue()) || !Modifier.isFinal(recordField.getModifiers())
            || Modifier.isStatic(recordField.getModifiers())) throw OriginalModelPrograms.absent("variant_accessor_program");
        recordAccessor.op(Opcode.ARETURN); recordAccessor.end();
        code.call(Opcode.INVOKEVIRTUAL, MT, "model", "()Ljava/lang/Object;");
        code.call(Opcode.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        code.cast(AdultAndBabyModelPair.class);
        code.load(1);
        Field age = programs.resolveField(state, code.field(Opcode.GETFIELD));
        if (age.getDeclaringClass() != LivingEntityRenderState.class || !age.getName().equals("isBaby")
            || age.getType() != boolean.class) throw OriginalModelPrograms.absent("age_field");
        code.call(Opcode.INVOKEVIRTUAL, PAIR, "getModel", "(Z)Lnet/minecraft/client/model/Model;");
        code.cast(EntityModel.class);
        Field destination = programs.resolveField(owner, code.field(Opcode.PUTFIELD));
        if (destination.getDeclaringClass() != LivingEntityRenderer.class || !destination.getName().equals("model")
            || destination.getType() != EntityModel.class) throw OriginalModelPrograms.absent("model_destination");
        code.load(0); code.load(1); code.load(2); code.load(3); code.load(4);
        code.call(Opcode.INVOKESPECIAL, MOB, "submit", BASE_SUBMIT);
        if (programs.resolveMethod(MobRenderer.class, "submit", BASE_SUBMIT).getDeclaringClass() != LivingEntityRenderer.class)
            throw OriginalModelPrograms.absent("submit_delegate");
        code.op(Opcode.RETURN); code.end();
        return new Selector(state, variant, map, stateVariant, accessor.name().stringValue());
    }

    private static Class<?> modelEnum(Class<?> variant, String accessor) {
        java.lang.reflect.RecordComponent selected = null;
        Class<?> enumType = null;
        for (var component : variant.getRecordComponents()) {
            if (component.getType() != ModelAndTexture.class) continue;
            if (selected != null || !(component.getGenericType() instanceof ParameterizedType generic)
                || !(generic.getActualTypeArguments()[0] instanceof Class<?> value)
                || !value.isEnum() || !StringRepresentable.class.isAssignableFrom(value))
                throw OriginalModelPrograms.absent("model_record_schema");
            selected = component; enumType = value;
        }
        if (selected == null || !selected.getAccessor().getName().equals(accessor))
            throw OriginalModelPrograms.absent("model_record_accessor");
        return enumType;
    }

    private static Inputs inputs(OriginalModelPrograms programs, Class<?> renderer, Selector selector) throws ReflectiveOperationException {
        Class<?> actor = null;
        java.lang.classfile.MethodModel extraction = null;
        for (var method : programs.model(renderer).methods()) {
            if (!method.methodName().stringValue().equals("extractRenderState")
                || method.flags().has(java.lang.reflect.AccessFlag.BRIDGE) || method.flags().has(java.lang.reflect.AccessFlag.SYNTHETIC)
                || method.flags().has(java.lang.reflect.AccessFlag.STATIC)) continue;
            var signature = MethodType.fromMethodDescriptorString(method.methodType().stringValue(), renderer.getClassLoader());
            if (signature.parameterCount() != 3 || signature.parameterType(1) != selector.state) continue;
            if (signature.returnType() != void.class || signature.parameterType(2) != float.class
                || !LivingEntity.class.isAssignableFrom(signature.parameterType(0)) || extraction != null)
                throw OriginalModelPrograms.absent("extraction_signature");
            actor = signature.parameterType(0); extraction = method;
        }
        if (extraction == null) throw OriginalModelPrograms.absent("extraction_missing");
        originalOrigin(actor, LivingEntity.class);
        // Pin the common original extractor and prevent virtual renderer overrides in its age path.
        if (programs.resolveMethod(MobRenderer.class, "extractRenderState", BASE_EXTRACT).getDeclaringClass() != LivingEntityRenderer.class
            || programs.resolveMethod(renderer, "isEntityUpsideDown", "(Lnet/minecraft/world/entity/LivingEntity;)Z").getDeclaringClass() != LivingEntityRenderer.class)
            throw OriginalModelPrograms.absent("base_extraction_override");
        var code = new OriginalModelPrograms.Code(extraction);
        code.load(0); code.load(1); code.load(2); code.loadFloat(3);
        code.call(Opcode.INVOKESPECIAL, MOB, "extractRenderState", BASE_EXTRACT);
        // Whole straight-line program: audited unrelated captures may precede the variant,
        // but cannot receive state aliases or alter entity/variant/age.
        InvokeInstruction getter = null;
        while (code.next < code.instructions.size()) {
            code.load(2);
            int capture = code.next;
            if (code.next + 1 < code.instructions.size() && code.instructions.get(code.next + 1) instanceof InvokeInstruction) {
                code.load(1);
                getter = code.virtual();
                if (!getter.type().stringValue().equals(HOLDER_GET)
                    || !programs.type(getter.owner().asInternalName()).isAssignableFrom(actor))
                    throw OriginalModelPrograms.absent("variant_getter");
                code.call(Opcode.INVOKEINTERFACE, HOLDER, "value", "()Ljava/lang/Object;");
                code.cast(selector.variant);
                if (!selector.stateVariant.equals(programs.resolveField(selector.state, code.field(Opcode.PUTFIELD))))
                    throw OriginalModelPrograms.absent("variant_capture_destination");
                code.op(Opcode.RETURN); code.end();
                break;
            }
            code.next = capture;
            otherCapture(programs, code, actor, selector);
        }
        if (getter == null) throw OriginalModelPrograms.absent("variant_capture_missing");
        var resolvedGetter = programs.resolveMethod(actor, getter.name().stringValue(), HOLDER_GET);
        if (resolvedGetter.getDeclaringClass() != actor) throw OriginalModelPrograms.absent("actor_subclass_unproven");
        var read = new OriginalModelPrograms.Code(programs.declared(resolvedGetter.getDeclaringClass(), resolvedGetter.getName(), HOLDER_GET));
        read.load(0);
        Field data = programs.resolveField(actor, read.field(Opcode.GETFIELD));
        if (!data.getName().equals("entityData") || data.getType() != net.minecraft.network.syncher.SynchedEntityData.class)
            throw OriginalModelPrograms.absent("variant_data_field");
        var dataAccessor = read.field(Opcode.GETSTATIC);
        var accessorOwner = programs.type(dataAccessor.owner().asInternalName());
        var resolvedAccessor = programs.resolveField(accessorOwner, dataAccessor);
        if (!Modifier.isStatic(resolvedAccessor.getModifiers()) || resolvedAccessor.getType() != net.minecraft.network.syncher.EntityDataAccessor.class)
            throw OriginalModelPrograms.absent("variant_data_accessor");
        read.call(Opcode.INVOKEVIRTUAL, "net/minecraft/network/syncher/SynchedEntityData", "get", "(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;");
        read.cast(Holder.class); read.op(Opcode.ARETURN); read.end();
        Field component = component(programs, selector.variant);
        componentBridge(programs, actor, component, resolvedGetter);
        return new Inputs(actor, resolvedGetter.getDeclaringClass().getName() + "#" + resolvedGetter.getName() + HOLDER_GET, component);
    }

    private static void otherCapture(OriginalModelPrograms programs, OriginalModelPrograms.Code code, Class<?> actor, Selector selector) {
        var first = code.instructions.get(code.next);
        if (first instanceof java.lang.classfile.instruction.LoadInstruction load && load.slot() == 3) {
            code.loadFloat(3); code.load(1);
            Field left = programs.resolveField(actor, code.field(Opcode.GETFIELD));
            code.load(1);
            Field right = programs.resolveField(actor, code.field(Opcode.GETFIELD));
            if (left.getType() != float.class || right.getType() != float.class || Modifier.isStatic(left.getModifiers()) || Modifier.isStatic(right.getModifiers()))
                throw OriginalModelPrograms.absent("float_capture_fields");
            programs.pinned(Mth.class, "4bf51de7ebd9315a20cad8bbffeba1f5ee0da98ffe0d4181d7f4528ce024aaef");
            code.call(Opcode.INVOKESTATIC, "net/minecraft/util/Mth", "lerp", "(FFF)F");
            Field target = programs.resolveField(selector.state, code.field(Opcode.PUTFIELD));
            if (target.getType() != float.class || Modifier.isStatic(target.getModifiers())) throw OriginalModelPrograms.absent("float_capture_destination");
        } else {
            code.load(1);
            var slot = code.field(Opcode.GETSTATIC);
            Field slotField = programs.resolveField(EquipmentSlot.class, slot);
            if (slotField.getDeclaringClass() != EquipmentSlot.class || slotField.getType() != EquipmentSlot.class
                || !Modifier.isStatic(slotField.getModifiers())) throw OriginalModelPrograms.absent("equipment_capture_slot");
            var call = code.virtual();
            String descriptor = "(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;";
            if (!call.name().stringValue().equals("getItemBySlot") || !call.type().stringValue().equals(descriptor)
                || !programs.type(call.owner().asInternalName()).isAssignableFrom(actor)
                || programs.resolveMethod(actor, "getItemBySlot", descriptor).getDeclaringClass() != LivingEntity.class)
                throw OriginalModelPrograms.absent("equipment_capture_reader");
            programs.pinned(LivingEntity.class, "eae909d66232bfa9cdadc374653f84a72cfbb547401db60a404c8088e65df6d8",
                "109337b372ccb23cd6b16abb666231f871ac69b6ac7b159c34a407c683b88d66");
            programs.pinned(ItemStack.class, "5a89b6794705ebf363d2b03529dbc96d656a1ca94a424d19765a75edc697fd89",
                "7f96d63f970807f0631fdce06c98af0a7a0240ca18b9b9236dec02fc7932a198");
            code.call(Opcode.INVOKEVIRTUAL, "net/minecraft/world/item/ItemStack", "copy", "()Lnet/minecraft/world/item/ItemStack;");
            Field target = programs.resolveField(selector.state, code.field(Opcode.PUTFIELD));
            if (target.getType() != ItemStack.class || Modifier.isStatic(target.getModifiers()))
                throw OriginalModelPrograms.absent("equipment_capture_destination");
        }
    }

    private static Field component(OriginalModelPrograms programs, Class<?> variant) {
        Field found = null;
        for (var field : DataComponents.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != DataComponentType.class
                || !(field.getGenericType() instanceof ParameterizedType component)
                || !(component.getActualTypeArguments()[0] instanceof ParameterizedType holder)
                || holder.getRawType() != Holder.class || holder.getActualTypeArguments()[0] != variant) continue;
            if (found != null) throw OriginalModelPrograms.absent("component_ambiguous");
            found = field;
        }
        if (found == null) throw OriginalModelPrograms.absent("component_missing");
        programs.model(DataComponents.class);
        return found;
    }

    private static void componentBridge(OriginalModelPrograms programs, Class<?> actor, Field component, java.lang.reflect.Method getter)
        throws ReflectiveOperationException {
        var method = programs.resolveMethod(actor, "get", COMPONENT_GET);
        if (method.getDeclaringClass() != actor) throw OriginalModelPrograms.absent("component_actor_subclass_unproven");
        var code = new OriginalModelPrograms.Code(programs.declared(method.getDeclaringClass(), "get", COMPONENT_GET));
        boolean witnessed = false;
        // Each complete original branch compares component identity and returns one local getter.
        while (code.next < code.instructions.size()) {
            if (code.instructions.get(code.next) instanceof java.lang.classfile.instruction.LoadInstruction load && load.slot() == 0) {
                code.load(0); code.load(1);
                if (!(code.take() instanceof InvokeInstruction parent) || parent.opcode() != Opcode.INVOKESPECIAL
                    || !parent.name().stringValue().equals("get") || !parent.type().stringValue().equals(COMPONENT_GET)
                    || !programs.type(parent.owner().asInternalName()).isAssignableFrom(actor)
                    || programs.type(parent.owner().asInternalName()) == actor) throw OriginalModelPrograms.absent("component_fallback");
                code.op(Opcode.ARETURN); code.end();
                break;
            }
            code.load(1);
            var observed = code.field(Opcode.GETSTATIC);
            Field branchComponent = programs.resolveField(DataComponents.class, observed);
            if (branchComponent.getDeclaringClass() != DataComponents.class || branchComponent.getType() != DataComponentType.class
                || !Modifier.isStatic(branchComponent.getModifiers())) throw OriginalModelPrograms.absent("component_identity");
            int next = code.branch(Opcode.IF_ACMPNE);
            code.load(1); code.load(0);
            var branchGetter = code.virtual();
            var actual = programs.resolveMethod(actor, branchGetter.name().stringValue(), branchGetter.type().stringValue());
            if (!programs.type(branchGetter.owner().asInternalName()).isAssignableFrom(actor)) throw OriginalModelPrograms.absent("component_getter_receiver");
            if (!(code.take() instanceof InvokeInstruction cast) || cast.opcode() != Opcode.INVOKESTATIC
                || !cast.name().stringValue().equals("castComponentValue") || !cast.type().stringValue().equals(CAST_COMPONENT))
                throw OriginalModelPrograms.absent("component_cast");
            var castMethod = programs.resolveMethod(programs.type(cast.owner().asInternalName()), cast.name().stringValue(), CAST_COMPONENT);
            var castCode = new OriginalModelPrograms.Code(programs.model(castMethod.getDeclaringClass()).methods().stream()
                .filter(candidate -> candidate.methodName().stringValue().equals("castComponentValue")
                    && candidate.methodType().stringValue().equals(CAST_COMPONENT)).findFirst().orElseThrow());
            castCode.load(1); castCode.op(Opcode.ARETURN); castCode.end();
            code.op(Opcode.ARETURN);
            if (next != code.next) throw OriginalModelPrograms.absent("component_branch_target");
            if (branchComponent.get(null) == component.get(null)) {
                if (witnessed || !actual.equals(getter)) throw OriginalModelPrograms.absent("component_getter_mismatch");
                witnessed = true;
            }
        }
        if (!witnessed) throw OriginalModelPrograms.absent("component_bridge_missing");
    }

    private static void bridges(OriginalModelPrograms programs, Class<?> renderer, Class<?> actor, Class<?> state) {
        for (Class<?> erasedState : List.of(net.minecraft.client.renderer.entity.state.EntityRenderState.class, LivingEntityRenderState.class)) {
            String descriptor = "(" + erasedState.descriptorString() + SUBMIT_TAIL;
            var resolved = programs.resolveMethod(renderer, "submit", descriptor);
            if (resolved.getDeclaringClass() != renderer) throw OriginalModelPrograms.absent("submit_entrypoint_not_typed");
            var method = programs.anyDeclared(renderer, "submit", descriptor);
            if (!method.flags().has(java.lang.reflect.AccessFlag.BRIDGE) || !method.flags().has(java.lang.reflect.AccessFlag.SYNTHETIC))
                throw OriginalModelPrograms.absent("submit_entrypoint_not_bridge");
            var code = new OriginalModelPrograms.Code(method);
            code.load(0); code.load(1); code.cast(state); code.load(2); code.load(3); code.load(4);
            code.call(Opcode.INVOKEVIRTUAL, renderer.getName().replace('.', '/'), "submit", "(" + state.descriptorString() + SUBMIT_TAIL);
            code.op(Opcode.RETURN); code.end();
        }
        for (Class<?> erasedActor : List.of(net.minecraft.world.entity.Entity.class, LivingEntity.class)) {
            Class<?> erasedState = erasedActor == LivingEntity.class ? LivingEntityRenderState.class : net.minecraft.client.renderer.entity.state.EntityRenderState.class;
            String descriptor = "(" + erasedActor.descriptorString() + erasedState.descriptorString() + "F)V";
            var resolved = programs.resolveMethod(renderer, "extractRenderState", descriptor);
            if (resolved.getDeclaringClass() != renderer) throw OriginalModelPrograms.absent("extract_entrypoint_not_typed");
            var method = programs.anyDeclared(renderer, "extractRenderState", descriptor);
            if (!method.flags().has(java.lang.reflect.AccessFlag.BRIDGE) || !method.flags().has(java.lang.reflect.AccessFlag.SYNTHETIC))
                throw OriginalModelPrograms.absent("extract_entrypoint_not_bridge");
            var code = new OriginalModelPrograms.Code(method);
            code.load(0); code.load(1); code.cast(actor); code.load(2); code.cast(state); code.loadFloat(3);
            code.call(Opcode.INVOKEVIRTUAL, renderer.getName().replace('.', '/'), "extractRenderState", "(" + actor.descriptorString() + state.descriptorString() + "F)V");
            code.op(Opcode.RETURN); code.end();
        }
    }

    private static void originalOrigin(Class<?> candidate, Class<?> originalPlatform) {
        var actual = candidate.getProtectionDomain().getCodeSource();
        var expected = originalPlatform.getProtectionDomain().getCodeSource();
        if (actual == null || expected == null || candidate.getClassLoader() != originalPlatform.getClassLoader()
            || !actual.getLocation().toExternalForm().equals(expected.getLocation().toExternalForm()))
            throw OriginalModelPrograms.absent("non_original_artifact");
    }
}
