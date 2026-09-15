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

/** S19 source-intent holdout for negative dimensions and post-inflation collapse. */
public final class S19PrimitiveIntentClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final Identifier NEGATIVE = Identifier.parse("test:s19_negative_source_dimension");
    private static final Identifier COLLAPSED = Identifier.parse("test:s19_collapsed_volumetric_cube");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs"))
                throw new AssertionError("S19 primitive-intent proof requires the pinned Alex external input");

            expectRejected(NEGATIVE, () -> withExtraBox(-1f, 2f, 2f, 0f),
                "Negative AdvancedModelBox source dimension",
                "S19 negative source dimension was sanitized into an apparently valid collider");

            // Nominal 1x1x1 with -0.5 inflation collapses both faces of every axis onto the center.
            // Source intent was volumetric, so this is corruption, not a legitimate render-only plane.
            expectRejected(COLLAPSED, () -> withExtraBox(1f, 1f, 1f, -.5f),
                "Volumetric AdvancedModelBox primitive collapsed after materialization",
                "S19 collapsed volumetric source cube was silently reclassified as render-only");

            System.out.println("S19_PRIMITIVE_INTENT PASS negative=closed collapsedVolumetric=closed");
        });
    }

    private static void expectRejected(Identifier id, Supplier<Object> factory, String expectedCause,
                                       String acceptedMessage) {
        AdvancedModelBoxGeometryEngine.registerSource(id,
            new AdvancedModelBoxGeometryEngine.Source(factory, Matrix4f::new));
        try {
            var result = clientDelegate().prepareDetailed(new GeometryEngine.Request(id));
            if (result.isPresent()) throw new AssertionError(acceptedMessage);
            throw new AssertionError("S19 malformed primitive returned Optional.empty instead of explicit fail-closed error: " + id);
        } catch (IllegalArgumentException expected) {
            if (!causeContains(expected, expectedCause))
                throw new AssertionError("S19 malformed primitive failed for wrong reason: id=" + id
                    + " expectedCause='" + expectedCause + "' actual=" + expected, expected);
        }
    }

    private static Object withExtraBox(float width, float height, float depth, float inflate) {
        try {
            Object model = Class.forName(GRIZZLY).getConstructor().newInstance();
            try { model.getClass().getField("young").setBoolean(model, false); }
            catch (NoSuchFieldException ignored) { /* exact pinned dialect may not expose age here */ }
            Object body = model.getClass().getField("body").get(model);
            Method addBox = body.getClass().getMethod("addBox",
                float.class, float.class, float.class, float.class, float.class, float.class, float.class);
            addBox.invoke(body, 0f, 0f, 0f, width, height, depth, inflate);
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare pinned primitive-intent Grizzly fixture", failure);
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
