package io.github.r3neer.scalebrews.client.collision.preparation;

import io.github.r3neer.scalebrews.client.mixin.ModelPartAccess;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram.Node;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram.Op;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram.Write;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Explicit PREPARATION compiler of complete original branch-free numeric animation programs. */
public final class OriginalLinearPosePreparation {
    public static final String TOOL_VERSION = "original-linear-pose-v2";
    private static final int MAX_CALLS = 16, MAX_LOCALS = 32, MAX_STACK = 32;
    private static final List<String> FIELDS = List.of("x", "y", "z", "xRot", "yRot", "zRot", "xScale", "yScale", "zScale");
    private OriginalLinearPosePreparation() {}

    /** Identifies a numeric field of the explicit original render-state input, not an actor channel provider. */
    public record Input(String channel, String owner, String name, String descriptor) {}
    public record Prepared(NumericPoseProgram program, List<Input> inputs, Map<String, String> originalResources, String digest) {
        public Prepared {
            inputs = List.copyOf(inputs);
            originalResources = java.util.Collections.unmodifiableMap(new TreeMap<>(originalResources));
        }
    }
    public record Result(Prepared prepared, String absence) {
        public boolean admitted() { return prepared != null; }
    }

    public static Result inspect(EntityModel<?> model) {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("linear preparation requires client thread");
        if (model == null) return new Result(null, "original_linear:missing_model");
        try { return new Result(new Compiler(model).compile(), null); }
        catch (RuntimeException failure) {
            String reason = failure.getMessage();
            return new Result(null, reason == null ? "original_linear:unavailable" : reason.substring(0, Math.min(256, reason.length())));
        }
    }

    private record Ref(String part, boolean state) {}
    private record Number(int node, boolean promoted) {}
    private record BooleanValue(boolean value) {}
    private record Target(String part, int field) {}
    private static final Ref THIS = new Ref(null, false), STATE = new Ref(null, true);

    /** Only the finite preparer handles this discovery signal; it is never a default value. */
    static final class BooleanNeeded extends RuntimeException {
        final Field field;
        BooleanNeeded(Field field) { this.field = field; }
    }

    /** PREPARATION-only pass, discarded before returning neutral immutable recipes. */
    static final class FinitePass {
        static final int MAX_WORK = 1_048_576;
        private record MethodKey(Class<?> owner, String descriptor) {}
        final EntityModel<?> model;
        final OriginalModelPrograms resources;
        final IdentityHashMap<ModelPart, String> parts = new IdentityHashMap<>();
        final OriginalResetPosePreparation.Recipe reset;
        final ModelPartStructure.Snapshot snapshot;
        private final Map<MethodKey, OriginalModelPrograms.Code> codes = new LinkedHashMap<>();
        private final Map<MethodKey, BitSet> coverage = new LinkedHashMap<>();
        int work;

        FinitePass(EntityModel<?> model) {
            this.model = model;
            resources = new OriginalModelPrograms(model.getClass().getClassLoader());
            reset = OriginalResetPosePreparation.baseResetCertificate(model, resources);
            collectParts(parts, model.root(), "root", 0);
            snapshot = ModelPartStructure.snapshot(model.root(), "original_reset/" + reset.digest(),
                OriginalFinitePosePreparation.TOOL_VERSION);
            OriginalResetGeometryPreparation.verify(reset, snapshot.geometry());
            if (!snapshot.fingerprint().equals(reset.initialStructure())) throw absent("structure_changed");
        }

        Prepared attempt(Map<Field, Boolean> values) { return new Compiler(this, values).compile(); }

        OriginalModelPrograms.Code code(Class<?> owner, String descriptor) {
            if (work >= MAX_WORK) throw absent("finite_work_limit");
            var key = new MethodKey(owner, descriptor);
            var template = codes.get(key);
            boolean fresh = template == null;
            if (fresh) {
                template = new OriginalModelPrograms.Code(resources.anyDeclared(owner, "setupAnim", descriptor));
            }
            int count = template.instructions.size();
            if (count > MAX_WORK - work) throw absent("finite_work_limit");
            work += count;
            if (fresh) {
                for (int i = 0; i < template.instructions.size(); i++) {
                    if (template.instructions.get(i) instanceof BranchInstruction branch) {
                        if (branch.opcode() != Opcode.IFEQ && branch.opcode() != Opcode.IFNE && branch.opcode() != Opcode.GOTO)
                            throw absent("finite_branch_opcode");
                        int target = template.target(branch);
                        if (target >= template.instructions.size()) throw absent("finite_branch_target");
                    }
                }
                codes.put(key, template); coverage.put(key, new BitSet(template.instructions.size()));
            }
            return template.fork();
        }

