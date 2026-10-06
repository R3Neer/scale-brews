package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartStructure;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalResetGeometryPreparation;
import io.github.r3neer.scalebrews.client.mixin.ModelPartAccess;
import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.monster.slime.SmallSulfurCubeModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** IMPLEMENTER: original model-local numeric and structural parity; no physical binding. */
public final class S55InitialResetGeometryProof implements FabricClientGameTest {
    private record State(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private record Evidence(ModelGeometry geometry, Map<String, List<Float>> nativeMatrices) {}
    private static void require(boolean ok, String why) { if (!ok) throw new AssertionError("S55 IMPL " + why); }

    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var evidence = new ArrayList<Evidence>();
            int matrices = 0, cubes = 0;
            for (EntityModel<?> model : List.of(new SlimeModel(SlimeModel.createInnerBodyLayer().bakeRoot()),
                new SmallSulfurCubeModel(SmallSulfurCubeModel.createInnerBodyLayer().bakeRoot()))) {
                var original = capture(model);
                try {
                    int index = 0;
                    for (var part : model.allParts()) {
                        int i = index++;
                        part.setInitialPose(new PartPose(i * .5f, -3 + i, 2.75f - i,
                            .2f + i * .07f, -.4f + i * .03f, .8f - i * .05f,
                            -1.2f - i * .1f, .75f + i * .02f, 1.4f + i * .04f));
                        part.loadPose(new PartPose(53, -31, 7, -1.3f, 2.1f, .6f, 3, 2, 4));
                        part.visible = false; part.skipDraw = true;
                    }
                    var before = capture(model);
                    var fingerprint = ModelPartStructure.inspect(model.root());
                    var result = OriginalResetGeometryPreparation.inspect(model);
                    require(result.admitted(), "export absent " + result.absence());
                    var prepared = result.prepared(); var geometry = prepared.geometry();
                    verify(before);
                    require(prepared.equals(OriginalResetGeometryPreparation.inspect(model).prepared()), "unstable export");
                    require(prepared.program().initialStructure().equals(fingerprint), "old fingerprint changed");
                    require(geometry.format() == 2 && geometry.modelTransform().equals(ModelGeometry.values(new Matrix4f())), "local frame inherited renderer root");
                    require(geometry.pieces().stream().allMatch(p -> p.excluded() == null), "current flags filter structural cubes");
                    var gson = new GsonBuilder().setPrettyPrinting().create();
                    var decoded = gson.fromJson(gson.toJson(geometry), ModelGeometry.class);
                    require(decoded.equals(geometry), "common DTO roundtrip");
                    var engine = CollisionEngines.pose(Identifier.parse("scalebrews:static")).orElseThrow();
                    var bound = engine.bind(decoded, Map.of(), Set.of(), PoseEngine.Resources.empty()).orElseThrow();
                    require(bound.evaluate(new PoseEngine.Inputs(0, 0, 0, 0, 0, false)).isEmpty(), "incompatible state admitted");
                    require(bound.evaluate(null).isEmpty(), "null bound inputs admitted");
                    require(engine.evaluate(decoded, new PoseEngine.Inputs(0, 0, 0, 0, 0, true), Map.of("unknown", "1")).isEmpty(), "unknown parameter admitted");

                    Map<String, Matrix4f> referenceMatrices = new LinkedHashMap<>();
                    for (int sample = 0; sample < 2; sample++) {
                        for (var part : model.allParts()) part.loadPose(new PartPose(41, 7, -3, .9f, -.7f, 1.1f, 4, 3, 2));
                        ((Model)model).setupAnim(sample == 0 ? null : new Object());
                        referenceMatrices.clear(); collect(model.root(), "root", new PoseStack(), referenceMatrices);
                        var replacements = bound.evaluate(new PoseEngine.Inputs(23, .7f, sample * 173, -49, 27, true)).orElseThrow();
                        var actual = decoded.transforms(replacements);
                        require(actual.keySet().equals(referenceMatrices.keySet()), "part membership");
                        for (var entry : referenceMatrices.entrySet()) {
                            require(entry.getValue().equals(actual.get(entry.getKey()), 3e-5f), "native hierarchy matrix " + entry.getKey());
                            matrices++;
                        }
                        var reference = referencePieces(model.root());
                        var exported = decoded.evaluate(new Matrix4f(), replacements, new AnatomyFilter(0, 0, 0));
                        require(reference.keySet().equals(exported.keySet()), "cube membership");
                        for (var entry : reference.entrySet()) {
                            var points = exported.get(entry.getKey()).vertices();
                            for (var point : entry.getValue()) require(contains(points, point), "missing original vertex " + entry.getKey());
                            for (var point : points) require(contains(entry.getValue(), point), "invented vertex " + entry.getKey());
                            cubes++;
                        }
                    }
                    var values = new LinkedHashMap<String, List<Float>>();
                    referenceMatrices.forEach((id, matrix) -> values.put(id, ModelGeometry.values(matrix)));
                    evidence.add(new Evidence(geometry, values));
                    for (var part : model.allParts()) { part.visible = true; part.skipDraw = false; }
                    require(prepared.equals(OriginalResetGeometryPreparation.inspect(model).prepared()), "current/flags contaminate export");
                    var first = model.allParts().getFirst(); var pose = first.getInitialPose();
                    first.setInitialPose(new PartPose(pose.x() + 1, pose.y(), pose.z(), pose.xRot(), pose.yRot(), pose.zRot(), pose.xScale(), pose.yScale(), pose.zScale()));
                    require(!prepared.geometry().source().equals(OriginalResetGeometryPreparation.inspect(model).prepared().geometry().source()), "initial change keeps identity");
                    first.setInitialPose(pose);
                    require(prepared.equals(OriginalResetGeometryPreparation.inspect(model).prepared()), "initial restore changed export");
                    try { geometry.parts().clear(); throw new AssertionError("Mutable parts"); } catch (UnsupportedOperationException expected) {}
                } finally { restore(original); }
                verify(original);
            }
            require(!OriginalResetGeometryPreparation.inspect(null).admitted(), "null model admitted");
            try {
                var out = FabricLoader.getInstance().getGameDir().resolve("s55-implementation"); Files.createDirectories(out);
                Files.writeString(out.resolve("exports.json"), new GsonBuilder().setPrettyPrinting().create().toJson(evidence));
            } catch (java.io.IOException failure) { throw new AssertionError(failure); }
            System.out.println("S55_IMPLEMENTER_PASS models=" + evidence.size() + " nativeMatrices=" + matrices + " nativeCubes=" + cubes);
        });
    }

    private static void collect(ModelPart part, String id, PoseStack stack, Map<String, Matrix4f> matrices) {
        stack.pushPose(); part.translateAndRotate(stack); matrices.put(id, new Matrix4f(stack.last().pose()));
        new java.util.TreeMap<>(((ModelPartAccess)(Object)part).scalebrews$children())
            .forEach((name, child) -> collect(child, id + "/" + name, stack, matrices));
        stack.popPose();
    }
    private static Map<String, List<net.minecraft.world.phys.Vec3>> referencePieces(ModelPart root) {
        var result = new LinkedHashMap<String, List<net.minecraft.world.phys.Vec3>>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            var points = new ArrayList<net.minecraft.world.phys.Vec3>();
            for (var polygon : cube.polygons) for (var vertex : polygon.vertices()) {
                var p = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                var point = new net.minecraft.world.phys.Vec3(p.x, p.y, p.z);
                if (!contains(points, point)) points.add(point);
            }
            result.put("root" + path + "/cube_" + index, points);
        });
        return result;
    }
    private static boolean contains(List<net.minecraft.world.phys.Vec3> points, net.minecraft.world.phys.Vec3 p) {
        return points.stream().anyMatch(q -> q.distanceToSqr(p) < 1e-10);
    }
    private static PartPose current(ModelPart p) { return new PartPose(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale); }
    private static Map<ModelPart, State> capture(Model<?> model) {
        var result = new IdentityHashMap<ModelPart, State>();
        for (var p : model.allParts()) result.put(p, new State(current(p),p.getInitialPose(),p.visible,p.skipDraw));
        return result;
    }
    private static void verify(Map<ModelPart, State> state) {
        state.forEach((p,s) -> require(current(p).equals(s.current()) && p.getInitialPose().equals(s.initial())
            && p.visible == s.visible() && p.skipDraw == s.skipDraw(), "preparation changed model"));
    }
    private static void restore(Map<ModelPart, State> state) {
        state.forEach((p,s) -> { p.loadPose(s.current()); p.setInitialPose(s.initial()); p.visible=s.visible(); p.skipDraw=s.skipDraw(); });
    }
}
