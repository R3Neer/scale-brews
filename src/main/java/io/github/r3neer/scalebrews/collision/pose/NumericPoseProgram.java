package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Neutral bounded float DAG, evaluated explicitly; not an actor source or registered physical binding. */
public record NumericPoseProgram(int format, String source, String version, ModelGeometry geometry,
                                 List<Node> nodes, List<Write> writes) {
    public static final int FORMAT = 1, MAX_NODES = 512, MAX_WRITES = 512;
    public enum Op { CONSTANT, INPUT, INITIAL, ADD, SUBTRACT, MULTIPLY, DIVIDE, NEGATE, SIN, COS }
    /** SIN/COS receive the exact float value promoted to double, as by JVM F2D. */
    public record Node(Op op, int left, int right, float constant, String input, String part, int field) {
        public Node {
            if (op == null || !Float.isFinite(constant)) throw new IllegalArgumentException("Invalid numeric node");
            switch (op) {
                case CONSTANT -> {
                    if (left != -1 || right != -1 || input != null || part != null || field != -1) bad();
                }
                case INPUT -> {
                    if (left != -1 || right != -1 || constant != 0 || input == null
                        || !input.matches("[a-z0-9_.-]{1,64}") || part != null || field != -1) bad();
                }
                case INITIAL -> {
                    if (left != -1 || right != -1 || constant != 0 || input != null || part == null
                        || part.isBlank() || part.length() > 256 || field < 0 || field > 8) bad();
                }
                case ADD, SUBTRACT, MULTIPLY, DIVIDE -> {
                    if (left < 0 || right < 0 || constant != 0 || input != null || part != null || field != -1) bad();
                }
                case NEGATE, SIN, COS -> {
                    if (left < 0 || right != -1 || constant != 0 || input != null || part != null || field != -1) bad();
                }
            }
        }
        private static void bad() { throw new IllegalArgumentException("Invalid numeric node shape"); }
    }
    public record Write(String part, int field, int node) {
        public Write {
            if (part == null || part.isBlank() || part.length() > 256 || field < 0 || field > 8 || node < 0)
                throw new IllegalArgumentException("Invalid numeric write");
        }
    }

    public NumericPoseProgram {
        if (format != FORMAT || source == null || source.isBlank() || source.length() > 256
            || version == null || version.isBlank() || version.length() > 128 || geometry == null
            || nodes == null || nodes.isEmpty() || nodes.size() > MAX_NODES
            || writes == null || writes.isEmpty() || writes.size() > MAX_WRITES)
            throw new IllegalArgumentException("Invalid numeric program");
        nodes = List.copyOf(nodes); writes = List.copyOf(writes);
        var parts = new HashMap<String, ModelGeometry.Part>();
        for (var part : geometry.parts()) {
            if (part.sourcePose() == null) throw new IllegalArgumentException("Numeric program requires source fields");
            parts.put(part.id(), part);
        }
        var inputs = new HashSet<String>();
        for (int i = 0; i < nodes.size(); i++) {
            var n = nodes.get(i);
            if (n.left() >= i || n.right() >= i || (n.op() == Op.INITIAL && !parts.containsKey(n.part())))
                throw new IllegalArgumentException("Invalid numeric DAG topology or joint");
            if (n.op() == Op.INPUT && inputs.add(n.input()) && inputs.size() > 64)
                throw new IllegalArgumentException("Oversized numeric input schema");
        }
        var targets = new HashSet<String>();
        for (var w : writes) {
            if (w.node() >= nodes.size() || !parts.containsKey(w.part()) || !targets.add(w.part() + "\u0000" + w.field()))
                throw new IllegalArgumentException("Invalid numeric output");
        }
    }

    /** PREPARATION: revalidate every nested constructor, with caps checked before copying lists. */
    public static NumericPoseProgram validatedCopy(NumericPoseProgram raw) {
        if (raw == null || raw.nodes() == null || raw.nodes().isEmpty() || raw.nodes().size() > MAX_NODES
            || raw.writes() == null || raw.writes().isEmpty() || raw.writes().size() > MAX_WRITES)
            throw new IllegalArgumentException("Missing/oversized numeric program");
        var g = raw.geometry();
        if (g == null || g.parts() == null || g.parts().isEmpty() || g.parts().size() > 512
            || g.pieces() == null || g.pieces().size() > 4096)
            throw new IllegalArgumentException("Missing/oversized numeric geometry");
        var parts = g.parts().stream().map(p -> {
            if (p == null || p.transform() == null || p.transform().size() != 16 || p.sourcePose() == null)
                throw new IllegalArgumentException("Invalid numeric geometry part");
            var f = p.sourcePose();
            var pose = new ModelGeometry.SourcePose(f.x(), f.y(), f.z(), f.xRot(), f.yRot(), f.zRot(),
                f.xScale(), f.yScale(), f.zScale());
            return new ModelGeometry.Part(p.id(), p.parent(), p.transform(), pose);
        }).toList();
        var pieces = g.pieces().stream().map(p -> {
            if (p == null || p.min() == null || p.min().size() != 3 || p.max() == null || p.max().size() != 3)
                throw new IllegalArgumentException("Invalid numeric geometry piece");
            return new ModelGeometry.Piece(p.id(), p.part(), p.min(), p.max(), p.excluded());
        }).toList();
        var geometry = new ModelGeometry(g.format(), g.source(), g.version(), parts, pieces, g.modelTransform());
        var nodes = raw.nodes().stream().map(n -> {
            if (n == null) throw new IllegalArgumentException("Missing numeric node");
            return new Node(n.op(), n.left(), n.right(), n.constant(), n.input(), n.part(), n.field());
        }).toList();
        var writes = raw.writes().stream().map(w -> {
            if (w == null) throw new IllegalArgumentException("Missing numeric write");
            return new Write(w.part(), w.field(), w.node());
        }).toList();
        return new NumericPoseProgram(raw.format(), raw.source(), raw.version(), geometry, nodes, writes);
    }

    /** Exact immutable geometry binding; preparation work is never repeated by the returned evaluator. */
    public Optional<PoseEngine.Bound> bind(ModelGeometry candidate) {
        if (!geometry.equals(candidate)) return Optional.empty();
        var initials = new HashMap<String, float[]>();
        for (var p : geometry.parts()) initials.put(p.id(), fields(p.sourcePose()));
        float[] initialNodes = new float[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            if (node.op() == Op.INITIAL) initialNodes[i] = initials.get(node.part())[node.field()];
        }
        var outputs = new LinkedHashMap<String, int[]>();
        for (var write : writes) {
            int[] indexes = outputs.computeIfAbsent(write.part(), ignored -> {
                int[] empty = new int[9]; java.util.Arrays.fill(empty, -1); return empty;
            });
            indexes[write.field()] = write.node();
        }
        // All scratch and result matrices belong to a call; concurrent evaluations do not share mutation.
        var programNodes = nodes;
        return Optional.of(inputs -> evaluate(programNodes, inputs, initials, initialNodes, outputs));
    }

    private static Optional<Map<String, Matrix4f>> evaluate(List<Node> nodes, PoseEngine.Inputs inputs, Map<String, float[]> initials,
                                                   float[] initialNodes, Map<String, int[]> outputs) {
        if (inputs == null || !inputs.ordinary()) return Optional.empty();
        float[] values = new float[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            float value;
            switch (n.op()) {
                case CONSTANT -> value = n.constant();
                case INPUT -> {
                    Float input = inputs.channels().get(n.input());
                    if (input == null) return Optional.empty();
                    value = input;
                }
                case INITIAL -> value = initialNodes[i];
                case ADD -> value = values[n.left()] + values[n.right()];
                case SUBTRACT -> value = values[n.left()] - values[n.right()];
                case MULTIPLY -> value = values[n.left()] * values[n.right()];
                case DIVIDE -> value = values[n.left()] / values[n.right()];
                case NEGATE -> value = -values[n.left()];
                case SIN -> value = Mth.sin((double)values[n.left()]);
                case COS -> value = Mth.cos((double)values[n.left()]);
                default -> throw new IllegalStateException("Unrecognized numeric operation");
            }
            // Validate each intermediate, before a subsequent LUT lookup could turn NaN into a finite value.
            if (!Float.isFinite(value)) return Optional.empty();
            values[i] = value;
        }
        var result = new LinkedHashMap<String, Matrix4f>();
        try {
            for (var entry : outputs.entrySet()) {
                float[] f = initials.get(entry.getKey()).clone(); int[] indexes = entry.getValue();
                for (int i = 0; i < 9; i++) if (indexes[i] >= 0) f[i] = values[indexes[i]];
                Matrix4f matrix = new ModelGeometry.SourcePose(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8]).matrix();
                if (!Float.isFinite(matrix.determinant()) || Math.abs(matrix.determinant()) < 1e-12f) return Optional.empty();
                for (float v : matrix.get(new float[16])) if (!Float.isFinite(v)) return Optional.empty();
                result.put(entry.getKey(), matrix);
            }
        } catch (IllegalArgumentException invalid) { return Optional.empty(); }
        return Optional.of(Collections.unmodifiableMap(result));
    }

    private static float[] fields(ModelGeometry.SourcePose p) {
        return new float[]{p.x(), p.y(), p.z(), p.xRot(), p.yRot(), p.zRot(), p.xScale(), p.yScale(), p.zScale()};
    }
}
