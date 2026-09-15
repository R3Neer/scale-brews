package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
 * S19 adversarial holdout for the frozen metamorphic property: reordering an auxiliary equivalent
 * catalog enumeration (getAllParts) must not alter the canonical hierarchy obtained from the
 * unchanged roots/children graph.
 *
 * The historical filename remains for continuity, but this proof deliberately does NOT reorder
 * childModels: childModels is renderer-authoritative children, not an auxiliary catalog view.
 */
public final class S19ChildOrderDeterminismClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier CONTROL = Identifier.parse("test:s19_catalog_order_control");
    private static final Identifier PERMUTED = Identifier.parse("test:s19_catalog_order_permuted");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 auxiliary-order proof requires the pinned Alex external input");

            var engine = clientDelegate();
            AdvancedModelBoxGeometryEngine.registerSource(CONTROL,
                new AdvancedModelBoxGeometryEngine.Source(() -> freshCatalogView(false), Matrix4f::new));
            var control = engine.prepareDetailed(new GeometryEngine.Request(CONTROL)).orElseThrow();

            AtomicInteger calls = new AtomicInteger();
            Supplier<Object> alternatingCatalogOrder = () -> freshCatalogView((calls.getAndIncrement() & 1) != 0);
            AdvancedModelBoxGeometryEngine.registerSource(PERMUTED,
                new AdvancedModelBoxGeometryEngine.Source(alternatingCatalogOrder, Matrix4f::new));

            AdvancedModelBoxGeometryEngine.Preparation permuted;
            try {
                permuted = engine.prepareDetailed(new GeometryEngine.Request(PERMUTED)).orElseThrow();
            } catch (IllegalArgumentException failure) {
                if (causeContains(failure, "source factory is not deterministic"))
                    throw new AssertionError("S19 auxiliary getAllParts order incorrectly participates in source determinism", failure);
                throw failure;
            }

            var controlParts = canonicalParts(control.geometry());
            var permutedParts = canonicalParts(permuted.geometry());
            if (!controlParts.equals(permutedParts))
                throw new AssertionError("S19 auxiliary catalog permutation changed canonical part identities/transforms");
            var controlPieces = canonicalPieces(control.geometry());
            var permutedPieces = canonicalPieces(permuted.geometry());
            if (!controlPieces.equals(permutedPieces))
                throw new AssertionError("S19 auxiliary catalog permutation changed canonical physical pieces");
            var controlOmissions = control.omissions().stream()
                .map(o -> o.pieceId() + "|" + o.partId() + "|" + o.reason()).sorted().toList();
            var permutedOmissions = permuted.omissions().stream()
                .map(o -> o.pieceId() + "|" + o.partId() + "|" + o.reason()).sorted().toList();
            if (!controlOmissions.equals(permutedOmissions))
                throw new AssertionError("S19 auxiliary catalog permutation changed canonical omissions");

            System.out.println("S19_AUXILIARY_ORDER_DETERMINISM PASS parts=" + controlParts.size()
                + " pieces=" + controlPieces.size() + " omissions=" + controlOmissions.size());
        });
    }

    private static CatalogView freshCatalogView(boolean reverseCatalog) {
        Object model = freshAdultGrizzly();
        try {
            List<Object> roots = copyIterable(model.getClass().getMethod("parts").invoke(model), "parts()");
            List<Object> catalog = copyIterable(model.getClass().getMethod("getAllParts").invoke(model), "getAllParts()");
            if (roots.isEmpty() || catalog.size() < 2)
                throw new AssertionError("Pinned Grizzly does not provide a useful roots/catalog fixture");
            if (reverseCatalog) {
                var before = List.copyOf(catalog);
                Collections.reverse(catalog);
                if (catalog.equals(before))
                    throw new AssertionError("S19 auxiliary-order fixture failed to permute getAllParts catalog");
            }
            return new CatalogView(roots, catalog);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot materialize pinned Grizzly roots/catalog views", failure);
        }
    }

    private static List<Object> copyIterable(Object value, String label) {
        if (!(value instanceof Iterable<?> iterable))
            throw new AssertionError("Pinned Grizzly " + label + " is not iterable");
        var result = new ArrayList<Object>();
        for (Object item : iterable) result.add(item);
        return result;
    }

    /** Public reflective surface consumed by the generic extractor. Roots/children stay untouched. */
    public static final class CatalogView {
        private final List<Object> roots;
        private final List<Object> catalog;
        CatalogView(List<Object> roots, List<Object> catalog) {
            this.roots = List.copyOf(roots);
            this.catalog = List.copyOf(catalog);
        }
        public Iterable<Object> parts() { return roots; }
        public Iterable<Object> getAllParts() { return catalog; }
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
