package io.github.r3neer.scalebrews.collision.internal;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.client.model.animal.cow.CowModel;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.collision.data.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.api.spi.*;
public final class S48AdversarialLiveBindingClientProof implements FabricClientGameTest {
 static void check(boolean ok,String text){if(!ok)throw new AssertionError("S48 ADV LIVE "+text);}
 static CollisionBinding row(String age,AnatomyFilter filter){return new CollisionBinding(1,S48AdversarialVariantFixtures.TYPE,Map.of("age",age,"adv_signal","x"),new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,S48AdversarialVariantFixtures.MODEL,Map.of(),filter),new CollisionBinding.Pose(S48AdversarialVariantFixtures.POSE,Map.of(),Set.of()),S48AdversarialVariantFixtures.ROOT,CollisionPolicy.Patch.EMPTY,Set.of());}
 @Override public void runTest(ClientGameTestContext c){
  var geometry=c.computeOnClient(mc->ModelPartGeometryEngine.inspectVisible(S48AdversarialVariantFixtures.TYPE,CowModel.createBodyLayer().bakeRoot(),new org.joml.Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
  String omitted=geometry.pieces().getFirst().id();var a=row("adult",AnatomyFilter.DEFAULT);var b=row("baby",new AnatomyFilter(AnatomyFilter.DEFAULT.minThickness(),AnatomyFilter.DEFAULT.minAspect(),AnatomyFilter.DEFAULT.minVolumeRatio(),Set.of(),Set.of(omitted)));var rows=List.of(a,b);var at=CollisionBindingIdentity.of(a);var bt=CollisionBindingIdentity.of(b);var id=new AtomicInteger(-1);var saved=new AtomicReference<AnatomyPosePayload>();var serverGen=new AtomicLong();var world=c.worldBuilder().create();
  try{
   world.getServer().runOnServer(s->{S48AdversarialVariantFixtures.registry();S48AdversarialVariantFixtures.signal.set("x");AnatomyRuntime.start(s);AnatomyRuntime.reload(s,S48AdversarialVariantFixtures.resources(s.getResourceManager(),geometry,rows));var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);check(cow!=null,"original factory");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);check(s.overworld().addFreshEntity(cow),"original insertion");id.set(cow.getId());});
   c.waitFor(mc->mc.level!=null&&mc.level.getEntity(id.get()) instanceof Cow cow&&AnatomyClientNetworking.presentationFrame(cow).map(f->at.equals(f.identity().bindingIdentity())).orElse(false),200);
   c.waitFor(mc->a.equals(AnatomyMovement.canonicalBinding((Cow)mc.level.getEntity(id.get()))),200);
   c.runOnClient(mc->{var cow=(Cow)mc.level.getEntity(id.get());var f=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();check(f.evaluated().pieces().containsKey(omitted),"original A piece");check(AnatomyMovement.canonicalBinding(cow).equals(a),"client physical exact A");S47AdversarialExactBindingClientProof.rendererSelection(f,cow,a.geometry().filter());capture("s48-live-adult",f);});
   world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(id.get());var player=s.getPlayerList().getPlayers().getFirst();var frame=AnatomyMovement.publishedFrame(cow).orElseThrow();saved.set(AnatomyNetworking.posePayload(frame,AnatomyRuntime.trackingGeneration(player,cow)));serverGen.set(frame.identity().bindingGeneration());cow.setBaby(true);check(AnatomyMovement.queryFrame(cow).isEmpty(),"server immediate B absence");});
   c.waitFor(mc->AnatomyClientNetworking.presentationFrame((Cow)mc.level.getEntity(id.get())).map(f->bt.equals(f.identity().bindingIdentity())).orElse(false),200);
   c.waitFor(mc->b.equals(AnatomyMovement.canonicalBinding((Cow)mc.level.getEntity(id.get()))),200);
   c.runOnClient(mc->{var cow=(Cow)mc.level.getEntity(id.get());var f=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();check(f.identity().bindingGeneration()>serverGen.get(),"real server generation advances");check(AnatomyMovement.canonicalBinding(cow).equals(b)&&!f.evaluated().pieces().containsKey(omitted),"physical/presentation agree on live B filter");S47AdversarialExactBindingClientProof.rendererSelection(f,cow,b.geometry().filter());capture("s48-live-baby",f);});
   world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(id.get());S48AdversarialVariantFixtures.signal.set(null);AnatomyRuntime.variantChanged(cow);check(AnatomyMovement.queryFrame(cow).isEmpty(),"unknown releases server material");});
   c.waitFor(mc->{var cow=(Cow)mc.level.getEntity(id.get());return AnatomyClientNetworking.presentationFrame(cow).isEmpty()&&AnatomyMovement.queryFrame(cow).isEmpty();},40);
   world.getServer().runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),saved.get()));c.waitTicks(3);
   c.runOnClient(mc->{var cow=(Cow)mc.level.getEntity(id.get());check(AnatomyClientNetworking.presentationFrame(cow).isEmpty()&&AnatomyMovement.queryFrame(cow).isEmpty(),"old A cannot revive after withdrawal without replacement");});
   world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(id.get());S48AdversarialVariantFixtures.signal.set("x");AnatomyRuntime.variantChanged(cow);});
   c.waitFor(mc->AnatomyClientNetworking.presentationFrame((Cow)mc.level.getEntity(id.get())).map(f->bt.equals(f.identity().bindingIdentity())).orElse(false),200);
   c.waitFor(mc->b.equals(AnatomyMovement.canonicalBinding((Cow)mc.level.getEntity(id.get()))),200);
   c.runOnClient(mc->{var cow=(Cow)mc.level.getEntity(id.get());check(AnatomyMovement.canonicalBinding(cow).equals(b),"fresh known recovery exact B");System.out.println("S48 ADV LIVE PASS native age transport unknown-withdrawal replay recovery");});
  }finally{world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
 }
 static void capture(String name,AnatomyClientNetworking.PresentationFrame frame){
  var root=frame.after().rootTransform();var p=root.origin();var q=root.quaternion();
  var inverse=new org.joml.Matrix4d().translation(p.x,p.y,p.z).rotate(new org.joml.Quaterniond(q)).scale(root.scale()).invert();
  var world=new TreeMap<String,List<List<Double>>>();var local=new TreeMap<String,List<List<Double>>>();
  frame.evaluated().pieces().forEach((key,box)->{world.put(key,box.vertices().stream().map(v->List.of(v.x,v.y,v.z)).toList());local.put(key,box.vertices().stream().map(v->{var x=inverse.transformPosition(new org.joml.Vector3d(v.x,v.y,v.z));return List.of(x.x,x.y,x.z);}).toList());});
  try{var dir=java.nio.file.Path.of(System.getProperty("scalebrews.s47SnapshotDir","s48-adversarial-snapshots"));java.nio.file.Files.createDirectories(dir);java.nio.file.Files.writeString(dir.resolve(name+"-actual-root.json"),new com.google.gson.Gson().toJson(Map.of("token",frame.identity().bindingIdentity().toString(),"generation",frame.identity().bindingGeneration(),"rootOrigin",List.of(p.x,p.y,p.z),"rootScale",root.scale(),"rootQuaternion",List.of(q.x,q.y,q.z,q.w),"world",world,"local",local)),java.nio.charset.StandardCharsets.UTF_8);}catch(java.io.IOException e){throw new AssertionError(e);}
 }
}
