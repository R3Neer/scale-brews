package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Real-Citadel S19 holdout: one AdvancedModelBox identity cannot be owned by two parents. */
public final class S19HierarchyOwnershipClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier SOURCE = Identifier.parse("test:s19_multiply_parented_grizzly");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 hierarchy-ownership proof requires the pinned Alex external input");

            Object fixture = freshMultiplyParentedGrizzly();
            Object head = get(fixture, "head");
            Object midbody = get(fixture, "midbody");
            Object snout = get(fixture, "snout");
            if (!containsIdentity(children(head), snout) || !containsIdentity(children(midbody), snout))
                throw new AssertionError("S19 fixture did not establish one child identity under two parents");

            Supplier<Object> factory = S19HierarchyOwnershipClientProof::freshMultiplyParentedGrizzly;
            AdvancedModelBoxGeometryEngine.registerSource(SOURCE,
                new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
            try {
                var result = clientDelegate().prepareDetailed(new GeometryEngine.Request(SOURCE));
                if (result.isPresent())
                    throw new AssertionError("S19 multiply-parented source was accepted and published duplicate ownership");
                throw new AssertionError("S19 multiply-parented source returned Optional.empty instead of explicit fail-closed error");
            } catch (IllegalArgumentException expected) {
                if (!causeContains(expected, "Cyclic or multiply-parented AdvancedModelBox hierarchy"))
                    throw new AssertionError("S19 multiply-parented source failed for the wrong reason: " + expected, expected);
            }

            System.out.println("S19_HIERARCHY_OWNERSHIP PASS duplicateChild=snout parents=head,midbody");
        });
    }

    private static Object freshMultiplyParentedGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) { /* exact pinned dialect may not expose age here */ }
            Object midbody = get(model, "midbody");
            Object snout = get(model, "snout");
            Method addChild = midbody.getClass().getMethod("addChild",
                Class.forName("com.github.alexthe666.alexsmobs.citadel.client.model.basic.BasicModelPart", false,
                    model.getClass().getClassLoader()));
            addChild.invoke(midbody, snout);
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare pinned multiply-parented Grizzly fixture", failure);
        }
    }

    private static Object get(Object owner, String field) {
        try {
            return owner.getClass().getField(field).get(owner);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot inspect pinned Grizzly field " + field, failure);
        }
    }

    private static Iterable<?> children(Object part) {
        try {
            Object value = field(part.getClass(), "childModels").get(part);
            if (!(value instanceof Iterable<?> iterable))
                throw new AssertionError("Pinned AdvancedModelBox childModels is not iterable");
            return iterable;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot inspect pinned AdvancedModelBox childModels", failure);
        }
    }

    private static boolean containsIdentity(Iterable<?> values, Object expected) {
        for (Object value : values) if (value == expected) return true;
        return false;
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
