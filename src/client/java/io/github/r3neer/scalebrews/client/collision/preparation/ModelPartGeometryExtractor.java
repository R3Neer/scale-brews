package io.github.r3neer.scalebrews.client.collision.preparation;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;

/** Extraction algorithm owned exclusively by the client/tooling ModelPart geometry family. */
final class ModelPartGeometryExtractor {
    private final List<ModelGeometry.Part> parts = new ArrayList<>();
    private final List<ModelGeometry.Piece> pieces = new ArrayList<>();
    private final Set<ModelPart> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    private ModelPartGeometryExtractor() {}

    static ModelGeometry extract(String source, String version, ModelPart root, Set<String> excludedParts) {
        if (source == null || source.isBlank() || version == null || version.isBlank() || root == null || excludedParts == null)
            throw new IllegalArgumentException("Invalid ModelPart extraction input");
        var extractor = new ModelPartGeometryExtractor();
        extractor.extract(root, "root", null, false, Set.copyOf(excludedParts));
        return new ModelGeometry(1, source, version, extractor.parts, extractor.pieces);
    }

    @SuppressWarnings("unchecked")
    private void extract(ModelPart part, String id, String parent, boolean hidden, Set<String> excludes) {
        if (!visited.add(part) || parts.size() >= 512) throw new IllegalArgumentException("Cyclic/oversized ModelPart model");
        var stack = new PoseStack();
        part.translateAndRotate(stack);
        parts.add(new ModelGeometry.Part(id, parent, ModelGeometry.values(new Matrix4f(stack.last().pose()))));

        String localName = id.substring(id.lastIndexOf('/') + 1);
        boolean excluded = hidden || !part.visible || excludes.contains(id) || excludes.contains(localName);
        List<ModelPart.Cube> cubes = (List<ModelPart.Cube>) field(part, "cubes");
        int cubeIndex = 0;
        for (var cube : cubes) {
            var vertices = new ArrayList<double[]>();
            for (var polygon : cube.polygons) for (var vertex : polygon.vertices())
                vertices.add(new double[]{vertex.worldX(), vertex.worldY(), vertex.worldZ()});
            piece(id + "/cube_" + cubeIndex++, id, vertices,
                excluded ? "hidden_or_cosmetic" : part.skipDraw ? "skip_draw" : null);
        }

        Map<String, ModelPart> children = (Map<String, ModelPart>) field(part, "children");
        children.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(entry -> extract(entry.getValue(), id + "/" + entry.getKey(), id, excluded, excludes));
    }

    private void piece(String id, String part, List<double[]> vertices, String excluded) {
        if (pieces.size() >= 4096) throw new IllegalArgumentException("Too many ModelPart cubes");
        if (vertices.isEmpty()) return;
        double[] low = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] high = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (var vertex : vertices) for (int axis = 0; axis < 3; axis++) {
            if (!Double.isFinite(vertex[axis])) throw new IllegalArgumentException("Non-finite ModelPart vertex");
            low[axis] = Math.min(low[axis], vertex[axis]);
            high[axis] = Math.max(high[axis], vertex[axis]);
        }
        // Renderer models legitimately contain zero-thickness visual primitives.
        // They are not material convex pieces and never enter ModelGeometry.
        for (int axis = 0; axis < 3; axis++) if (!(high[axis] > low[axis])) return;
        pieces.add(new ModelGeometry.Piece(id, part,
            List.of(low[0], low[1], low[2]), List.of(high[0], high[1], high[2]), excluded));
    }

    private static Object field(Object owner, String name) {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException inaccessible) {
            throw new IllegalArgumentException(inaccessible);
        }
        throw new IllegalArgumentException("Missing " + owner.getClass().getName() + "." + name);
    }
}
