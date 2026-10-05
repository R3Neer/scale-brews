package io.github.r3neer.scalebrews.test;
import io.github.r3neer.scalebrews.client.collision.preparation.*;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public final class S45AdversarialIdentityProof implements FabricClientGameTest {
 private static boolean alternate;
 private enum Variant { A { public String toString(){return alternate?"changed-A":"label";} }, B { public String toString(){return "label";} } }
 @SuppressWarnings("unchecked") private static List<ModelFamilyCensus.Slot> slots(Map<?,?> map) throws ReflectiveOperationException {
  var passClass=Class.forName(ModelFamilyCensus.class.getName()+"$Pass");var pc=passClass.getDeclaredConstructor();pc.setAccessible(true);
  var ownerClass=Class.forName(ModelFamilyCensus.class.getName()+"$RendererInspection");var oc=ownerClass.getDeclaredConstructor(passClass);oc.setAccessible(true);
  var owner=oc.newInstance(pc.newInstance());var w=ownerClass.getDeclaredMethod("walk",Object.class,String.class,int.class);w.setAccessible(true);w.invoke(owner,map,"probe",0);
  var f=ownerClass.getDeclaredField("slots");f.setAccessible(true);return (List<ModelFamilyCensus.Slot>)f.get(owner);
 }
 public void runTest(ClientGameTestContext context){context.runOnClient(c->{try{
  Model<?> model=new CowModel(CowModel.createBodyLayer().bakeRoot());
  alternate=false;var map=Map.of(Variant.A,model,Variant.B,model);var a=slots(map);alternate=true;var b=slots(map);
  if(a.size()!=2||!a.equals(b)||a.get(0).path().equals(a.get(1).path()))throw new AssertionError("Enum label mutation changed/aliased stable slots");
  var hash=ModelPartStructure.class.getDeclaredMethod("hash",String.class);hash.setAccessible(true);
  for(String invalid:new String[]{"\uD800","\uDC00","ok\uD800bad"}){
   try{hash.invoke(null,invalid);throw new AssertionError("Malformed Unicode accepted as structural identity");}
   catch(InvocationTargetException expected){if(!(expected.getCause() instanceof IllegalArgumentException))throw expected;}
  }
  if(hash.invoke(null,"valid\uD83D\uDE00").equals(hash.invoke(null,"valid?")))throw new AssertionError("Valid Unicode collapsed in identity");
  var original=new ModelPart(List.of(),Map.of());original.setInitialPose(PartPose.ZERO);
  var root=new ModelPart(List.of(),Map.of("\uD800",original));root.setInitialPose(PartPose.ZERO);
  try{ModelPartStructure.inspect(root);throw new AssertionError("Malformed child identity accepted");}catch(IllegalArgumentException expected){}
  System.out.println("S45_PRIVATE_IDENTITY_PASS enumDistinctStable=2 malformedRejected=4 supplementaryPreserved=true");
 }catch(ReflectiveOperationException e){throw new AssertionError(e);}});}
}
