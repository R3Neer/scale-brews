package io.github.r3neer.scalebrews.client.collision.preparation;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Bounded, pass-local original class-resource inspection. Never a live bytecode evaluator. */
final class OriginalModelPrograms {
    static final int MAX_BYTES = 1_048_576, MAX_CLASSES = 64, MAX_TOTAL_BYTES = 8_388_608;
    static final int MAX_METHODS = 1024, MAX_INSTRUCTIONS = 512;
    private final ClassLoader loader;
    private final Map<Class<?>, ClassModel> classes = new IdentityHashMap<>();
    private final Map<String, String> digests = new TreeMap<>();
    private int totalBytes;

    OriginalModelPrograms(ClassLoader loader) { this.loader = loader; }

    Class<?> type(String internal) {
        try { return Class.forName(internal.replace('/', '.'), false, loader); }
        catch (ClassNotFoundException failure) { throw absent("class_missing"); }
    }

    ClassModel model(Class<?> owner) {
        var cached = classes.get(owner);
        if (cached != null) return cached;
        if (classes.size() >= MAX_CLASSES) throw absent("class_limit");
        String resource = "/" + owner.getName().replace('.', '/') + ".class";
        try (var input = owner.getResourceAsStream(resource)) {
            if (input == null) throw absent("resource_missing");
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES || bytes.length > MAX_TOTAL_BYTES - totalBytes)
                throw absent("byte_limit");
            var parsed = ClassFile.of().parse(bytes);
            if (!parsed.thisClass().asInternalName().equals(owner.getName().replace('.', '/'))
                || parsed.methods().size() > MAX_METHODS) throw absent("class_identity_or_method_limit:" + owner.getName() + ":" + parsed.methods().size());
            classes.put(owner, parsed);
            totalBytes += bytes.length;
            digests.put(resource, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            return parsed;
        } catch (IOException failure) { throw absent("resource_unreadable"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    void pinned(Class<?> owner, String... approvedDigests) {
        model(owner);
        String actual = digests.get("/" + owner.getName().replace('.', '/') + ".class");
        if (java.util.Arrays.stream(approvedDigests).noneMatch(actual::equals))
            throw absent("original_dependency_changed:" + owner.getName() + ":" + digests.get("/" + owner.getName().replace('.', '/') + ".class"));
    }

    Map<String, String> digests() { return Map.copyOf(digests); }

    MethodModel declared(Class<?> owner, String name, String descriptor) {
        var method = anyDeclared(owner, name, descriptor);
        if (method.flags().has(AccessFlag.BRIDGE) || method.flags().has(AccessFlag.SYNTHETIC)) throw absent("unexpected_bridge");
        return method;
    }

    MethodModel anyDeclared(Class<?> owner, String name, String descriptor) {
        MethodModel found = null;
        for (var method : model(owner).methods()) {
            if (!method.methodName().stringValue().equals(name) || !method.methodType().stringValue().equals(descriptor)
                || method.flags().has(AccessFlag.STATIC)) continue;
            if (found != null) throw absent("ambiguous_method");
            found = method;
        }
        if (found == null) throw absent("method_missing");
        return found;
    }

    Method resolveMethod(Class<?> receiver, String name, String descriptor) {
        for (Class<?> owner = receiver; owner != null; owner = owner.getSuperclass()) {
            var methods = owner.getDeclaredMethods();
            if (methods.length > MAX_METHODS) throw absent("method_limit");
            for (var method : methods) {
                if (method.getName().equals(name) && descriptor.equals(MethodType.methodType(method.getReturnType(),
                    method.getParameterTypes()).descriptorString())) return method;
            }
        }
        throw absent("method_resolution_missing");
    }

    Field resolveField(Class<?> receiver, FieldInstruction instruction) {
        var symbolicOwner = type(instruction.owner().asInternalName());
        if (!symbolicOwner.isAssignableFrom(receiver)) throw absent("field_receiver");
        try {
            for (Class<?> owner = symbolicOwner; owner != null; owner = owner.getSuperclass()) {
                Field field;
                try { field = owner.getDeclaredField(instruction.name().stringValue()); }
                catch (NoSuchFieldException missing) { continue; }
                if (!field.getType().descriptorString().equals(instruction.type().stringValue()))
                    throw absent("field_descriptor");
                model(owner);
                return field;
            }
        } catch (SecurityException denied) { throw absent("field_access"); }
        throw absent("field_missing");
    }

    static IllegalArgumentException absent(String reason) { return new IllegalArgumentException("original_selector:" + reason); }

    static final class Code {
        final List<Instruction> instructions;
        private final Map<java.lang.classfile.Label, Integer> labels = new HashMap<>();
        int next;

        Code(MethodModel method) {
            var code = method.code().orElseThrow(() -> absent("code_missing"));
            if (!code.exceptionHandlers().isEmpty()) throw absent("exception_handlers");
            var list = new ArrayList<Instruction>();
            for (var element : code) {
                if (element instanceof LabelTarget target) labels.put(target.label(), list.size());
                if (element instanceof Instruction instruction) {
                    if (list.size() >= MAX_INSTRUCTIONS) throw absent("instruction_limit");
                    list.add(instruction);
                }
            }
            instructions = List.copyOf(list);
        }

        Instruction take() {
            if (next >= instructions.size()) throw absent("truncated_program");
            return instructions.get(next++);
        }
        void op(Opcode expected) { if (take().opcode() != expected) throw absent("program_opcode"); }
        void load(int slot) {
            if (!(take() instanceof LoadInstruction load) || load.slot() != slot || load.typeKind() != java.lang.classfile.TypeKind.REFERENCE)
                throw absent("program_local");
        }
        void loadFloat(int slot) {
            if (!(take() instanceof LoadInstruction load) || load.slot() != slot || load.typeKind() != java.lang.classfile.TypeKind.FLOAT)
                throw absent("program_float_local");
        }
        FieldInstruction field(Opcode opcode) {
            if (!(take() instanceof FieldInstruction field) || field.opcode() != opcode)
                throw absent("program_field");
            return field;
        }
        InvokeInstruction call(Opcode opcode, String owner, String name, String descriptor) {
            if (!(take() instanceof InvokeInstruction call) || call.opcode() != opcode
                || !call.owner().asInternalName().equals(owner) || !call.name().stringValue().equals(name)
                || !call.type().stringValue().equals(descriptor)) throw absent("program_call");
            return call;
        }
        InvokeInstruction virtual() {
            if (!(take() instanceof InvokeInstruction call) || call.opcode() != Opcode.INVOKEVIRTUAL)
                throw absent("program_virtual_call");
            return call;
        }
        void cast(Class<?> type) {
            if (!(take() instanceof TypeCheckInstruction cast) || cast.opcode() != Opcode.CHECKCAST
                || !cast.type().asInternalName().equals(type.getName().replace('.', '/')))
                throw absent("program_cast");
        }
        int branch(Opcode opcode) {
            if (!(take() instanceof BranchInstruction branch) || branch.opcode() != opcode)
                throw absent("program_branch");
            var target = labels.get(branch.target());
            if (target == null) throw absent("branch_target_missing");
            return target;
        }
        void end() { if (next != instructions.size()) throw absent("extra_program"); }
    }
}
