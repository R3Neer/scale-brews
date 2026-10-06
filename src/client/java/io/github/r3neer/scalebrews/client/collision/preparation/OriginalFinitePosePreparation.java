package io.github.r3neer.scalebrews.client.collision.preparation;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;

/** Explicit PREPARATION of all typed boolean assignments of a complete original animation. */
public final class OriginalFinitePosePreparation {
    public static final String TOOL_VERSION = "original-finite-pose-v1";
    public static final int MAX_BOOLEANS = 5, MAX_ROWS = 32;
    private static final int MAX_DISCOVERY_ATTEMPTS = MAX_ROWS * 2 - 1;
    private OriginalFinitePosePreparation() {}

    /** A render-state predicate schema, without any authority to read an actor. */
    public record BooleanInput(String channel, String owner, String name, String descriptor) {}
    /** Complete assignment and its neutral float program; no default or partially known row. */
    public record Row(Map<String, Boolean> predicate, OriginalLinearPosePreparation.Prepared numeric) {
        public Row { predicate = Collections.unmodifiableMap(new TreeMap<>(predicate)); }
    }
    public record Prepared(String tool, String modelClass, List<BooleanInput> booleanInputs, List<Row> rows,
                           Map<String, String> originalResources, String digest, int verifiedInstructions) {
        public Prepared {
            booleanInputs = List.copyOf(booleanInputs); rows = List.copyOf(rows);
            originalResources = Collections.unmodifiableMap(new TreeMap<>(originalResources));
        }
    }
    public record Result(Prepared prepared, String absence) {
        public boolean admitted() { return prepared != null; }
    }

    public static Result inspect(EntityModel<?> model) {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("finite preparation requires client thread");
        if (model == null) return new Result(null, "original_finite:missing_model");
        try { return new Result(prepare(model), null); }
        catch (RuntimeException failure) {
            String reason = failure.getMessage();
            return new Result(null, reason == null ? "original_finite:unavailable"
                : reason.substring(0, Math.min(256, reason.length())));
        }
    }

    private static Prepared prepare(EntityModel<?> model) {
        var pass = new OriginalLinearPosePreparation.FinitePass(model);
        var pending = new ArrayDeque<Map<Field, Boolean>>(); pending.add(Map.of());
        var fields = new HashSet<Field>();
        int attempts = 0;
        while (!pending.isEmpty()) {
            if (attempts++ >= MAX_DISCOVERY_ATTEMPTS) throw absent("discovery_limit");
            var assignment = pending.removeFirst();
            try { pass.attempt(assignment); }
            catch (OriginalLinearPosePreparation.BooleanNeeded missing) {
                if (!fields.contains(missing.field) && fields.size() >= MAX_BOOLEANS) throw absent("boolean_limit");
                fields.add(missing.field);
                if (assignment.containsKey(missing.field)) throw absent("contradictory_discovery");
                if (pending.size() > MAX_ROWS - 2) throw absent("row_limit");
                for (boolean value : new boolean[]{false, true}) {
                    var expanded = new HashMap<>(assignment); expanded.put(missing.field, value);
                    pending.addLast(Map.copyOf(expanded));
                }
            }
        }

        var ordered = new ArrayList<>(fields);
        ordered.sort(Comparator.comparing(f -> f.getDeclaringClass().getName() + "\u0000" + f.getName() + "\u0000Z"));
        var schema = new ArrayList<BooleanInput>();
        for (int i = 0; i < ordered.size(); i++) {
            var field = ordered.get(i);
            schema.add(new BooleanInput("boolean." + i, field.getDeclaringClass().getName(), field.getName(), "Z"));
        }
        int rowCount = 1 << ordered.size();
        if (rowCount > MAX_ROWS) throw absent("row_limit");
        var rows = new ArrayList<Row>(rowCount);
        var interned = new LinkedHashMap<String, OriginalLinearPosePreparation.Prepared>();
        for (int mask = 0; mask < rowCount; mask++) {
            var assignment = new HashMap<Field, Boolean>(); var predicate = new TreeMap<String, Boolean>();
            for (int i = 0; i < ordered.size(); i++) {
                boolean value = (mask & (1 << i)) != 0;
                assignment.put(ordered.get(i), value); predicate.put(schema.get(i).channel(), value);
            }
            var numeric = pass.attempt(assignment);
            var same = interned.putIfAbsent(numeric.digest(), numeric);
            if (same != null) {
                if (!same.equals(numeric)) throw absent("program_digest_collision");
                numeric = same;
            }
            if (numeric.program().geometry() != pass.snapshot.geometry()) throw absent("geometry_ownership");
            rows.add(new Row(predicate, numeric));
        }
        pass.finish();
        var resources = new TreeMap<>(pass.reset.originalResources());
        pass.resources.digests().forEach((path, digest) -> {
            String old = resources.putIfAbsent(path, digest);
            if (old != null && !old.equals(digest)) throw absent("resource_changed");
        });
        for (var row : rows) if (!row.numeric().originalResources().equals(resources)) throw absent("resource_set_changed");

        var text = new StringBuilder(TOOL_VERSION).append('|');
        ModelPartStructure.frame(text, model.getClass().getName()); ModelPartStructure.frame(text, pass.reset.digest());
        resources.forEach((path, digest) -> { ModelPartStructure.frame(text, path); ModelPartStructure.frame(text, digest); });
        for (var input : schema) for (String value : List.of(input.channel(), input.owner(), input.name(), input.descriptor()))
            ModelPartStructure.frame(text, value);
        for (var row : rows) {
            row.predicate().forEach((key, value) -> { ModelPartStructure.frame(text, key); text.append(value ? "1|" : "0|"); });
            ModelPartStructure.frame(text, row.numeric().digest());
        }
        return new Prepared(TOOL_VERSION, model.getClass().getName(), schema, rows, resources,
            ModelPartStructure.hash(text.toString()), pass.work);
    }

    private static IllegalArgumentException absent(String reason) { return new IllegalArgumentException("original_finite:" + reason); }
}
