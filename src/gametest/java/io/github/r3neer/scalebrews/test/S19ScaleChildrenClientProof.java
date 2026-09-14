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
 * Real Citadel render oracle for AdvancedModelBox scaleChildren=false.
 * The production extractor must insert inverse scale only in the transform chain of children.
 */
public final class S19ScaleChildrenClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier SOURCE = Identifier.parse("test:s19_scale_children_grizzly");
    private static final double EPS2 = 2.5e-9;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 scaleChildren proof requires the pinned Alex external input");

            Supplier<Object> factory = S19ScaleChildrenClientProof::freshMutatedAdultGrizzly;
            AdvancedModelBoxGeometryEngine.registerSource(SOURCE,
                new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
            var engine = (AdvancedModelBoxGeometryEngine) clientDelegate();
            var prepared = engine.prepareDetailed(new GeometryEngine.Request(SOURCE)).orElseThrow();
            var geometry = prepared.geometry();

            long helpers = geometry.parts().stream().filter(part -> part.id().endsWith("/unscaled_children")).count();
            if (helpers != 1)
                throw new AssertionError("Real scaleChildren=false mutation must produce exactly one transform-only helper, found " + helpers);
            if (geometry.pieces().stream().anyMatch(piece -> piece.id().contains("/unscaled_children")))
                throw new AssertionError("Transform-only child-scale helper leaked into stable physical piece ids");
            var helper = geometry.parts().stream().filter(part -> part.id().endsWith("/unscaled_children")).findFirst().orElseThrow();
            if (!helper.parent().endsWith("/body"))
                throw new AssertionError("Child-scale compensation must belong directly below mutated body, helper=" + helper);

            Object oracleModel = freshMutatedAdultGrizzly();
            List<Vec3> rendered = renderVertices(oracleModel);
            if (rendered.isEmpty()) throw new AssertionError("Pinned mutated Grizzly renderer emitted no vertices");
            var boxes = geometry.evaluate(new Matrix4f(), Map.of(), AnatomyFilter.DEFAULT);
            if (boxes.isEmpty()) throw new AssertionError("Mutated real Grizzly extraction produced no physical boxes");
            int compared = 0;
            for (var entry : boxes.entrySet()) {
                for (Vec3 vertex : entry.getValue().vertices()) {
                    compared++;
                    if (rendered.stream().noneMatch(reference -> reference.distanceToSqr(vertex) <= EPS2))
                        throw new AssertionError("S19 scaleChildren collider vertex diverges from real Citadel renderer: piece="
                            + entry.getKey() + " vertex=" + vertex);
                }
            }
            if (compared == 0) throw new AssertionError("S19 scaleChildren proof compared no collider vertices");
            System.out.println("S19_SCALE_CHILDREN PASS helpers=" + helpers + " pieces=" + boxes.size()
                + " comparedVertices=" + compared + " renderVertices=" + rendered.size());
        });
    }

    private static Object freshMutatedAdultGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            Field young = model.getClass().getField("young");
            young.setBoolean(model, false);
            Object body = model.getClass().getField("body").get(model);
            Method setScale = body.getClass().getMethod("setScale", float.class, float.class, float.class);
            Method setScaleChildren = body.getClass().getMethod("setShouldScaleChildren", boolean.class);
            setScale.invoke(body, 1.5f, .75f, 1.25f);
            setScaleChildren.invoke(body, false);
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare real pinned Grizzly scaleChildren fixture", failure);
        }
    }

    private static List<Vec3> renderVertices(Object model) {
        var result = new ArrayList<Vec3>();
        VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(S19ScaleChildrenClientProof.class.getClassLoader(),
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
            throw new AssertionError("Cannot capture real pinned Grizzly renderer vertices", failure);
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

    private static Object clientDelegate() {
        try {
            Field instance = AdvancedModelBoxGeometryEngine.class.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot reach S19 client engine singleton", failure);
        }
    }
}
