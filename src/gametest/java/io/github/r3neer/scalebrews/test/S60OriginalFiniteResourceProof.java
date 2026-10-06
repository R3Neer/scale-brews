package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalFinitePosePreparation;
import io.github.r3neer.scalebrews.client.collision.preparation.OriginalFiniteResourcePreparation;
import io.github.r3neer.scalebrews.collision.pose.FinitePoseEngine;
import io.github.r3neer.scalebrews.collision.pose.FinitePoseProgram;
import io.github.r3neer.scalebrews.collision.pose.PoseProgram;
import net.minecraft.resources.Identifier;
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
public final class S60OriginalFiniteResourceProof implements FabricClientGameTest {
    private record State(PartPose current, PartPose initial, boolean visible, boolean skipDraw) {}
    private record Sample(Map<String, Float> channels, Map<String, List<Float>> matrices) {}
    private record Evidence(OriginalFiniteResourcePreparation.Prepared exported, int alias, List<Sample> samples) {}
    private static void require(boolean ok, String why) { if (!ok) throw new AssertionError("S60 IMPL " + why); }

    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var gson = new GsonBuilder().setPrettyPrinting().create();
            var evidence = new ArrayList<Evidence>(); var tables = new ArrayList<OriginalFinitePosePreparation.Prepared>();
            int matrices = 0, cubes = 0, variant = 0;
            var ordinaryMatrices = new ArrayList<Map<String,List<Float>>>(); boolean aliasChanges = false;
            for (EntityModel<?> model : List.of(new BabyTurtleModel(BabyTurtleModel.createBodyLayer().bakeRoot()),
                new BabyTurtleModel(BabyTurtleModel.createBodyLayer().bakeRoot()),
                new CowModel(CowModel.createBodyLayer().bakeRoot()))) {
                int alias = variant++ == 1 ? 1 : 0;
                if(alias==1) aliasLeg(model);
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
                    var exported=OriginalFiniteResourcePreparation.export(prepared,"proof");
                    require(exported.equals(OriginalFiniteResourcePreparation.export(prepared,"proof")),"unstable export");
                    var channelsRequired=new TreeSet<String>(exported.table().booleans());
                    exported.inputs().forEach(v->channelsRequired.add(v.channel()));
                    require(channelsRequired.size()<=64,"union input cap");
                    var resourceGeometry=exported.numericPrograms().values().iterator().next().geometry();
                    for(var p:exported.numericPrograms().values()) require(p.geometry()==resourceGeometry,"export geometry not shared");
                    var resources=new PoseEngine.Resources(){
                        @Override public Optional<PoseProgram> program(Identifier id){return Optional.empty();}
                        @Override public Optional<FinitePoseProgram> finiteProgram(Identifier id){return id.toString().equals(exported.tableId())?Optional.of(exported.table()):Optional.empty();}
                        @Override public Optional<NumericPoseProgram> numericProgram(Identifier id){return Optional.ofNullable(exported.numericPrograms().get(id.toString()));}
                    };
                    var finiteBound=new FinitePoseEngine().bind(resourceGeometry,Map.of("program",exported.tableId()),channelsRequired,resources).orElseThrow();
                    var allSamples=new ArrayList<Sample>(); int sampleIndex=0;
                    var shared=prepared.rows().getFirst().numeric().program().geometry();
                    for(var row:prepared.rows()) {
                        require(row.numeric().program().geometry()==shared,"row geometry duplicated");
                        require(row.predicate().keySet().equals(new HashSet<>(prepared.booleanInputs().stream().map(v->v.channel()).toList())),"predicate schema incomplete");
                        var program=NumericPoseProgram.validatedCopy(gson.fromJson(gson.toJson(row.numeric().program()),NumericPoseProgram.class));
                        require(program.equals(row.numeric().program()),"neutral DTO changed"); var bound=finiteBound;
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
                            try {for(var input:exported.inputs()) channels.put(input.channel(),Class.forName(input.owner(),false,model.getClass().getClassLoader())
                                .getDeclaredField(input.name()).getFloat(state));}
                            catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
                            row.predicate().forEach((name,value)->channels.put(name,value?1f:0f));
                            for(var p:model.allParts())p.loadPose(new PartPose(41,-3,12,.2f,1.4f,-.7f,2,4,3));
                            ((Model)model).setupAnim(state);
                            var nativeMatrices=new TreeMap<String,Matrix4f>();collect(model.root(),"root",new PoseStack(),nativeMatrices);
                            var replacement=bound.evaluate(new PoseEngine.Inputs(0,0,0,0,0,true,channels)).orElseThrow();
                            var actual=resourceGeometry.transforms(replacement);require(actual.keySet().equals(nativeMatrices.keySet()),"part membership");
                            var recorded=new TreeMap<String,List<Float>>();
                            for(var entry:nativeMatrices.entrySet()) {
                                require(entry.getValue().equals(actual.get(entry.getKey()),3e-5f),"original matrix "+entry.getKey()+" sample"+s);
                                recorded.put(entry.getKey(),ModelGeometry.values(entry.getValue()));matrices++;
                            }
                            var pieces=resourceGeometry.evaluate(new Matrix4f(),replacement,new AnatomyFilter(0,0,0));
                            var reference=materialPieces(model.root());require(pieces.keySet().equals(reference.keySet()),"material membership");
                            for(var entry:pieces.entrySet()) {
                                for(var p:entry.getValue().vertices())require(contains(reference.get(entry.getKey()),p),"DTO corner absent");
                                for(var p:reference.get(entry.getKey()))require(contains(entry.getValue().vertices(),p),"native corner absent");cubes++;
                            }
                            if(variant==1) ordinaryMatrices.add(recorded);
                            else if(alias==1){var baseline=ordinaryMatrices.get(sampleIndex);for(var key:recorded.keySet())if(!recorded.get(key).equals(baseline.get(key)))aliasChanges=true;}
                            sampleIndex++; var sample=new Sample(channels,recorded);samples.add(sample);allSamples.add(sample);
                        }

                    }
                    evidence.add(new Evidence(exported,alias,allSamples));
                    require(prepared.equals(OriginalFinitePosePreparation.inspect(model).prepared()),"animation contaminated table");tables.add(prepared);
                } finally {restore(original);} verify(original);
            }
            require(aliasChanges,"alias oracle vacuous");
            try {var out=FabricLoader.getInstance().getGameDir().resolve("s60-implementation");Files.createDirectories(out);
                Files.writeString(out.resolve("exports.json"),gson.toJson(evidence));Files.writeString(out.resolve("tables.json"),gson.toJson(tables));}
            catch(java.io.IOException failure){throw new AssertionError(failure);}
            System.out.println("S60_IMPLEMENTER_NATIVE_PASS models="+tables.size()+" exportedFamilies="+evidence.size()+" nativeMatrices="+matrices+" materialCubes="+cubes);
        });
    }
    private static void aliasLeg(EntityModel<?> model) {
        try {java.lang.reflect.Field left=null,right=null;
            for(Class<?> owner=model.getClass();owner!=null;owner=owner.getSuperclass())for(var field:owner.getDeclaredFields()){
                if(field.getName().equals("leftFrontLeg")){field.setAccessible(true);left=field;}
                if(field.getName().equals("rightHindLeg")){field.setAccessible(true);right=field;}
            }
            require(left!=null&&right!=null,"alias fields missing");left.set(model,right.get(model));
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
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
