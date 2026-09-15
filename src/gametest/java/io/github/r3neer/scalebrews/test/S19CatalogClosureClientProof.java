package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** S19 malformed-input holdout: every catalog part must be reachable exactly once from render roots. */
public final class S19CatalogClosureClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final String ADVANCED_MODEL = "com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedEntityModel";
    private static final String ADVANCED_BOX = "com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedModelBox";
    private static final Identifier SOURCE = Identifier.parse("test:s19_detached_catalog_part");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 catalog-closure proof requires the pinned Alex external input");

            Object fixture = detachedFixture();
            if (!(fixture instanceof FixtureModel model) || model.getAllParts().size() != 2 || model.parts().size() != 1)
                throw new AssertionError("S19 detached-part fixture did not establish catalog/tree mismatch");

            Supplier<Object> factory = S19CatalogClosureClientProof::detachedFixture;
            AdvancedModelBoxGeometryEngine.registerSource(SOURCE,
                new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
            try {
                var result = clientDelegate().prepareDetailed(new GeometryEngine.Request(SOURCE));
                if (result.isPresent())
                    throw new AssertionError("S19 detached catalog part was silently ignored and partial tree published");
                throw new AssertionError("S19 detached catalog source returned Optional.empty instead of explicit fail-closed error");
            } catch (IllegalArgumentException expected) {
                if (!causeContains(expected, "AdvancedModelBox catalog contains detached or multiply-owned parts"))
                    throw new AssertionError("S19 detached catalog source failed for the wrong reason: " + expected, expected);
            }

            System.out.println("S19_CATALOG_CLOSURE PASS roots=1 catalog=2 detached=1");
        });
    }

    private static Object detachedFixture() {
        Object owner = freshOwner();
        Object root = newBox(owner, "root");
        Object detached = newBox(owner, "detached");
        return new FixtureModel(List.of(root), List.of(root, detached));
    }

    private static Object freshOwner() {
        try {
            Object owner = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { owner.getClass().getField("young").setBoolean(owner, false); }
            catch (NoSuchFieldException ignored) { /* pinned model may not expose age here */ }
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

    public static final class FixtureModel {
        private final List<Object> roots;
        private final List<Object> all;
        FixtureModel(List<Object> roots, List<Object> all) { this.roots = List.copyOf(roots); this.all = List.copyOf(all); }
        public List<Object> parts() { return roots; }
        public List<Object> getAllParts() { return all; }
    }

    private static boolean causeContains(Throwable failure, String needle) {
        for (Throwable current = failure; current != null; current = current.getCause())
            if (current.getMessage() != null && current.getMessage().contains(needle)) return true;
        return false;
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
