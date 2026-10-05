package io.github.r3neer.scalebrews.collision.internal;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.packs.resources.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.collision.data.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.api.spi.*;
/** ADVERSARY: real receiver and original Cow geometry; explicit publication does not certify live trait selection. */
public final class S47AdversarialExactBindingClientProof implements FabricClientGameTest {
 static final Identifier MODEL=Identifier.parse("s47_adversary:cow"),TYPE=Identifier.parse("minecraft:cow"),POSE=Identifier.parse("scalebrews:static"),ROOT=Identifier.parse("scalebrews:entity_root");
 static CollisionBinding row(String v,AnatomyFilter filter){return new CollisionBinding(1,TYPE,Map.of("shape",v),new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,MODEL,Map.of(),filter),new CollisionBinding.Pose(POSE,Map.of(),Set.of()),ROOT,CollisionPolicy.Patch.EMPTY,Set.of());}
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 static AnatomyPosePayload packet(Cow cow,UUID epoch,long rev,long serial,long generation,Identifier token){Vec3 p=cow.position();return new AnatomyPosePayload(epoch,rev,cow.level().dimension().identifier(),cow.getId(),cow.getUUID(),MODEL,POSE,ROOT,serial,serial,serial,serial,serial,generation,1,true,new PoseEngine.Inputs(0,0,0,0,0,true),p,0,1,Direction.DOWN,new RootTransformProvider.RootTransform(p,new Quaternionf(),1),token);}
 static ResourceManager resources(ResourceManager installed,ModelGeometry model,List<CollisionBinding> rows){
  var pack=installed.listPacks().findFirst().orElseThrow();var geometry=new HashMap<Identifier,Resource>();var bindings=new HashMap<Identifier,Resource>();
  String json=AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE,model).getOrThrow().toString();byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
  geometry.put(Identifier.parse("s47_adversary:scalebrews/entity_geometry/cow.json"),new Resource(pack,()->new ByteArrayInputStream(bytes)));
  for(int i=0;i<rows.size();i++){byte[] body=CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE,rows.get(i)).getOrThrow().toString().getBytes(StandardCharsets.UTF_8);bindings.put(Identifier.parse("s47_adversary:scalebrews/entity_collision/row"+i+".json"),new Resource(pack,()->new ByteArrayInputStream(body)));}
  return (ResourceManager)Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class[]{ResourceManager.class},(o,m,args)->{
   if(m.getName().equals("listPacks"))return installed.listPacks();if(m.getName().equals("listResources"))return switch((String)args[0]){case "scalebrews/entity_geometry"->geometry;case "scalebrews/entity_collision"->bindings;default->Map.of();};throw new UnsupportedOperationException(m.getName());});
 }
 @Override public void runTest(ClientGameTestContext c){
  var geometry=c.computeOnClient(mc->ModelPartGeometryEngine.inspectVisible(TYPE,CowModel.createBodyLayer().bakeRoot(),new org.joml.Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
  String omitted=geometry.pieces().getFirst().id();var a=row("a",AnatomyFilter.DEFAULT);var b=row("b",new AnatomyFilter(AnatomyFilter.DEFAULT.minThickness(),AnatomyFilter.DEFAULT.minAspect(),AnatomyFilter.DEFAULT.minVolumeRatio(),Set.of(),Set.of(omitted)));var rows=List.of(a,b);var models=Map.of(MODEL.toString(),geometry);
  var world=c.worldBuilder().create();var id=new AtomicInteger(-1);var prior=new AtomicReference<AnatomyPosePayload>();var newer=new AtomicReference<AnatomyPosePayload>();var at=CollisionBindingIdentity.of(a);var bt=CollisionBindingIdentity.of(b);var baseline=new AtomicReference<Map<String,io.github.r3neer.scalebrews.collision.geometry.ConvexBox>>();
  try{
   world.getServer().runOnServer(s->{AnatomyRuntime.start(s);AnatomyRuntime.reload(s,resources(s.getResourceManager(),geometry,rows));var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);check(cow!=null,"Factory cow");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);s.overworld().addFreshEntity(cow);id.set(cow.getId());var player=s.getPlayerList().getPlayers().getFirst();AnatomyNetworking.sendCatalog(player,AnatomyCatalogTransfer.encode(AnatomyNetworking.epoch(s),AnatomyNetworking.revision(s),models,rows));prior.set(packet(cow,AnatomyNetworking.epoch(s),AnatomyNetworking.revision(s),1,1,at));});
   c.waitFor(mc->mc.level!=null&&mc.level.getEntity(id.get()) instanceof Cow&&AnatomyClientNetworking.catalog().ready()&&AnatomyClientNetworking.catalog().snapshot().prepared().size()==2,150);
   c.runOnClient(mc->check(AnatomyClientNetworking.catalog().snapshot().bindings().isEmpty(),"Client variant-only projection has no default"));
   world.getServer().runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),prior.get()));
   c.waitFor(mc->AnatomyClientNetworking.presentationFrame((Cow)mc.level.getEntity(id.get())).map(f->at.equals(f.identity().bindingIdentity())).orElse(false),150);
   c.waitFor(mc->AnatomyMovement.queryFrame((Cow)mc.level.getEntity(id.get())).isPresent(),150);
   c.runOnClient(mc->{var cow=(Cow)mc.level.getEntity(id.get());check(AnatomyMovement.queryFrame(cow).isPresent(),"Variant-only physics activated");check(AnatomyMovement.canonicalBinding(cow).equals(a),"Exact first physical policy row");var f=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();check(f.evaluated().pieces().containsKey(omitted),"Original piece exists in A");rendererSelection(f,cow,a.geometry().filter());baseline.set(f.evaluated().pieces());capture("variant-a",f.evaluated().pieces(),at);});
   world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(id.get());newer.set(packet(cow,AnatomyNetworking.epoch(s),AnatomyNetworking.revision(s),2,2,bt));ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),newer.get());});
   c.waitFor(mc->AnatomyClientNetworking.presentationFrame((Cow)mc.level.getEntity(id.get())).map(f->bt.equals(f.identity().bindingIdentity())).orElse(false),150);
   c.waitFor(mc->b.equals(AnatomyMovement.canonicalBinding((Cow)mc.level.getEntity(id.get()))),150);
   c.runOnClient(mc->{var cow=(Cow)mc.level.getEntity(id.get());check(AnatomyMovement.canonicalBinding(cow).equals(b),"Exact replacement physical policy row");var f=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();check(!f.evaluated().pieces().containsKey(omitted),"B filter applied despite same model/pose/root");rendererSelection(f,cow,b.geometry().filter());var expected=new TreeMap<>(baseline.get());expected.remove(omitted);check(expected.equals(f.evaluated().pieces()),"Exactly the declared piece removed, all other vertices preserved");capture("variant-b",f.evaluated().pieces(),bt);check(!AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().containsKey(omitted),"Physical B filter agrees");});
   world.getServer().runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),prior.get()));c.waitTicks(3);
   c.runOnClient(mc->{var cow=(Cow)mc.level.getEntity(id.get());check(AnatomyClientNetworking.presentationFrame(cow).orElseThrow().identity().bindingIdentity().equals(bt)&&AnatomyMovement.canonicalBinding(cow).equals(b),"Old-token replay cannot change either consumer");System.out.println("S47 ADV EXACT_CLIENT PASS original="+geometry.pieces().size()+" excluded="+omitted);});

   var early=new AtomicReference<Cow>();
   world.getServer().runOnServer(server->{var cow=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);check(cow!=null,"Early factory cow");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(4,3,2);early.set(cow);var bad=packet(cow,AnatomyNetworking.epoch(server),AnatomyNetworking.revision(server),100,1,Identifier.parse("scalebrews:binding/v1/"+"0".repeat(64)));ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),bad);});
   c.waitTicks(2);
   world.getServer().runOnServer(server->{var cow=early.get();ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),packet(cow,AnatomyNetworking.epoch(server),AnatomyNetworking.revision(server),1,1,at));});
   c.waitTicks(2);
   world.getServer().runOnServer(server->check(server.overworld().addFreshEntity(early.get()),"Early cow insertion"));
   c.waitFor(mc->mc.level.getEntity(early.get().getId()) instanceof Cow cow&&AnatomyClientNetworking.presentationFrame(cow).map(f->f.after().frameSerial()==1&&f.identity().bindingIdentity().equals(at)).orElse(false),150);
   c.waitFor(mc->AnatomyMovement.queryFrame((Cow)mc.level.getEntity(early.get().getId())).isPresent(),150);
   c.runOnClient(mc->System.out.println("S47 ADV BEFORE_ENTITY PASS rejectedSerial=100 acceptedSerial=1 exactToken="+at));
  }finally{world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
 }

 static void capture(String name,Map<String,io.github.r3neer.scalebrews.collision.geometry.ConvexBox> boxes,Identifier token){
  var out=new TreeMap<String,List<List<Double>>>();boxes.forEach((key,box)->out.put(key,box.vertices().stream().map(v->List.of(v.x,v.y,v.z)).toList()));
  try{var dir=java.nio.file.Path.of(System.getProperty("scalebrews.s47SnapshotDir","s47-adversarial-snapshots"));java.nio.file.Files.createDirectories(dir);java.nio.file.Files.writeString(dir.resolve(name+".json"),new com.google.gson.Gson().toJson(Map.of("token",token.toString(),"origin",List.of(2,3,2),"pose","neutral","rootQuaternion",List.of(0,0,0,1),"pieces",out)),StandardCharsets.UTF_8);}catch(java.io.IOException e){throw new AssertionError(e);}
 }

 static void rendererSelection(AnatomyClientNetworking.PresentationFrame frame,Cow cow,AnatomyFilter expected){
  try{var method=io.github.r3neer.scalebrews.client.collision.presentation.CausalModelRender.class.getDeclaredMethod("selection",AnatomyClientNetworking.PresentationFrame.class,LivingEntity.class);method.setAccessible(true);var selection=method.invoke(null,frame,cow);check(selection!=null,"Renderer has exact variant selection without default");var getter=selection.getClass().getDeclaredMethod("filter");getter.setAccessible(true);check(expected.equals(getter.invoke(selection)),"Renderer selection agrees with physical/presentation row");}catch(ReflectiveOperationException e){throw new AssertionError(e);}
 }
}
