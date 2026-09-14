package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Negative S19 control: genuinely ambiguous sibling source ids must remain fail-closed. */
public final class S19DuplicateSourceIdClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier SOURCE = Identifier.parse("test:s19_duplicate_source_siblings");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 duplicate-source-id proof requires the pinned Alex external input");

            Object rendererProbe = freshDuplicateSiblingNameGrizzly();
            if (renderVertexCount(rendererProbe) == 0)
                throw new AssertionError("Pinned renderer rejected duplicate-name fixture before Scale extraction");

            Supplier<Object> factory = S19DuplicateSourceIdClientProof::freshDuplicateSiblingNameGrizzly;
            AdvancedModelBoxGeometryEngine.registerSource(SOURCE,
                new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));

            try {
                var result = clientDelegate().prepareDetailed(new GeometryEngine.Request(SOURCE));
                if (result.isPresent())
                    throw new AssertionError("S19 genuinely duplicate sibling source ids were accepted");
                throw new AssertionError("S19 duplicate sibling source ids returned Optional.empty instead of explicit fail-closed error");
            } catch (IllegalArgumentException expected) {
                if (!causeContains(expected, "Ambiguous AdvancedModelBox part id"))
                    throw new AssertionError("S19 duplicate sibling source ids failed for the wrong reason: " + expected, expected);
            }

            System.out.println("S19_DUPLICATE_SOURCE_ID PASS renderer_valid=true extractor_fail_closed=true");
        });
    }

    private static Object freshDuplicateSiblingNameGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) {}

            Object body = model.getClass().getField("body").get(model);
            Object childrenValue = field(body.getClass(), "childModels").get(body);
            if (!(childrenValue instanceof List<?> children) || children.size() < 2)
                throw new AssertionError("Pinned Grizzly body lacks two direct siblings for duplicate-id fixture");
            Object first = children.get(0), second = children.get(1);
            Field firstName = field(first.getClass(), "boxName"), secondName = field(second.getClass(), "boxName");
            String canonical = String.valueOf(firstName.get(first));
            if (canonical == null || canonical.isBlank())
                throw new AssertionError("Pinned first sibling has no canonical boxName");
            if (first == second)
                throw new AssertionError("Pinned Grizzly direct siblings unexpectedly alias the same object");
            secondName.set(second, canonical);
            if (!canonical.equals(secondName.get(second)))
                throw new AssertionError("Could not establish duplicate sibling boxName fixture");
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare duplicate sibling Grizzly fixture", failure);
        }
    }

    private static int renderVertexCount(Object model) {
        var vertices = new ArrayList<Object>();
        VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(S19DuplicateSourceIdClientProof.class.getClassLoader(),
            new Class[]{VertexConsumer.class}, (proxy, method, args) -> {
                if (method.getName().equals("addVertex") && args != null && args.length >= 3 && args[0] instanceof Number)
                    vertices.add(Boolean.TRUE);
                return method.getReturnType() == VertexConsumer.class ? proxy : primitiveDefault(method.getReturnType());
            });
        try {
            model.getClass().getMethod("renderToBuffer", PoseStack.class, VertexConsumer.class, int.class, int.class,
                    float.class, float.class, float.class, float.class)
                .invoke(model, new PoseStack(), consumer, 0, 0, 1f, 1f, 1f, 1f);
            return vertices.size();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot render duplicate sibling Grizzly fixture", failure);
        }
    }

    private static boolean causeContains(Throwable failure, String needle) {
        for (Throwable current = failure; current != null; current = current.getCause())
            if (current.getMessage() != null && current.getMessage().contains(needle)) return true;
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
