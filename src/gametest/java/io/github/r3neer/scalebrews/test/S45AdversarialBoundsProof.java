package io.github.r3neer.scalebrews.test;
import io.github.r3neer.scalebrews.client.collision.preparation.*;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.*;

/** Explicit preparation bounds, no world/body/material mutation. */
public final class S45AdversarialBoundsProof implements FabricClientGameTest {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private record Probe(int entries,int slots,List<String> gaps){}
 @SuppressWarnings("unchecked") private static Probe probe(Object input) throws ReflectiveOperationException {
  var pc=Class.forName(ModelFamilyCensus.class.getName()+"$Pass");var pctor=pc.getDeclaredConstructor();pctor.setAccessible(true);
  var oc=Class.forName(ModelFamilyCensus.class.getName()+"$RendererInspection");var ctor=oc.getDeclaredConstructor(pc);ctor.setAccessible(true);
  var owner=ctor.newInstance(pctor.newInstance());var w=oc.getDeclaredMethod("walk",Object.class,String.class,int.class);w.setAccessible(true);w.invoke(owner,input,"probe",0);
  var e=oc.getDeclaredField("entries");e.setAccessible(true);var s=oc.getDeclaredField("slots");s.setAccessible(true);var g=oc.getDeclaredField("gaps");g.setAccessible(true);
  return new Probe(e.getInt(owner),((List<?>)s.get(owner)).size(),(List<String>)g.get(owner));
 }
 private static ModelPart tree(int edges){ModelPart root=new ModelPart(List.of(),Map.of());root.setInitialPose(PartPose.ZERO);for(int i=0;i<edges;i++){root=new ModelPart(List.of(),Map.of("c",root));root.setInitialPose(PartPose.ZERO);}return root;}
 public void runTest(ClientGameTestContext context){context.runOnClient(c->{try{
  var model=new CowModel(CowModel.createBodyLayer().bakeRoot());
  var map=new LinkedHashMap<String,Object>();for(int i=0;i<513;i++)map.put("k"+i,model);
  var over=probe(map);check(over.entries()==0 && over.slots()==0 && over.gaps().stream().anyMatch(x->x.contains("container_limit")),"Oversized container was traversed");
  map.clear();for(int i=0;i<8;i++)map.put("k"+i,Collections.nCopies(512,model));
  var capped=probe(map);check(capped.entries()==4096 && capped.slots()==256 && capped.gaps().stream().anyMatch(x->x.contains("entry_limit")) && capped.gaps().stream().anyMatch(x->x.contains("slot_limit")),"Nested entry/slot budget not bounded: "+capped);
  Object chain=model;for(int i=0;i<14;i++)chain=List.of(chain);var depth=probe(chain);check(depth.slots()==0 && depth.gaps().stream().anyMatch(x->x.contains("depth_limit")),"Container depth exhausted silently");
  check(ModelPartStructure.inspect(tree(64)).parts()==65,"Exact structural depth rejected");
  try{ModelPartStructure.inspect(tree(65));throw new AssertionError("Structural depth overflow accepted");}catch(IllegalArgumentException e){check(e.getMessage().contains("depth_limit"),"Wrong structure depth failure");}
  var cube=new ModelPart.Cube(0,0,0,0,0,1,1,1,0,0,0,false,16,16,Set.of());
  for(int n:new int[]{4096,4097}){var root=new ModelPart(Collections.nCopies(n,cube),Map.of());root.setInitialPose(PartPose.ZERO);
   if(n==4096)check(ModelPartStructure.inspect(root).cubes()==4096,"Exact cube cap rejected");
   else try{ModelPartStructure.inspect(root);throw new AssertionError("Cube overflow accepted");}catch(IllegalArgumentException e){check(e.getMessage().contains("cube_limit"),"Wrong cube overflow failure");}
  }
  var solidCube=new ModelPart.Cube(0,0,0,0,0,1,1,1,0,0,0,false,16,16,EnumSet.allOf(net.minecraft.core.Direction.class));
  int perCube=Arrays.stream(solidCube.polygons).mapToInt(x->x.vertices().length).sum();check(perCube>0,"Missing real cube vertices");
  int limit=ModelPartStructure.MAX_VERTICES/perCube;
  var exactVertices=new ModelPart(Collections.nCopies(limit,solidCube),Map.of());exactVertices.setInitialPose(PartPose.ZERO);check(ModelPartStructure.inspect(exactVertices).cubes()==limit,"Vertex-bound predecessor rejected");
  var overflowVertices=new ModelPart(Collections.nCopies(limit+1,solidCube),Map.of());overflowVertices.setInitialPose(PartPose.ZERO);
  try{ModelPartStructure.inspect(overflowVertices);throw new AssertionError("Vertex overflow accepted");}catch(IllegalArgumentException e){check(e.getMessage().contains("vertex_limit"),"Wrong vertex overflow failure");}
  var rejected=new java.util.concurrent.atomic.AtomicBoolean();var worker=new Thread(()->{try{ModelFamilyCensus.inspect(c,null,null,null);}catch(IllegalStateException expected){rejected.set(true);}},"s45-census-owner-probe");worker.start();worker.join(3000);check(!worker.isAlive()&&rejected.get(),"Census admitted nonclient thread");
  System.out.println("S45_BOUNDS_PASS entries4096 slots256 container513Reject depth64/65 cubes4096/4097 verticesLimitExercised=true clientThreadFence=true");
 }catch(ReflectiveOperationException|InterruptedException e){throw new AssertionError(e);}});}
}
