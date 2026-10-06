package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalLinearPosePreparation;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalResetPosePreparation;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalResetGeometryPreparation;
import io.github.r3neer.scalebrews.client.mixin.ModelPartAccess;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** IMPLEMENTER differential oracle: invoke original setupAnim, never a reconstructed species formula. */
public final class S56OriginalLinearPoseProof implements FabricClientGameTest {
    private record State(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private record Sample(Map<String, Float> channels, Map<String, List<Float>> matrices) {}
    private record Evidence(NumericPoseProgram program, List<OriginalLinearPosePreparation.Input> schema, List<Sample> samples) {}
    private static void require(boolean ok, String why) { if (!ok) throw new AssertionError("S56 IMPL " + why); }

    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var evidence = new ArrayList<Evidence>(); int matrices = 0, cubes = 0;
            var gson = new GsonBuilder().setPrettyPrinting().create();
            for (EntityModel<?> model : List.of(new CowModel(CowModel.createBodyLayer().bakeRoot()),
                new PigModel(PigModel.createBodyLayer(CubeDeformation.NONE).bakeRoot()))) {
                var original = capture(model);
                try {
                    int index = 0;
                    for (var part : model.allParts()) {
                        int i = index++;
                        part.setInitialPose(new PartPose(i*.4f, -3+i, 2.75f-i, .2f+i*.07f, -.4f+i*.03f, .8f-i*.05f,
                            -1.2f-i*.1f, .75f+i*.02f, 1.4f+i*.04f));
                        part.loadPose(new PartPose(53,-31,7,-1.3f,2.1f,.6f,3,2,4));
                        part.visible=false; part.skipDraw=true;
                    }
                    var before = capture(model);
                    var result = OriginalLinearPosePreparation.inspect(model);
                    require(result.admitted(), "compiler absent " + result.absence());
                    var prepared = result.prepared(); verify(before);
                    require(prepared.equals(OriginalLinearPosePreparation.inspect(model).prepared()), "unstable program");
                    require(!OriginalResetPosePreparation.inspect(model).admitted(), "old reset API accepted animation override");
                    NumericPoseProgram program = gson.fromJson(gson.toJson(prepared.program()), NumericPoseProgram.class);
                    require(program.equals(prepared.program()), "common program roundtrip");
                    var geometry=program.geometry(); var bound=program.bind(geometry).orElseThrow();
                    require(program.bind(geometry.withModelTransform(new Matrix4f().scaling(2))).isEmpty(), "foreign geometry bound");
                    require(bound.evaluate(null).isEmpty(), "null admitted");
                    require(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,false)).isEmpty(), "incompatible admitted");
                    require(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true)).isEmpty(), "missing inputs admitted");
                    var samples = new ArrayList<Sample>();
                    float[] phases={-3,0,1,2,173,10000000}; float[] speeds={0,.4f,1,3,.1f,2};
                    for(int s=0;s<phases.length;s++) {
                        var state = new LivingEntityRenderState();
                        state.xRot=-45+s*23; state.yRot=83-s*41;
                        state.walkAnimationPos=phases[s]; state.walkAnimationSpeed=speeds[s];
                        var channels=new LinkedHashMap<String,Float>();
                        for(var input:prepared.inputs()) {
                            try {
                                Class<?> owner=Class.forName(input.owner(),false,model.getClass().getClassLoader());
                                channels.put(input.channel(),owner.getField(input.name()).getFloat(state));
                            } catch(ReflectiveOperationException failure) { throw new AssertionError(failure); }
                        }
                        for(var part:model.allParts()) part.loadPose(new PartPose(41,7,-3,.9f,-.7f,1.1f,4,3,2));
                        ((Model)model).setupAnim(state);
                        var nativeMatrices=new LinkedHashMap<String,Matrix4f>(); collect(model.root(),"root",new PoseStack(),nativeMatrices);
                        var replacements=bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).orElseThrow();
                        var actual=geometry.transforms(replacements);
                        require(actual.keySet().equals(nativeMatrices.keySet()),"part membership");
                        var recorded=new LinkedHashMap<String,List<Float>>();
                        for(var entry:nativeMatrices.entrySet()) {
                            require(entry.getValue().equals(actual.get(entry.getKey()),3e-5f),"original matrix " + entry.getKey()+" sample"+s);
                            recorded.put(entry.getKey(),ModelGeometry.values(entry.getValue())); matrices++;
                        }
                        var pieces=geometry.evaluate(new Matrix4f(),replacements,new AnatomyFilter(0,0,0));
                        var nativePieces=referencePieces(model.root());
                        require(pieces.keySet().equals(nativePieces.keySet()),"cube membership");
                        for(var entry:pieces.entrySet()) {
                            var points=nativePieces.get(entry.getKey());
                            for(var p:entry.getValue().vertices()) require(contains(points,p),"exported corner absent");
                            for(var p:points) require(contains(entry.getValue().vertices(),p),"native corner absent");
                            cubes++;
                        }
                        samples.add(new Sample(channels,recorded));
                        require(prepared.equals(OriginalLinearPosePreparation.inspect(model).prepared()),"animation contaminated compiler");
                    }
                    evidence.add(new Evidence(program,prepared.inputs(),samples));
                } finally {restore(original);} verify(original);
            }
            var reset = new SlimeModel(SlimeModel.createInnerBodyLayer().bakeRoot());
            require(OriginalResetPosePreparation.inspect(reset).admitted(),"old reset rejected base");
            require(OriginalResetGeometryPreparation.inspect(reset).admitted(),"old export rejected base");
            require(!OriginalLinearPosePreparation.inspect(reset).admitted(),"reset only became numeric writes");
            require(!OriginalLinearPosePreparation.inspect(null).admitted(),"null admitted");
            try {
                var out=FabricLoader.getInstance().getGameDir().resolve("s56-implementation"); Files.createDirectories(out);
                Files.writeString(out.resolve("programs.json"),gson.toJson(evidence));
            } catch(java.io.IOException failure) {throw new AssertionError(failure);}
            System.out.println("S56_IMPLEMENTER_PASS models="+evidence.size()+" nativeMatrices="+matrices+" nativeCubes="+cubes);
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
