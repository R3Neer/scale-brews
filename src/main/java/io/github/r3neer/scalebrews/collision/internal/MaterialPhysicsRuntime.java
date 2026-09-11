package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.physics.AnatomySeparation;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * S07 live bridge from replay-fenced material handles to the generic causal dispatcher.
 * It owns scheduling/adaptation only; interval identity remains in S06 and the temporal
 * solver remains in collision.physics.
 */
public final class MaterialPhysicsRuntime {
    private MaterialPhysicsRuntime() {}
    private static final int MAX_BODIES=128,MAX_DEPTH=16,MAX_EVENTS=512;
    private static final int RESPONSE_EVENTS=32,QUERY_BUDGET=256,SEPARATION_BUDGET=128;
    private static final double MAX_ENVELOPE_SPAN=64,MAX_SEPARATION=4,OVERLAP_EPS=1e-12;
    private static final Map<ServerLevel,MaterialEventDispatcher<Entity>> DISPATCHERS=
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerLevel,Metrics> METRICS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Comparator<Entity> BODY_ORDER=Comparator.comparing(Entity::getUUID).thenComparingInt(Entity::getId);

    public record Metrics(long tick,long admitted,long candidates,long evaluations,long quarantined,long exhausted) {}
    private record Prepared(MaterialIntervalRuntime.Pending pending,MaterialEventDispatcher.MaterialInterval interval,
            GeometryProvider.MotionSnapshot motion) {}
    private record Plan(Entity body,Vec3 displacement,int evaluations) {}

    public static Metrics metrics(ServerLevel level) {
        return METRICS.getOrDefault(level,new Metrics(level.getGameTime(),0,0,0,0,0));
    }

    /**
     * Drain whatever S06 has published at this exact causal point. Root callers invoke this
     * synchronously after commitRoot; END_LEVEL_TICK invokes it after publish for the joint batch.
     */
    public static void drain(ServerLevel level) {
        if(MaterialIntervalRuntime.consumeSaturation(level))record(level,0,0,0,1,1);
        var pending=MaterialIntervalRuntime.poll(level);
        if(pending.isEmpty())return;
        var prepared=new ArrayList<Prepared>();
        for(var next:pending) {
            var value=prepare(next);
            if(value==null) {
                AnatomyMovement.invalidateSupport(next.support());
                record(level,1,0,0,1,0);
            } else prepared.add(value);
        }
        if(prepared.isEmpty())return;
        var dispatcher=DISPATCHERS.computeIfAbsent(level,ignored->new MaterialEventDispatcher<>(MAX_BODIES,MAX_DEPTH,MAX_EVENTS,Entity::getUUID));
        var backend=new Backend(level,prepared);
        for(int index=0;index<prepared.size();) {
            var first=prepared.get(index);
            if(first.pending().source()==MaterialIntervalRuntime.Source.ROOT) {
                dispatcher.ingest(first.pending().support(),MaterialEventDispatcher.Source.ROOT_MUTATION,first.interval(),backend);
                index++;
                continue;
            }
            long tick=first.pending().handle().after().authorityTick();
            var sameCadence=new ArrayList<Prepared>();
            while(index<prepared.size()) {
                var next=prepared.get(index);
                if(next.pending().source()!=MaterialIntervalRuntime.Source.JOINT
                        || next.pending().handle().after().authorityTick()!=tick)break;
                sameCadence.add(next);index++;
            }
            for(var cluster:jointClusters(sameCadence)) {
                var group=new ArrayList<MaterialEventDispatcher.JointInput<Entity>>(cluster.size());
                for(var next:cluster)group.add(new MaterialEventDispatcher.JointInput<>(next.pending().support(),next.interval()));
                dispatcher.ingestJointBatch(group,backend);
            }
        }
    }

    public static void clear(ServerLevel level) {DISPATCHERS.remove(level);METRICS.remove(level);}
    public static void clear(MinecraftServer server) {if(server!=null)for(var level:server.getAllLevels())clear(level);}

    private static Prepared prepare(MaterialIntervalRuntime.Pending pending) {
        var motion=AnatomyRuntime.interval(pending.support(),pending.handle()).orElse(null);
        if(motion==null)return null;
        var envelope=envelope(motion);
        if(envelope==null || envelope.getXsize()>MAX_ENVELOPE_SPAN || envelope.getYsize()>MAX_ENVELOPE_SPAN || envelope.getZsize()>MAX_ENVELOPE_SPAN)return null;
        try {return new Prepared(pending,new MaterialEventDispatcher.MaterialInterval(pending.handle(),envelope),motion);}
        catch(RuntimeException rejected){return null;}
    }

