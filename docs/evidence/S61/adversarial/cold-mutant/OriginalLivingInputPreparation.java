package io.github.r3neer.scalebrews.client.collision.preparation;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.collision.pose.LivingPoseInputSource;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;

/** Cold original-resource proof of explicitly requested base fields; no runtime registration. */
public final class OriginalLivingInputPreparation {
    public static final String TOOL_VERSION = LivingPoseInputSource.VERSION;
    private OriginalLivingInputPreparation() {}

    public record Input(String channel, String owner, String name, String descriptor) {
        public Input {
            if (channel == null || !channel.matches("[a-z0-9_.-]{1,64}") || owner == null || owner.isBlank()
                || owner.length() > 256 || name == null || name.isBlank() || name.length() > 128
                || !("F".equals(descriptor) || "Z".equals(descriptor)))
                throw new IllegalArgumentException("Invalid original Living input schema");
        }
    }
    public record Recipe(String tool, String renderer, String state, float partialTicks,
                         LivingPoseInputSource.Program program, List<Input> inputs,
                         Map<String, String> originalResources, String digest) {
        public Recipe {
            inputs = List.copyOf(inputs);
            originalResources = Collections.unmodifiableMap(new TreeMap<>(originalResources));
        }
    }
    public record Result(Recipe recipe, String absence) {
        public boolean admitted() { return recipe != null; }
    }

    public static Result inspect(EntityRenderer<?, ?> renderer, OriginalFiniteResourcePreparation.Prepared exported) {
        if (exported == null || exported.inputs() == null || exported.booleanInputs() == null
            || exported.inputs().size() + exported.booleanInputs().size() > LivingPoseInputSource.MAX_CHANNELS)
            return new Result(null, "original_living_input:missing_or_oversized_export");
        try {
            var schema = new ArrayList<Input>();
            for (var input : exported.inputs()) schema.add(new Input(input.channel(), input.owner(), input.name(), input.descriptor()));
            for (var input : exported.booleanInputs()) schema.add(new Input(input.channel(), input.owner(), input.name(), input.descriptor()));
            return inspect(renderer, schema);
        } catch (RuntimeException failure) { return failure(failure); }
    }

    public static Result inspect(EntityRenderer<?, ?> renderer, List<Input> schema) {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("input preparation requires client thread");
        if (renderer == null || schema == null || schema.isEmpty() || schema.size() > LivingPoseInputSource.MAX_CHANNELS)
            return new Result(null, "original_living_input:missing_or_oversized_schema");
        try {
            return new Result(prepare(renderer, schema), null);
        } catch (RuntimeException | ReflectiveOperationException unavailable) { return failure(unavailable); }
    }

    private static Result failure(Exception failure) {
        String reason = failure.getMessage();
        return new Result(null, reason == null ? "original_living_input:unavailable" : reason.substring(0, Math.min(256, reason.length())));
    }