        BitSet visits(Class<?> owner, String descriptor) { return coverage.get(new MethodKey(owner, descriptor)); }

        void finish() {
            codes.forEach((key, code) -> {
                if (coverage.get(key).nextClearBit(0) < code.instructions.size()) throw absent("finite_uncovered_program");
            });
            if (!ModelPartStructure.inspect(model.root()).equals(reset.initialStructure())) throw absent("structure_changed");
        }
    }

    private static void collectParts(IdentityHashMap<ModelPart, String> parts, ModelPart part, String path, int depth) {
        if (part == null || depth > ModelPartStructure.MAX_DEPTH || parts.size() >= ModelPartStructure.MAX_PARTS
            || parts.put(part, path) != null) throw absent("part_identity");
        var children = ((ModelPartAccess)(Object)part).scalebrews$children();
        for (var entry : new TreeMap<>(children).entrySet()) collectParts(parts, entry.getValue(), path + "/" + entry.getKey(), depth + 1);
    }

    private static final class Compiler {
        final EntityModel<?> model;
        final OriginalModelPrograms resources;
        final IdentityHashMap<ModelPart, String> parts;
        final FinitePass finite;
        final Map<Field, Boolean> booleans;
        final List<Node> nodes = new ArrayList<>();
        final LinkedHashMap<Target, Integer> writes = new LinkedHashMap<>();
        final LinkedHashMap<Field, Input> inputs = new LinkedHashMap<>();
        Class<?> stateClass;
        int calls;

        Compiler(EntityModel<?> model) {
            this.model = model;
            resources = new OriginalModelPrograms(model.getClass().getClassLoader());
            parts = new IdentityHashMap<>(); finite = null; booleans = Map.of();
        }

        Compiler(FinitePass pass, Map<Field, Boolean> values) {
            model = pass.model; resources = pass.resources; parts = pass.parts;
            finite = pass; booleans = Map.copyOf(values);
        }

        Prepared compile() {
            var reset = finite == null ? OriginalResetPosePreparation.baseResetCertificate(model) : finite.reset;
            if (finite == null) collectParts(parts, model.root(), "root", 0);
            var entry = resources.resolveMethod(model.getClass(), "setupAnim", "(Ljava/lang/Object;)V");
            bridge(entry.getDeclaringClass(), "(Ljava/lang/Object;)V");
            if (writes.isEmpty()) throw absent("no_numeric_writes");
            String version = finite == null ? TOOL_VERSION : OriginalFinitePosePreparation.TOOL_VERSION;
            var snapshot = finite == null
                ? ModelPartStructure.snapshot(model.root(), "original_reset/" + reset.digest(), version) : finite.snapshot;
            if (!snapshot.fingerprint().equals(reset.initialStructure())) throw absent("structure_changed");
            OriginalResetGeometryPreparation.verify(reset, snapshot.geometry());
            var allResources = new TreeMap<>(reset.originalResources());
            resources.digests().forEach((path, digest) -> {
                String old = allResources.putIfAbsent(path, digest);
                if (old != null && !old.equals(digest)) throw absent("resource_changed");
            });
            var schema = List.copyOf(inputs.values());
            var output = new ArrayList<Write>();
            writes.forEach((target, node) -> output.add(new Write(target.part(), target.field(), node)));
            var text = new StringBuilder(version).append('|');
            ModelPartStructure.frame(text, reset.digest());
            allResources.forEach((path, digest) -> { ModelPartStructure.frame(text, path); ModelPartStructure.frame(text, digest); });
            for (var input : schema) for (String value : List.of(input.channel(), input.owner(), input.name(), input.descriptor()))
                ModelPartStructure.frame(text, value);
            for (var node : nodes) {
                text.append(node.op().ordinal()).append('|').append(node.left()).append('|').append(node.right()).append('|')
                    .append(Float.floatToRawIntBits(node.constant())).append('|').append(node.field()).append('|');
                optionalFrame(text, node.input()); optionalFrame(text, node.part());
            }
            for (var write : output) {
                ModelPartStructure.frame(text, write.part()); text.append(write.field()).append('|').append(write.node()).append('|');
            }
            String digest = ModelPartStructure.hash(text.toString());
            var program = new NumericPoseProgram(NumericPoseProgram.FORMAT,
                (finite == null ? "original_linear/" : "original_finite/") + digest,
                version, snapshot.geometry(), nodes, output);
            return new Prepared(program, schema, allResources, digest);
        }