    /** Conservative full-trajectory envelope: every point stays within maxPointSpeed of its t=0 position. */
    static AABB envelope(GeometryProvider.MotionSnapshot snapshot) {
        AABB envelope=null;
        try {
            for(var motion:snapshot.pieces().values()) {
                var first=motion.at().apply(0);if(first==null)return null;
                double margin=motion.maxPointSpeed()+ConservativeSweep.SKIN;
                if(!Double.isFinite(margin))return null;
                var bounds=first.bounds().inflate(margin);
                envelope=envelope==null?bounds:union(envelope,bounds);
            }
        } catch(RuntimeException rejected){return null;}
        return finite(envelope)?envelope:null;
    }

    /**
     * One provider cadence is simultaneous, but disjoint swept regions cannot affect the same body.
     * Partition into deterministic connected components so far-away supports never create one giant query.
     */
    static List<List<Prepared>> jointClusters(List<Prepared> input) {
        if(input.isEmpty())return List.of();
        boolean[] used=new boolean[input.size()];var result=new ArrayList<List<Prepared>>();
        for(int seed=0;seed<input.size();seed++)if(!used[seed]) {
            used[seed]=true;var queue=new ArrayDeque<Integer>();queue.add(seed);var indices=new ArrayList<Integer>();
            while(!queue.isEmpty()) {
                int current=queue.removeFirst();indices.add(current);
                var envelope=input.get(current).interval().envelope();
                for(int next=0;next<input.size();next++)if(!used[next] && touches(envelope,input.get(next).interval().envelope())) {
                    used[next]=true;queue.addLast(next);
                }
            }
            indices.sort(Integer::compareTo);var cluster=new ArrayList<Prepared>(indices.size());
            for(int index:indices)cluster.add(input.get(index));result.add(List.copyOf(cluster));
        }
        return List.copyOf(result);
    }

    private static boolean touches(AABB a,AABB b) {
        double skin=ConservativeSweep.SKIN;
        return a.maxX+skin>=b.minX && b.maxX+skin>=a.minX
            && a.maxY+skin>=b.minY && b.maxY+skin>=a.minY
            && a.maxZ+skin>=b.minZ && b.maxZ+skin>=a.minZ;
    }

    private static final class Backend implements MaterialEventDispatcher.Backend<Entity> {
        private final ServerLevel level;
        private final Map<GeometryProvider.MotionIntervalHandle,Prepared> prepared=new IdentityHashMap<>();
        Backend(ServerLevel level,List<Prepared> values){this.level=level;for(var value:values)prepared.put(value.pending().handle(),value);}

        @Override public MaterialEventDispatcher.Candidates<Entity> capture(MaterialEventDispatcher.Event<Entity> event,int maximumBodies) {
            if(!(event.support() instanceof LivingEntity support))return new MaterialEventDispatcher.Candidates<>(List.of(),true);
            return capture(event.interval().envelope(),List.of(support),maximumBodies);
        }
        @Override public MaterialEventDispatcher.Candidates<Entity> captureJointBatch(List<MaterialEventDispatcher.Event<Entity>> events,int maximumBodies) {
            AABB envelope=null;var supports=new ArrayList<LivingEntity>();
            for(var event:events){
                if(!(event.support() instanceof LivingEntity support))return new MaterialEventDispatcher.Candidates<>(List.of(),true);
                envelope=envelope==null?event.interval().envelope():union(envelope,event.interval().envelope());supports.add(support);
            }
            if(!finite(envelope) || envelope.getXsize()>MAX_ENVELOPE_SPAN || envelope.getYsize()>MAX_ENVELOPE_SPAN || envelope.getZsize()>MAX_ENVELOPE_SPAN)
                return new MaterialEventDispatcher.Candidates<>(List.of(),true);
            return capture(envelope,supports,maximumBodies);
        }
        private MaterialEventDispatcher.Candidates<Entity> capture(AABB envelope,List<LivingEntity> supports,int maximumBodies) {
            var entities=new ArrayList<Entity>(Math.min(maximumBodies+1,256));
            level.getEntities(EntityTypeTest.forClass(Entity.class),envelope,body->{
                if(body.isRemoved() || supports.stream().anyMatch(s->s==body))return false;
                for(var support:supports)if(Platforms.eligible(body,support))return true;
                return false;
            },entities,maximumBodies+1);
            if(entities.size()>maximumBodies)return new MaterialEventDispatcher.Candidates<>(List.of(),true);
            entities.sort(BODY_ORDER);
            var result=new ArrayList<MaterialEventDispatcher.Candidate<Entity>>(entities.size());
            for(var body:entities)result.add(new MaterialEventDispatcher.Candidate<>(body,body.getBoundingBox()));
            record(level,0,result.size(),0,0,0);
            return new MaterialEventDispatcher.Candidates<>(result,false);
        }

