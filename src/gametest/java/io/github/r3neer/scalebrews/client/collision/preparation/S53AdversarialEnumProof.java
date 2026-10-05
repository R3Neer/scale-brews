package io.github.r3neer.scalebrews.client.collision.preparation;
import java.lang.reflect.*;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.util.StringRepresentable;
import com.mojang.blaze3d.vertex.PoseStack;
public final class S53AdversarialEnumProof implements FabricClientGameTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError("S53 ADV "+m);}
 static List<Object> submitted(EntityRenderer renderer,Object state){var camera=new CameraRenderState();camera.pos=net.minecraft.world.phys.Vec3.ZERO;camera.orientation=new org.joml.Quaternionf();var queue=new SubmitNodeStorage();renderer.submit((net.minecraft.client.renderer.entity.state.EntityRenderState)state,new PoseStack(),queue,camera);var out=new ArrayList<Object>();queue.drainPhases(p->p.sortInto((node,f)->{if(node instanceof ModelFeatureRenderer.Submit<?> m)out.add(m.model());}));return out;}
 static void programFaults(Class<?> renderer) throws ReflectiveOperationException,java.io.IOException {
  var classes=OriginalModelPrograms.class.getDeclaredField("classes");classes.setAccessible(true);
  for(int f=0;f<2;f++){
   var programs=new OriginalModelPrograms(renderer.getClassLoader());var selector=OriginalEnumSelectorProof.selector(programs,renderer,OriginalEnumSelectorProof.Family.ENUM_ONLY);
   Class<?> owner=f==0?Mob.class:renderer;byte[] bytes;try(var stream=owner.getResourceAsStream("/"+owner.getName().replace('.','/')+".class")){bytes=stream.readAllBytes();}
   int patched=0;
   if(f==0){for(int j=0;j<bytes.length-7;j++)if(bytes[j]==1&&bytes[j+1]==0&&bytes[j+2]==4&&bytes[j+3]=='B'&&bytes[j+4]=='O'&&bytes[j+5]=='D'&&bytes[j+6]=='Y'){bytes[j+3]='H';bytes[j+4]='E';bytes[j+5]='A';bytes[j+6]='D';patched++;}}
   else{for(int j=0;j<bytes.length-8;j++)if(bytes[j]==43&&(bytes[j+1]&255)==180&&(bytes[j+4]&255)==199&&bytes[j+5]==0&&bytes[j+6]==4&&(bytes[j+7]&255)==177){bytes[j+5]=(byte)255;bytes[j+6]=(byte)252;patched++;}}
   check(patched==1,"native operand witness count "+f+"/"+patched);
   ((Map<Class<?>,java.lang.classfile.ClassModel>)classes.get(programs)).put(owner,java.lang.classfile.ClassFile.of().parse(bytes));
   try{if(f==0)OriginalEnumSelectorProof.inputs(programs,renderer,selector);else OriginalEnumSelectorProof.selector(programs,renderer,OriginalEnumSelectorProof.Family.ENUM_ONLY);throw new AssertionError("S53 ADV program fault admitted "+f);}catch(IllegalArgumentException expected){check(expected.getMessage().contains(f==0?"body_equipment_capture_slot":"null_guard_target"),"wrong fault boundary "+f+" "+expected);System.out.println("S53_PROGRAM_FAULT "+f+" "+expected.getMessage());}
  }
 }
 static void previousRecipes(net.minecraft.client.Minecraft mc) throws java.io.IOException {
  var prior=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("scalebrews.s53Reference")))).getAsJsonArray();int matched=0;
  for(var type:List.of(EntityTypes.COW,EntityTypes.PIG,EntityTypes.CHICKEN)){
   var actor=type.create(mc.level,EntitySpawnReason.COMMAND);var renderer=mc.getEntityRenderDispatcher().getRenderer(actor);var result=EnumAgeModelPreparation.inspect(renderer);check(result.admitted(),"shared legacy family rejected "+result.absence());var serialized=new com.google.gson.Gson().toJsonTree(result.recipe());var original=prior.asList().stream().filter(x->x.getAsJsonObject().get("rendererClass").getAsString().equals(renderer.getClass().getName())).findFirst().orElseThrow();check(serialized.equals(original),"shared extraction changed prior canonical recipe "+type);matched++;
  }
  System.out.println("S53_PRIOR_RECIPES_PASS exact="+matched);
 }
 @SuppressWarnings({"rawtypes","unchecked"}) public void runTest(ClientGameTestContext context){
  try{EnumModelPreparation.inspect(null);throw new AssertionError("off-thread accepted");}catch(IllegalStateException expected){}
  var world=context.worldBuilder().create();try{context.runOnClient(mc->{try{
   previousRecipes(mc);try{OriginalEnumSelectorProof.originalOrigin(S53AdversarialEnumProof.class,LivingEntityRenderer.class);throw new AssertionError("S53 ADV foreign resource accepted");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("non_original_artifact"),"origin fault boundary");}var e=EntityTypes.ZOMBIE_NAUTILUS.create(mc.level,EntitySpawnReason.COMMAND);e.setId(9971);mc.level.addEntity(e);EntityRenderer renderer=mc.getEntityRenderDispatcher().getRenderer(e);
   programFaults(renderer.getClass());var result=EnumModelPreparation.inspect(renderer);check(result.admitted(),"native rejected "+result.absence());var recipe=result.recipe();check(recipe.tool().equals("original-enum-selector-v1"),"family/version changed");check(recipe.exactActorClass().equals(e.getClass().getName()),"actor authority differs");check(recipe.equals(EnumModelPreparation.inspect(renderer).recipe()),"repeat identity differs");
   check(!EnumAgeModelPreparation.inspect(renderer).admitted(),"single family leaked into age API");
   var field=renderer.getClass().getDeclaredField("models");field.setAccessible(true);Map map=(Map)field.get(renderer);var keys=new ArrayList(map.keySet());keys.sort(Comparator.comparing(k->((Enum)k).name()));check(keys.size()==recipe.rows().size(),"full enum domain omitted");
   DataComponentType component=(DataComponentType)BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(net.minecraft.resources.Identifier.parse(recipe.componentId()));check(component!=null,"native component missing");Holder holder=(Holder)e.get(component);var original=holder.value();var fields=original.getClass().getRecordComponents();var values=new Object[fields.length];int modelIndex=-1;for(int j=0;j<fields.length;j++){values[j]=fields[j].getAccessor().invoke(original);if(values[j] instanceof ModelAndTexture)modelIndex=j;}check(modelIndex>=0,"variant schema missing");var old=(ModelAndTexture)values[modelIndex];var ctor=original.getClass().getConstructor(Arrays.stream(fields).map(RecordComponent::getType).toArray(Class[]::new));
   var sheet=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1400\" height=\"280\"><rect width=\"1400\" height=\"280\" fill=\"white\"/>");int combinations=0;
   for(Object key:keys){values[modelIndex]=new ModelAndTexture(key,old.asset());var variant=ctor.newInstance(values);e.setComponent(component,Holder.direct(variant));var state=renderer.createRenderState(e,0f);var stateVariant=state.getClass().getField("variant");check(stateVariant.get(state)==variant,"native extraction did not use component");var model=(EntityModel)map.get(key);String name=((StringRepresentable)key).getSerializedName();var row=recipe.rows().stream().filter(v->v.modelValue().equals(name)).findFirst().orElseThrow();check(row.initialStructure().equals(ModelPartStructure.inspect(model.root())),"native fingerprint mismatch");check(name.equals(io.github.r3neer.scalebrews.collision.api.CollisionVariantSources.snapshot().get(recipe.sourceKey()).reader().value(e)),"source reader differs from original");check(submitted(renderer,state).stream().anyMatch(v->v==model),"exact deferred model differs");sheet.append(S51AdversarialNativeProof.snapshot(model.root(),name,combinations++));stateVariant.set(state,null);check(submitted(renderer,state).isEmpty(),"null guard replayed previous model");}
   try{recipe.rows().clear();throw new AssertionError("mutable rows");}catch(UnsupportedOperationException expected){}try{recipe.originalResources().clear();throw new AssertionError("mutable resources");}catch(UnsupportedOperationException expected){}
   Object key=keys.getFirst();Object first=map.get(key);var root=((EntityModel)first).root();var pose=root.storePose();boolean visible=root.visible;root.x+=151;root.visible=false;String animated=S51AdversarialNativeProof.state(root);check(recipe.equals(EnumModelPreparation.inspect(renderer).recipe()),"current pose polluted recipe");check(animated.equals(S51AdversarialNativeProof.state(root)),"shared model mutation");root.loadPose(pose);root.visible=visible;
   map.remove(key);try{check(!EnumModelPreparation.inspect(renderer).admitted(),"partial domain admitted");}finally{map.put(key,first);}map.put(key,null);try{check(!EnumModelPreparation.inspect(renderer).admitted(),"null model admitted");}finally{map.put(key,first);}
   Map wrong=new LinkedHashMap(map);wrong.remove(key);wrong.put(Thread.State.NEW,first);field.set(renderer,wrong);try{check(!EnumModelPreparation.inspect(renderer).admitted(),"foreign enum admitted");}finally{field.set(renderer,map);}
   if(keys.size()>1){Object last=keys.getLast(),saved=map.get(last);map.put(last,first);try{var alias=EnumModelPreparation.inspect(renderer);check(alias.admitted()&&alias.recipe().rows().size()==recipe.rows().size()&&alias.recipe().uniqueModels()==recipe.uniqueModels()-1,"identity alias incorrectly copied/collapsed");}finally{map.put(last,saved);}}
   check(recipe.equals(EnumModelPreparation.inspect(renderer).recipe()),"failure poisoned later preparation");check(!EnumModelPreparation.inspect(null).admitted(),"null renderer admitted");var cow=EntityTypes.COW.create(mc.level,EntitySpawnReason.COMMAND);check(!EnumModelPreparation.inspect(mc.getEntityRenderDispatcher().getRenderer(cow)).admitted(),"age-only family admitted");
   mc.level.removeEntity(e.getId(),Entity.RemovalReason.DISCARDED);sheet.append("</svg>");var out=Path.of("s53-adversarial-snapshots");Files.createDirectories(out);Files.writeString(out.resolve("native-single-model.svg"),sheet);Files.writeString(out.resolve("recipe.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(recipe));System.out.println("S53_INDEPENDENT_ENUM_PASS combinations="+combinations+" faults=2");
  }catch(ReflectiveOperationException|java.io.IOException exception){throw new AssertionError(exception);}});}finally{world.close();}
 }
}