        void original(Class<?> owner) {
            OriginalEnumSelectorProof.originalOrigin(owner, Model.class);
            resources.model(owner);
        }

        void bridge(Class<?> owner, String descriptor) {
            if (++calls > MAX_CALLS) throw absent("call_limit");
            original(owner);
            var method = resources.anyDeclared(owner, "setupAnim", descriptor);
            if (!method.flags().has(AccessFlag.BRIDGE)) {
                if (descriptor.equals("(Ljava/lang/Object;)V")) throw absent("reset_only_or_untyped");
                body(owner, descriptor); return;
            }
            var code = finite == null ? new OriginalModelPrograms.Code(method) : finite.code(owner, descriptor);
            code.load(0); code.load(1);
            // The effective Object bridge must narrow the original state, not execute unrelated work.
            var cast = code.take();
            if (!(cast instanceof java.lang.classfile.instruction.TypeCheckInstruction check) || check.opcode() != Opcode.CHECKCAST)
                throw absent("bridge_cast");
            Class<?> narrowed = resources.type(check.type().asInternalName());
            if (!LivingEntityRenderState.class.isAssignableFrom(narrowed)) throw absent("state_type");
            OriginalEnumSelectorProof.originalOrigin(narrowed, LivingEntityRenderState.class);
            resources.model(narrowed);
            if (stateClass == null) stateClass = narrowed;
            else if (!narrowed.isAssignableFrom(stateClass)) throw absent("state_cast");
            var invoke = code.virtual();
            if (!invoke.name().stringValue().equals("setupAnim")
                || !invoke.type().stringValue().equals("(" + narrowed.descriptorString() + ")V")
                || !resources.type(invoke.owner().asInternalName()).isAssignableFrom(model.getClass())) throw absent("bridge_dispatch");
            code.op(Opcode.RETURN); code.end();
            if (finite != null) finite.visits(owner, descriptor).set(0, code.instructions.size());
            var actual = resources.resolveMethod(model.getClass(), "setupAnim", invoke.type().stringValue());
            bridge(actual.getDeclaringClass(), invoke.type().stringValue());
        }