        @Override public MaterialEventDispatcher.Resolution<Entity> resolve(MaterialEventDispatcher.Event<Entity> event,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var motion=motion(event.interval().handle());
            if(motion==null)return new MaterialEventDispatcher.Resolution<>(fail(List.of(event),MaterialEventDispatcher.Reason.BACKEND_FAILURE),List.of());
            var outcome=resolveAll(List.of(event),motion.pieces(),candidates);
            return new MaterialEventDispatcher.Resolution<>(outcome,List.of());
        }
        @Override public MaterialEventDispatcher.BatchResolution<Entity> resolveJointBatch(List<MaterialEventDispatcher.Event<Entity>> events,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var motions=new IdentityHashMap<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot>();
            for(var event:events) {
                var motion=motion(event.interval().handle());
                if(motion==null)return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_FAILURE);
                motions.put(event.interval().handle(),motion);
            }
            var plans=new ArrayList<Plan>();int evaluations=0;
            for(var candidate:candidates) {
                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return batchFailure(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var pieces=new TreeMap<String,ConservativeSweep.Motion>();
                for(var event:events) {
                    if(!(event.support() instanceof LivingEntity support) || !Platforms.eligible(body,support))continue;
                    var motion=motions.get(event.interval().handle());
                    String prefix=support.getUUID()+"/"+event.interval().handle().materialSerial()+"/";
                    for(var entry:motion.pieces().entrySet())if(pieces.put(prefix+entry.getKey(),entry.getValue())!=null)
                        return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_FAILURE);
                }
                if(pieces.isEmpty()) {plans.add(new Plan(body,Vec3.ZERO,0));continue;}
                var plan=plan(body,candidate.bounds(),pieces);
                if(plan==null)return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                plans.add(plan);evaluations+=plan.evaluations();
            }
            if(worsensBodyOverlap(plans,candidates))return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
            apply(plans);record(level,events.size(),0,evaluations,0,0);
            var outcomes=new ArrayList<MaterialEventDispatcher.Outcome>(events.size());
            for(int i=0;i<events.size();i++)outcomes.add(MaterialEventDispatcher.Outcome.applied(1));
            return new MaterialEventDispatcher.BatchResolution<>(outcomes,List.of());
        }
        @Override public void quarantine(MaterialEventDispatcher.Event<Entity> event,MaterialEventDispatcher.Reason reason) {
            if(event.support() instanceof LivingEntity support)AnatomyMovement.invalidateSupport(support);
            record(level,0,0,0,1,reason==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED?1:0);
        }
        private GeometryProvider.MotionSnapshot motion(GeometryProvider.MotionIntervalHandle handle) {
            var value=prepared.get(handle);return value==null?null:value.motion();
        }
        private MaterialEventDispatcher.Outcome resolveAll(List<MaterialEventDispatcher.Event<Entity>> events,
                Map<String,ConservativeSweep.Motion> pieces,List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var plans=new ArrayList<Plan>();int evaluations=0;
            for(var candidate:candidates) {
                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return fail(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var plan=plan(body,candidate.bounds(),pieces);
                if(plan==null)return fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                plans.add(plan);evaluations+=plan.evaluations();
            }
            if(worsensBodyOverlap(plans,candidates))return fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
            apply(plans);record(level,events.size(),0,evaluations,0,0);
            return MaterialEventDispatcher.Outcome.applied(1);
        }
        private Plan plan(Entity body,AABB captured,Map<String,ConservativeSweep.Motion> pieces) {
            if(pieces.isEmpty())return new Plan(body,Vec3.ZERO,0);
            var clip=clip(body);TemporalResponse.Result response;
            Entity previous=PlatformPhysics.enter(body);
            try {response=TemporalResponse.resolve(captured,Vec3.ZERO,pieces,RESPONSE_EVENTS,QUERY_BUDGET,clip);}
            catch(RuntimeException rejected){return null;}
            finally {PlatformPhysics.exit(previous);}
            int evaluations=response.evaluations();
            if(response.status()==TemporalResponse.Status.ITERATION_LIMIT)return null;
            if(response.status()==TemporalResponse.Status.INITIAL_OVERLAP) {
                var start=pieces.values().stream().map(m->m.at().apply(0)).toList();
                AnatomySeparation.Result separation;
                previous=PlatformPhysics.enter(body);
                try {separation=AnatomySeparation.resolve(captured,start,MAX_SEPARATION,SEPARATION_BUDGET,clip);}
                catch(RuntimeException rejected){return null;}
                finally {PlatformPhysics.exit(previous);}
                evaluations+=separation.candidates();if(!separation.separated())return null;
                previous=PlatformPhysics.enter(body);
                try {response=TemporalResponse.resolve(captured.move(separation.displacement()),Vec3.ZERO,pieces,RESPONSE_EVENTS,QUERY_BUDGET,clip);}
                catch(RuntimeException rejected){return null;}
                finally {PlatformPhysics.exit(previous);}
                evaluations+=response.evaluations();if(response.status()!=TemporalResponse.Status.COMPLETE)return null;
                return new Plan(body,separation.displacement().add(response.displacement()),evaluations);
            }
            return new Plan(body,response.displacement(),evaluations);
        }
        private java.util.function.BiFunction<AABB,Vec3,Vec3> clip(Entity body) {
            return (box,delta)->Entity.collideBoundingBox(body,delta,box,level,level.getEntityCollisions(body,box.expandTowards(delta)));
        }
        private MaterialEventDispatcher.Outcome fail(List<MaterialEventDispatcher.Event<Entity>> events,MaterialEventDispatcher.Reason reason) {
            for(var event:events)if(event.support() instanceof LivingEntity support)AnatomyMovement.invalidateSupport(support);
            record(level,0,0,0,events.size(),reason==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED?1:0);
            return MaterialEventDispatcher.Outcome.quarantined(reason);
        }
        private MaterialEventDispatcher.BatchResolution<Entity> batchFailure(List<MaterialEventDispatcher.Event<Entity>> events,MaterialEventDispatcher.Reason reason) {
            var outcome=fail(events,reason);var outcomes=new ArrayList<MaterialEventDispatcher.Outcome>(events.size());
            for(int i=0;i<events.size();i++)outcomes.add(outcome);
            return new MaterialEventDispatcher.BatchResolution<>(outcomes,List.of());
        }
        private void apply(List<Plan> plans) {
            for(var plan:plans)if(plan.displacement().lengthSqr()>1e-20)plan.body().setPos(plan.body().position().add(plan.displacement()));
        }
        private boolean worsensBodyOverlap(List<Plan> plans,List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            for(int i=0;i<plans.size();i++)for(int j=i+1;j<plans.size();j++) {
                var beforeA=candidates.get(i).bounds();var beforeB=candidates.get(j).bounds();
                var afterA=beforeA.move(plans.get(i).displacement());var afterB=beforeB.move(plans.get(j).displacement());
                if(overlapVolume(afterA,afterB)>overlapVolume(beforeA,beforeB)+OVERLAP_EPS)return true;
            }
            return false;
        }
    }

    private static double overlapVolume(AABB a,AABB b) {
        double x=Math.max(0,Math.min(a.maxX,b.maxX)-Math.max(a.minX,b.minX));
        double y=Math.max(0,Math.min(a.maxY,b.maxY)-Math.max(a.minY,b.minY));
        double z=Math.max(0,Math.min(a.maxZ,b.maxZ)-Math.max(a.minZ,b.minZ));
        return x*y*z;
    }
    private static void record(ServerLevel level,long admitted,long candidates,long evaluations,long quarantined,long exhausted) {
        synchronized(METRICS) {
            var old=metrics(level);if(old.tick()!=level.getGameTime())old=new Metrics(level.getGameTime(),0,0,0,0,0);
            METRICS.put(level,new Metrics(old.tick(),old.admitted()+admitted,old.candidates()+candidates,old.evaluations()+evaluations,
                old.quarantined()+quarantined,old.exhausted()+exhausted));
        }
    }
    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }
    private static boolean finite(AABB box) {
        return box!=null && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
            && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
            && box.minX<=box.maxX && box.minY<=box.maxY && box.minZ<=box.maxZ;
    }
}