    private static Recipe prepare(EntityRenderer<?, ?> renderer, List<Input> schema)
            throws ReflectiveOperationException {
        Class<?> owner = renderer.getClass();
        OriginalEnumSelectorProof.originalOrigin(owner, LivingEntityRenderer.class);
        var programs = new OriginalModelPrograms(owner.getClassLoader());
        // Fixed 26.2 whole base programs, including their helper bodies and branch order.
        programs.pinned(EntityRenderer.class, "99c26f6552f5524144ba34093d241cecb818a745531d7592657d99a23ef97011",
            "a37ef501fb2ffe5386fdfbe7b3c929aca8d4dacda4cb66db17b8843aa3cdaad1");
        programs.pinned(LivingEntityRenderer.class, "cf44b4e0b89d04e5ea4f56049443b4304edda55b4ebad6378cf486738251629b",
            "d3c5792a8ea3c6ebc74bfdaee0f81cb994755ba86a23e9f3778b9b5f574550c9");
        programs.pinned(Mth.class, "4bf51de7ebd9315a20cad8bbffeba1f5ee0da98ffe0d4181d7f4528ce024aaef");
        // Only technology recognition may try another family. A failed input proof
        // never falls back and loses its actual failure reason.
        OriginalEnumSelectorProof.Selector selector;
        try { selector = OriginalEnumSelectorProof.selector(programs, owner, OriginalEnumSelectorProof.Family.ENUM_AGE); }
        catch (IllegalArgumentException unsupportedAge) {
            selector = OriginalEnumSelectorProof.selector(programs, owner, OriginalEnumSelectorProof.Family.ENUM_ONLY);
        }
        var actor = OriginalEnumSelectorProof.inputs(programs, owner, selector).actor();
        OriginalEnumSelectorProof.bridges(programs, owner, actor, selector.state());
        // Entity's base extractor passes the state to this virtual helper after writing age.
        for (Class<?> erasedActor : List.of(net.minecraft.world.entity.Entity.class, LivingEntity.class)) {
            Class<?> erasedState = erasedActor == LivingEntity.class ? LivingEntityRenderState.class : EntityRenderState.class;
            String descriptor = "(" + erasedActor.descriptorString() + erasedState.descriptorString() + "F)V";
            if (programs.resolveMethod(owner, "extractNameTags", descriptor).getDeclaringClass() != LivingEntityRenderer.class)
                throw absent("name_capture_override");
        }
        var channels = new ArrayList<LivingPoseInputSource.Channel>();
        var normalized = new ArrayList<Input>();
        var fields = new HashSet<Field>();
        var names = new HashSet<String>();
        for (var raw : schema) {
            if (raw == null) throw absent("missing_input");
            var input = new Input(raw.channel(), raw.owner(), raw.name(), raw.descriptor());
            if (!names.add(input.channel())) throw absent("duplicate_channel");
            Class<?> symbolicOwner = Class.forName(input.owner(), false, owner.getClassLoader());
            if (!symbolicOwner.isAssignableFrom(selector.state())) throw absent("state_owner_mismatch");
            OriginalEnumSelectorProof.originalOrigin(symbolicOwner, EntityRenderState.class);
            Field field = symbolicOwner.getField(input.name());
            if (Modifier.isStatic(field.getModifiers()) || !field.getType().descriptorString().equals(input.descriptor()))
                throw absent("state_field_descriptor");
            var source = source(field);
            fields.add(field); programs.model(field.getDeclaringClass());
            normalized.add(new Input(input.channel(), field.getDeclaringClass().getName(), field.getName(), input.descriptor()));
            channels.add(new LivingPoseInputSource.Channel(input.channel(), source));
        }
        // Existing proof consumes the complete typed program. Inspect EVERY write by
        // resolved identity too: an unrelated float capture must not replace a base input.
        String descriptor = "(" + actor.descriptorString() + selector.state().descriptorString() + "F)V";
        var extraction = new OriginalModelPrograms.Code(programs.declared(owner, "extractRenderState", descriptor));
        for (var instruction : extraction.instructions) {
            if (instruction instanceof FieldInstruction write && write.opcode() == Opcode.PUTFIELD
                && fields.contains(programs.resolveField(selector.state(), write))) throw absent("base_input_overwrite");
        }
        // These common methods are invoked, not guessed or rewritten. Record exact
        // resource provenance for their actual dispatch owners on the exact actor.
        for (var entry : Map.of("getXRot", "(F)F", "getCustomName", "()Lnet/minecraft/network/chat/Component;",
            "getVehicle", "()Lnet/minecraft/world/entity/Entity;", "isPassenger", "()Z", "isAlive", "()Z",
            "getScale", "()F", "getAgeScale", "()F", "isBaby", "()Z", "isInWater", "()Z",
            "isFullyFrozen", "()Z").entrySet()) recordGetter(programs, actor, entry.getKey(), entry.getValue());
        recordGetter(programs, actor, "isAutoSpinAttack", "()Z");
        programs.model(WalkAnimationState.class);
        var program = new LivingPoseInputSource.Program(LivingPoseInputSource.FORMAT, actor.getName(), channels);
        var resources = new TreeMap<>(programs.digests());
        String digest = ModelPartStructure.hash(new Gson().toJson(List.of(TOOL_VERSION, owner.getName(),
            selector.state().getName(), 1f, program, normalized, resources)));
        return new Recipe(TOOL_VERSION, owner.getName(), selector.state().getName(), 1f, program, normalized, resources, digest);
    }

    private static void recordGetter(OriginalModelPrograms programs, Class<?> actor, String name, String descriptor) {
        var method = programs.resolveMethod(actor, name, descriptor);
        OriginalEnumSelectorProof.originalOrigin(method.getDeclaringClass(), LivingEntity.class);
        programs.model(method.getDeclaringClass());
    }

    private static LivingPoseInputSource.Source source(Field field) {
        if (field.getDeclaringClass() == EntityRenderState.class && field.getType() == float.class && field.getName().equals("ageInTicks"))
            return LivingPoseInputSource.Source.AGE_IN_TICKS;
        if (field.getDeclaringClass() != LivingEntityRenderState.class) throw absent("unsupported_field");
        var source = switch (field.getName()) {
            case "walkAnimationPos" -> LivingPoseInputSource.Source.WALK_POSITION;
            case "walkAnimationSpeed" -> LivingPoseInputSource.Source.WALK_SPEED;
            case "xRot" -> LivingPoseInputSource.Source.X_ROT;
            case "yRot" -> LivingPoseInputSource.Source.Y_ROT;
            case "bodyRot" -> LivingPoseInputSource.Source.BODY_ROT;
            case "scale" -> LivingPoseInputSource.Source.SCALE;
            case "ageScale" -> LivingPoseInputSource.Source.AGE_SCALE;
            case "isBaby" -> LivingPoseInputSource.Source.IS_BABY;
            case "isInWater" -> LivingPoseInputSource.Source.IS_IN_WATER;
            case "isUpsideDown" -> LivingPoseInputSource.Source.IS_UPSIDE_DOWN;
            case "isFullyFrozen" -> LivingPoseInputSource.Source.IS_FULLY_FROZEN;
            case "isAutoSpinAttack" -> LivingPoseInputSource.Source.IS_AUTO_SPIN_ATTACK;
            default -> LivingPoseInputSource.Source.AGE_IN_TICKS;
        };
        boolean flag = source.ordinal() >= LivingPoseInputSource.Source.IS_BABY.ordinal();
        if (field.getType() != (flag ? boolean.class : float.class)) throw absent("source_type");
        return source;
    }
    private static IllegalArgumentException absent(String reason) { return new IllegalArgumentException("original_living_input:" + reason); }
}
