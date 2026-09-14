package io.github.r3neer.scalebrews.client.collision.preparation;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

/**
 * Single extraction authority for the pinned Alex 2.1.9 AdvancedModelBox dialect.
 *
 * <p>The implementation is reflection based because the external jars are acceptance/tooling
 * inputs rather than ordinary dependencies. Reflection is deliberately strict: it binds fields
 * to the exact declaring classes of the pinned dialect instead of resolving homonymous fields
 * by walking the hierarchy.</p>
 */
final class AdvancedModelBoxGeometryExtractor {
    static final int MAX_PARTS = 512;
    static final int MAX_PRIMITIVES = 4096;
    static final int MAX_DEPTH = 64;

    static final String DIALECT = "alexsmobs-2.1.9-fabric-26.2";
    private static final String ADVANCED_BOX = "com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedModelBox";
    private static final String BASIC_PART = "com.github.alexthe666.alexsmobs.citadel.client.model.basic.BasicModelPart";
    private static final String MODEL_BOX = "com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModelRenderUtils$ModelBox";
    private static final String TEXTURED_QUAD = "com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModelRenderUtils$TexturedQuad";
    private static final String POSITION_VERTEX = "com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModelRenderUtils$PositionTextureVertex";
    private static final float MATRIX_EPS = 3e-5f;

    record Result(ModelGeometry geometry, List<AdvancedModelBoxGeometryEngine.Omission> omissions) {
        Result {
            omissions = List.copyOf(omissions);
        }
    }

    private final Dialect dialect;
    private final String source;
    private final String version;
    private final List<ModelGeometry.Part> parts = new ArrayList<>();
    private final List<ModelGeometry.Piece> pieces = new ArrayList<>();
    private final List<AdvancedModelBoxGeometryEngine.Omission> omissions = new ArrayList<>();
    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<String> partIds = new LinkedHashSet<>();
    private int primitiveCount;

    private AdvancedModelBoxGeometryExtractor(Dialect dialect, String source, String version) {
        this.dialect = dialect;
        this.source = source;
        this.version = version;
    }

    static Result extract(String source, String version, String dialectId, Object model) {
        if (source == null || version == null || dialectId == null || model == null)
            throw new IllegalArgumentException("Invalid AdvancedModelBox extraction input");
        try {
            Identifier.parse(source);
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Invalid AdvancedModelBox model id " + source, invalid);
        }
        if (!"2.1.9".equals(version) || !DIALECT.equals(dialectId))
            throw new IllegalArgumentException("Unsupported AdvancedModelBox dialect/version: " + dialectId + " / " + version);

        Dialect dialect = Dialect.resolve(model.getClass().getClassLoader());
        var extractor = new AdvancedModelBoxGeometryExtractor(dialect, source, version);
        extractor.extractModel(model);
        return new Result(new ModelGeometry(2, source, version, extractor.parts, extractor.pieces,
            ModelGeometry.values(new Matrix4f())), extractor.omissions);
    }

    private void extractModel(Object model) {
        Iterable<?> roots = iterable(invokeNoArgs(model, "parts"), "parts()");
        Iterable<?> allParts = iterable(invokeNoArgs(model, "getAllParts"), "getAllParts()");

        var catalog = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        int catalogCount = 0;
        for (Object part : allParts) {
            if (!dialect.advancedBox.isInstance(part))
                throw new IllegalArgumentException("getAllParts() contains a non-AdvancedModelBox node");
            catalogCount++;
            if (catalogCount > MAX_PARTS) throw new IllegalArgumentException("Too many AdvancedModelBox parts");
            if (!catalog.add(part)) throw new IllegalArgumentException("Duplicate AdvancedModelBox in getAllParts()");
        }
        if (catalog.isEmpty()) throw new IllegalArgumentException("AdvancedModelBox source has no parts");

        int rootCount = 0;
        for (Object root : roots) {
            rootCount++;
            if (rootCount > MAX_PARTS) throw new IllegalArgumentException("Too many AdvancedModelBox roots");
            if (!dialect.advancedBox.isInstance(root))
                throw new IllegalArgumentException("parts() contains a non-AdvancedModelBox root");
            if (!catalog.contains(root)) throw new IllegalArgumentException("AdvancedModelBox root is absent from getAllParts()");
            extractPart(root, null, null, false, 0);
        }
        if (rootCount == 0) throw new IllegalArgumentException("AdvancedModelBox source has no render roots");
        if (visited.size() != catalog.size() || !visited.containsAll(catalog))
            throw new IllegalArgumentException("AdvancedModelBox getAllParts() contains detached or multiply-owned nodes");
    }

