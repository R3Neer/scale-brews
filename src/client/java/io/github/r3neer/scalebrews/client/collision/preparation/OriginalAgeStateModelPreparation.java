package io.github.r3neer.scalebrews.client.collision.preparation;

import java.lang.classfile.Opcode;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Explicit PREPARATION proof of an original render-state predicate to model-slot assignment.
 * Does not prove entity age extraction, poses, root transforms, layers or physical geometry.
 */
public final class OriginalAgeStateModelPreparation {
    public static final String TOOL_VERSION = "original-age-state-selector-v1";
    private static final String BASE = "net/minecraft/client/renderer/entity/AgeableMobRenderer";
    private static final String MOB = "net/minecraft/client/renderer/entity/MobRenderer";
    private static final String TAIL = "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";
    private static final String LIVING_SUBMIT = "(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;" + TAIL;
    private static final String ENTITY_SUBMIT = "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;" + TAIL;
    private static final String CONSTRUCTOR = "(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/client/model/EntityModel;F)V";
    private OriginalAgeStateModelPreparation() {}

    public record Row(boolean statePredicate, String slot, String modelClass,
                      ModelPartStructure.Fingerprint initialStructure) {}
    public record Recipe(String tool, String rendererClass, String statePredicateField,
                         String assignmentField, String extractionAuthority,
                         Map<String, String> originalResources, List<Row> rows,
                         int uniqueModels, String canonicalText, String digest) {
        public Recipe {
            originalResources = Collections.unmodifiableMap(new TreeMap<>(originalResources));
            rows = List.copyOf(rows);
        }
    }
    public record Result(Recipe recipe, String absence) {
        public boolean admitted() { return recipe != null; }
    }

    public static Result inspect(EntityRenderer<?, ?> renderer) {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("selector preparation requires client thread");
        if (renderer == null) return new Result(null, "original_selector:missing_renderer");
        try { return new Result(prepare(renderer), null); }
        catch (RuntimeException | ReflectiveOperationException failure) {
            String reason = failure.getMessage();
            return new Result(null, reason == null ? "original_selector:unavailable" : reason.substring(0, Math.min(256, reason.length())));
        }
    }

    private record Slots(Field predicate, Field adult, Field baby, Field assignment) {}

    private static Recipe prepare(EntityRenderer<?, ?> renderer) throws ReflectiveOperationException {
        Class<?> owner = renderer.getClass();
        if (!AgeableMobRenderer.class.isAssignableFrom(owner)) throw OriginalModelPrograms.absent("other_selector_family");
        var programs = new OriginalModelPrograms(owner.getClassLoader());
        // Each native hierarchy resource is recorded. No external subclass inherits this proof.
        int depth = 0;
        for (Class<?> current = owner;; current = current.getSuperclass()) {
            if (current == null || ++depth > 12) throw OriginalModelPrograms.absent("renderer_hierarchy_limit");
            originalOrigin(current);
            programs.model(current);
            if (current == AgeableMobRenderer.class) break;
        }
        programs.pinned(AgeableMobRenderer.class,
            "92c23aeb955c0bd560db2e8e7d0fdb1d2bb3faf22b8a4ea5897437ce5e6ee70f",
            "5019f524aa756e09fcb672d3cce17a3803953e7e1015baaf9e06433415121e34");
        programs.pinned(LivingEntityRenderer.class,
            "cf44b4e0b89d04e5ea4f56049443b4304edda55b4ebad6378cf486738251629b",
            "d3c5792a8ea3c6ebc74bfdaee0f81cb994755ba86a23e9f3778b9b5f574550c9");
        if (programs.resolveMethod(owner, "submit", LIVING_SUBMIT).getDeclaringClass() != AgeableMobRenderer.class
            || programs.resolveMethod(owner, "submit", ENTITY_SUBMIT).getDeclaringClass() != AgeableMobRenderer.class
            || programs.resolveMethod(MobRenderer.class, "submit", LIVING_SUBMIT).getDeclaringClass() != LivingEntityRenderer.class)
            throw OriginalModelPrograms.absent("submit_entrypoint_override");
        var slots = selector(programs);
        constructor(programs, slots);
        bridge(programs);
        var structures = new IdentityHashMap<EntityModel<?>, ModelPartStructure.Fingerprint>();
        var rows = new java.util.ArrayList<Row>(2);
        for (boolean baby : List.of(false, true)) {
            Field field = baby ? slots.baby : slots.adult;
            field.setAccessible(true);
            Object candidate = field.get(renderer);
            if (!(candidate instanceof EntityModel<?> model)) throw OriginalModelPrograms.absent("model_slot_missing");
            var initial = structures.get(model);
            if (initial == null) {
                initial = ModelPartStructure.inspect(model.root());
                structures.put(model, initial);
            }
            rows.add(new Row(baby, fieldId(field), model.getClass().getName(), initial));
        }
        var resources = programs.digests();
        var text = new StringBuilder(TOOL_VERSION + "|");
        for (String value : List.of(owner.getName(), fieldId(slots.predicate), fieldId(slots.assignment), "UNPROVEN"))
            ModelPartStructure.frame(text, value);
        new TreeMap<>(resources).forEach((name, digest) -> {
            ModelPartStructure.frame(text, name); ModelPartStructure.frame(text, digest);
        });
        for (var row : rows) {
            text.append(row.statePredicate()).append('|');
            ModelPartStructure.frame(text, row.slot()); ModelPartStructure.frame(text, row.modelClass());
            ModelPartStructure.frame(text, row.initialStructure().digest());
        }
        text.append("unique:").append(structures.size()).append('|');
        return new Recipe(TOOL_VERSION, owner.getName(), fieldId(slots.predicate), fieldId(slots.assignment),
            "UNPROVEN", resources, rows, structures.size(), text.toString(), ModelPartStructure.hash(text.toString()));
    }

