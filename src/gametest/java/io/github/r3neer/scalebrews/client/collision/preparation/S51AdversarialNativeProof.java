package io.github.r3neer.scalebrews.client.collision.preparation;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.util.StringRepresentable;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.EntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
public final class S51AdversarialNativeProof implements FabricClientGameTest {
 static String state(net.minecraft.client.model.geom.ModelPart part){var text=new StringBuilder();var todo=new ArrayDeque<net.minecraft.client.model.geom.ModelPart>();todo.add(part);while(!todo.isEmpty()){var p=todo.removeFirst();text.append(p.storePose()).append(p.visible).append(p.skipDraw).append(p.getInitialPose());todo.addAll(((io.github.r3neer.scalebrews.client.mixin.ModelPartAccess)(Object)p).scalebrews$children().values());}return text.toString();}
 static String snapshot(net.minecraft.client.model.geom.ModelPart root,String title,int index){var b=new StringBuilder();int x=(index%7)*200,y=(index/7)*280;b.append("<g transform=\"translate(").append(x+100).append(',').append(y+60).append(")\"><text x=\"-95\" y=\"-35\" font-size=\"13\">").append(title).append("</text>");draw(root,new org.joml.Matrix4f(),b);return b.append("</g>").toString();}
 static void draw(net.minecraft.client.model.geom.ModelPart p,org.joml.Matrix4f parent,StringBuilder b){var pose=p.getInitialPose();var m=new org.joml.Matrix4f(parent).translate(pose.x(),pose.y(),pose.z()).rotateZYX(pose.zRot(),pose.yRot(),pose.xRot()).scale(pose.xScale(),pose.yScale(),pose.zScale());var a=(io.github.r3neer.scalebrews.client.mixin.ModelPartAccess)(Object)p;for(var c:a.scalebrews$cubes()){double loX=Double.POSITIVE_INFINITY,loY=loX,hiX=Double.NEGATIVE_INFINITY,hiY=hiX;for(int i=0;i<8;i++){var v=m.transformPosition(new org.joml.Vector3f((i&1)==0?c.minX:c.maxX,(i&2)==0?c.minY:c.maxY,(i&4)==0?c.minZ:c.maxZ));double px=4*(v.x+.35*v.z),py=4*(v.y-.15*v.z);loX=Math.min(loX,px);hiX=Math.max(hiX,px);loY=Math.min(loY,py);hiY=Math.max(hiY,py);}b.append("<rect x=\"").append(loX).append("\" y=\"").append(loY).append("\" width=\"").append(hiX-loX).append("\" height=\"").append(hiY-loY).append("\" fill=\"#68a9db\" fill-opacity=\".3\" stroke=\"#194f70\"/>");}a.scalebrews$children().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->draw(e.getValue(),m,b));}
 static void check(boolean b,String s){if(!b)throw new AssertionError("S51 ADV "+s);}
 @SuppressWarnings({"rawtypes","unchecked"}) public void runTest(ClientGameTestContext context){
  var world=context.worldBuilder().create();
  try{context.runOnClient(mc->{try{
   var types=List.of(EntityTypes.COW,EntityTypes.PIG,EntityTypes.CHICKEN);
   var components=List.of(DataComponents.COW_VARIANT,DataComponents.PIG_VARIANT,DataComponents.CHICKEN_VARIANT);
   int combinations=0,failClosed=0; var snapshots=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1400\" height=\"560\" viewBox=\"0 0 1400 560\"><rect width=\"1400\" height=\"560\" fill=\"white\"/>");
   for(int family=0;family<types.size();family++){
    var entity=(AgeableMob)types.get(family).create(mc.level,EntitySpawnReason.COMMAND);entity.setId(9900+family);mc.level.addEntity(entity);
    EntityRenderer renderer=mc.getEntityRenderDispatcher().getRenderer(entity);
    var result=EnumAgeModelPreparation.inspect(renderer);check(result.admitted(),"native family rejected "+result.absence());var recipe=result.recipe();
    check(recipe.equals(EnumAgeModelPreparation.inspect(renderer).recipe()),"repeat recipe drift");
    try{recipe.rows().clear();throw new AssertionError("mutable rows");}catch(UnsupportedOperationException expected){}
    try{recipe.originalResources().clear();throw new AssertionError("mutable resources");}catch(UnsupportedOperationException expected){}
    Map map=null;Field mapField=null;for(Field f:renderer.getClass().getDeclaredFields())if(Map.class.isAssignableFrom(f.getType())){f.setAccessible(true);check(map==null,"ambiguous fixture map");map=(Map)f.get(renderer);mapField=f;}
    check(map!=null,"map absent");DataComponentType component=components.get(family);var initial=(Holder)entity.get(component);Object variant=initial.value();
    var fields=variant.getClass().getRecordComponents();Object[] values=new Object[fields.length];int modelIndex=-1;
    for(int i=0;i<fields.length;i++){values[i]=fields[i].getAccessor().invoke(variant);if(values[i] instanceof ModelAndTexture)modelIndex=i;}
    check(modelIndex>=0,"native record model missing");var old=(ModelAndTexture)values[modelIndex];
    var constructor=variant.getClass().getConstructor(Arrays.stream(fields).map(RecordComponent::getType).toArray(Class[]::new));
    var keys=new ArrayList(map.keySet()); keys.sort(java.util.Comparator.comparing(Object::toString));
    for(Object key:keys){values[modelIndex]=new ModelAndTexture(key,old.asset());var next=constructor.newInstance(values);entity.setComponent(component,Holder.direct(next));
     for(boolean baby:new boolean[]{false,true}){entity.setBaby(baby);var state=renderer.createRenderState(entity,0f);var pair=(AdultAndBabyModelPair)map.get(key);var expected=pair.getModel(baby);
      var store=new SubmitNodeStorage();var camera=new CameraRenderState();camera.pos=net.minecraft.world.phys.Vec3.ZERO;camera.orientation=new org.joml.Quaternionf();renderer.submit(state,new PoseStack(),store,camera);
      List<Object> submitted=new ArrayList<>();store.drainPhases(phase->phase.sortInto((node,flag)->{if(node instanceof ModelFeatureRenderer.Submit<?> m)submitted.add(m.model());}));
      check(submitted.stream().anyMatch(m->m==expected),"native queued model differs from original map "+key+"/"+baby);
      String name=((StringRepresentable)key).getSerializedName(),age=baby?"baby":"adult";var row=recipe.rows().stream().filter(r->r.modelValue().equals(name)&&r.age().equals(age)).findFirst().orElseThrow();
      check(name.equals(io.github.r3neer.scalebrews.collision.api.CollisionVariantSources.snapshot().get(recipe.sourceKey()).reader().value(entity)),"public source differs from native queued selector");check(age.equals(io.github.r3neer.scalebrews.collision.api.CollisionVariantSources.snapshot().get("age").reader().value(entity)),"public age differs from native extractor"); check(row.initialStructure().equals(ModelPartStructure.inspect(((EntityModel)expected).root())),"recipe initial fingerprint differs from original selected model");snapshots.append(snapshot(((EntityModel)expected).root(),family+" "+name+" "+age,combinations)); combinations++;
     }
    }
    var firstPair=(AdultAndBabyModelPair)map.get(keys.getFirst()); var root=((EntityModel)firstPair.adultModel()).root(); var pose=root.storePose(); boolean visibility=root.visible; root.x+=137;root.visible=false;var before=state(root);check(recipe.equals(EnumAgeModelPreparation.inspect(renderer).recipe()),"initial recipe depends on animated current pose");check(before.equals(state(root)),"preparation mutated shared current model");root.loadPose(pose);root.visible=visibility; var key=keys.getFirst();Object removed=map.remove(key);try{check(!EnumAgeModelPreparation.inspect(renderer).admitted(),"partial map admitted");failClosed++;}finally{map.put(key,removed);}
    map.put(key,null);try{check(!EnumAgeModelPreparation.inspect(renderer).admitted(),"null pair admitted");failClosed++;}finally{map.put(key,removed);}
    Map wrong=new LinkedHashMap(map);wrong.remove(key);wrong.put(Thread.State.NEW,removed);mapField.set(renderer,wrong);try{check(!EnumAgeModelPreparation.inspect(renderer).admitted(),"foreign enum owner admitted");failClosed++;}finally{mapField.set(renderer,map);} var savedPair=(AdultAndBabyModelPair)removed; map.put(key,new AdultAndBabyModelPair(null,savedPair.babyModel()));try{check(!EnumAgeModelPreparation.inspect(renderer).admitted(),"null adult model admitted");failClosed++;}finally{map.put(key,removed);} map.put(key,new AdultAndBabyModelPair(savedPair.adultModel(),savedPair.adultModel()));try{var alias=EnumAgeModelPreparation.inspect(renderer);check(alias.admitted()&&alias.recipe().rows().size()==recipe.rows().size()&&alias.recipe().uniqueModels()==recipe.uniqueModels()-1,"model identity cache fails shared slots");}finally{map.put(key,removed);} check(recipe.equals(EnumAgeModelPreparation.inspect(renderer).recipe()),"failed preparation poisoned later recipe");
    mc.level.removeEntity(entity.getId(),Entity.RemovalReason.DISCARDED);
   }
   check(combinations==14,"original domain cardinality "+combinations);check(!EnumAgeModelPreparation.inspect(null).admitted(),"null renderer admitted");
   snapshots.append("</svg>"); try{var out=java.nio.file.Path.of("s51-adversarial-snapshots");java.nio.file.Files.createDirectories(out);java.nio.file.Files.writeString(out.resolve("native-initial-models.svg"),snapshots);}catch(java.io.IOException e){throw new AssertionError(e);} System.out.println("S51_INDEPENDENT_NATIVE_PASS combinations="+combinations+" failClosed="+failClosed);
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}});}finally{world.close();}
 }
}