    private void extractPart(Object part, String sourceParent, String transformParent, boolean structurallyHidden, int depth) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("AdvancedModelBox hierarchy too deep");
        if (!visited.add(part)) throw new IllegalArgumentException("Cyclic or multiply-parented AdvancedModelBox hierarchy");
        if (visited.size() > MAX_PARTS) throw new IllegalArgumentException("Too many AdvancedModelBox parts");

        String name = validName(string(read(dialect.boxName, part, "boxName"), "boxName"));
        String id = sourceParent == null ? name : sourceParent + "/" + name;
        requireId(id);
        if (!partIds.add(id)) throw new IllegalArgumentException("Ambiguous AdvancedModelBox part id " + id);

        float px = number(read(dialect.rotationPointX, part, "rotationPointX"), "rotationPointX");
        float py = number(read(dialect.rotationPointY, part, "rotationPointY"), "rotationPointY");
        float pz = number(read(dialect.rotationPointZ, part, "rotationPointZ"), "rotationPointZ");
        float rx = number(read(dialect.rotateAngleX, part, "rotateAngleX"), "rotateAngleX");
        float ry = number(read(dialect.rotateAngleY, part, "rotateAngleY"), "rotateAngleY");
        float rz = number(read(dialect.rotateAngleZ, part, "rotateAngleZ"), "rotateAngleZ");
        float sx = number(read(dialect.scaleX, part, "scaleX"), "scaleX");
        float sy = number(read(dialect.scaleY, part, "scaleY"), "scaleY");
        float sz = number(read(dialect.scaleZ, part, "scaleZ"), "scaleZ");
        var sourcePose = new ModelGeometry.SourcePose(px, py, pz, rx, ry, rz, sx, sy, sz);
        Matrix4f local = sourcePose.matrix();
        verifyRendererTransform(part, local);
        addPart(new ModelGeometry.Part(id, transformParent, ModelGeometry.values(local), sourcePose));

        boolean hidden = structurallyHidden || !bool(read(dialect.showModel, part, "showModel"), "showModel");
        Iterable<?> cubes = iterable(read(dialect.cubeList, part, "cubeList"), "AdvancedModelBox.cubeList");
        int cubeIndex = 0;
        for (Object cube : cubes) {
            if (!dialect.modelBox.isInstance(cube))
                throw new IllegalArgumentException("AdvancedModelBox cubeList contains the wrong ModelBox dialect");
            primitiveCount++;
            if (primitiveCount > MAX_PRIMITIVES) throw new IllegalArgumentException("Too many AdvancedModelBox primitives");
            extractCube(cube, id + "/cube_" + cubeIndex++, id, hidden);
        }

        boolean scaleChildren = bool(read(dialect.scaleChildren, part, "scaleChildren"), "scaleChildren");
        String childTransformParent = id;
        if (!scaleChildren && !identityScale(sx, sy, sz)) {
            // This is the renderer's exact propagation rule, not a physical epsilon repair. A zero
            // local scale has already failed SourcePose validation before this inverse is considered.
            float ix = 1f / Math.max(sx, 1.0e-4f);
            float iy = 1f / Math.max(sy, 1.0e-4f);
            float iz = 1f / Math.max(sz, 1.0e-4f);
            if (!Float.isFinite(ix) || !Float.isFinite(iy) || !Float.isFinite(iz))
                throw new IllegalArgumentException("Invalid AdvancedModelBox child-scale compensation");
            childTransformParent = id + "/unscaled_children";
            requireId(childTransformParent);
            if (!partIds.add(childTransformParent))
                throw new IllegalArgumentException("Ambiguous AdvancedModelBox helper id " + childTransformParent);
            addPart(new ModelGeometry.Part(childTransformParent, id,
                ModelGeometry.values(new Matrix4f().scaling(ix, iy, iz))));
        }

