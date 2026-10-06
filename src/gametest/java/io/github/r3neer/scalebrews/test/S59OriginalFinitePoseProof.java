package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalFinitePosePreparation;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalLinearPosePreparation;
import io.github.r3neer.scalebrews.client.mixin.ModelPartAccess;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.pose.NumericPoseProgram;
import java.nio.file.Files;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.animal.turtle.BabyTurtleModel;
import net.minecraft.client.model.animal.turtle.AdultTurtleModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** IMPLEMENTER oracle: every declared predicate invokes complete original setupAnim. */
public final class S59OriginalFinitePoseProof implements FabricClientGameTest {
    private record State(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private record Sample(Map<String, Float> channels, Map<String, List<Float>> matrices) {}
    private record Evidence(NumericPoseProgram program, List<OriginalLinearPosePreparation.Input> schema,
                            Map<String, Boolean> predicate, List<Sample> samples) {}
    private static void require(boolean ok, String why) { if (!ok) throw new AssertionError("S59 IMPL " + why); }

    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var gson = new GsonBuilder().setPrettyPrinting().create();
            var evidence = new ArrayList<Evidence>(); var tables = new ArrayList<OriginalFinitePosePreparation.Prepared>();
            int matrices = 0, cubes = 0;
            for (EntityModel<?> model : List.of(new BabyTurtleModel(BabyTurtleModel.createBodyLayer().bakeRoot()),
                new CowModel(CowModel.createBodyLayer().bakeRoot()))) {
                var original = capture(model);
                try {
                    int i = 0;
                    for (var p : model.allParts()) {
                        p.setInitialPose(new PartPose(i*.21f,-2+i*.3f,3-i*.4f,.11f+i*.03f,-.2f+i*.05f,.3f-i*.02f,
                            -1.1f-i*.03f,.8f+i*.04f,1.3f+i*.02f)); i++;
                        p.loadPose(new PartPose(31,7,-18,2,-.6f,.4f,3,4,2)); p.visible=false; p.skipDraw=true;
                    }
                    var before=capture(model); var result=OriginalFinitePosePreparation.inspect(model);
                    require(result.admitted(),"finite absent "+result.absence()); var prepared=result.prepared(); verify(before);
                    require(prepared.equals(OriginalFinitePosePreparation.inspect(model).prepared()),"unstable finite table");
                    require(prepared.rows().size()==(1<<prepared.booleanInputs().size()),"incomplete predicate assignments");
                    require(prepared.verifiedInstructions()<=1048576,"work cap");
                    require(new HashSet<>(prepared.rows().stream().map(r->r.predicate()).toList()).size()==prepared.rows().size(),"duplicate predicate");
                    if(model instanceof BabyTurtleModel) {
                        require(prepared.booleanInputs().size()==2,"native boolean schema");
                        require(!OriginalLinearPosePreparation.inspect(model).admitted(),"strict linear now admits branches");
                    }
                    for(var a:prepared.rows()) for(var b:prepared.rows()) if(a.numeric().equals(b.numeric()))
                        require(a.numeric()==b.numeric(),"equivalent programs not interned");
                    var shared=prepared.rows().getFirst().numeric().program().geometry();
                    for(var row:prepared.rows()) {
                        require(row.numeric().program().geometry()==shared,"row geometry duplicated");
                        require(row.predicate().keySet().equals(new HashSet<>(prepared.booleanInputs().stream().map(v->v.channel()).toList())),"predicate schema incomplete");
                        var program=NumericPoseProgram.validatedCopy(gson.fromJson(gson.toJson(row.numeric().program()),NumericPoseProgram.class));
                        require(program.equals(row.numeric().program()),"neutral DTO changed"); var bound=program.bind(program.geometry()).orElseThrow();
                        require(bound.evaluate(null).isEmpty(),"null admitted");
                        require(bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true)).isEmpty(),"missing float inputs admitted");
                        var samples=new ArrayList<Sample>();float[] phase={-3,0,2,10000000},amount={0,.2f,1,1.3f};
                        for(int s=0;s<phase.length;s++) {
                            LivingEntityRenderState state=model instanceof BabyTurtleModel?new TurtleRenderState():new LivingEntityRenderState();
                            state.walkAnimationPos=phase[s];state.walkAnimationSpeed=amount[s];state.xRot=-50+s*37;state.yRot=80-s*43;
                            try {for(var input:prepared.booleanInputs()) Class.forName(input.owner(),false,model.getClass().getClassLoader())
                                .getDeclaredField(input.name()).setBoolean(state,row.predicate().get(input.channel()));}
                            catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
                            var channels=new TreeMap<String,Float>();
                            try {for(var input:row.numeric().inputs()) channels.put(input.channel(),Class.forName(input.owner(),false,model.getClass().getClassLoader())
                                .getDeclaredField(input.name()).getFloat(state));}
                            catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
                            for(var p:model.allParts())p.loadPose(new PartPose(41,-3,12,.2f,1.4f,-.7f,2,4,3));
                            ((Model)model).setupAnim(state);
                            var nativeMatrices=new TreeMap<String,Matrix4f>();collect(model.root(),"root",new PoseStack(),nativeMatrices);
                            var replacement=bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).orElseThrow();
                            var actual=program.geometry().transforms(replacement);require(actual.keySet().equals(nativeMatrices.keySet()),"part membership");
                            var recorded=new TreeMap<String,List<Float>>();
                            for(var entry:nativeMatrices.entrySet()) {
                                require(entry.getValue().equals(actual.get(entry.getKey()),3e-5f),"original matrix "+entry.getKey()+" sample"+s);
                                recorded.put(entry.getKey(),ModelGeometry.values(entry.getValue()));matrices++;
                            }
                            var pieces=program.geometry().evaluate(new Matrix4f(),replacement,new AnatomyFilter(0,0,0));
                            var reference=materialPieces(model.root());require(pieces.keySet().equals(reference.keySet()),"material membership");
                            for(var entry:pieces.entrySet()) {
                                for(var p:entry.getValue().vertices())require(contains(reference.get(entry.getKey()),p),"DTO corner absent");
                                for(var p:reference.get(entry.getKey()))require(contains(entry.getValue().vertices(),p),"native corner absent");cubes++;
                            }
                            samples.add(new Sample(channels,recorded));
                        }
                        evidence.add(new Evidence(program,row.numeric().inputs(),row.predicate(),samples));
                    }
                    require(prepared.equals(OriginalFinitePosePreparation.inspect(model).prepared()),"animation contaminated table");tables.add(prepared);
                } finally {restore(original);} verify(original);
            }
            var adult=new AdultTurtleModel(AdultTurtleModel.createBodyLayer().bakeRoot());var adultBefore=capture(adult);
            require(!OriginalFinitePosePreparation.inspect(adult).admitted(),"visible write ignored");verify(adultBefore);
            require(!OriginalFinitePosePreparation.inspect(null).admitted(),"null admitted");
            try {var out=FabricLoader.getInstance().getGameDir().resolve("s59-implementation");Files.createDirectories(out);
                Files.writeString(out.resolve("programs.json"),gson.toJson(evidence));Files.writeString(out.resolve("tables.json"),gson.toJson(tables));}
            catch(java.io.IOException failure){throw new AssertionError(failure);}
            System.out.println("S59_IMPLEMENTER_PASS models="+tables.size()+" rows="+evidence.size()+" nativeMatrices="+matrices+" materialCubes="+cubes);
        });
    }
    private static void collect(ModelPart p,String id,PoseStack stack,Map<String,Matrix4f> out) {
        stack.pushPose();p.translateAndRotate(stack);out.put(id,new Matrix4f(stack.last().pose()));
        new TreeMap<>(((ModelPartAccess)(Object)p).scalebrews$children()).forEach((k,v)->collect(v,id+"/"+k,stack,out));stack.popPose();
    }
    private static Map<String,List<Vec3>> materialPieces(ModelPart root) {
        var out=new TreeMap<String,List<Vec3>>();root.visit(new PoseStack(),(pose,path,index,cube)->{
            double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
            var points=new ArrayList<Vec3>();for(var polygon:cube.polygons)for(var vertex:polygon.vertices()) {
                float[] local={vertex.worldX(),vertex.worldY(),vertex.worldZ()};for(int a=0;a<3;a++){lo[a]=Math.min(lo[a],local[a]);hi[a]=Math.max(hi[a],local[a]);}
                var p=pose.pose().transformPosition(local[0],local[1],local[2],new Vector3f());var v=new Vec3(p.x,p.y,p.z);if(!contains(points,v))points.add(v);
            }
            if(hi[0]>lo[0]&&hi[1]>lo[1]&&hi[2]>lo[2])out.put("root"+path+"/cube_"+index,points);
        });return out;
    }
    private static boolean contains(List<Vec3> points,Vec3 p){return points.stream().anyMatch(v->v.distanceToSqr(p)<1e-10);}
    private static PartPose current(ModelPart p){return new PartPose(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale);}
    private static Map<ModelPart,State> capture(Model<?> model){var out=new IdentityHashMap<ModelPart,State>();for(var p:model.allParts())out.put(p,new State(current(p),p.getInitialPose(),p.visible,p.skipDraw));return out;}
    private static void verify(Map<ModelPart,State> state){state.forEach((p,s)->require(current(p).equals(s.current())&&p.getInitialPose().equals(s.initial())&&p.visible==s.visible()&&p.skipDraw==s.skipDraw(),"preparation changed model"));}
    private static void restore(Map<ModelPart,State> state){state.forEach((p,s)->{p.loadPose(s.current());p.setInitialPose(s.initial());p.visible=s.visible();p.skipDraw=s.skipDraw();});}
}
