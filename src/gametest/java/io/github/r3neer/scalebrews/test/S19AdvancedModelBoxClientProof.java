package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Real optional-mod acceptance proof for the S19 AdvancedModelBox family. */
public final class S19AdvancedModelBoxClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final String GAZELLE = "com.github.alexthe666.alexsmobs.client.model.ModelGazelle";
    private static final String ADVANCED_BOX = "com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedModelBox";
    private static final String BASIC_PART = "com.github.alexthe666.alexsmobs.citadel.client.model.basic.BasicModelPart";
    private static final String MODEL_BOX = "com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModelRenderUtils$ModelBox";
    private static final String TEXTURED_QUAD = "com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModelRenderUtils$TexturedQuad";
    private static final String POSITION_VERTEX = "com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModelRenderUtils$PositionTextureVertex";
    private static final double EPS2 = 2.5e-9;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs")) {
                System.out.println("S19_ADVANCED_MODEL_BOX optional Alex input absent; real-model proof skipped");
                return;
            }
            prove(Identifier.parse("alexsmobs:grizzly_bear"), GRIZZLY, true);
            prove(Identifier.parse("alexsmobs:gazelle"), GAZELLE, true);
        });
    }

    private static void prove(Identifier id, String modelClass, boolean adultGrizzly) {
        Supplier<Object> factory = () -> fresh(modelClass, adultGrizzly);
        AdvancedModelBoxGeometryEngine.registerSource(id,
            new AdvancedModelBoxGeometryEngine.Source(factory, () -> sourceModelTransform(id)));
        var engine = (AdvancedModelBoxGeometryEngine) clientDelegate();
        var prepared = engine.prepareDetailed(new GeometryEngine.Request(id)).orElseThrow();
        var repeated = engine.prepareDetailed(new GeometryEngine.Request(id)).orElseThrow();
        if (!prepared.equals(repeated))
            throw new AssertionError("S19 repeated preparation changed DTO/diagnostics for " + id);

        Object oracleModel = fresh(modelClass, adultGrizzly);
        Oracle oracle = oracle(oracleModel);
        var geometry = prepared.geometry();
        Matrix4f expectedModelTransform = sourceModelTransform(id);
        if (!geometry.modelTransform().equals(ModelGeometry.values(expectedModelTransform)))
            throw new AssertionError("S19 serialized concrete renderer modelTransform mismatch for " + id
                + ": expected=" + ModelGeometry.values(expectedModelTransform)
                + " actual=" + geometry.modelTransform());
        Set<String> actualPieces = new HashSet<>();
        geometry.pieces().forEach(piece -> actualPieces.add(piece.id()));
        if (!actualPieces.equals(oracle.volumetricPieces))
            throw new AssertionError("S19 source cube coverage mismatch for " + id + ": expected="
                + oracle.volumetricPieces.size() + " actual=" + actualPieces.size());
        Set<String> actualOmissions = new HashSet<>();
        prepared.omissions().forEach(omission -> actualOmissions.add(omission.pieceId()));
        if (!actualOmissions.equals(oracle.nonVolumetricPieces))
            throw new AssertionError("S19 deliberate omission mismatch for " + id + ": expected="
                + oracle.nonVolumetricPieces + " actual=" + actualOmissions);
        for (String piece : actualPieces) if (piece.contains("/unscaled_children/"))
            throw new AssertionError("Transform-only scaleChildren helper leaked into stable source piece id: " + piece);

        // The independent renderer oracle emits local model vertices under an identity PoseStack.
        // Apply the frozen concrete renderer transform separately, rather than reading it back from
        // the exported DTO, so a lost modelTransform cannot be hidden by two matching mistakes.
        List<Vec3> renderedLocal = renderVertices(oracleModel);
        if (renderedLocal.isEmpty()) throw new AssertionError("S19 reference renderer emitted no vertices for " + id);
        List<Vec3> renderedCommon = renderedLocal.stream()
            .map(vertex -> transform(vertex, expectedModelTransform)).toList();
        var boxes = geometry.evaluate(expectedModelTransform, Map.of(), AnatomyFilter.DEFAULT);
        for (var entry : boxes.entrySet()) {
            for (Vec3 vertex : entry.getValue().vertices()) {
                if (renderedCommon.stream().noneMatch(reference -> reference.distanceToSqr(vertex) <= EPS2))
                    throw new AssertionError("S19 collider vertex is absent from independently transformed real renderer for " + id
                        + " piece=" + entry.getKey() + " vertex=" + vertex);
            }
        }
        if (id.getPath().equals("gazelle") && actualOmissions.isEmpty())
            throw new AssertionError("Gazelle proof did not exercise any source render-only non-volume primitive");
        if (id.getPath().equals("gazelle") && Math.abs(expectedModelTransform.getScale(new Vector3f()).x() - .8f) > 1e-6f)
            throw new AssertionError("Gazelle proof lost the pinned renderer.scale(0.8) precondition");
        System.out.println("S19_ADVANCED_MODEL_BOX " + id + " parts=" + geometry.parts().size()
            + " pieces=" + actualPieces.size() + " omissions=" + actualOmissions.size()
            + " renderVertices=" + renderedLocal.size() + " scaleChildrenCompensations=" + oracle.compensatedScales
            + " nominalFlatInflated=" + oracle.nominalFlatInflated
            + " modelTransformScale=" + expectedModelTransform.getScale(new Vector3f()));
    }

    /**
     * Pinned adult render-space transform. Bytecode order is flip, concrete renderer scale, then
     * the common -1.501 translation. Source-specific transforms belong here, not in the extractor.
     */
    private static Matrix4f sourceModelTransform(Identifier id) {
        Matrix4f transform = new Matrix4f().scaling(-1f, -1f, 1f);
        if (id.getPath().equals("gazelle")) transform.scale(.8f, .8f, .8f);
        return transform.translate(0f, -1.501f, 0f);
    }

    private static Vec3 transform(Vec3 source, Matrix4f transform) {
        Vector3f value = transform.transformPosition((float)source.x, (float)source.y, (float)source.z, new Vector3f());
        return new Vec3(value.x(), value.y(), value.z());
    }

    /** Retrieve the installed client singleton through its public source view, without adding a second engine registry. */
    private static Object clientDelegate() {
        try {
            Field instance = AdvancedModelBoxGeometryEngine.class.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot reach S19 client engine singleton for acceptance proof", failure);
        }
    }

    private static Object fresh(String className, boolean adultGrizzly) {
        try {
            Object model = Class.forName(className).getConstructor().newInstance();
            if (adultGrizzly) {
                try { model.getClass().getField("young").setBoolean(model, false); }
                catch (NoSuchFieldException ignored) { /* exact 2.1.9 may not expose age state here */ }
            }
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned Alex model " + className, failure);
        }
    }

    private record Oracle(Set<String> volumetricPieces, Set<String> nonVolumetricPieces,
                          int compensatedScales, int nominalFlatInflated) {}

    private static Oracle oracle(Object model) {
        try {
            ClassLoader loader = model.getClass().getClassLoader();
            Class<?> advanced = Class.forName(ADVANCED_BOX, false, loader);
            Class<?> basic = Class.forName(BASIC_PART, false, loader);
            Class<?> boxType = Class.forName(MODEL_BOX, false, loader);
            Class<?> quadType = Class.forName(TEXTURED_QUAD, false, loader);
            Class<?> vertexType = Class.forName(POSITION_VERTEX, false, loader);
            Field name = field(advanced, "boxName"), cubes = field(advanced, "cubeList"), children = field(advanced, "childModels");
            Field sx = field(advanced, "scaleX"), sy = field(advanced, "scaleY"), sz = field(advanced, "scaleZ"), scaleChildren = field(advanced, "scaleChildren");
            Field show = field(basic, "showModel");
            Field x1 = field(boxType, "posX1"), y1 = field(boxType, "posY1"), z1 = field(boxType, "posZ1");
            Field x2 = field(boxType, "posX2"), y2 = field(boxType, "posY2"), z2 = field(boxType, "posZ2"), quads = field(boxType, "quads");
            Field vertices = field(quadType, "vertexPositions"), position = field(vertexType, "position");
            Method parts = model.getClass().getMethod("parts");
            var volume = new HashSet<String>();
            var omitted = new HashSet<String>();
            int[] metrics = {0, 0};
            int rootCount = 0;
            for (Object root : iterable(parts.invoke(model))) {
                rootCount++;
                inspect(root, null, false, advanced, name, cubes, children, sx, sy, sz, scaleChildren, show,
                    x1, y1, z1, x2, y2, z2, quads, vertices, position, volume, omitted, metrics);
            }
            if (rootCount == 0) throw new AssertionError("Pinned Alex model has no parts() roots");
            return new Oracle(Set.copyOf(volume), Set.copyOf(omitted), metrics[0], metrics[1]);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Independent S19 source oracle failed", failure);
        }
    }

    private static void inspect(Object part, String parentId, boolean parentHidden, Class<?> advanced,
                                Field name, Field cubes, Field children, Field sx, Field sy, Field sz, Field scaleChildren, Field show,
                                Field x1, Field y1, Field z1, Field x2, Field y2, Field z2, Field quads, Field vertices, Field position,
                                Set<String> volume, Set<String> omitted, int[] metrics) throws IllegalAccessException {
        if (!advanced.isInstance(part)) throw new AssertionError("Independent oracle encountered non-AdvancedModelBox child");
        String local = (String)name.get(part);
        String id = parentId == null ? local : parentId + "/" + local;
        boolean hidden = parentHidden || !show.getBoolean(part);
        int index = 0;
        for (Object cube : iterable(cubes.get(part))) {
            String piece = id + "/cube_" + index++;
            boolean nominalFlat = dimension(x1, x2, cube) == 0 || dimension(y1, y2, cube) == 0 || dimension(z1, z2, cube) == 0;
            double[][] bounds = materializedBounds(quads.get(cube), vertices, position);
            boolean effective = bounds[1][0] > bounds[0][0] && bounds[1][1] > bounds[0][1] && bounds[1][2] > bounds[0][2];
            if (effective) {
                if (!volume.add(piece)) throw new AssertionError("Duplicate independent source piece id " + piece);
                if (nominalFlat) metrics[1]++;
            } else {
                if (!nominalFlat) throw new AssertionError("Volumetric source cube collapsed in pinned model: " + piece);
                if (!omitted.add(piece)) throw new AssertionError("Duplicate independent omission id " + piece);
            }
            if (hidden && effective) { /* counted structurally; renderer-vertex comparison filters it via ModelGeometry.evaluate */ }
        }
        float ax = ((Number)sx.get(part)).floatValue(), ay = ((Number)sy.get(part)).floatValue(), az = ((Number)sz.get(part)).floatValue();
        if (!scaleChildren.getBoolean(part) && (ax != 1f || ay != 1f || az != 1f)) metrics[0]++;
        for (Object child : iterable(children.get(part)))
            inspect(child, id, hidden, advanced, name, cubes, children, sx, sy, sz, scaleChildren, show,
                x1, y1, z1, x2, y2, z2, quads, vertices, position, volume, omitted, metrics);
    }

    private static double dimension(Field low, Field high, Object cube) throws IllegalAccessException {
        double result = ((Number)high.get(cube)).doubleValue() - ((Number)low.get(cube)).doubleValue();
        if (!Double.isFinite(result) || result < 0) throw new AssertionError("Invalid nominal source dimension in independent oracle");
        return result;
    }

    private static double[][] materializedBounds(Object quadsValue, Field vertices, Field position) throws IllegalAccessException {
        if (!(quadsValue instanceof Object[] quads) || quads.length == 0) throw new AssertionError("Pinned ModelBox has no quads");
        double[] low = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] high = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (Object quad : quads) {
            Object raw = vertices.get(quad);
            if (!(raw instanceof Object[] points) || points.length != 4) throw new AssertionError("Pinned quad has invalid vertices");
            for (Object point : points) {
                Object rawPosition = position.get(point);
                if (!(rawPosition instanceof Vector3fc p)) throw new AssertionError("Pinned vertex position is incompatible");
                double[] value = {p.x() / 16d, p.y() / 16d, p.z() / 16d};
                for (int axis = 0; axis < 3; axis++) {
                    low[axis] = Math.min(low[axis], value[axis]);
                    high[axis] = Math.max(high[axis], value[axis]);
                }
            }
        }
        return new double[][]{low, high};
    }

    private static List<Vec3> renderVertices(Object model) {
        var result = new ArrayList<Vec3>();
        VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(S19AdvancedModelBoxClientProof.class.getClassLoader(),
            new Class[]{VertexConsumer.class}, (proxy, method, args) -> {
                if (method.getName().equals("addVertex") && args != null && args.length >= 3 && args[0] instanceof Number) {
                    result.add(new Vec3(((Number)args[0]).doubleValue(), ((Number)args[1]).doubleValue(), ((Number)args[2]).doubleValue()));
                }
                return method.getReturnType() == VertexConsumer.class ? proxy : primitiveDefault(method.getReturnType());
            });
        try {
            model.getClass().getMethod("renderToBuffer", PoseStack.class, VertexConsumer.class, int.class, int.class,
                    float.class, float.class, float.class, float.class)
                .invoke(model, new PoseStack(), consumer, 0, 0, 1f, 1f, 1f, 1f);
            return List.copyOf(result);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot capture pinned Alex renderer vertices", failure);
        }
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return (char)0;
        if (type == byte.class) return (byte)0;
        if (type == short.class) return (short)0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Iterable<?> iterable(Object value) {
        if (!(value instanceof Iterable<?> iterable)) throw new AssertionError("Pinned Alex source collection is not iterable");
        return iterable;
    }
}