        void body(Class<?> owner, String descriptor) {
            original(owner);
            var signature = MethodType.fromMethodDescriptorString(descriptor, owner.getClassLoader());
            if (signature.returnType() != void.class || signature.parameterCount() != 1
                || stateClass == null || !signature.parameterType(0).isAssignableFrom(stateClass)) throw absent("body_signature");
            var method = resources.declared(owner, "setupAnim", descriptor);
            var code = finite == null ? new OriginalModelPrograms.Code(method) : finite.code(owner, descriptor);
            var visits = finite == null ? null : finite.visits(owner, descriptor);
            code.load(0); code.load(1);
            if (!(code.take() instanceof InvokeInstruction invoke) || invoke.opcode() != Opcode.INVOKESPECIAL
                || !invoke.name().stringValue().equals("setupAnim")) throw absent("base_reset_prefix");
            Class<?> superOwner = resources.type(invoke.owner().asInternalName());
            if (superOwner == owner || !superOwner.isAssignableFrom(owner) || !Model.class.isAssignableFrom(superOwner))
                throw absent("super_owner");
            if (visits != null) visits.set(0, code.next);
            // JVM ACC_SUPER lookup starts at the current declaring class's direct superclass.
            var actual = resources.resolveMethod(owner.getSuperclass(), "setupAnim", invoke.type().stringValue());
            if (actual.getDeclaringClass() != Model.class) {
                if (++calls > MAX_CALLS) throw absent("call_limit");
                body(actual.getDeclaringClass(), invoke.type().stringValue());
            } else if (!invoke.type().stringValue().equals("(Ljava/lang/Object;)V")) throw absent("reset_descriptor");
            // Base reset semantics are separately certified; all remaining instructions must be consumed.
            var stack = new ArrayList<Object>();
            Object[] locals = new Object[MAX_LOCALS]; locals[0] = THIS; locals[1] = STATE;
            while (code.next < code.instructions.size()) {
                int instructionIndex = code.next;
                var instruction = code.take();
                if (visits != null) visits.set(instructionIndex, code.next);
                if (instruction instanceof BranchInstruction branch) {
                    if (finite == null) throw absent("instruction:" + instruction.opcode());
                    int target = code.target(branch);
                    if (branch.opcode() == Opcode.GOTO) code.next = target;
                    else {
                        var value = booleanValue(pop(stack));
                        if ((branch.opcode() == Opcode.IFEQ && !value.value())
                            || (branch.opcode() == Opcode.IFNE && value.value())) code.next = target;
                    }
                } else if (instruction instanceof LoadInstruction load) {
                    if (load.slot() >= MAX_LOCALS || locals[load.slot()] == null) throw absent("local");
                    Object value = locals[load.slot()];
                    if (load.typeKind() == TypeKind.FLOAT) numeric(value, false);
                    else if (load.typeKind() == TypeKind.INT && finite != null) booleanValue(value);
                    else if (load.typeKind() != TypeKind.REFERENCE || !(value instanceof Ref)) throw absent("local_type");
                    push(stack, value);
                } else if (instruction instanceof StoreInstruction store) {
                    if (store.slot() < 2 || store.slot() >= MAX_LOCALS) throw absent("store");
                    if (store.typeKind() == TypeKind.FLOAT) locals[store.slot()] = numeric(pop(stack), false);
                    else if (store.typeKind() == TypeKind.INT && finite != null) locals[store.slot()] = booleanValue(pop(stack));
                    else throw absent("store");
                } else if (instruction instanceof ConstantInstruction constant) {
                    if (constant.typeKind() != TypeKind.FLOAT || !(constant.constantValue() instanceof Float value)) throw absent("constant");
                    push(stack, new Number(node(new Node(Op.CONSTANT, -1, -1, value, null, null, -1)), false));
                } else if (instruction instanceof FieldInstruction field) {
                    if (field.opcode() == Opcode.GETFIELD) read(stack, field);
                    else if (field.opcode() == Opcode.PUTFIELD) {
                        var number = numeric(pop(stack), false); var ref = part(pop(stack));
                        int channel = partChannel(field); writes.put(new Target(ref.part(), channel), number.node());
                    } else throw absent("field_opcode");
                } else if (instruction instanceof InvokeInstruction call) {
                    if (call.opcode() != Opcode.INVOKESTATIC || !call.owner().asInternalName().equals("net/minecraft/util/Mth")
                        || !call.type().stringValue().equals("(D)F")
                        || !(call.name().stringValue().equals("sin") || call.name().stringValue().equals("cos"))) throw absent("call");
                    resources.pinned(Mth.class, "4bf51de7ebd9315a20cad8bbffeba1f5ee0da98ffe0d4181d7f4528ce024aaef");
                    var value = numeric(pop(stack), true);
                    Op operation = call.name().stringValue().equals("sin") ? Op.SIN : Op.COS;
                    push(stack, new Number(node(new Node(operation, value.node(), -1, 0, null, null, -1)), false));
                } else {
                    switch (instruction.opcode()) {
                        case DUP -> {
                            if (finite != null && !stack.isEmpty() && stack.getLast() instanceof BooleanValue)
                                push(stack, stack.getLast());
                            else duplicateCategoryOne(stack);
                        }
                        case FADD, FSUB, FMUL, FDIV -> {
                            var right = numeric(pop(stack), false); var left = numeric(pop(stack), false);
                            Op operation = switch (instruction.opcode()) {
                                case FADD -> Op.ADD; case FSUB -> Op.SUBTRACT; case FMUL -> Op.MULTIPLY; default -> Op.DIVIDE;
                            };
                            push(stack, new Number(node(new Node(operation, left.node(), right.node(), 0, null, null, -1)), false));
                        }
                        case FNEG -> {
                            var value = numeric(pop(stack), false);
                            push(stack, new Number(node(new Node(Op.NEGATE, value.node(), -1, 0, null, null, -1)), false));
                        }
                        case F2D -> {
                            var value = numeric(pop(stack), false); push(stack, new Number(value.node(), true));
                        }
                        case RETURN -> {
                            if (!stack.isEmpty()) throw absent("return_stack");
                            if (finite == null) code.end(); return;
                        }
                        default -> throw absent("instruction:" + instruction.opcode());
                    }
                }
            }
            throw absent("missing_return");
        }

