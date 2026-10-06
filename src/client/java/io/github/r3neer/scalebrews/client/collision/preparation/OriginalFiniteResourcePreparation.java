package io.github.r3neer.scalebrews.client.collision.preparation;

import com.google.gson.Gson;
import io.github.r3neer.scalebrews.collision.pose.FinitePoseProgram;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/** Pure PREPARATION export of explicit-input finite recipes; metadata is never an actor reader. */
public final class OriginalFiniteResourcePreparation {
    public static final String TOOL_VERSION = "original-finite-resource-v1";
    private OriginalFiniteResourcePreparation() {}

    public record Prepared(String tableId, FinitePoseProgram table, Map<String, NumericPoseProgram> numericPrograms,
                           List<OriginalFinitePosePreparation.BooleanInput> booleanInputs,
                           List<OriginalLinearPosePreparation.Input> inputs, String digest) {
        public Prepared {
            numericPrograms = Collections.unmodifiableMap(new TreeMap<>(numericPrograms));
            booleanInputs = List.copyOf(booleanInputs); inputs = List.copyOf(inputs);
        }
    }

    public static Prepared export(OriginalFinitePosePreparation.Prepared original, String namespace) {
        if (original == null || !OriginalFinitePosePreparation.TOOL_VERSION.equals(original.tool())
            || original.digest() == null || !original.digest().matches("[0-9a-f]{64}")
            || original.booleanInputs() == null || original.booleanInputs().size() > FinitePoseProgram.MAX_BOOLEANS
            || original.rows() == null || original.rows().isEmpty() || original.rows().size() > FinitePoseProgram.MAX_ROWS)
            throw absent("missing_or_oversized_prepared");
        var booleanNames = new ArrayList<String>();
        String previous = null;
        for (var input : original.booleanInputs()) {
            String key = field(input.owner(), input.name(), input.descriptor(), "Z");
            if (!input.channel().equals("boolean." + booleanNames.size()) || (previous != null && previous.compareTo(key) >= 0))
                throw absent("boolean_schema");
            previous = key; booleanNames.add(input.channel());
        }
        if (original.rows().size() != 1 << booleanNames.size()) throw absent("incomplete_rows");
        var fields = new TreeMap<String, OriginalLinearPosePreparation.Input>();
        for (var row : original.rows()) {
            if (row == null || row.numeric() == null || row.numeric().inputs() == null || row.numeric().inputs().size() > 64)
                throw absent("row_schema");
            for (var input : row.numeric().inputs()) {
                String key = field(input.owner(), input.name(), input.descriptor(), "F");
                if (!fields.containsKey(key) && fields.size() >= 64 - booleanNames.size()) throw absent("input_union_limit");
                fields.putIfAbsent(key, input);
            }
        }
        var schema = new ArrayList<OriginalLinearPosePreparation.Input>();
        var channels = new HashMap<String, String>();
        fields.forEach((key, input) -> {
            String name = "float." + schema.size(); channels.put(key, name);
            schema.add(new OriginalLinearPosePreparation.Input(name, input.owner(), input.name(), "F"));
        });
        var programs = new TreeMap<String, NumericPoseProgram>();
        var rows = new ArrayList<FinitePoseProgram.Row>();
        var gson = new Gson();
        var first = NumericPoseProgram.validatedCopy(original.rows().getFirst().numeric().program());
        var geometry = first.geometry();
        int rowIndex = 0;
        for (var row : original.rows()) {
            if (row.predicate() == null || !row.predicate().keySet().equals(new HashSet<>(booleanNames)))
                throw absent("partial_predicate");
            int mask = 0;
            for (int i = 0; i < booleanNames.size(); i++) {
                Boolean value = row.predicate().get(booleanNames.get(i));
                if (value == null) throw absent("missing_boolean");
                if (value) mask |= 1 << i;
            }
            var numeric = rowIndex++ == 0 ? first : NumericPoseProgram.validatedCopy(row.numeric().program());
            if (!numeric.geometry().equals(geometry)) throw absent("foreign_geometry");
            var local = new HashMap<String, String>();
            var seenFields = new HashSet<String>();
            for (var input : row.numeric().inputs()) {
                String key = field(input.owner(), input.name(), input.descriptor(), "F");
                if (input.channel() == null || !input.channel().matches("[a-z0-9_.-]{1,64}")
                    || local.putIfAbsent(input.channel(), channels.get(key)) != null || !seenFields.add(key))
                    throw absent("duplicate_float_schema");
            }
            var used = new HashSet<String>();
            var nodes = new ArrayList<NumericPoseProgram.Node>(numeric.nodes().size());
            for (var node : numeric.nodes()) {
                if (node.op() == NumericPoseProgram.Op.INPUT) {
                    String name = local.get(node.input());
                    if (name == null) throw absent("unknown_float_input");
                    used.add(node.input());
                    nodes.add(new NumericPoseProgram.Node(node.op(), node.left(), node.right(), node.constant(), name, node.part(), node.field()));
                } else nodes.add(node);
            }
            if (!used.equals(local.keySet())) throw absent("unused_float_schema");
            var content = new NumericPoseProgram(numeric.format(), "original_finite_export/content", TOOL_VERSION,
                geometry, nodes, numeric.writes());
            String digest = ModelPartStructure.hash(gson.toJson(content));
            String id = id(namespace, "original_finite/" + digest);
            var program = new NumericPoseProgram(content.format(), "original_finite_export/" + digest,
                content.version(), geometry, content.nodes(), content.writes());
            var prior = programs.putIfAbsent(id, program);
            if (prior != null && !prior.equals(program)) throw absent("digest_collision");
            rows.add(new FinitePoseProgram.Row(mask, id));
        }
        rows.sort(java.util.Comparator.comparingInt(FinitePoseProgram.Row::mask));
        String digest = ModelPartStructure.hash(gson.toJson(List.of(TOOL_VERSION, original.digest(),
            original.booleanInputs(), schema, rows)));
        var table = new FinitePoseProgram(FinitePoseProgram.FORMAT, "original_finite_export/" + digest,
            TOOL_VERSION, booleanNames, rows);
        return new Prepared(id(namespace, "original_finite/" + digest), table, programs,
            original.booleanInputs(), schema, digest);
    }

    private static String field(String owner, String name, String descriptor, String expected) {
        if (owner == null || owner.isBlank() || owner.length() > 256 || name == null || name.isBlank()
            || name.length() > 128 || !expected.equals(descriptor)) throw absent("field_schema");
        return owner + "\u0000" + name + "\u0000" + descriptor;
    }
    private static String id(String namespace, String path) {
        if (namespace == null || namespace.isBlank() || namespace.length() > 128) throw absent("namespace");
        return FinitePoseProgram.resourceId(Identifier.fromNamespaceAndPath(namespace, path).toString());
    }
    private static IllegalArgumentException absent(String why) { return new IllegalArgumentException("original_finite_resource:" + why); }
}
