package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Live-wire meso benchmark and fixed-fraction geometry capture, not the G9 tick benchmark. */
public final class S29AdversarialPresentationBatchProof implements FabricClientGameTest {
    private static volatile long blackhole;
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var ids=new ArrayList<Integer>();var serials=new HashMap<Integer,Long>();var origin=new AtomicReference<Vec3>();
        var frames=new ArrayList<Object>();var performance=new ArrayList<Object>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(s->{for(int i=0;i<64;i++){
                var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();
                cow.setPos(2+(i%8)*2.5,3,2+(i/8)*2.5);cow.setYRot(0);cow.yBodyRot=0;cow.yHeadRot=0;
                s.overworld().addFreshEntity(cow);ids.add(cow.getId());}
                AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("s29_adversary:batch",profile));});
            context.waitFor(c->c.level!=null && ids.stream().allMatch(id->c.level.getEntity(id) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent() && AnatomyMovement.queryFrame(cow).isPresent()),260);
            world.getServer().runOnServer(s->{s.tickRateManager().setFrozen(true);for(int id:ids){var cow=(Cow)s.overworld().getEntity(id);
                cow.move(MoverType.SELF,new Vec3(.5,0,0));serials.put(id,AnatomyMovement.publishedFrame(cow).orElseThrow().endpoint().frameSerial());}
                origin.set(((Cow)s.overworld().getEntity(ids.getFirst())).position().subtract(.5,0,0));});
            context.waitTicks(3);
            context.runOnClient(c->{
                var cows=ids.stream().map(id->(Cow)c.level.getEntity(id)).toList();var material=new HashMap<Integer,Long>();
                for(var cow:cows){var certificate=inbox().certificates(cow.getUUID(),tick()).stream()
                    .filter(p->p.afterFrameSerial()==serials.get(cow.getId())).findFirst().orElseThrow();material.put(cow.getId(),certificate.materialSerial());}
                capture(cows.getFirst(),material.get(ids.getFirst()),"ROOT",origin.get(),frames);
                for(int supports:new int[]{1,8,64})for(int consumers:new int[]{1,16})
                    performance.add(measure(cows.subList(0,supports),material,consumers,false));
                for(int consumers:new int[]{1,16})performance.add(measure(cows,material,consumers,true));
            });
            world.getServer().runOnServer(s->{s.tickRateManager().setFrozen(false);((Cow)s.overworld().getEntity(ids.getFirst())).setXRot(35);});
            context.waitFor(c->{var cow=(Cow)c.level.getEntity(ids.getFirst());return inbox().certificates(cow.getUUID(),tick()).stream().anyMatch(p->{
                var frame=AnatomyClientNetworking.presentationInterval(cow,p.materialSerial(),.5).orElse(null);
                return frame!=null && Math.abs(frame.after().sample().inputs().headPitch()-frame.before().sample().inputs().headPitch())>20;});},120);
            world.getServer().runOnServer(s->s.tickRateManager().setFrozen(true));
            context.runOnClient(c->{var cow=(Cow)c.level.getEntity(ids.getFirst());var certificate=inbox().certificates(cow.getUUID(),tick()).stream().filter(p->{
                var frame=AnatomyClientNetworking.presentationInterval(cow,p.materialSerial(),.5).orElse(null);
                return frame!=null && Math.abs(frame.after().sample().inputs().headPitch()-frame.before().sample().inputs().headPitch())>20;}).findFirst().orElseThrow();
                capture(cow,certificate.materialSerial(),"JOINT",origin.get(),frames);
                var data=new LinkedHashMap<String,Object>();data.put("java",System.getProperty("java.version"));data.put("os",System.getProperty("os.name"));
                data.put("processors",Runtime.getRuntime().availableProcessors());data.put("minecraft","26.2");data.put("warmupBatches",200);data.put("measuredBatches",1000);
                data.put("method","paired baseline CURRENT_ENDPOINT versus CURRENT_ENDPOINT + certified interval, one actual client, same fraction per consumer batch, held server clock; not NFR-014");
                data.put("measurements",performance);data.put("geometryFrames",frames);write("s29-presentation-batch.json",data);
            });
        } finally {world.getServer().runOnServer(s->{s.tickRateManager().setFrozen(false);AnatomyRuntime.stop(s);});context.waitTicks(2);world.close();}
    }
    private static Object measure(List<Cow> cows,Map<Integer,Long> material,int consumers,boolean changing){
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();
        for(int i=0;i<200;i++){double fraction=changing?.25+(i%3)*.25:.5;batch(cows,material,consumers,false,fraction);batch(cows,material,consumers,true,fraction);}
        var baseline=new long[1000];var candidate=new long[1000];long baseBytes=0,candidateBytes=0;
        long gcBefore=gc();long jointsBefore=evaluations(cows);long preparationsBefore=preparations();
        for(int i=0;i<1000;i++){double fraction=changing?.25+(i%3)*.25:.5;
            if((i&1)==0){long bytes=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();batch(cows,material,consumers,false,fraction);baseline[i]=System.nanoTime()-start;baseBytes+=bean.getThreadAllocatedBytes(thread)-bytes;
                bytes=bean.getThreadAllocatedBytes(thread);start=System.nanoTime();batch(cows,material,consumers,true,fraction);candidate[i]=System.nanoTime()-start;candidateBytes+=bean.getThreadAllocatedBytes(thread)-bytes;}
            else {long bytes=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();batch(cows,material,consumers,true,fraction);candidate[i]=System.nanoTime()-start;candidateBytes+=bean.getThreadAllocatedBytes(thread)-bytes;
                bytes=bean.getThreadAllocatedBytes(thread);start=System.nanoTime();batch(cows,material,consumers,false,fraction);baseline[i]=System.nanoTime()-start;baseBytes+=bean.getThreadAllocatedBytes(thread)-bytes;}
        }
        long duplicate=evaluations(cows)-jointsBefore;if(duplicate!=0)throw new AssertionError("NFR-009: repeated consumer batch duplicated joints "+duplicate);
        var data=new LinkedHashMap<String,Object>();data.put("changingFraction",changing);data.put("supports",cows.size());data.put("consumers",consumers);data.put("baselineP95us",percentile(baseline,.95)/1000d);data.put("candidateP95us",percentile(candidate,.95)/1000d);
        data.put("baselineP99us",percentile(baseline,.99)/1000d);data.put("candidateP99us",percentile(candidate,.99)/1000d);data.put("baselineAllocatedBytesPerBatch",baseBytes/1000d);data.put("candidateAllocatedBytesPerBatch",candidateBytes/1000d);
        data.put("gcCycles",gc()-gcBefore);data.put("duplicateJointEvaluations",duplicate);data.put("compiledMotionPreparations",preparationsBefore<0?-1:preparations()-preparationsBefore);System.out.println("S29 PRESENTATION BATCH "+data);
        if(changing && preparationsBefore>=0 && preparations()!=preparationsBefore)throw new AssertionError("HOT_PRESENTATION: identical certified endpoints recompile invariant motion preparation per fraction: "+(preparations()-preparationsBefore));return data;
    }
    private static void batch(List<Cow> cows,Map<Integer,Long> material,int consumers,boolean interval,double fraction){for(int i=0;i<consumers;i++)for(var cow:cows){
        var current=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();blackhole+=current.after().frameSerial();
        if(interval){var frame=AnatomyClientNetworking.presentationInterval(cow,material.get(cow.getId()),fraction).orElseThrow();blackhole+=frame.after().frameSerial();}
    }}
    @SuppressWarnings("unchecked") private static long evaluations(List<Cow> cows){var map=(Map<UUID,ModelGeometryProvider>)S29AdversarialContactLifecycleProof.field("evaluators");long sum=0;for(var cow:cows)sum+=map.get(cow.getUUID()).jointEvaluations();return sum;}
    private static long preparations(){try{return (long)HierarchyMotion.class.getMethod("s29AdversarialMotionPreparations").invoke(null);}catch(NoSuchMethodException absent){return -1;}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static long gc(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
    private static long percentile(long[] samples,double fraction){var copy=samples.clone();Arrays.sort(copy);return copy[(int)Math.ceil(copy.length*fraction)-1];}
    private static void capture(Cow cow,long material,String kind,Vec3 origin,List<Object> output){for(double fraction:new double[]{0,.25,.5,.75,1}){
        var frame=AnatomyClientNetworking.presentationInterval(cow,material,fraction).orElseThrow();var pieces=new TreeMap<String,Object>();
        frame.evaluated().pieces().forEach((name,piece)->pieces.put(name,piece.vertices().stream().map(v->List.of(v.x-origin.x,v.y-origin.y,v.z-origin.z)).toList()));
        var data=new LinkedHashMap<String,Object>();data.put("kind",kind);data.put("fraction",fraction);data.put("materialSerial",material);data.put("beforeSerial",frame.before().frameSerial());data.put("afterSerial",frame.after().frameSerial());
        data.put("beforeRoot",List.of(frame.before().sample().origin().x-origin.x,frame.before().sample().origin().y-origin.y,frame.before().sample().origin().z-origin.z));
        data.put("afterRoot",List.of(frame.after().sample().origin().x-origin.x,frame.after().sample().origin().y-origin.y,frame.after().sample().origin().z-origin.z));data.put("pieces",pieces);output.add(data);
        if(kind.equals("JOINT") && !frame.before().rootTransform().equals(frame.after().rootTransform()))throw new AssertionError("JOINT visual changed root");
    }}
    private static AnatomyCertifiedIntervalInbox inbox(){return (AnatomyCertifiedIntervalInbox)S29AdversarialContactLifecycleProof.field("certifiedIntervals");}
    private static long tick(){return (long)S29AdversarialContactLifecycleProof.field("clientTick");}
    private static void write(String name,Object data){try{java.nio.file.Files.writeString(java.nio.file.Path.of(name),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data));}catch(java.io.IOException e){throw new AssertionError(e);}}
}
