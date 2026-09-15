package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Adversarial S19 lifecycle proof: source preparation must be history-independent and fail closed
 * when tooling supplies reused mutable model identity, unstable root transforms, alternating fresh
 * model geometry, or malformed geometry discovered only after a valid traversal prefix. The tooling
 * source view must also be deterministic regardless of registration order, and caller-owned mutable
 * transform objects must be materialized defensively.
 */
public final class S19SourceDeterminismClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier CONTROL = Identifier.parse("test:s19_deterministic_source_control");
    private static final Identifier REUSED = Identifier.parse("test:s19_reused_model_identity");
    private static final Identifier TRANSFORM = Identifier.parse("test:s19_unstable_model_transform");
    private static final Identifier GEOMETRY = Identifier.parse("test:s19_unstable_model_geometry");
    private static final Identifier LATE_MALFORMED = Identifier.parse("test:s19_late_malformed_geometry");
    private static final Identifier ORDER_Z = Identifier.parse("test:s19_order_z");
    private static final Identifier ORDER_A = Identifier.parse("test:s19_order_a");
    private static final Identifier ORDER_M = Identifier.parse("test:s19_order_m");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 source-determinism proof requires the pinned Alex external input");

            var engine = clientDelegate();

            AdvancedModelBoxGeometryEngine.registerSource(CONTROL,
                new AdvancedModelBoxGeometryEngine.Source(S19SourceDeterminismClientProof::freshAdultGrizzly, Matrix4f::new));
            var controlA = engine.prepareDetailed(new GeometryEngine.Request(CONTROL)).orElseThrow();
            var controlB = engine.prepareDetailed(new GeometryEngine.Request(CONTROL)).orElseThrow();
            if (!controlA.equals(controlB))
                throw new AssertionError("S19 deterministic fresh source changed across repeated preparation");

            Object reused = freshAdultGrizzly();
            AdvancedModelBoxGeometryEngine.registerSource(REUSED,
                new AdvancedModelBoxGeometryEngine.Source(() -> reused, Matrix4f::new));
            requireFailure(engine, REUSED, "reused mutable model identity", "S19 reused model identity was accepted");

            // Return the SAME mutable object twice and mutate it before the second read. The engine
            // must have snapshotted the first value immediately; retaining the caller-owned reference
            // would make both reads appear equal after the mutation.
            Matrix4f sharedTransform = new Matrix4f();
            AtomicInteger transformCalls = new AtomicInteger();
            Supplier<Matrix4f> mutatingSharedTransform = () -> {
                if (transformCalls.getAndIncrement() != 0) sharedTransform.translation(.125f, 0f, 0f);
                return sharedTransform;
            };
            AdvancedModelBoxGeometryEngine.registerSource(TRANSFORM,
                new AdvancedModelBoxGeometryEngine.Source(S19SourceDeterminismClientProof::freshAdultGrizzly, mutatingSharedTransform));
            requireFailure(engine, TRANSFORM, "source transform is not reproducible",
                "S19 unstable model transform was accepted");

            AtomicInteger modelCalls = new AtomicInteger();
            Supplier<Object> alternatingGeometry = () -> {
                Object model = freshAdultGrizzly();
                if (modelCalls.getAndIncrement() % 2 != 0) setBodyScale(model, 1.25f, 1f, 1f);
                return model;
            };
            AdvancedModelBoxGeometryEngine.registerSource(GEOMETRY,
                new AdvancedModelBoxGeometryEngine.Source(alternatingGeometry, Matrix4f::new));
            requireFailure(engine, GEOMETRY, "source factory is not deterministic",
                "S19 alternating fresh model geometry was accepted");

            // Corrupt only the LAST cube in the actual depth-first renderer hierarchy. Valid parts
            // and cubes have already been traversed before this empty-quad primitive is discovered.
            // The whole source must fail closed; publishing the prefix already accumulated is forbidden.
            AdvancedModelBoxGeometryEngine.registerSource(LATE_MALFORMED,
                new AdvancedModelBoxGeometryEngine.Source(S19SourceDeterminismClientProof::freshLateMalformedGrizzly, Matrix4f::new));
            requireFailure(engine, LATE_MALFORMED, "ModelBox has no materialized quads",
                "S19 late malformed cube published a partial geometry prefix");

            // Registration order is deliberately Z, A, M. The public tooling view must expose the
            // same canonical ordering independent of insertion history.
            AdvancedModelBoxGeometryEngine.registerSource(ORDER_Z,
                new AdvancedModelBoxGeometryEngine.Source(S19SourceDeterminismClientProof::freshAdultGrizzly, Matrix4f::new));
            AdvancedModelBoxGeometryEngine.registerSource(ORDER_A,
                new AdvancedModelBoxGeometryEngine.Source(S19SourceDeterminismClientProof::freshAdultGrizzly, Matrix4f::new));
            AdvancedModelBoxGeometryEngine.registerSource(ORDER_M,
                new AdvancedModelBoxGeometryEngine.Source(S19SourceDeterminismClientProof::freshAdultGrizzly, Matrix4f::new));
            List<Identifier> order = AdvancedModelBoxGeometryEngine.sources().keySet().stream()
                .filter(id -> id.getNamespace().equals("test") && id.getPath().startsWith("s19_order_"))
                .toList();
            List<Identifier> expectedOrder = List.of(ORDER_A, ORDER_M, ORDER_Z);
            if (!order.equals(expectedOrder))
                throw new AssertionError("S19 source registry view depends on registration order: expected="
                    + expectedOrder + " actual=" + order);

            System.out.println("S19_SOURCE_DETERMINISM PASS control=repeated reused=closed transform=closed geometry=closed lateMalformed=closed order=canonical");
        });
    }

    private static void requireFailure(AdvancedModelBoxGeometryEngine engine, Identifier source,
                                       String expectedCause, String acceptedMessage) {
        try {
            var result = engine.prepareDetailed(new GeometryEngine.Request(source));
            if (result.isPresent()) throw new AssertionError(acceptedMessage);
            throw new AssertionError("S19 invalid source returned Optional.empty instead of an explicit fail-closed preparation error: " + source);
        } catch (IllegalArgumentException expected) {
            if (!causeContains(expected, expectedCause))
                throw new AssertionError("S19 invalid source failed for the wrong reason: source=" + source
                    + " expectedCause='" + expectedCause + "' actual=" + expected, expected);
        }
    }

    private static boolean causeContains(Throwable failure, String needle) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(needle)) return true;
        }
        return false;
    }

    private static Object freshAdultGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) { /* exact pinned dialect may not expose age state here */ }
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned Grizzly source", failure);
        }
    }

    private static Object freshLateMalformedGrizzly() {
        try {
            Object model = freshAdultGrizzly();
            Method parts = model.getClass().getMethod("parts");
            Object rootsValue = parts.invoke(model);
            if (!(rootsValue instanceof Iterable<?> roots))
                throw new AssertionError("Pinned Grizzly parts() is not iterable");

            var cubes = new ArrayList<Object>();
            for (Object root : roots) collectCubesDepthFirst(root, cubes);
            if (cubes.size() < 2)
                throw new AssertionError("Pinned Grizzly does not provide a valid traversal prefix before malformed tail cube");

            Object tailCube = cubes.getLast();
            Field quads = field(tailCube.getClass(), "quads");
            Class<?> component = quads.getType().getComponentType();
            if (component == null)
                throw new AssertionError("Pinned Grizzly ModelBox quads field is not an array");
            quads.set(tailCube, Array.newInstance(component, 0));
            if (Array.getLength(quads.get(tailCube)) != 0)
                throw new AssertionError("Could not establish late malformed empty-quad cube fixture");
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare late malformed pinned Grizzly source", failure);
        }
    }

    private static void collectCubesDepthFirst(Object part, List<Object> cubes) throws ReflectiveOperationException {
        Object cubeValue = field(part.getClass(), "cubeList").get(part);
        if (!(cubeValue instanceof Iterable<?> cubeList))
            throw new AssertionError("Pinned Grizzly AdvancedModelBox cubeList is not iterable");
        for (Object cube : cubeList) cubes.add(cube);

        Object childrenValue = field(part.getClass(), "childModels").get(part);
        if (!(childrenValue instanceof Iterable<?> children))
            throw new AssertionError("Pinned Grizzly AdvancedModelBox childModels is not iterable");
        for (Object child : children) collectCubesDepthFirst(child, cubes);
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

    private static void setBodyScale(Object model, float x, float y, float z) {
        try {
            Field bodyField = model.getClass().getField("body");
            Object body = bodyField.get(model);
            Method setScale = body.getClass().getMethod("setScale", float.class, float.class, float.class);
            setScale.invoke(body, x, y, z);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot mutate pinned Grizzly body scale", failure);
        }
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
