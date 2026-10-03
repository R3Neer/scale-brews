package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/** Production compatibility holdout using the original 26.2 Cow with changed retained material. */
public final class S30AdversarialModelCompatibilityProof implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var catalogRoot = CowModel.createBodyLayer().bakeRoot();
            var rendererRoot = CowModel.createBodyLayer().bakeRoot();
            try {
                Field cubesField = ModelPart.class.getDeclaredField("cubes");
                cubesField.setAccessible(true);
                var body = rendererRoot.getChild("body");
                @SuppressWarnings("unchecked")
                var cubes = new ArrayList<ModelPart.Cube>((java.util.List<ModelPart.Cube>) cubesField.get(body));
                var cube = cubes.getFirst();
                cubes.set(0, new ModelPart.Cube(0, 0, cube.minX, cube.minY, cube.minZ,
                    cube.maxX - cube.minX + 16, cube.maxY - cube.minY, cube.maxZ - cube.minZ,
                    0, 0, 0, false, 64, 64, EnumSet.allOf(Direction.class)));
                cubesField.set(body, java.util.List.copyOf(cubes));
                var accepted = GeometryExtractor.vanilla("minecraft:cow", "26.2", catalogRoot, Set.of());
                var visible = GeometryExtractor.vanilla("minecraft:cow", "26.2", rendererRoot, Set.of());
                boolean identityMatches = accepted.source().equals(visible.source()) && accepted.version().equals(visible.version());
                boolean pathsAndNeutralTransformsMatch = accepted.parts().equals(visible.parts());
                if (!identityMatches || !pathsAndNeutralTransformsMatch)
                    throw new AssertionError("Invalid counterexample: changed more than cube geometry");
                if (accepted.pieces().equals(visible.pieces()))
                    throw new AssertionError("Vacuous counterexample: cube geometry did not change");
                var expected = accepted.pieces().stream().filter(p -> p.id().equals("root/body/cube_0")).findFirst().orElseThrow();
                var actual = visible.pieces().stream().filter(p -> p.id().equals("root/body/cube_0")).findFirst().orElseThrow();
                double widthDelta = (actual.max().getFirst() - actual.min().getFirst())
                    - (expected.max().getFirst() - expected.min().getFirst());
                if (Math.abs(widthDelta - 1) > 1e-6)
                    throw new AssertionError("Expected one-block width difference, got " + widthDelta);
                if (io.github.r3neer.scalebrews.client.collision.presentation.CausalModelPartAdapter.prepare(
                    new CowModel(catalogRoot), net.minecraft.resources.Identifier.parse("minecraft:cow"),
                    accepted.withModelTransform(new org.joml.Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)),
                    io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter.DEFAULT).isEmpty())
                    throw new AssertionError("Compatible original material was rejected by production adapter");
                if (io.github.r3neer.scalebrews.client.collision.presentation.CausalModelPartAdapter.prepare(
                    new CowModel(rendererRoot), net.minecraft.resources.Identifier.parse("minecraft:cow"),
                    accepted.withModelTransform(new org.joml.Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)),
                    io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter.DEFAULT).isPresent())
                    throw new AssertionError("Altered retained material was accepted by production adapter");
                String evidence = "{\"classification\":\"PRODUCTION_GEOMETRY_MISMATCH_REJECTED\",\"source\":\"minecraft:cow\","
                    + "\"version\":\"26.2\",\"identityMatches\":true,\"partsAndNeutralPosesMatch\":true,"
                    + "\"cubeGeometryMatches\":false,\"widthDeltaBlocks\":" + widthDelta + "}";
                Files.writeString(Path.of("s30-model-compatibility-production.json"), evidence);
                System.out.println("S30 ADVERSARY actual geometry mismatch rejected PASS: " + evidence);
            } catch (ReflectiveOperationException | java.io.IOException failure) {
                throw new AssertionError("Model compatibility fixture failed", failure);
            }
        });
    }
}
