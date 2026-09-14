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

/** Adversarial real-Citadel proof for signed AdvancedModelBox scales and child-scale cancellation. */
public final class S19SignedScaleChildrenClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier PROPAGATING = Identifier.parse("test:s19_signed_scale_propagating");
    private static final Identifier CANCELLED_CHILDREN = Identifier.parse("test:s19_signed_scale_cancelled_children");
    private static final double EPS2 = 2.5e-9;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 signed-scale proof requires the pinned Alex external input");

            // Control: signed scale itself is representable end-to-end when it propagates to children.
            prove(PROPAGATING, true, false);
            // Target: the same signed scale with scaleChildren=false must cancel with the exact signed reciprocal.
            prove(CANCELLED_CHILDREN, false, true);
        });
    }

    private static void prove(Identifier source, boolean scaleChildren, boolean requireHelper) {
        Supplier<Object> factory = () -> freshSignedAdultGrizzly(scaleChildren);
        AdvancedModelBoxGeometryEngine.registerSource(source,
            new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
        var engine = (AdvancedModelBoxGeometryEngine) clientDelegate();
        var geometry = engine.prepareDetailed(new GeometryEngine.Request(source)).orElseThrow().geometry();

        var body = geometry.parts().stream().filter(part -> part.id().endsWith("/body") && part.sourcePose() != null)
            .findFirst().orElseThrow(() -> new AssertionError("Signed-scale fixture lost Grizzly body SourcePose"));
        if (Math.abs(body.sourcePose().xScale() + 1.5f) > 1e-6f
                || Math.abs(body.sourcePose().yScale() - .75f) > 1e-6f
                || Math.abs(body.sourcePose().zScale() - 1.25f) > 1e-6f)
            throw new AssertionError("Signed source scale was not preserved in exported body pose: " + body.sourcePose());

        long helpers = geometry.parts().stream().filter(part -> part.id().endsWith("/unscaled_children")).count();
        if (requireHelper && helpers != 1)
            throw new AssertionError("scaleChildren=false signed fixture requires one inverse helper, found " + helpers);
        if (!requireHelper && helpers != 0)
            throw new AssertionError("scaleChildren=true signed control must not create inverse helpers, found " + helpers);

        if (requireHelper) {
            var helper = geometry.parts().stream().filter(part -> part.id().endsWith("/unscaled_children")).findFirst().orElseThrow();
            Matrix4f inverse = ModelGeometry.matrix(helper.transform());
            float[] expected = {-2f / 3f, 4f / 3f, .8f};
            float[] actual = {inverse.m00(), inverse.m11(), inverse.m22()};
            for (int i = 0; i < 3; i++) if (Math.abs(actual[i] - expected[i]) > 1e-5f)
                throw new AssertionError("Signed scaleChildren inverse is not the exact reciprocal: expected="
                    + java.util.Arrays.toString(expected) + " actual=" + java.util.Arrays.toString(actual));
        }

        Object oracle = freshSignedAdultGrizzly(scaleChildren);
        List<Vec3> rendered = renderVertices(oracle);
        var boxes = geometry.evaluate(new Matrix4f(), Map.of(), AnatomyFilter.DEFAULT);
        if (rendered.isEmpty() || boxes.isEmpty()) throw new AssertionError("Signed-scale real-model proof has no geometry");
        int compared = 0;
        for (var entry : boxes.entrySet()) for (Vec3 vertex : entry.getValue().vertices()) {
            compared++;
            if (rendered.stream().noneMatch(reference -> reference.distanceToSqr(vertex) <= EPS2))
                throw new AssertionError("S19 signed scaleChildren collider vertex diverges from real Citadel renderer: source="
                    + source + " piece=" + entry.getKey() + " vertex=" + vertex);
        }
        System.out.println("S19_SIGNED_SCALE PASS source=" + source + " scaleChildren=" + scaleChildren
            + " helpers=" + helpers + " comparedVertices=" + compared + " renderVertices=" + rendered.size());
    }

    private static Object freshSignedAdultGrizzly(boolean scaleChildren) {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            model.getClass().getField("young").setBoolean(model, false);
            Object body = model.getClass().getField("body").get(model);
            Method setScale = body.getClass().getMethod("setScale", float.class, float.class, float.class);
            Method setScaleChildren = body.getClass().getMethod("setShouldScaleChildren", boolean.class);
            setScale.invoke(body, -1.5f, .75f, 1.25f);
            setScaleChildren.invoke(body, scaleChildren);
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare real pinned Grizzly signed-scale fixture", failure);
        }
    }

    private static List<Vec3> renderVertices(Object model) {
        var result = new ArrayList<Vec3>();
        VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(S19SignedScaleChildrenClientProof.class.getClassLoader(),
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
            throw new AssertionError("Cannot capture signed-scale real Grizzly renderer vertices", failure);
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