        void read(List<Object> stack, FieldInstruction instruction) {
            Object receiver = pop(stack);
            if (receiver == THIS) {
                Field field = resources.resolveField(model.getClass(), instruction);
                original(field.getDeclaringClass());
                if (field.getType() != ModelPart.class || !Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    throw absent("model_field");
                try {
                    if (!field.trySetAccessible()) throw absent("field_access");
                    String id = parts.get((ModelPart)field.get(model));
                    if (id == null) throw absent("foreign_part");
                    push(stack, new Ref(id, false));
                } catch (IllegalAccessException failure) { throw absent("field_access"); }
            } else if (receiver == STATE) {
                Field field = resources.resolveField(stateClass, instruction);
                OriginalEnumSelectorProof.originalOrigin(field.getDeclaringClass(), LivingEntityRenderState.class);
                if (finite != null && field.getType() == boolean.class && !Modifier.isStatic(field.getModifiers())) {
                    Boolean value = booleans.get(field);
                    if (value == null) throw new BooleanNeeded(field);
                    push(stack, new BooleanValue(value)); return;
                }
                if (field.getType() != float.class || Modifier.isStatic(field.getModifiers())) throw absent("state_field");
                Input input = inputs.get(field);
                if (input == null) {
                    if (inputs.size() >= 64) throw absent("input_limit");
                    input = new Input("input." + inputs.size(), field.getDeclaringClass().getName(), field.getName(), "F");
                    inputs.put(field, input);
                }
                push(stack, new Number(node(new Node(Op.INPUT, -1, -1, 0, input.channel(), null, -1)), false));
            } else {
                var ref = part(receiver); int channel = partChannel(instruction);
                Integer written = writes.get(new Target(ref.part(), channel));
                int value = written == null ? node(new Node(Op.INITIAL, -1, -1, 0, null, ref.part(), channel)) : written;
                push(stack, new Number(value, false));
            }
        }

        int partChannel(FieldInstruction field) {
            if (!field.owner().asInternalName().equals("net/minecraft/client/model/geom/ModelPart")
                || !field.type().stringValue().equals("F")) throw absent("part_field");
            int channel = FIELDS.indexOf(field.name().stringValue());
            if (channel < 0) throw absent("part_field"); return channel;
        }
        int node(Node value) {
            if (nodes.size() >= NumericPoseProgram.MAX_NODES) throw absent("node_limit");
            nodes.add(value); return nodes.size() - 1;
        }
        static void push(List<Object> stack, Object value) {
            if (stack.size() >= MAX_STACK) throw absent("stack_limit"); stack.add(value);
        }
        static void duplicateCategoryOne(List<Object> stack) {
            if (stack.isEmpty()) throw absent("stack_underflow");
            var value = stack.getLast();
            if (!(value instanceof Ref) && (!(value instanceof Number number) || number.promoted()))
                throw absent("dup_category");
            push(stack, value);
        }
        static Object pop(List<Object> stack) {
            if (stack.isEmpty()) throw absent("stack_underflow"); return stack.removeLast();
        }
        static Number numeric(Object value, boolean promoted) {
            if (!(value instanceof Number number) || number.promoted() != promoted) throw absent("numeric_type"); return number;
        }
        static BooleanValue booleanValue(Object value) {
            if (!(value instanceof BooleanValue bool)) throw absent("boolean_type"); return bool;
        }
        static Ref part(Object value) {
            if (!(value instanceof Ref ref) || ref.state() || ref.part() == null) throw absent("part_receiver"); return ref;
        }
    }
    private static void optionalFrame(StringBuilder text, String value) {
        text.append(value == null ? "absent|" : "present|");
        if (value != null) ModelPartStructure.frame(text, value);
    }
    private static IllegalArgumentException absent(String reason) { return new IllegalArgumentException("original_linear:" + reason); }
}
