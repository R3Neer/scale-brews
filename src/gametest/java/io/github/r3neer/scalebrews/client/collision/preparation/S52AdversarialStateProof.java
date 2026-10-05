package io.github.r3neer.scalebrews.client.collision.preparation;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.model.EntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
public final class S52AdversarialStateProof implements FabricClientGameTest {
  static final class ForeignRabbit extends RabbitRenderer {ForeignRabbit(EntityRendererProvider.Context context){super(context);}}
 static EntityRenderer<?,?> foreign(EntityRenderer<?,?> original) throws ReflectiveOperationException {
  var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);var unsafe=(sun.misc.Unsafe)field.get(null);var forged=(EntityRenderer<?,?>)unsafe.allocateInstance(ForeignRabbit.class);
  for(String name:List.of("adultModel","babyModel")){var slot=AgeableMobRenderer.class.getDeclaredField(name);slot.setAccessible(true);slot.set(forged,slot.get(original));}return forged;
 }
 static void programFaults() throws ReflectiveOperationException {
  var selector=OriginalAgeStateModelPreparation.class.getDeclaredMethod("selector",OriginalModelPrograms.class);selector.setAccessible(true);var bridge=OriginalAgeStateModelPreparation.class.getDeclaredMethod("bridge",OriginalModelPrograms.class);bridge.setAccessible(true);
  var constructor=OriginalAgeStateModelPreparation.class.getDeclaredMethod("constructor",OriginalModelPrograms.class,selector.getReturnType());constructor.setAccessible(true);var cache=OriginalModelPrograms.class.getDeclaredField("classes");cache.setAccessible(true);
  for(int fault=0;fault<3;fault++){
   var programs=new OriginalModelPrograms(AgeableMobRenderer.class.getClassLoader());var original=programs.model(AgeableMobRenderer.class);Object slots=selector.invoke(null,programs);final int f=fault;
   var classfile=java.lang.classfile.ClassFile.of(java.lang.classfile.ClassFile.StackMapsOption.DROP_STACK_MAPS);
   java.lang.classfile.ClassTransform change;var target=new java.lang.classfile.Label[1];
   if(f==2)change=java.lang.classfile.ClassTransform.transformingMethods(m->m.flags().has(java.lang.reflect.AccessFlag.BRIDGE),(b,e)->{if(e instanceof java.lang.classfile.AccessFlags a)b.withFlags(a.flagsMask() & ~java.lang.classfile.ClassFile.ACC_SYNTHETIC);else b.with(e);});
   else change=java.lang.classfile.ClassTransform.transformingMethodBodies(m->f==0?m.methodName().stringValue().equals("<init>"):m.methodName().stringValue().equals("submit")&&!m.flags().has(java.lang.reflect.AccessFlag.BRIDGE),(b,e)->{
    if(f==0&&e instanceof java.lang.classfile.instruction.LoadInstruction load&&load.slot()==3)b.aload(2);
    else if(f==1&&e instanceof java.lang.classfile.instruction.BranchInstruction branch&&branch.opcode()==java.lang.classfile.Opcode.IFEQ){target[0]=branch.target();b.with(e);}
    else if(f==1&&e instanceof java.lang.classfile.instruction.BranchInstruction branch&&branch.opcode()==java.lang.classfile.Opcode.GOTO)b.goto_(target[0]);
    else b.with(e);
   });
   byte[] changed=classfile.transformClass(original,change); if(f==1){try(var stream=AgeableMobRenderer.class.getResourceAsStream("/net/minecraft/client/renderer/entity/AgeableMobRenderer.class")){changed=stream.readAllBytes();}catch(java.io.IOException io){throw new AssertionError(io);}int patched=0;for(int i=0;i<changed.length-5;i++)if((changed[i]&255)==167&&changed[i+1]==0&&changed[i+2]==7&&(changed[i+3]&255)==42&&(changed[i+4]&255)==180){changed[i+2]=3;patched++;}check(patched==1,"fixture exact native goto byte witness");}((Map<Class<?>,java.lang.classfile.ClassModel>)cache.get(programs)).put(AgeableMobRenderer.class,classfile.parse(changed));
   try{if(f==0)constructor.invoke(null,programs,slots);else if(f==1)selector.invoke(null,programs);else bridge.invoke(null,programs);throw new AssertionError("S52 ADV malformed program admitted "+f);}catch(InvocationTargetException expected){System.out.println("S52 FAULT "+f+" reason="+expected.getCause());check(expected.getCause() instanceof IllegalArgumentException,"unexpected malformed-program exception "+expected.getCause());check(expected.getCause().getMessage().contains(new String[]{"program_local","model_join_target","submit_bridge_flags"}[f]),"incorrect fault boundary "+expected.getCause());}
  }
  System.out.println("S52_PROGRAM_FAULTS_PASS constructor-alias/join-target/bridge-flags");
 }
 static void check(boolean value,String message){if(!value)throw new AssertionError("S52 ADV "+message);}
 @SuppressWarnings({"rawtypes","unchecked"}) public void runTest(ClientGameTestContext c){
  try{OriginalAgeStateModelPreparation.inspect(null);throw new AssertionError("off-thread preparation accepted");}catch(IllegalStateException expected){}
  var world=c.worldBuilder().create();try{c.runOnClient(mc->{try{
   programFaults(); int selections=0,rejections=0;var sheet=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1400\" height=\"280\"><rect width=\"1400\" height=\"280\" fill=\"white\"/>");
   var types=List.of(EntityTypes.RABBIT,EntityTypes.GOAT,EntityTypes.TURTLE);
   var adult=AgeableMobRenderer.class.getDeclaredField("adultModel");var baby=AgeableMobRenderer.class.getDeclaredField("babyModel");adult.setAccessible(true);baby.setAccessible(true);
   for(int k=0;k<types.size();k++){
    var e=(AgeableMob)types.get(k).create(mc.level,EntitySpawnReason.COMMAND);e.setId(9950+k);mc.level.addEntity(e);EntityRenderer renderer=mc.getEntityRenderDispatcher().getRenderer(e);
    if(k==0)check(!OriginalAgeStateModelPreparation.inspect(foreign(renderer)).admitted(),"foreign hierarchy inherited proof");var result=OriginalAgeStateModelPreparation.inspect(renderer);check(result.admitted(),"original rejected "+result.absence());var recipe=result.recipe();check(recipe.extractionAuthority().equals("UNPROVEN"),"state relation promoted extraction authority");check(recipe.rows().size()==2&&recipe.uniqueModels()==2,"native two-slot domain");
    check(recipe.equals(OriginalAgeStateModelPreparation.inspect(renderer).recipe()),"non-deterministic recipe");
    try{recipe.rows().clear();throw new AssertionError("mutable rows");}catch(UnsupportedOperationException expected){}
    try{recipe.originalResources().clear();throw new AssertionError("mutable resources");}catch(UnsupportedOperationException expected){}
    for(boolean entityBaby:new boolean[]{false,true}){e.setBaby(entityBaby);var state=(LivingEntityRenderState)renderer.createRenderState(e,0f);check(state.isBaby==entityBaby,"fixture native extraction disagrees");
     for(boolean invert:new boolean[]{false,true}){state.isBaby=entityBaby^invert;var model=(EntityModel)(state.isBaby?baby.get(renderer):adult.get(renderer));var row=recipe.rows().stream().filter(r->r.statePredicate()==state.isBaby).findFirst().orElseThrow();
      check(row.initialStructure().equals(ModelPartStructure.inspect(model.root())),"fingerprint differs from exact native slot");var store=new SubmitNodeStorage();var camera=new CameraRenderState();camera.pos=net.minecraft.world.phys.Vec3.ZERO;camera.orientation=new org.joml.Quaternionf();renderer.submit(state,new PoseStack(),store,camera);var queued=new ArrayList<Object>();store.drainPhases(p->p.sortInto((node,flag)->{if(node instanceof ModelFeatureRenderer.Submit<?> m)queued.add(m.model());}));check(queued.stream().anyMatch(m->m==model),"submit ignored declared state predicate");if(!invert)sheet.append(S51AdversarialNativeProof.snapshot(model.root(),types.get(k).toString()+" "+entityBaby,k*2+(entityBaby?1:0)));selections++;
     }
    }
    var a=(EntityModel)adult.get(renderer);var b=(EntityModel)baby.get(renderer);var pose=a.root().storePose();boolean visible=a.root().visible;a.root().x+=123;a.root().visible=false;var saved=S51AdversarialNativeProof.state(a.root());check(recipe.equals(OriginalAgeStateModelPreparation.inspect(renderer).recipe()),"current pose polluted recipe");check(saved.equals(S51AdversarialNativeProof.state(a.root())),"shared animation mutated");a.root().loadPose(pose);a.root().visible=visible;
    baby.set(renderer,null);try{check(!OriginalAgeStateModelPreparation.inspect(renderer).admitted(),"partial slot admitted");rejections++;}finally{baby.set(renderer,b);}
    adult.set(renderer,null);try{check(!OriginalAgeStateModelPreparation.inspect(renderer).admitted(),"missing adult admitted");rejections++;}finally{adult.set(renderer,a);}
    baby.set(renderer,a);try{var alias=OriginalAgeStateModelPreparation.inspect(renderer);check(alias.admitted()&&alias.recipe().uniqueModels()==1&&alias.recipe().rows().size()==2,"shared slots incorrectly collapsed/copied");}finally{baby.set(renderer,b);}
    check(recipe.equals(OriginalAgeStateModelPreparation.inspect(renderer).recipe()),"failure/replacement poisoned recipe");mc.level.removeEntity(e.getId(),Entity.RemovalReason.DISCARDED);
   }
   var outside=EntityTypes.COW.create(mc.level,EntitySpawnReason.COMMAND);check(!OriginalAgeStateModelPreparation.inspect(mc.getEntityRenderDispatcher().getRenderer(outside)).admitted(),"other selector family admitted");check(!OriginalAgeStateModelPreparation.inspect(null).admitted(),"null renderer admitted");
   sheet.append("</svg>");var out=java.nio.file.Path.of("s52-adversarial-snapshots");java.nio.file.Files.createDirectories(out);java.nio.file.Files.writeString(out.resolve("initial-slots.svg"),sheet);
   System.out.println("S52_INDEPENDENT_STATE_PASS selections="+selections+" missingSlots="+rejections+" stateAuthority=UNPROVEN");
  }catch(ReflectiveOperationException|java.io.IOException failure){throw new AssertionError(failure);}});}finally{world.close();}
 }
}