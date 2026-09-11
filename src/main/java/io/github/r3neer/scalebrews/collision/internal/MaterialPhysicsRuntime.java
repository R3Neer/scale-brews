package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.physics.AnatomySeparation;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
    private static final double MAX_ENVELOPE_SPAN=64,MAX_SEPARATION=4;
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
            var group=new ArrayList<MaterialEventDispatcher.JointInput<Entity>>();
            while(index<prepared.size()) {
                var next=prepared.get(index);
                if(next.pending().source()!=MaterialIntervalRuntime.Source.JOINT
                        || next.pending().handle().after().authorityTick()!=tick)break;
                group.add(new MaterialEventDispatcher.JointInput<>(next.pending().support(),next.interval()));index++;
            }
            dispatcher.ingestJointBatch(group,backend);
        }
    }

    public static void clear(ServerLevel level) {DISPATCHERS.remove(level);METRICS.remove(level);}

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
            if(motion==null)return new MaterialEventDispatcher.Resolution<>(MaterialEventDispatcher.Outcome.quarantined(MaterialEventDispatcher.Reason.BACKEND_FAILURE),List.of());
            var outcome=resolveAll(motion,candidates);
            return new MaterialEventDispatcher.Resolution<>(outcome,List.of());
        }
        @Override public MaterialEventDispatcher.BatchResolution<Entity> resolveJointBatch(List<MaterialEventDispatcher.Event<Entity>> events,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            // Simultaneous joint supports share one capture but keep independent certified trajectories.
            // Compute/apply them in the canonical event order supplied by S06 publish.
            var outcomes=new ArrayList<MaterialEventDispatcher.Outcome>();
            for(var event:events) {
                var motion=motion(event.interval().handle());
                outcomes.add(motion==null?MaterialEventDispatcher.Outcome.quarantined(MaterialEventDispatcher.Reason.BACKEND_FAILURE)
                    :resolveAll(motion,candidates));
            }
            return new MaterialEventDispatcher.BatchResolution<>(outcomes,List.of());
        }
        @Override public void quarantine(MaterialEventDispatcher.Event<Entity> event,MaterialEventDispatcher.Reason reason) {
            if(event.support() instanceof LivingEntity support)AnatomyMovement.invalidateSupport(support);
            record(level,0,0,0,1,reason==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED?1:0);
        }
        private GeometryProvider.MotionSnapshot motion(GeometryProvider.MotionIntervalHandle handle) {
            var value=prepared.get(handle);return value==null?null:value.motion();
        }
        private MaterialEventDispatcher.Outcome resolveAll(GeometryProvider.MotionSnapshot motion,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var plans=new ArrayList<Plan>();int evaluations=0;
            for(var candidate:candidates) {
                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return MaterialEventDispatcher.Outcome.quarantined(MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var plan=plan(body,candidate.bounds(),motion);
                if(plan==null)return MaterialEventDispatcher.Outcome.quarantined(MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                plans.add(plan);evaluations+=plan.evaluations();
            }
            if(finalOverlap(plans,candidates))return MaterialEventDispatcher.Outcome.quarantined(MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
            for(var plan:plans)if(plan.displacement().lengthSqr()>1e-20)plan.body().setPos(plan.body().position().add(plan.displacement()));
            record(level,1,0,evaluations,0,0);
            return MaterialEventDispatcher.Outcome.applied(1);
        }
        private Plan plan(Entity body,AABB captured,GeometryProvider.MotionSnapshot motion) {
            var clip=clip(body);TemporalResponse.Result response;
            Entity previous=PlatformPhysics.enter(body);
            try {response=TemporalResponse.resolve(captured,Vec3.ZERO,motion.pieces(),RESPONSE_EVENTS,QUERY_BUDGET,clip);}
            catch(RuntimeException rejected){return null;}
            finally {PlatformPhysics.exit(previous);}
            int evaluations=response.evaluations();
            if(response.status()==TemporalResponse.Status.ITERATION_LIMIT)return null;
            if(response.status()==TemporalResponse.Status.INITIAL_OVERLAP) {
                var start=motion.pieces().values().stream().map(m->m.at().apply(0)).toList();
                AnatomySeparation.Result separation;
                previous=PlatformPhysics.enter(body);
                try {separation=AnatomySeparation.resolve(captured,start,MAX_SEPARATION,SEPARATION_BUDGET,clip);}
                catch(RuntimeException rejected){return null;}
                finally {PlatformPhysics.exit(previous);}
                evaluations+=separation.candidates();if(!separation.separated())return null;
                previous=PlatformPhysics.enter(body);
                try {response=TemporalResponse.resolve(captured.move(separation.displacement()),Vec3.ZERO,motion.pieces(),RESPONSE_EVENTS,QUERY_BUDGET,clip);}
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
        private boolean finalOverlap(List<Plan> plans,List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            for(int i=0;i<plans.size();i++)for(int j=i+1;j<plans.size();j++) {
                var a=candidates.get(i).bounds().move(plans.get(i).displacement());
                var b=candidates.get(j).bounds().move(plans.get(j).displacement());
                if(a.intersects(b))return true;
            }
            return false;
        }
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
