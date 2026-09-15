package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

/**
 * Adversarial S19 proof for the pinned Citadel rule that nominal pre-inflation dimensions are not
 * renderer authority. A zero-width source cube with positive inflation is physically volumetric
 * because its materialized quads have positive extent on every axis.
 */
public final class S19MaterializedInflationClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier SOURCE = Identifier.parse("test:s19_materialized_inflation");
    private static final double EPS = 1e-7;
    private static final double RENDER_EPS2 = 2.5e-9;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 materialized-inflation proof requires the pinned Alex external input");

            Object baseline = freshAdultGrizzly();
            int baseBodyCubes = bodyCubes(baseline).size();
            if (baseBodyCubes <= 0)
                throw new AssertionError("Pinned Grizzly body has no baseline cube prefix for inflation holdout");

            Supplier<Object> factory = S19MaterializedInflationClientProof::freshInflatedFlatGrizzly;
            AdvancedModelBoxGeometryEngine.registerSource(SOURCE,
                new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
            var prepared = clientDelegate().prepareDetailed(new GeometryEngine.Request(SOURCE)).orElseThrow();
            var repeated = clientDelegate().prepareDetailed(new GeometryEngine.Request(SOURCE)).orElseThrow();
            if (!prepared.equals(repeated))
                throw new AssertionError("S19 materialized-inflation source is not deterministic across repeated preparation");

            ModelGeometry geometry = prepared.geometry();
            String bodyId = geometry.parts().stream().map(ModelGeometry.Part::id)
                .filter(id -> id.equals("body") || id.endsWith("/body"))
                .findFirst().orElseThrow(() -> new AssertionError("Pinned Grizzly fixture lost body structural id"));
            String expectedPieceId = bodyId + "/cube_" + baseBodyCubes;

            Object oracleModel = freshInflatedFlatGrizzly();
            Object sourceCube = bodyCubes(oracleModel).get(baseBodyCubes);
            Bounds oracle = materializedBounds(sourceCube);
            if (Math.abs(oracle.nominalX()) > EPS || oracle.nominalY() <= 0 || oracle.nominalZ() <= 0)
                throw new AssertionError("Citadel holdout did not establish exactly one zero nominal dimension: " + oracle);
            if (oracle.xSize() <= EPS || oracle.ySize() <= EPS || oracle.zSize() <= EPS)
                throw new AssertionError("Positive Citadel inflation failed to materialize a volumetric quad box: " + oracle);

            ModelGeometry.Piece piece = geometry.pieces().stream()
                .filter(candidate -> expectedPieceId.equals(candidate.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                    "S19 nominally flat inflated cube was not exported as physical piece: " + expectedPieceId));
            if (piece.excluded() != null)
                throw new AssertionError("S19 inflated physical cube was exported as excluded metadata: " + piece.excluded());
            if (prepared.omissions().stream().anyMatch(omission -> expectedPieceId.equals(omission.pieceId())))
                throw new AssertionError("S19 inflated physical cube was simultaneously classified as an omission");

            assertNear(piece.min().get(0), oracle.lowX(), "minX");
            assertNear(piece.min().get(1), oracle.lowY(), "minY");
            assertNear(piece.min().get(2), oracle.lowZ(), "minZ");
            assertNear(piece.max().get(0), oracle.highX(), "maxX");
            assertNear(piece.max().get(1), oracle.highY(), "maxY");
            assertNear(piece.max().get(2), oracle.highZ(), "maxZ");

            var evaluated = geometry.evaluate(new Matrix4f(), Map.of(), AnatomyFilter.DEFAULT);
            var collider = evaluated.get(expectedPieceId);
            if (collider == null)
                throw new AssertionError("S19 inflated physical cube disappeared during default anatomy evaluation");
            List<Vec3> rendered = renderVertices(oracleModel);
            if (rendered.isEmpty()) throw new AssertionError("Pinned inflated Grizzly renderer emitted no vertices");
            for (Vec3 vertex : collider.vertices()) {
                if (rendered.stream().noneMatch(reference -> reference.distanceToSqr(vertex) <= RENDER_EPS2))
                    throw new AssertionError("S19 inflated collider vertex absent from real Citadel renderer: " + vertex);
            }

            System.out.println("S19_MATERIALIZED_INFLATION PASS piece=" + expectedPieceId
                + " nominal=(" + oracle.nominalX() + "," + oracle.nominalY() + "," + oracle.nominalZ() + ")"
                + " materialized=(" + oracle.xSize() + "," + oracle.ySize() + "," + oracle.zSize() + ")"
                + " renderVertices=" + rendered.size());
        });
    }

    private static Object freshAdultGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) { /* exact pinned dialect may not expose age here */ }
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned Grizzly source", failure);
        }
    }

    private static Object freshInflatedFlatGrizzly() {
        try {
            Object model = freshAdultGrizzly();
            Object body = model.getClass().getField("body").get(model);
            int before = bodyCubes(model).size();
            Method addBox = body.getClass().getMethod("addBox",
                float.class, float.class, float.class, float.class, float.class, float.class, float.class);
            // Citadel ModelBox stores nominal posX2 = posX1 + width BEFORE inflation, while its
            // materialized quad vertices are expanded by inflate. Zero width + positive inflate
            // therefore gives exactly the S19 nominal-flat/materialized-volume decision boundary.
            addBox.invoke(body, 0f, 0f, 0f, 0f, 2f, 2f, .25f);
            if (bodyCubes(model).size() != before + 1)
                throw new AssertionError("Pinned AdvancedModelBox.addBox did not append exactly one inflation fixture cube");
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot create pinned Citadel inflation fixture", failure);
        }
    }

    private static List<Object> bodyCubes(Object model) {
        try {
            Object body = model.getClass().getField("body").get(model);
            Object value = field(body.getClass(), "cubeList").get(body);
            if (!(value instanceof Iterable<?> iterable))
                throw new AssertionError("Pinned Grizzly body cubeList is not iterable");
            var cubes = new ArrayList<Object>();
            for (Object cube : iterable) cubes.add(cube);
            return List.copyOf(cubes);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot inspect pinned Grizzly body cubes", failure);
        }
    }

    private static Bounds materializedBounds(Object cube) {
        try {
            double x1 = number(field(cube.getClass(), "posX1").get(cube));
            double y1 = number(field(cube.getClass(), "posY1").get(cube));
            double z1 = number(field(cube.getClass(), "posZ1").get(cube));
            double x2 = number(field(cube.getClass(), "posX2").get(cube));
            double y2 = number(field(cube.getClass(), "posY2").get(cube));
            double z2 = number(field(cube.getClass(), "posZ2").get(cube));
            Object quadsValue = field(cube.getClass(), "quads").get(cube);
            if (!(quadsValue instanceof Object[] quads) || quads.length == 0)
                throw new AssertionError("Pinned inflation fixture has no materialized quads");
            double[] low = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            double[] high = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (Object quad : quads) {
                Object verticesValue = field(quad.getClass(), "vertexPositions").get(quad);
                if (!(verticesValue instanceof Object[] vertices) || vertices.length != 4)
                    throw new AssertionError("Pinned inflation fixture quad has invalid vertices");
                for (Object vertex : vertices) {
                    Object position = field(vertex.getClass(), "position").get(vertex);
                    if (!(position instanceof Vector3fc p))
                        throw new AssertionError("Pinned inflation fixture vertex position is incompatible");
                    double[] value = {p.x() / 16d, p.y() / 16d, p.z() / 16d};
                    for (int axis = 0; axis < 3; axis++) {
                        low[axis] = Math.min(low[axis], value[axis]);
                        high[axis] = Math.max(high[axis], value[axis]);
                    }
                }
            }
            return new Bounds(x2 - x1, y2 - y1, z2 - z1,
                low[0], low[1], low[2], high[0], high[1], high[2]);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot inspect pinned inflation fixture bounds", failure);
        }
    }

    private record Bounds(double nominalX, double nominalY, double nominalZ,
                          double lowX, double lowY, double lowZ,
                          double highX, double highY, double highZ) {
        double xSize() { return highX - lowX; }
        double ySize() { return highY - lowY; }
        double zSize() { return highZ - lowZ; }
    }

    private static double number(Object value) {
        if (!(value instanceof Number number)) throw new AssertionError("Pinned numeric field changed type");
        double result = number.doubleValue();
        if (!Double.isFinite(result)) throw new AssertionError("Pinned numeric field became non-finite");
        return result;
    }

    private static void assertNear(double actual, double expected, String axis) {
        if (Math.abs(actual - expected) > 1e-7)
            throw new AssertionError("S19 materialized bound mismatch " + axis + ": expected=" + expected + " actual=" + actual);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static List<Vec3> renderVertices(Object model) {
        var result = new ArrayList<Vec3>();
        VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(S19MaterializedInflationClientProof.class.getClassLoader(),
            new Class[]{VertexConsumer.class}, (proxy, method, args) -> {
                if (method.getName().equals("addVertex") && args != null && args.length >= 3 && args[0] instanceof Number)
                    result.add(new Vec3(((Number)args[0]).doubleValue(), ((Number)args[1]).doubleValue(), ((Number)args[2]).doubleValue()));
                return method.getReturnType() == VertexConsumer.class ? proxy : primitiveDefault(method.getReturnType());
            });
        try {
            model.getClass().getMethod("renderToBuffer", PoseStack.class, VertexConsumer.class, int.class, int.class,
                    float.class, float.class, float.class, float.class)
                .invoke(model, new PoseStack(), consumer, 0, 0, 1f, 1f, 1f, 1f);
            return List.copyOf(result);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot capture pinned inflated Grizzly renderer vertices", failure);
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

    private static AdvancedModelBoxGeometryEngine clientDelegate() {
        try {
            Field instance = AdvancedModelBoxGeometryEngine.class.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            return (AdvancedModelBoxGeometryEngine)instance.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot reach S19 client geometry engine singleton", failure);
        }
    }
}
