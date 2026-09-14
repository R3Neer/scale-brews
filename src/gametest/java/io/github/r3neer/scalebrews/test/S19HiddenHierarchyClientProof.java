package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import java.lang.reflect.Field;
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

/** Real-Citadel holdout for showModel=false propagation through an AdvancedModelBox subtree. */
public final class S19HiddenHierarchyClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier SOURCE = Identifier.parse("test:s19_hidden_grizzly_body");
    private static final double EPS2 = 2.5e-9;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 hidden-hierarchy proof requires the pinned Alex external input");

            Supplier<Object> factory = S19HiddenHierarchyClientProof::freshHiddenBodyGrizzly;
            AdvancedModelBoxGeometryEngine.registerSource(SOURCE,
                new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
            var geometry = clientDelegate().prepareDetailed(new GeometryEngine.Request(SOURCE)).orElseThrow().geometry();

            String bodyId = geometry.parts().stream()
                .map(part -> part.id())
                .filter(id -> id.equals("body") || id.endsWith("/body"))
                .findFirst().orElseThrow(() -> new AssertionError("Hidden Grizzly fixture lost structural body id"));

            var bodyPieces = geometry.pieces().stream()
                .filter(piece -> piece.part().equals(bodyId) || piece.part().startsWith(bodyId + "/"))
                .toList();
            if (bodyPieces.isEmpty())
                throw new AssertionError("Hidden Grizzly body fixture contains no physical source pieces");
            long descendantPieces = bodyPieces.stream().filter(piece -> piece.part().startsWith(bodyId + "/")).count();
            if (descendantPieces == 0)
                throw new AssertionError("Hidden Grizzly body fixture does not exercise descendant geometry");
            for (var piece : bodyPieces) {
                if (!"source_hidden".equals(piece.excluded()))
                    throw new AssertionError("S19 hidden ancestor did not propagate source_hidden to subtree piece: "
                        + piece.id() + " part=" + piece.part() + " excluded=" + piece.excluded());
            }

            var evaluated = geometry.evaluate(new Matrix4f(), Map.of(), AnatomyFilter.DEFAULT);
            for (var piece : bodyPieces) if (evaluated.containsKey(piece.id()))
                throw new AssertionError("S19 hidden ancestor leaked descendant collider into evaluated anatomy: " + piece.id());

            Object oracleModel = freshHiddenBodyGrizzly();
            List<Vec3> rendered = renderVertices(oracleModel);
            for (var entry : evaluated.entrySet()) for (Vec3 vertex : entry.getValue().vertices()) {
                if (rendered.stream().noneMatch(reference -> reference.distanceToSqr(vertex) <= EPS2))
                    throw new AssertionError("S19 evaluated collider is absent from hidden real renderer: piece="
                        + entry.getKey() + " vertex=" + vertex);
            }

            System.out.println("S19_HIDDEN_HIERARCHY PASS body=" + bodyId + " hiddenPieces=" + bodyPieces.size()
                + " hiddenDescendantPieces=" + descendantPieces + " retainedPieces=" + evaluated.size()
                + " renderVertices=" + rendered.size());
        });
    }

    private static Object freshHiddenBodyGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) { /* exact pinned dialect may not expose age here */ }
            Object body = model.getClass().getField("body").get(model);
            Field children = field(body.getClass(), "childModels");
            Object rawChildren = children.get(body);
            if (!(rawChildren instanceof Iterable<?> iterable) || !iterable.iterator().hasNext())
                throw new AssertionError("Pinned Grizzly body has no child hierarchy to hide");
            boolean visibleDescendant = false;
            for (Object child : iterable) {
                if (field(child.getClass(), "showModel").getBoolean(child)) {
                    visibleDescendant = true;
                    break;
                }
            }
            if (!visibleDescendant)
                throw new AssertionError("Pinned Grizzly body has no independently visible direct child");
            field(body.getClass(), "showModel").setBoolean(body, false);
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare pinned hidden Grizzly fixture", failure);
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
        VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(S19HiddenHierarchyClientProof.class.getClassLoader(),
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
            throw new AssertionError("Cannot capture hidden pinned Grizzly renderer vertices", failure);
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
