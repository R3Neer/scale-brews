package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.preparation.*;
import io.github.r3neer.scalebrews.collision.catalog.*;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Independent registry class census and original mutable-state observation. */
public final class S45AdversarialCensusProof implements FabricClientGameTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private record State(float x,float y,float z,float xr,float yr,float zr,float xs,float ys,float zs,
                         boolean visible,boolean skip,PartPose initial) {
        static State of(ModelPart p){return new State(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale,p.visible,p.skipDraw,p.getInitialPose());}
    }
    private static Set<Identifier> population(net.minecraft.server.MinecraftServer server){
        var ids=new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
        var nullFactories=new TreeSet<String>();
        for(var type:BuiltInRegistries.ENTITY_TYPE) {
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if(!id.getNamespace().equals("minecraft"))continue;
            var entity=type==net.minecraft.world.entity.EntityTypes.PLAYER
                ?server.getPlayerList().getPlayers().getFirst()
                :type.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if(entity instanceof LivingEntity)ids.add(id);
            else if(entity==null)nullFactories.add(id.toString());
        }
        System.out.println("S45_FACTORY_POPULATION living="+ids.size()+" nullFactories="+nullFactories);
        return ids;
    }
    private static void models(Object value,Set<Object> seen,Set<Model<?>> found) throws IllegalAccessException {
        if(value==null || !seen.add(value))return;
        if(value instanceof Model<?> model){found.add(model);return;}
        if(value instanceof Map<?,?> map){for(var v:map.values())models(v,seen,found);return;}
        if(value instanceof Iterable<?> items){for(var v:items)models(v,seen,found);return;}
        if(value instanceof Object[] items){for(var v:items)models(v,seen,found);return;}
        String name=value.getClass().getName();
        if(!name.startsWith("net.minecraft.client.renderer.entity.") && !name.equals("net.minecraft.client.renderer.entity.EntityRenderDispatcher") && !(value instanceof net.minecraft.client.renderer.entity.layers.RenderLayer<?,?>) && !value.getClass().isRecord())return;
        for(Class<?> c=value.getClass();c!=null && c!=Object.class;c=c.getSuperclass())for(var f:c.getDeclaredFields()) {
            if(Modifier.isStatic(f.getModifiers())||f.isSynthetic())continue;
            if(f.trySetAccessible())models(f.get(value),seen,found);
        }
    }
    private static IdentityHashMap<ModelPart,State> snapshot(Set<Model<?>> models){
        var states=new IdentityHashMap<ModelPart,State>();
        for(var m:models)for(var p:m.root().getAllParts())states.put(p,State.of(p));
        return states;
    }
    private static void structureChecks(){
        var root=new ModelPart(new ArrayList<>(),new LinkedHashMap<>());
        root.setInitialPose(PartPose.ZERO);root.loadPose(PartPose.ZERO);
        String initial=ModelPartStructure.inspect(root).digest();
        root.x=12;root.yRot=.7f;root.visible=false;root.skipDraw=true;
        var state=State.of(root);
        check(initial.equals(ModelPartStructure.inspect(root).digest()),"Fingerprint borrowed current renderer pose/visibility");
        check(state.equals(State.of(root)),"Inspection mutated original part");
        root.setInitialPose(PartPose.offset(1,2,3));
        check(!initial.equals(ModelPartStructure.inspect(root).digest()),"Changed initial transform retained fingerprint");
        root.setInitialPose(PartPose.offset(Float.NaN,0,0));
        try{ModelPartStructure.inspect(root);throw new AssertionError("NaN initial pose accepted");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("non_finite"),"Wrong invalid-pose classification");}
        var children=new LinkedHashMap<String,ModelPart>();root=new ModelPart(List.of(),children);root.setInitialPose(PartPose.ZERO);children.put("cycle",root);
        try{ModelPartStructure.inspect(root);throw new AssertionError("Cycle accepted");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("cycle"),"Wrong cycle classification");}
        for(int count:new int[]{512,513}){
            children=new LinkedHashMap<>();for(int i=1;i<count;i++){var child=new ModelPart(List.of(),Map.of());child.setInitialPose(PartPose.ZERO);children.put("p"+i,child);}
            root=new ModelPart(List.of(),children);root.setInitialPose(PartPose.ZERO);
            if(count==512)check(ModelPartStructure.inspect(root).parts()==512,"Exact part bound rejected");
            else try{ModelPartStructure.inspect(root);throw new AssertionError("Part overflow accepted");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("part_limit"),"Wrong overflow classification");}
        }
        System.out.println("S45_PRIVATE_STRUCTURE_PASS exactParts=512 overflow=513 currentStateIndependent=true");
    }
    @Override public void runTest(ClientGameTestContext context){
        var world=context.worldBuilder().create();
        try{
            var serverIds=new java.util.concurrent.atomic.AtomicReference<Set<Identifier>>();
            var bindings=new java.util.concurrent.atomic.AtomicReference<CollisionBindingCatalog>();
            world.getServer().runOnServer(s->{serverIds.set(population(s));bindings.set(CollisionBindingCatalog.load(s.getResourceManager()));});
            context.runOnClient(c->{
                try{
                    var expected=serverIds.get();check(!expected.isEmpty(),"Actual factory population was empty");
                    var originals=Collections.newSetFromMap(new IdentityHashMap<Model<?>,Boolean>());
                    models(c.getEntityRenderDispatcher(),Collections.newSetFromMap(new IdentityHashMap<>()),originals);
                    check(!originals.isEmpty(),"Independent model observer found none");
                    var before=snapshot(originals);
                    var target=new CollisionCoverageDiscovery.Target(Identifier.parse("s45_adversary:minecraft"),"26.2",Set.of("minecraft"),Map.of("minecraft","26.2","tool","independent-v1"));
                    var catalog=bindings.get();
                    var a=ModelFamilyCensus.inspect(c,target,catalog,Map.of());
                    var b=ModelFamilyCensus.inspect(c,target,catalog,Map.of());
                    check(a.cost().uniqueStructures()<=originals.size(),"Independent original observer did not cover all inspected models");
                    var actual=new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
                    for(var row:a.rows())check(actual.add(row.entity()),"Duplicate census row "+row.entity());
                    check(actual.equals(expected),"Registry class denominator mismatch expected="+expected+" actual="+actual);
                    check(a.digest().equals(b.digest()) && a.canonicalText().equals(b.canonicalText()),"Original census nondeterministic");
                    for(var entry:before.entrySet())check(entry.getValue().equals(State.of(entry.getKey())),"Original model mutated by census");
                    var changed=new CollisionCoverageDiscovery.Target(target.id(),"26.2",Set.of("minecraft"),Map.of("minecraft","26.2","tool","independent-v2"));
                    check(!a.digest().equals(ModelFamilyCensus.inspect(c,changed,catalog,Map.of()).digest()),"Changed target input did not retire identity");
                    check(a.cost().uniqueStructures()<=ModelFamilyCensus.MAX_STRUCTURES,"Structure work unbounded");
                    check(a.cost().modelSlots()<=a.rows().size()*ModelFamilyCensus.MAX_SLOTS,"Slot work unbounded");
                    check(a.coverage().coverage().rows().stream().noneMatch(r->r.status().name().equals("FULL")),"Census fabricated FULL from model inventory");
                    structureChecks();
                    System.out.println("S45_PRIVATE_CENSUS_PASS population="+actual.size()+" observedModels="+originals.size()+" observedParts="+before.size()+" cost="+a.cost()+" digest="+a.digest());
                }catch(ReflectiveOperationException e){throw new AssertionError("Independent observation failed",e);}
            });
        }finally{world.close();}
    }
}