    private static Slots selector(OriginalModelPrograms programs) {
        var code = new OriginalModelPrograms.Code(programs.declared(AgeableMobRenderer.class, "submit", LIVING_SUBMIT));
        code.load(0); code.load(1);
        Field predicate = programs.resolveField(LivingEntityRenderState.class, code.field(Opcode.GETFIELD));
        if (predicate.getDeclaringClass() != LivingEntityRenderState.class || !predicate.getName().equals("isBaby")
            || predicate.getType() != boolean.class || Modifier.isStatic(predicate.getModifiers()))
            throw OriginalModelPrograms.absent("state_predicate_field");
        int adultBranch = code.branch(Opcode.IFEQ);
        code.load(0);
        Field baby = modelSlot(programs, code.field(Opcode.GETFIELD), "babyModel");
        int join = code.branch(Opcode.GOTO);
        if (adultBranch != code.next) throw OriginalModelPrograms.absent("adult_branch_target");
        code.load(0);
        Field adult = modelSlot(programs, code.field(Opcode.GETFIELD), "adultModel");
        if (join != code.next) throw OriginalModelPrograms.absent("model_join_target");
        Field assignment = programs.resolveField(AgeableMobRenderer.class, code.field(Opcode.PUTFIELD));
        if (assignment.getDeclaringClass() != LivingEntityRenderer.class || !assignment.getName().equals("model")
            || assignment.getType() != EntityModel.class || Modifier.isStatic(assignment.getModifiers()))
            throw OriginalModelPrograms.absent("assignment_field");
        code.load(0); code.load(1); code.load(2); code.load(3); code.load(4);
        code.call(Opcode.INVOKESPECIAL, MOB, "submit", LIVING_SUBMIT);
        code.op(Opcode.RETURN); code.end();
        return new Slots(predicate, adult, baby, assignment);
    }

    private static Field modelSlot(OriginalModelPrograms programs, java.lang.classfile.instruction.FieldInstruction instruction, String name) {
        Field field = programs.resolveField(AgeableMobRenderer.class, instruction);
        if (field.getDeclaringClass() != AgeableMobRenderer.class || !field.getName().equals(name)
            || field.getType() != EntityModel.class || !Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
            throw OriginalModelPrograms.absent("original_model_slot");
        return field;
    }

    private static void constructor(OriginalModelPrograms programs, Slots slots) {
        var code = new OriginalModelPrograms.Code(programs.declared(AgeableMobRenderer.class, "<init>", CONSTRUCTOR));
        code.load(0); code.load(1); code.load(2); code.loadFloat(4);
        code.call(Opcode.INVOKESPECIAL, MOB, "<init>", "(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Lnet/minecraft/client/model/EntityModel;F)V");
        code.load(0); code.load(2);
        if (!slots.adult.equals(modelSlot(programs, code.field(Opcode.PUTFIELD), "adultModel")))
            throw OriginalModelPrograms.absent("adult_constructor_slot");
        code.load(0); code.load(3);
        if (!slots.baby.equals(modelSlot(programs, code.field(Opcode.PUTFIELD), "babyModel")))
            throw OriginalModelPrograms.absent("baby_constructor_slot");
        code.op(Opcode.RETURN); code.end();
    }

    private static void bridge(OriginalModelPrograms programs) {
        var method = programs.anyDeclared(AgeableMobRenderer.class, "submit", ENTITY_SUBMIT);
        if (!method.flags().has(AccessFlag.BRIDGE) || !method.flags().has(AccessFlag.SYNTHETIC))
            throw OriginalModelPrograms.absent("submit_bridge_flags");
        var code = new OriginalModelPrograms.Code(method);
        code.load(0); code.load(1); code.cast(LivingEntityRenderState.class);
        code.load(2); code.load(3); code.load(4);
        code.call(Opcode.INVOKEVIRTUAL, BASE, "submit", LIVING_SUBMIT);
        code.op(Opcode.RETURN); code.end();
    }

    private static String fieldId(Field field) {
        return field.getDeclaringClass().getName() + "#" + field.getName() + ":" + field.getType().descriptorString();
    }

    private static void originalOrigin(Class<?> candidate) {
        var actual = candidate.getProtectionDomain().getCodeSource();
        var expected = AgeableMobRenderer.class.getProtectionDomain().getCodeSource();
        if (actual == null || expected == null || candidate.getClassLoader() != AgeableMobRenderer.class.getClassLoader()
            || !actual.getLocation().toExternalForm().equals(expected.getLocation().toExternalForm()))
            throw OriginalModelPrograms.absent("non_original_artifact");
    }
}