        Iterable<?> children = iterable(read(dialect.childModels, part, "childModels"), "AdvancedModelBox.childModels");
        for (Object child : children) {
            if (!dialect.advancedBox.isInstance(child))
                throw new IllegalArgumentException("AdvancedModelBox childModels contains a non-AdvancedModelBox child");
            // The helper is transform-only. Stable source ids continue to follow the boxName hierarchy.
            extractPart(child, id, childTransformParent, hidden, depth + 1);
        }
    }

    private void extractCube(Object cube, String pieceId, String partId, boolean hidden) {
        requireId(pieceId);
        double x1 = number(read(dialect.posX1, cube, "posX1"), "posX1");
        double y1 = number(read(dialect.posY1, cube, "posY1"), "posY1");
        double z1 = number(read(dialect.posZ1, cube, "posZ1"), "posZ1");
        double x2 = number(read(dialect.posX2, cube, "posX2"), "posX2");
        double y2 = number(read(dialect.posY2, cube, "posY2"), "posY2");
        double z2 = number(read(dialect.posZ2, cube, "posZ2"), "posZ2");
        double[] nominal = {x2 - x1, y2 - y1, z2 - z1};
        boolean nominalFlat = false;
        for (double dimension : nominal) {
            if (!Double.isFinite(dimension)) throw new IllegalArgumentException("Non-finite AdvancedModelBox source dimension");
            if (dimension < 0) throw new IllegalArgumentException("Negative AdvancedModelBox source dimension");
            nominalFlat |= dimension == 0;
        }

        Object quadsValue = read(dialect.quads, cube, "quads");
        if (!(quadsValue instanceof Object[] quads) || quads.length == 0)
            throw new IllegalArgumentException("AdvancedModelBox ModelBox has no materialized quads");
        double[] low = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] high = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        int vertexCount = 0;
        for (Object quad : quads) {
            if (quad == null || !dialect.texturedQuad.isInstance(quad))
                throw new IllegalArgumentException("Invalid AdvancedModelBox textured quad");
            Object verticesValue = read(dialect.vertexPositions, quad, "vertexPositions");
            if (!(verticesValue instanceof Object[] vertices) || vertices.length != 4)
                throw new IllegalArgumentException("Invalid AdvancedModelBox quad vertices");
            for (Object vertex : vertices) {
                if (vertex == null || !dialect.positionVertex.isInstance(vertex))
                    throw new IllegalArgumentException("Invalid AdvancedModelBox vertex dialect");
                Object position = read(dialect.position, vertex, "position");
                if (!(position instanceof Vector3fc vector))
                    throw new IllegalArgumentException("AdvancedModelBox vertex has incompatible position");
                double[] values = {vector.x() / 16d, vector.y() / 16d, vector.z() / 16d};
                for (int axis = 0; axis < 3; axis++) {
                    if (!Double.isFinite(values[axis])) throw new IllegalArgumentException("Non-finite AdvancedModelBox vertex");
                    low[axis] = Math.min(low[axis], values[axis]);
                    high[axis] = Math.max(high[axis], values[axis]);
                }
                vertexCount++;
            }
        }
        if (vertexCount == 0) throw new IllegalArgumentException("AdvancedModelBox primitive has no vertices");

        boolean effectiveVolume = high[0] > low[0] && high[1] > low[1] && high[2] > low[2];
        if (!effectiveVolume) {
            if (!nominalFlat)
                throw new IllegalArgumentException("Volumetric AdvancedModelBox primitive collapsed after materialization");
            omissions.add(new AdvancedModelBoxGeometryEngine.Omission(pieceId, partId, "render_only_non_volumetric"));
            return;
        }
        pieces.add(new ModelGeometry.Piece(pieceId, partId,
            List.of(low[0], low[1], low[2]), List.of(high[0], high[1], high[2]),
            hidden ? "source_hidden" : null));
    }

    private void verifyRendererTransform(Object part, Matrix4f expected) {
        try {
            var stack = new PoseStack();
            dialect.translateAndRotate.invoke(part, stack);
            Matrix4f actual = new Matrix4f(stack.last().pose());
            float[] a = expected.get(new float[16]);
            float[] b = actual.get(new float[16]);
            for (int i = 0; i < 16; i++) if (Math.abs(a[i] - b[i]) > MATRIX_EPS)
                throw new IllegalArgumentException("AdvancedModelBox transform contract does not match pinned dialect");
        } catch (IllegalAccessException | InvocationTargetException failure) {
            throw new IllegalArgumentException("Cannot invoke AdvancedModelBox translateAndRotate", failure);
        }
    }

    private void addPart(ModelGeometry.Part part) {
        if (parts.size() >= MAX_PARTS) throw new IllegalArgumentException("Too many prepared AdvancedModelBox parts");
        parts.add(part);
    }

    private static boolean identityScale(float x, float y, float z) {
        return Float.floatToIntBits(x) == Float.floatToIntBits(1f)
            && Float.floatToIntBits(y) == Float.floatToIntBits(1f)
            && Float.floatToIntBits(z) == Float.floatToIntBits(1f);
    }

    private static String validName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]{1,64}"))
            throw new IllegalArgumentException("Invalid or missing AdvancedModelBox boxName");
        return name;
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank() || id.length() > 256)
            throw new IllegalArgumentException("Invalid AdvancedModelBox structural id");
    }

    private static Iterable<?> iterable(Object value, String label) {
        if (!(value instanceof Iterable<?> iterable)) throw new IllegalArgumentException(label + " is not iterable");
        return iterable;
    }

    private static Object invokeNoArgs(Object owner, String name) {
        try {
            Method method = owner.getClass().getMethod(name);
            if (method.getParameterCount() != 0) throw new IllegalArgumentException("Invalid " + name + " signature");
            return method.invoke(owner);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException failure) {
            throw new IllegalArgumentException("Missing/incompatible AdvancedModelBox model method " + name, failure);
        }
    }

    private static Object read(Field field, Object owner, String label) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException failure) {
            throw new IllegalArgumentException("Cannot read AdvancedModelBox " + label, failure);
        }
    }

    private static String string(Object value, String label) {
        if (!(value instanceof String text)) throw new IllegalArgumentException("Invalid AdvancedModelBox " + label);
        return text;
    }

    private static boolean bool(Object value, String label) {
        if (!(value instanceof Boolean result)) throw new IllegalArgumentException("Invalid AdvancedModelBox " + label);
        return result;
    }

    private static float number(Object value, String label) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Invalid AdvancedModelBox " + label);
        float result = number.floatValue();
        if (!Float.isFinite(result)) throw new IllegalArgumentException("Non-finite AdvancedModelBox " + label);
        return result;
    }

    private static final class Dialect {
        final Class<?> advancedBox, basicPart, modelBox, texturedQuad, positionVertex;
        final Field boxName, cubeList, childModels, scaleX, scaleY, scaleZ, scaleChildren;
        final Field rotationPointX, rotationPointY, rotationPointZ, rotateAngleX, rotateAngleY, rotateAngleZ, showModel;
        final Field posX1, posY1, posZ1, posX2, posY2, posZ2, quads, vertexPositions, position;
        final Method translateAndRotate;

        private Dialect(ClassLoader loader) throws ReflectiveOperationException {
            advancedBox = Class.forName(ADVANCED_BOX, false, loader);
            basicPart = Class.forName(BASIC_PART, false, loader);
            modelBox = Class.forName(MODEL_BOX, false, loader);
            texturedQuad = Class.forName(TEXTURED_QUAD, false, loader);
            positionVertex = Class.forName(POSITION_VERTEX, false, loader);
            if (!basicPart.isAssignableFrom(advancedBox))
                throw new IllegalArgumentException("Pinned AdvancedModelBox no longer extends pinned BasicModelPart");

            boxName = field(advancedBox, "boxName");
            cubeList = field(advancedBox, "cubeList");
            childModels = field(advancedBox, "childModels");
            scaleX = field(advancedBox, "scaleX");
            scaleY = field(advancedBox, "scaleY");
            scaleZ = field(advancedBox, "scaleZ");
            scaleChildren = field(advancedBox, "scaleChildren");
            rotationPointX = field(basicPart, "rotationPointX");
            rotationPointY = field(basicPart, "rotationPointY");
            rotationPointZ = field(basicPart, "rotationPointZ");
            rotateAngleX = field(basicPart, "rotateAngleX");
            rotateAngleY = field(basicPart, "rotateAngleY");
            rotateAngleZ = field(basicPart, "rotateAngleZ");
            showModel = field(basicPart, "showModel");
            posX1 = field(modelBox, "posX1");
            posY1 = field(modelBox, "posY1");
            posZ1 = field(modelBox, "posZ1");
            posX2 = field(modelBox, "posX2");
            posY2 = field(modelBox, "posY2");
            posZ2 = field(modelBox, "posZ2");
            quads = field(modelBox, "quads");
            vertexPositions = field(texturedQuad, "vertexPositions");
            position = field(positionVertex, "position");
            translateAndRotate = advancedBox.getMethod("translateAndRotate", PoseStack.class);
        }

        static Dialect resolve(ClassLoader loader) {
            try {
                return new Dialect(loader);
            } catch (ReflectiveOperationException | LinkageError failure) {
                throw new IllegalArgumentException("Unsupported/incomplete AdvancedModelBox dialect", failure);
            }
        }

        private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }
}
