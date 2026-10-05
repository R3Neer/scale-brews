package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.data.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class S50AdversarialHolderLifecycleTests {
    static void check(boolean value,String message){if(!value)throw new AssertionError("S50 ADV "+message);}
    @GameTest public void nativeComponentRetiresExactMaterial(GameTestHelper h){
        var level=h.getLevel();var server=level.getServer();var fox=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        check(fox!=null,"original Fox factory");fox.setNoAi(true);fox.setNoGravity(true);fox.setPos(h.absoluteVec(new Vec3(2,3,2)));Object original=fox.get(DataComponents.COW_VARIANT).value();var values=net.minecraft.world.entity.animal.cow.CowVariant.ModelType.values();check(values.length>=2,"native positive variants");fox.setComponent(DataComponents.COW_VARIANT,net.minecraft.core.Holder.direct((net.minecraft.world.entity.animal.cow.CowVariant)S50AdversarialHolderPreparationTests.cloneModel(original,values[0],S50AdversarialHolderPreparationTests.asset("one"))));level.addFreshEntity(fox);
        String key=S49AdversarialEnumPreparationTests.expectedKey(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DataComponents.COW_VARIANT))+".model";var a=row(key,values[0].getSerializedName());var b=row(key,values[1].getSerializedName());
        AnatomyRuntime.start(server);AnatomyRuntime.reload(server,S48AdversarialVariantFixtures.resources(server.getResourceManager(),List.of(a,b)));AnatomyRuntime.prepare(level);AnatomyMovement.tickGeometry(level);
        var first=AnatomyMovement.queryFrame(fox).orElseThrow();long generation=first.identity().bindingGeneration();var token=first.identity().bindingIdentity();check(AnatomyBindingState.binding(fox).variant().equals(a.variant()),"native first row selected");
        var provider=AnatomyBindingState.provider(fox);for(int i=0;i<1000;i++)check(AnatomyMovement.queryFrame(fox).orElseThrow().identity().bindingGeneration()==generation&&AnatomyBindingState.provider(fox)==provider,"stable native inputs no rebind");
        var actor=new ItemEntity(level,fox.getX(),fox.getY()+1.05,fox.getZ(),new ItemStack(Items.STONE));actor.setNoGravity(true);level.addFreshEntity(actor);actor.move(MoverType.SELF,new Vec3(0,-.2,0));AnatomyMovement.afterMove(actor);check(AnatomyMovement.contact(actor)!=null&&AnatomyMovement.contact(actor).support()==fox,"real positive contact");var held=actor.position();fox.setComponent(DataComponents.COW_VARIANT,net.minecraft.core.Holder.direct((net.minecraft.world.entity.animal.cow.CowVariant)S50AdversarialHolderPreparationTests.cloneModel(original,values[0],S50AdversarialHolderPreparationTests.asset("two"))));check(AnatomyMovement.contact(actor)!=null&&AnatomyMovement.queryFrame(fox).orElseThrow().identity().bindingGeneration()==generation&&AnatomyBindingState.provider(fox)==provider,"texture-only Holder replacement preserves material/contact");
        fox.setComponent(DataComponents.COW_VARIANT,net.minecraft.core.Holder.direct((net.minecraft.world.entity.animal.cow.CowVariant)S50AdversarialHolderPreparationTests.cloneModel(original,values[1],S50AdversarialHolderPreparationTests.asset("one"))));check(fox.get(DataComponents.COW_VARIANT).value().modelAndTexture().model()==values[1],"original native component changed");check(AnatomyMovement.contact(actor)==null&&actor.position().equals(held),"synchronous component mutation retires physical contact without movement");check(AnatomyMovement.queryFrame(fox).isEmpty(),"no query creates next variant endpoint");AnatomyMovement.carry(actor);check(actor.position().equals(held),"retired variant cannot carry");
        h.runAfterDelay(2,()->{try{AnatomyRuntime.prepare(level);AnatomyMovement.tickGeometry(level);var next=AnatomyMovement.queryFrame(fox).orElseThrow();check(next.identity().bindingGeneration()>generation&&!next.identity().bindingIdentity().equals(token),"new exact native row generation/token");check(AnatomyBindingState.binding(fox).variant().equals(b.variant()),"native next row physical identity");long second=next.identity().bindingGeneration();fox.setComponent(DataComponents.COW_VARIANT,net.minecraft.core.Holder.direct((net.minecraft.world.entity.animal.cow.CowVariant)S50AdversarialHolderPreparationTests.cloneModel(original,values[0],S50AdversarialHolderPreparationTests.asset("one"))));check(AnatomyMovement.queryFrame(fox).isEmpty()&&AnatomyBindingState.descriptor(fox).bindingGeneration()>second,"native round trip cannot revive A endpoint");System.out.println("S50 ADV LIVE PASS nativeHolder A-texture-B-A exact-token contact-retired stable1000");h.succeed();}finally{actor.discard();fox.discard();AnatomyRuntime.stop(server);}});
    }
    static CollisionBinding row(String key,String value){return new CollisionBinding(1,net.minecraft.resources.Identifier.parse("minecraft:cow"),Map.of(key,value),new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,S48AdversarialVariantFixtures.MODEL,Map.of(),AnatomyFilter.DEFAULT),new CollisionBinding.Pose(S48AdversarialVariantFixtures.POSE,Map.of(),Set.of()),S48AdversarialVariantFixtures.ROOT,CollisionPolicy.Patch.EMPTY,Set.of());}
}
