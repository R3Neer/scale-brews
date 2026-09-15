package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * S19 adversarial holdout: sibling enumeration order is not structural identity. Stable boxName
 * paths, transforms and materialized cubes must prepare identically even when childModels yields
 * equivalent siblings in another order.
 */
public final class S19ChildOrderDeterminismClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier CONTROL = Identifier.parse("test:s19_child_order_control");
    private static final Identifier PERMUTED = Identifier.parse("test:s19_child_order_permuted");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 child-order proof requires the pinned Alex external input");

            var engine = clientDelegate();
            AdvancedModelBoxGeometryEngine.registerSource(CONTROL,
                new AdvancedModelBoxGeometryEngine.Source(S19ChildOrderDeterminismClientProof::freshAdultGrizzly, Matrix4f::new));
            var control = engine.prepareDetailed(new GeometryEngine.Request(CONTROL)).orElseThrow();

            AtomicInteger calls = new AtomicInteger();
            Supplier<Object> alternatingOrder = () -> {
                Object model = freshAdultGrizzly();
                if ((calls.getAndIncrement() & 1) != 0) reverseBodyChildren(model);
                return model;
            };
            AdvancedModelBoxGeometryEngine.registerSource(PERMUTED,
                new AdvancedModelBoxGeometryEngine.Source(alternatingOrder, Matrix4f::new));

            AdvancedModelBoxGeometryEngine.Preparation permuted;
            try {
                permuted = engine.prepareDetailed(new GeometryEngine.Request(PERMUTED)).orElseThrow();
            } catch (IllegalArgumentException failure) {
                if (causeContains(failure, "source factory is not deterministic"))
                    throw new AssertionError("S19 sibling enumeration order incorrectly participates in source determinism", failure);
                throw failure;
            }

            var controlParts = canonicalParts(control.geometry());
            var permutedParts = canonicalParts(permuted.geometry());
            if (!controlParts.equals(permutedParts))
                throw new AssertionError("S19 sibling permutation changed canonical part identities/transforms");
            var controlPieces = canonicalPieces(control.geometry());
            var permutedPieces = canonicalPieces(permuted.geometry());
            if (!controlPieces.equals(permutedPieces))
                throw new AssertionError("S19 sibling permutation changed canonical physical pieces");
            var controlOmissions = control.omissions().stream()
                .map(o -> o.pieceId() + "|" + o.partId() + "|" + o.reason()).sorted().toList();
            var permutedOmissions = permuted.omissions().stream()
                .map(o -> o.pieceId() + "|" + o.partId() + "|" + o.reason()).sorted().toList();
            if (!controlOmissions.equals(permutedOmissions))
                throw new AssertionError("S19 sibling permutation changed canonical omissions");

            System.out.println("S19_CHILD_ORDER_DETERMINISM PASS parts=" + controlParts.size()
                + " pieces=" + controlPieces.size() + " omissions=" + controlOmissions.size());
        });
    }

    private static List<String> canonicalParts(ModelGeometry geometry) {
        return geometry.parts().stream()
            .map(part -> part.id() + "|" + part.parent() + "|" + part.transform() + "|" + part.sourcePose())
            .sorted().toList();
    }

    private static List<String> canonicalPieces(ModelGeometry geometry) {
        return geometry.pieces().stream()
            .map(piece -> piece.id() + "|" + piece.part() + "|" + piece.min() + "|" + piece.max() + "|" + piece.excluded())
            .sorted().toList();
    }

    private static Object freshAdultGrizzly() {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) { /* pinned dialect may not expose age here */ }
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned Grizzly source", failure);
        }
    }

    private static void reverseBodyChildren(Object model) {
        try {
            Object body = model.getClass().getField("body").get(model);
            Object value = field(body.getClass(), "childModels").get(body);
            if (!(value instanceof List<?> raw) || raw.size() < 2)
                throw new AssertionError("Pinned Grizzly body has fewer than two authoritative childModels entries");
            @SuppressWarnings("unchecked") List<Object> children = (List<Object>)raw;
            var before = new ArrayList<>(children);
            Collections.reverse(children);
            if (children.equals(before))
                throw new AssertionError("S19 child-order fixture failed to permute authoritative siblings");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot permute pinned Grizzly childModels", failure);
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
