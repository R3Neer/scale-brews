package io.github.r3neer.scalebrews.collision.internal;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import io.github.r3neer.scalebrews.collision.api.spi.*;
import io.github.r3neer.scalebrews.collision.data.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
/** ADVERSARY: exact binding transport, causal barriers and fixed reader budget. */
public final class S47AdversarialBindingTransportTests {
 static Identifier id(String s){return Identifier.parse(s);}
 static final Identifier ENTITY=id("minecraft:cow"), MODEL=id("scalebrews:s47_adv_model"),POSE=id("scalebrews:static"),ROOT=id("scalebrews:entity_root");
 static final UUID EPOCH=new UUID(47,1),SUPPORT=new UUID(47,2);
 static CollisionBinding row(String value,AnatomyFilter filter){return new CollisionBinding(1,ENTITY,Map.of("shape",value),new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,MODEL,Map.of(),filter),new CollisionBinding.Pose(POSE,Map.of(),Set.of()),ROOT,CollisionPolicy.Patch.EMPTY,Set.of());}
 static ModelGeometry model(){return new ModelGeometry(1,"adversarial","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new org.joml.Matrix4f()))),List.of(new ModelGeometry.Piece("solid","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)));}
 static AnatomyPosePayload packet(long serial,long gen,long tracking,Identifier token){var position=new Vec3(2,3,4);return new AnatomyPosePayload(EPOCH,1,id("minecraft:overworld"),47,SUPPORT,MODEL,POSE,ROOT,serial,serial,serial,serial,serial,gen,tracking,true,new PoseEngine.Inputs(.5f,.2f,3,4,5,true,Map.of("x",.7f)),position,17,2,Direction.DOWN,new RootTransformProvider.RootTransform(position,new Quaternionf().rotateXYZ(.1f,.2f,.3f),2),token);}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 @GameTest public void codecExactAndLocalAuthorityRejected(GameTestHelper h){
  var world=new WorldAnatomyCatalog();var row=row("one",AnatomyFilter.DEFAULT);var s=world.replace(Map.of(MODEL.toString(),model()),List.of(row));var token=s.bindingIdentity(s.resolve(ENTITY,Map.of("shape","one")).orElseThrow()).orElseThrow();var original=packet(7,3,5,token);
  var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());try{AnatomyPosePayload.CODEC.encode(buffer,original);var received=AnatomyPosePayload.CODEC.decode(buffer);check(received.bindingIdentity().equals(token)&&received.epoch().equals(original.epoch())&&received.revision()==1&&received.bindingGeneration()==3&&received.trackingGeneration()==5&&received.inputs().equals(original.inputs()),"Exact real codec tuple and token");check(received.rootProvider().equals(ROOT)&&received.rootTransform().quaternion().dot(original.rootTransform().quaternion())>.999999f,"Exact independent root survived codec");check(buffer.readableBytes()==0,"All v7 bytes consumed");}finally{buffer.release();}
  var local=packet(7,3,5,null);var missing=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());try{boolean reject=false;try{AnatomyPosePayload.CODEC.encode(missing,local);}catch(IllegalArgumentException x){reject=true;}check(reject&&missing.writerIndex()==0,"Incomplete local identity never emits wire bytes");}finally{missing.release();}
  check(AnatomyPosePayload.TYPE.id().getPath().equals("anatomy_pose_v7"),"Explicit wire protocol boundary");h.succeed();
 }
 @GameTest public void exactMembersAndNoDefaultBeforeEntity(GameTestHelper h){
  var a=row("a",AnatomyFilter.DEFAULT);var b=row("b",new AnatomyFilter(.01,.01,.001,Set.of(),Set.of("solid")));var world=new WorldAnatomyCatalog();var s=world.replace(Map.of(MODEL.toString(),model()),List.of(b,a));
  var aa=s.resolve(ENTITY,Map.of("shape","a")).orElseThrow();var bb=s.resolve(ENTITY,Map.of("shape","b")).orElseThrow();var at=s.bindingIdentity(aa).orElseThrow();var bt=s.bindingIdentity(bb).orElseThrow();check(!at.equals(bt)&&s.bindings().isEmpty(),"Two same nominal variants without default");
  check(s.resolvePose(null,packet(1,1,1,bt)).orElseThrow()==bb,"Exact member admitted before entity");check(s.resolvePose(ENTITY,packet(1,1,1,at)).orElseThrow()==aa,"Typed member exact");check(s.resolvePose(id("minecraft:pig"),packet(1,1,1,bt)).isEmpty(),"Wrong entity cannot borrow token");check(s.resolvePose(null,packet(1,1,1,id("scalebrews:binding/v1/"+"0".repeat(64)))).isEmpty(),"Unknown token not admitted early");check(s.resolvePose(null,packet(1,1,1,null)).isEmpty(),"Missing token not admitted early");
  var mismatch=packet(1,1,1,bt);mismatch=new AnatomyPosePayload(mismatch.epoch(),mismatch.revision(),mismatch.dimension(),mismatch.entityId(),mismatch.entity(),id("scalebrews:other"),mismatch.provider(),mismatch.rootProvider(),mismatch.frameSerial(),mismatch.authorityTick(),mismatch.jointSampleTick(),mismatch.rootFrameSequence(),mismatch.rootFrameTick(),mismatch.bindingGeneration(),mismatch.trackingGeneration(),true,mismatch.inputs(),mismatch.origin(),mismatch.yaw(),mismatch.scale(),mismatch.gravity(),mismatch.rootTransform(),bt);check(s.resolvePose(null,mismatch).isEmpty(),"Token and nominal tuple must describe same row");h.succeed();
 }
 @GameTest public void tokenAxisIndependentFromBindingAndTracking(GameTestHelper h){
  var a=CollisionBindingIdentity.of(row("a",AnatomyFilter.DEFAULT));var b=CollisionBindingIdentity.of(row("b",AnatomyFilter.DEFAULT));var history=new AnatomyFrameHistory();var first=packet(1,1,1,a);check(history.accept(first),"Initial history");check(history.transition(packet(2,1,1,b))==AnatomyFrameHistory.LifecycleTransition.REJECT,"Same generation cannot change only token");check(history.transition(packet(2,1,2,b))==AnatomyFrameHistory.LifecycleTransition.REJECT,"Retrack cannot launder token change");check(history.transition(packet(2,2,1,b))==AnatomyFrameHistory.LifecycleTransition.RESTART,"Binding restart permits new token");check(history.current()==first,"Classification pure");
  var fence=new TrackingReplayFence();fence.retire(first);check(fence.rejects(packet(2,1,1,b))&&fence.rejects(packet(2,1,2,b)),"Compact tombstone retains token axis");check(!fence.rejects(packet(2,2,1,b)),"Fresh physical generation admitted");fence.retireWindow(first);check(fence.rejects(packet(3,2,1,b)),"Closed tracking not revived by physical rebind");check(!fence.rejects(packet(3,2,2,b)),"Both required generations advance");h.succeed();
 }

 @GameTest public void fixedWireBudgetAndPreparedLookup(GameTestHelper h){
  check(CollisionBindingIdentity.TOKEN_LENGTH==86,"Token length fixed to current format");
  for(String text:List.of("scalebrews:binding/v1/"+"a".repeat(1000),"x".repeat(87),"scalebrews:binding/v2/"+"a".repeat(64),"scalebrews:binding/v1/"+"g".repeat(64))) {
   var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());try{buf.writeUtf(text);buf.writeZero(400);boolean rejected=false;try{AnatomyPosePayload.CODEC.decode(buf);}catch(RuntimeException expected){rejected=true;}check(rejected,"Invalid wire token rejected before remaining frame");check(buf.readerIndex()<=3+Math.min(text.length(),86),"Reader bounded before frame body");}finally{buf.release();}
  }
  var a=row("a",AnatomyFilter.DEFAULT);var b=row("b",new AnatomyFilter(.01,.01,.001,Set.of(),Set.of("solid")));var world=new WorldAnatomyCatalog();var s=world.replace(Map.of(MODEL.toString(),model()),List.of(a,b));var expected=s.resolve(ENTITY,Map.of("shape","b")).orElseThrow();var token=s.bindingIdentity(expected).orElseThrow();var p=packet(1,1,1,token);
  long before=hashCount();if(Boolean.getBoolean("scalebrews.s47RequireHashCounter"))check(before>0,"Instrumented preparation counter active");for(int i=0;i<100000;i++){check(s.resolvePose(null,p).orElseThrow()==expected,"Early lookup exact");check(s.resolvePose(ENTITY,p).orElseThrow()==expected,"Typed lookup exact");}check(hashCount()==before,"No preparation/hash during200k prepared lookups");System.out.println("S47 ADV PREPARED_LOOKUP PASS count=200000 repeatedHashes="+(hashCount()-before));h.succeed();
 }
 static long hashCount(){try{return ((java.util.concurrent.atomic.AtomicLong)CollisionBindingIdentity.class.getDeclaredField("ADVERSARIAL_HASH_COUNT").get(null)).get();}catch(NoSuchFieldException absent){return 0;}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 @GameTest public void zeroGenerationPreservesIndependentRootAndToken(GameTestHelper h){
  var cow=net.minecraft.world.entity.EntityTypes.COW.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);check(cow!=null,"Real factory cow");var token=CollisionBindingIdentity.of(row("zero",AnatomyFilter.DEFAULT));var root=id("scalebrews:s47_custom_root");GeometryProvider provider=entity->Optional.empty();
  try{AnatomyBindingState.rebind(cow,provider,new GeometryProvider.GeometryIdentityDescriptor(EPOCH,1,MODEL,POSE,root,0,token));var captured=AnatomyBindingState.descriptor(cow);check(captured.bindingGeneration()>0&&captured.rootProvider().equals(root)&&captured.bindingIdentity().equals(token),"Generation assignment preserves exact root/token");}finally{cow.discard();}h.succeed();
 }
}
