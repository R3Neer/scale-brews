package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Exact N/N+1 adversarial boundaries for S19 parts, hierarchy depth and primitives. */
public final class S19ExtractionLimitsClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final String ADVANCED_MODEL = "com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedEntityModel";
    private static final String ADVANCED_BOX = "com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedModelBox";
    private static final String BASIC_PART = "com.github.alexthe666.alexsmobs.citadel.client.model.basic.BasicModelPart";

    private static final int MAX_PARTS = 512;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_PRIMITIVES = 4096;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 extraction-limit proof requires the pinned Alex external input");

            expectAccepted(Identifier.parse("test:s19_parts_n"), () -> flatRoots(MAX_PARTS),
                MAX_PARTS, 0, "S19 exact part limit N was rejected");
            expectRejected(Identifier.parse("test:s19_parts_n_plus_one"), () -> flatRoots(MAX_PARTS + 1),
                "Too many AdvancedModelBox parts", "S19 part limit N+1 was accepted");

            // depth is measured from root=0, so MAX_DEPTH=64 means a 65-node chain is valid.
            expectAccepted(Identifier.parse("test:s19_depth_n"), () -> chain(MAX_DEPTH),
                MAX_DEPTH + 1, 0, "S19 exact hierarchy depth N was rejected");
            expectRejected(Identifier.parse("test:s19_depth_n_plus_one"), () -> chain(MAX_DEPTH + 1),
                "AdvancedModelBox hierarchy too deep", "S19 hierarchy depth N+1 was accepted");

            expectAccepted(Identifier.parse("test:s19_primitives_n"), () -> primitiveFixture(MAX_PRIMITIVES),
                1, MAX_PRIMITIVES, "S19 exact primitive limit N was rejected");
            expectRejected(Identifier.parse("test:s19_primitives_n_plus_one"), () -> primitiveFixture(MAX_PRIMITIVES + 1),
                "Too many AdvancedModelBox primitives", "S19 primitive limit N+1 was accepted");

            System.out.println("S19_EXTRACTION_LIMITS PASS parts=" + MAX_PARTS + "/" + (MAX_PARTS + 1)
                + " depth=" + MAX_DEPTH + "/" + (MAX_DEPTH + 1)
                + " primitives=" + MAX_PRIMITIVES + "/" + (MAX_PRIMITIVES + 1));
        });
    }

    private static void expectAccepted(Identifier id, Supplier<Object> factory, int expectedParts,
                                       int expectedPieces, String failureMessage) {
        AdvancedModelBoxGeometryEngine.registerSource(id,
            new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
        try {
            var prepared = clientDelegate().prepareDetailed(new GeometryEngine.Request(id)).orElseThrow();
            if (prepared.geometry().parts().size() != expectedParts)
                throw new AssertionError(failureMessage + ": expectedParts=" + expectedParts
                    + " actual=" + prepared.geometry().parts().size());
            if (prepared.geometry().pieces().size() != expectedPieces)
                throw new AssertionError(failureMessage + ": expectedPieces=" + expectedPieces
                    + " actual=" + prepared.geometry().pieces().size());
        } catch (IllegalArgumentException failure) {
            throw new AssertionError(failureMessage + ": " + failure, failure);
        }
    }

    private static void expectRejected(Identifier id, Supplier<Object> factory, String expectedCause,
                                       String acceptedMessage) {
        AdvancedModelBoxGeometryEngine.registerSource(id,
            new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
        try {
            var result = clientDelegate().prepareDetailed(new GeometryEngine.Request(id));
            if (result.isPresent()) throw new AssertionError(acceptedMessage);
            throw new AssertionError("S19 oversized source returned Optional.empty instead of explicit fail-closed error: " + id);
        } catch (IllegalArgumentException expected) {
            if (!causeContains(expected, expectedCause))
                throw new AssertionError("S19 oversized source failed for wrong reason: id=" + id
                    + " expectedCause='" + expectedCause + "' actual=" + expected, expected);
        }
    }

    private static FixtureModel flatRoots(int count) {
        Object owner = freshOwner();
        var roots = new ArrayList<Object>(count);
        for (int i = 0; i < count; i++) roots.add(newBox(owner, "p" + i));
        return new FixtureModel(roots, roots);
    }

    /** @param depth edge depth from root; depth 0 is one node. */
    private static FixtureModel chain(int depth) {
        Object owner = freshOwner();
        var all = new ArrayList<Object>(depth + 1);
        Object root = newBox(owner, "d0");
        all.add(root);
        Object parent = root;
        for (int i = 1; i <= depth; i++) {
            Object child = newBox(owner, "d" + i);
            addChild(parent, child);
            all.add(child);
            parent = child;
        }
        return new FixtureModel(List.of(root), all);
    }

    private static FixtureModel primitiveFixture(int count) {
        Object owner = freshOwner();
        Object root = newBox(owner, "root");
        try {
            Method addBox = root.getClass().getMethod("addBox",
                float.class, float.class, float.class, float.class, float.class, float.class);
            for (int i = 0; i < count; i++)
                addBox.invoke(root, 0f, 0f, 0f, 1f, 1f, 1f);
            Object cubes = field(root.getClass(), "cubeList").get(root);
            if (!(cubes instanceof java.util.Collection<?> collection) || collection.size() != count)
                throw new AssertionError("Pinned AdvancedModelBox did not materialize requested primitive count " + count);
            return new FixtureModel(List.of(root), List.of(root));
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot build pinned primitive-limit fixture", failure);
        }
    }

    private static Object freshOwner() {
        try {
            Object owner = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { owner.getClass().getField("young").setBoolean(owner, false); }
            catch (NoSuchFieldException ignored) { /* pinned model may not expose age state here */ }
            return owner;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned AdvancedEntityModel owner", failure);
        }
    }

    private static Object newBox(Object owner, String name) {
        try {
            ClassLoader loader = owner.getClass().getClassLoader();
            Class<?> box = Class.forName(ADVANCED_BOX, false, loader);
            Class<?> model = Class.forName(ADVANCED_MODEL, false, loader);
            Constructor<?> constructor = box.getConstructor(model, String.class);
            return constructor.newInstance(owner, name);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned AdvancedModelBox " + name, failure);
        }
    }

    private static void addChild(Object parent, Object child) {
        try {
            Class<?> basic = Class.forName(BASIC_PART, false, parent.getClass().getClassLoader());
            parent.getClass().getMethod("addChild", basic).invoke(parent, child);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot link pinned AdvancedModelBox hierarchy", failure);
        }
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

    private static boolean causeContains(Throwable failure, String needle) {
        for (Throwable current = failure; current != null; current = current.getCause())
            if (current.getMessage() != null && current.getMessage().contains(needle)) return true;
        return false;
    }

    /** Public reflective surface intentionally matching the two methods consumed by the extractor. */
    public static final class FixtureModel {
        private final List<Object> roots;
        private final List<Object> all;

        FixtureModel(List<Object> roots, List<Object> all) {
            this.roots = List.copyOf(roots);
            this.all = List.copyOf(all);
        }

        public Iterable<Object> parts() { return roots; }
        public Iterable<Object> getAllParts() { return all; }
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
