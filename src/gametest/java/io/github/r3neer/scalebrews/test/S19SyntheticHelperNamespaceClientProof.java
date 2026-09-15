package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
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

/**
 * Adversarial S19 proof that transform-only helper identifiers cannot collide with valid source
 * boxName paths. A real Citadel child called "unscaled_children" remains valid renderer input.
 */
public final class S19SyntheticHelperNamespaceClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier SOURCE = Identifier.parse("test:s19_synthetic_helper_namespace");
    private static final double EPS2 = 2.5e-9;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 helper-namespace proof requires the pinned Alex external input");

            Object rendererProbe = freshCollidingNameGrizzly();
            List<Vec3> probeVertices = renderVertices(rendererProbe);
            if (probeVertices.isEmpty())
                throw new AssertionError("Pinned Grizzly renderer rejected the helper-name collision fixture");

            Supplier<Object> factory = S19SyntheticHelperNamespaceClientProof::freshCollidingNameGrizzly;
            AdvancedModelBoxGeometryEngine.registerSource(SOURCE,
                new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));

            var prepared = clientDelegate().prepareDetailed(new GeometryEngine.Request(SOURCE)).orElseThrow();
            var geometry = prepared.geometry();
            assertDisjointHelperNamespace(geometry);
            var boxes = geometry.evaluate(new Matrix4f(), Map.of(), AnatomyFilter.DEFAULT);
            if (boxes.isEmpty())
                throw new AssertionError("Valid helper-name collision fixture prepared no physical geometry");

            Object oracleModel = freshCollidingNameGrizzly();
            List<Vec3> rendered = renderVertices(oracleModel);
            int compared = 0;
            for (var entry : boxes.entrySet()) for (Vec3 vertex : entry.getValue().vertices()) {
                compared++;
                if (rendered.stream().noneMatch(reference -> reference.distanceToSqr(vertex) <= EPS2))
                    throw new AssertionError("S19 helper-namespace collider diverges from real Citadel renderer: piece="
                        + entry.getKey() + " vertex=" + vertex);
            }
            if (compared == 0) throw new AssertionError("S19 helper-namespace proof compared no collider vertices");

            System.out.println("S19_HELPER_NAMESPACE PASS pieces=" + boxes.size()
                + " comparedVertices=" + compared + " renderVertices=" + rendered.size());
        });
    }

    private static void assertDisjointHelperNamespace(io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry) {
        String canonicalChildId = "root/body/unscaled_children";
        if (!sourceNamespacePath(canonicalChildId))
            throw new AssertionError("Helper proof canonical source path fixture is invalid");

        var sourceChild = geometry.parts().stream()
            .filter(part -> canonicalChildId.equals(part.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Valid source child lost canonical id " + canonicalChildId));
        if (sourceChild.sourcePose() == null)
            throw new AssertionError("Canonical source child was replaced by a transform-only helper");

        String helperId = sourceChild.parent();
        if (helperId == null)
            throw new AssertionError("scaleChildren=false source child has no compensation parent");
        if (sourceNamespacePath(helperId))
            throw new AssertionError("Synthetic helper leaked into valid Citadel source namespace: " + helperId);

        var helper = geometry.parts().stream()
            .filter(part -> helperId.equals(part.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing scaleChildren=false transform-only helper " + helperId));
        if (!"root/body".equals(helper.parent()))
            throw new AssertionError("Synthetic helper has wrong structural parent: " + helper.parent());
        if (helper.sourcePose() != null)
            throw new AssertionError("Synthetic helper incorrectly carries source pose metadata");
        if (geometry.pieces().stream().anyMatch(piece -> helperId.equals(piece.part())))
            throw new AssertionError("Synthetic helper incorrectly owns physical source pieces");
    }

    private static boolean sourceNamespacePath(String id) {
        if (id == null || id.isBlank()) return false;
        for (String segment : id.split("/", -1))
            if (!segment.matches("[A-Za-z0-9_.-]{1,64}")) return false;
        return true;
    }

    private static Object freshCollidingNameGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) { /* exact pinned dialect may not expose age here */ }

            Object body = model.getClass().getField("body").get(model);
            Method setScale = body.getClass().getMethod("setScale", float.class, float.class, float.class);
            Method setScaleChildren = body.getClass().getMethod("setShouldScaleChildren", boolean.class);
            setScale.invoke(body, 1.5f, .75f, 1.25f);
            setScaleChildren.invoke(body, false);

            Object childrenValue = field(body.getClass(), "childModels").get(body);
            if (!(childrenValue instanceof List<?> children) || children.isEmpty())
                throw new AssertionError("Pinned Grizzly body has no mutable direct child list for helper namespace fixture");
            Object child = children.getFirst();
            Field boxName = field(child.getClass(), "boxName");
            String original = String.valueOf(boxName.get(child));
            if ("unscaled_children".equals(original))
                throw new AssertionError("Pinned Grizzly unexpectedly already uses the synthetic helper name");
            boxName.set(child, "unscaled_children");
            if (!"unscaled_children".equals(boxName.get(child)))
                throw new AssertionError("Could not establish source boxName/helper collision fixture");
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare pinned helper-namespace Grizzly fixture", failure);
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

    private static List<Vec3> renderVertices(Object model) {
        var result = new ArrayList<Vec3>();
        VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(S19SyntheticHelperNamespaceClientProof.class.getClassLoader(),
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
            throw new AssertionError("Cannot capture helper-namespace pinned Grizzly renderer vertices", failure);
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
