package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.AdvancedModelBoxGeometryEngine;
import io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Independent S19 render-space oracle.  The source descriptor is materialized with JOML while
 * the expected renderer transform is replayed through PoseStack in bytecode-observed call order.
 */
public final class S19RendererTransformClientProof implements FabricClientGameTest {
    private static final String GRIZZLY = "com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear";
    private static final String GAZELLE = "com.github.alexthe666.alexsmobs.client.model.ModelGazelle";
    private static final Identifier GRIZZLY_ID = Identifier.parse("test:s19_renderer_grizzly");
    private static final Identifier GAZELLE_ID = Identifier.parse("test:s19_renderer_gazelle");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FabricLoader.getInstance().isModLoaded("alexsmobs")) {
                throw new AssertionError("S19 renderer-transform proof requires the pinned Alex external input");
            }
            prove(GRIZZLY_ID, GRIZZLY, false);
            prove(GAZELLE_ID, GAZELLE, true);
        });
    }

    private static void prove(Identifier sourceId, String modelClass, boolean gazelle) {
        Supplier<Object> factory = () -> freshAdult(modelClass);
        AdvancedModelBoxGeometryEngine.registerSource(sourceId,
            new AdvancedModelBoxGeometryEngine.Source(factory, () -> declaredSourceTransform(gazelle)));
        var engine = (AdvancedModelBoxGeometryEngine) clientDelegate();
        var geometry = engine.prepareDetailed(new GeometryEngine.Request(sourceId)).orElseThrow().geometry();

        Matrix4f actual = ModelGeometry.matrix(geometry.modelTransform());
        Matrix4f expected = rendererTransformOracle(gazelle);
        float maxDelta = maxDelta(actual, expected);
        if (maxDelta > 1e-6f) {
            throw new AssertionError("S19 source modelTransform differs from independent renderer oracle for "
                + (gazelle ? "alexsmobs:gazelle" : "alexsmobs:grizzly_bear") + ": maxDelta=" + maxDelta
                + " actual=" + geometry.modelTransform() + " expected=" + ModelGeometry.values(expected));
        }
        if (gazelle && Math.abs(expected.getScale(new Vector3f()).x() - .8f) > 1e-6f)
            throw new AssertionError("Independent Gazelle renderer oracle lost scale(0.8)");
        System.out.println("S19_RENDERER_TRANSFORM " + (gazelle ? "gazelle" : "grizzly")
            + " maxDelta=" + maxDelta + " matrix=" + geometry.modelTransform());
    }

    /** Tooling/source declaration under test. Deliberately independent from the PoseStack oracle below. */
    private static Matrix4f declaredSourceTransform(boolean gazelle) {
        Matrix4f transform = new Matrix4f().scaling(-1f, -1f, 1f);
        if (gazelle) transform.scale(.8f, .8f, .8f);
        return transform.translate(0f, -1.501f, 0f);
    }

    /** Exact bytecode-observed renderer call order: common flip, concrete scale hook, common translation. */
    private static Matrix4f rendererTransformOracle(boolean gazelle) {
        PoseStack poses = new PoseStack();
        poses.scale(-1f, -1f, 1f);
        if (gazelle) poses.scale(.8f, .8f, .8f);
        poses.translate(0f, -1.501f, 0f);
        return new Matrix4f(poses.last().pose());
    }

    private static float maxDelta(Matrix4f left, Matrix4f right) {
        float[] a = left.get(new float[16]);
        float[] b = right.get(new float[16]);
        float result = 0f;
        for (int i = 0; i < a.length; i++) result = Math.max(result, Math.abs(a[i] - b[i]));
        return result;
    }

    private static Object freshAdult(String className) {
        try {
            Object model = Class.forName(className).getConstructor().newInstance();
            try {
                Field young = model.getClass().getField("young");
                young.setBoolean(model, false);
                if (young.getBoolean(model)) throw new AssertionError("Pinned Alex model refused adult-state normalization");
            } catch (NoSuchFieldException missing) {
                throw new AssertionError("Pinned Alex model lost the expected public young state", missing);
            }
            return model;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot instantiate pinned Alex model " + className, failure);
        }
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
