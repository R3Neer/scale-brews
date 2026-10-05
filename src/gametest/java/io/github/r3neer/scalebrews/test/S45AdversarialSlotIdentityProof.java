package io.github.r3neer.scalebrews.test;
import io.github.r3neer.scalebrews.client.collision.preparation.ModelFamilyCensus;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.cow.CowModel;

/** Slot identity fault probe; real original ModelPart model, isolated walker owner. */
public final class S45AdversarialSlotIdentityProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(c->{try{
            Model<?> model=new CowModel(CowModel.createBodyLayer().bakeRoot());
            var passClass=Class.forName(ModelFamilyCensus.class.getName()+"$Pass");
            var passCtor=passClass.getDeclaredConstructor();passCtor.setAccessible(true);
            var ownerClass=Class.forName(ModelFamilyCensus.class.getName()+"$RendererInspection");
            var ctor=ownerClass.getDeclaredConstructor(passClass);ctor.setAccessible(true);
            var owner=ctor.newInstance(passCtor.newInstance());
            var walk=ownerClass.getDeclaredMethod("walk",Object.class,String.class,int.class);walk.setAccessible(true);
            var source=new LinkedHashMap<String,Object>();
            source.put("a",Map.of("b",model));
            source.put("a][java.lang.String:b",model);
            walk.invoke(owner,source,"probe",0);
            var field=ownerClass.getDeclaredField("slots");field.setAccessible(true);
            @SuppressWarnings("unchecked") var slots=(List<ModelFamilyCensus.Slot>)field.get(owner);
            if(slots.size()!=2)throw new AssertionError("Fault probe lost a distinct semantic slot: "+slots);
            System.out.println("S45_SLOT_IDENTITIES "+slots.stream().map(ModelFamilyCensus.Slot::path).toList());
            if(slots.get(0).path().equals(slots.get(1).path()))throw new AssertionError("Distinct nested and flat model slots alias the same census path: "+slots.get(0).path());
            System.out.println("S45_SLOT_IDENTITY_PASS");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}});
    }
}
