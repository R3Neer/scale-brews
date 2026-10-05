package io.github.r3neer.scalebrews.collision.internal;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Proxy;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.api.spi.*;
import io.github.r3neer.scalebrews.collision.data.*;
import io.github.r3neer.scalebrews.collision.geometry.*;
public final class S48AdversarialVariantFixtures {
 static final Identifier TYPE=Identifier.parse("minecraft:cow"),MODEL=Identifier.parse("s48_adversary:solid"),POSE=Identifier.parse("scalebrews:static"),ROOT=Identifier.parse("scalebrews:entity_root");
 static final AtomicReference<String> signal=new AtomicReference<>("x");
 static final AtomicInteger reads=new AtomicInteger();
 static boolean registered;
 static void check(boolean ok,String message){if(!ok)throw new AssertionError("S48 ADV "+message);}
 static CollisionBinding row(Map<String,String> variant){return new CollisionBinding(1,TYPE,variant,new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,MODEL,Map.of(),AnatomyFilter.DEFAULT),new CollisionBinding.Pose(POSE,Map.of(),Set.of()),ROOT,CollisionPolicy.Patch.EMPTY,Set.of());}
 static ModelGeometry model(){return new ModelGeometry(1,"independent","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new org.joml.Matrix4f()))),List.of(new ModelGeometry.Piece("solid","root",List.of(-1d,0d,-1d),List.of(1d,1d,1d),null)));}
 static ResourceManager resources(ResourceManager installed,List<CollisionBinding> rows){return resources(installed,model(),rows);}
 static ResourceManager resources(ResourceManager installed,ModelGeometry geometryModel,List<CollisionBinding> rows){
  var pack=installed.listPacks().findFirst().orElseThrow();var geometry=new HashMap<Identifier,Resource>();var bindings=new HashMap<Identifier,Resource>();
  byte[] bytes=AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE,geometryModel).getOrThrow().toString().getBytes(StandardCharsets.UTF_8);
  geometry.put(Identifier.parse("s48_adversary:scalebrews/entity_geometry/solid.json"),new Resource(pack,()->new ByteArrayInputStream(bytes)));
  for(int i=0;i<rows.size();i++){byte[] b=CollisionCodecs.BINDING.encodeStart(JsonOps.INSTANCE,rows.get(i)).getOrThrow().toString().getBytes(StandardCharsets.UTF_8);bindings.put(Identifier.parse("s48_adversary:scalebrews/entity_collision/row"+i+".json"),new Resource(pack,()->new ByteArrayInputStream(b)));}
  return (ResourceManager)Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class[]{ResourceManager.class},(o,m,args)->{if(m.getName().equals("listPacks"))return installed.listPacks();if(m.getName().equals("listResources"))return switch((String)args[0]){case "scalebrews/entity_geometry"->geometry;case "scalebrews/entity_collision"->bindings;default->Map.of();};throw new UnsupportedOperationException(m.getName());});
 }
 static Optional<?> staleSnapshot(Cow cow,GeometryProvider provider){try{var m=AnatomyMovement.class.getDeclaredMethod("currentSnapshot",net.minecraft.world.entity.LivingEntity.class,GeometryProvider.class);m.setAccessible(true);return (Optional<?>)m.invoke(null,cow,provider);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static long resolveCount(){try{return ((AtomicLong)PreparedVariantBindings.class.getDeclaredField("ADV_RESOLVES").get(null)).get();}catch(NoSuchFieldException absent){return 0;}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static void registry(){if(!registered){CollisionVariantSources.register("adv_signal",Set.of("x","y"),e->{reads.incrementAndGet();return signal.get();});registered=true;}}

}
