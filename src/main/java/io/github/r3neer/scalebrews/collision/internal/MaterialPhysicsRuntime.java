package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
    /** Contact pieces are scoped keys from real CCD hits or actual t=0 overlap recovery, never endpoint proximity. */
    private record Plan(Entity body,Vec3 displacement,int evaluations,Set<String> contactPieces) {
        private Plan {contactPieces=Set.copyOf(contactPieces);}
    }

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
            if(!AnatomyRuntime.acceptsIntervalIdentity(next.support(),next.handle())) {
                // Old queued work belongs to a prior lifecycle. Dropping it is conservative;
                // invalidating the current support would destroy a new binding/contact.
                record(level,1,0,0,1,0);
                continue;
            }
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
            var seen=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
            var combined=new ArrayList<MaterialEventDispatcher.Candidate<Entity>>();
            for(var event:events) {
                if(!(event.support() instanceof LivingEntity support) || !finite(event.interval().envelope()))
                    return new MaterialEventDispatcher.Candidates<>(List.of(),true);
                var envelope=event.interval().envelope();
                if(envelope.getXsize()>MAX_ENVELOPE_SPAN || envelope.getYsize()>MAX_ENVELOPE_SPAN || envelope.getZsize()>MAX_ENVELOPE_SPAN)
                    return new MaterialEventDispatcher.Candidates<>(List.of(),true);
                var local=capture(envelope,List.of(support),maximumBodies);
                if(local.overflow())return new MaterialEventDispatcher.Candidates<>(List.of(),true);
                for(var candidate:local.bodies())if(seen.add(candidate.body())) {
                    combined.add(candidate);
                    if(combined.size()>maximumBodies)return new MaterialEventDispatcher.Candidates<>(List.of(),true);
                }
            }
            combined.sort(Comparator.comparing((MaterialEventDispatcher.Candidate<Entity> c)->c.body().getUUID())
                .thenComparingInt(c->c.body().getId()));
            return new MaterialEventDispatcher.Candidates<>(combined,false);
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
            var pieces=scopedPieces(event,motion);
            var outcome=resolveAll(List.of(event),pieces,candidates);
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
                    var scoped=scopedPieces(event,motions.get(event.interval().handle()));
                    for(var entry:scoped.entrySet())if(pieces.put(entry.getKey(),entry.getValue())!=null)
                        return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_FAILURE);
                }
                if(pieces.isEmpty()) {plans.add(new Plan(body,Vec3.ZERO,0,Set.of()));continue;}
                var plan=plan(body,candidate.bounds(),pieces);
                if(plan==null) {
                    suspendUncertainPairs(body,candidate.bounds(),events);
                    revalidateRetainedContacts(candidates,events);
                    return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                }
                plans.add(plan);evaluations+=plan.evaluations();
            }
            var conflicts=bodyOverlapPairs(plans,candidates);
            if(!conflicts.isEmpty()) {
                for(var conflict:conflicts) {
                    suspendConflictRelation(plans.get(conflict.first()),candidates.get(conflict.first()).bounds(),
                        plans.get(conflict.second()),candidates.get(conflict.second()).bounds(),events,motions);
                    suspendConflictRelation(plans.get(conflict.second()),candidates.get(conflict.second()).bounds(),
                        plans.get(conflict.first()),candidates.get(conflict.first()).bounds(),events,motions);
                }
                revalidateRetainedContacts(candidates,events);
                return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
            }
            apply(plans,events);record(level,events.size(),0,evaluations,0,0);
            var outcomes=new ArrayList<MaterialEventDispatcher.Outcome>(events.size());
            for(int i=0;i<events.size();i++)outcomes.add(MaterialEventDispatcher.Outcome.applied(1));
            return new MaterialEventDispatcher.BatchResolution<>(outcomes,List.of());
        }
        @Override public void quarantine(MaterialEventDispatcher.Event<Entity> event,MaterialEventDispatcher.Reason reason) {
            if(reason!=MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED && event.support() instanceof LivingEntity support)
                AnatomyMovement.invalidateSupport(support);
            record(level,0,0,0,1,reason==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED?1:0);
        }
        private GeometryProvider.MotionSnapshot motion(GeometryProvider.MotionIntervalHandle handle) {
            var value=prepared.get(handle);return value==null?null:value.motion();
        }
        private TreeMap<String,ConservativeSweep.Motion> scopedPieces(MaterialEventDispatcher.Event<Entity> event,
                GeometryProvider.MotionSnapshot motion) {
            var pieces=new TreeMap<String,ConservativeSweep.Motion>();String prefix=prefix(event);
            for(var entry:motion.pieces().entrySet())pieces.put(prefix+entry.getKey(),entry.getValue());
            return pieces;
        }
        private String prefix(MaterialEventDispatcher.Event<Entity> event) {
            return event.support().getUUID()+"/"+event.interval().handle().materialSerial()+"/";
        }
        private MaterialEventDispatcher.Outcome resolveAll(List<MaterialEventDispatcher.Event<Entity>> events,
                Map<String,ConservativeSweep.Motion> pieces,List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var plans=new ArrayList<Plan>();int evaluations=0;
            for(var candidate:candidates) {
                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return fail(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var plan=plan(body,candidate.bounds(),pieces);
                if(plan==null) {
                    suspendUncertainPairs(body,candidate.bounds(),events);
                    return fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                }
                plans.add(plan);evaluations+=plan.evaluations();
            }
            var conflicts=bodyOverlapConflicts(plans,candidates);
            if(!conflicts.isEmpty()) {
                for(int index:conflicts)suspendUncertainPairs(plans.get(index).body(),candidates.get(index).bounds(),events);
                return fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
            }
            apply(plans,events);record(level,events.size(),0,evaluations,0,0);
            return MaterialEventDispatcher.Outcome.applied(1);
        }
        private Plan plan(Entity body,AABB captured,Map<String,ConservativeSweep.Motion> pieces) {
            if(pieces.isEmpty())return new Plan(body,Vec3.ZERO,0,Set.of());
            var evidence=new TreeSet<String>();
            var clip=clip(body);TemporalResponse.Result response;
            Entity previous=PlatformPhysics.enter(body);
            try {response=TemporalResponse.resolve(captured,Vec3.ZERO,pieces,RESPONSE_EVENTS,QUERY_BUDGET,clip);}
            catch(RuntimeException rejected){return null;}
            finally {PlatformPhysics.exit(previous);}
            int evaluations=response.evaluations();
            if(response.status()==TemporalResponse.Status.ITERATION_LIMIT)return null;
            if(response.status()==TemporalResponse.Status.INITIAL_OVERLAP) {
                for(var entry:pieces.entrySet())if(entry.getValue().at().apply(0).overlaps(captured))evidence.add(entry.getKey());
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
                addContacts(evidence,response);
                return new Plan(body,separation.displacement().add(response.displacement()),evaluations,evidence);
            }
            addContacts(evidence,response);
            return new Plan(body,response.displacement(),evaluations,evidence);
        }
        private void addContacts(Set<String> evidence,TemporalResponse.Result response) {
            for(var contact:response.contacts())evidence.add(contact.piece());
        }
        /**
         * A successful pairwise-plan conflict has stronger provenance than a generic solver exhaustion.
         * Re-plan this body without its retained support. Only when that removal eliminates the actual
         * body/body overlap conflict is the retained relation causally implicated and suspended.
         */
        private void suspendConflictRelation(Plan full,AABB captured,Plan other,AABB otherCaptured,
                List<MaterialEventDispatcher.Event<Entity>> events,
                Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> motions) {
            var retained=AnatomyMovement.contact(full.body());if(retained==null)return;
            boolean participates=false;var without=new TreeMap<String,ConservativeSweep.Motion>();
            for(var event:events) {
                if(!(event.support() instanceof LivingEntity support))continue;
                if(support==retained.support()) {participates=true;continue;}
                if(!Platforms.eligible(full.body(),support))continue;
                var motion=motions.get(event.interval().handle());
                if(motion==null) {AnatomyMovement.suspend(full.body(),retained.support());return;}
                for(var entry:scopedPieces(event,motion).entrySet())
                    if(without.put(entry.getKey(),entry.getValue())!=null) {
                        AnatomyMovement.suspend(full.body(),retained.support());return;
                    }
            }
            if(!participates)return;
            var alternate=plan(full.body(),captured,without);
            if(alternate==null) {AnatomyMovement.suspend(full.body(),retained.support());return;}
            double before=overlapVolume(captured,otherCaptured);
            double alternateOverlap=overlapVolume(captured.move(alternate.displacement()),
                otherCaptured.move(other.displacement()));
            if(alternateOverlap<=before+OVERLAP_EPS)AnatomyMovement.suspend(full.body(),retained.support());
        }
        private void suspendUncertainPairs(Entity body,AABB captured,List<MaterialEventDispatcher.Event<Entity>> events) {
            var retained=AnatomyMovement.contact(body);
            for(var event:events) {
                if(!(event.support() instanceof LivingEntity support) || !Platforms.eligible(body,support))continue;
                // A solver failure makes an already-retained relation non-authoritative even
                // when the body began separated. Initial overlaps have no prior contact, so
                // retain the existing overlap test for that recovery path. In both cases the
                // quarantine is body/support-local; unrelated bodies on the support survive.
                boolean uncertain=retained!=null && retained.support()==support;
                var motion=motion(event.interval().handle());
                if(!uncertain && motion!=null)try {
                    for(var piece:motion.pieces().values())if(piece.at().apply(0).overlaps(captured)){uncertain=true;break;}
                } catch(RuntimeException rejected){uncertain=true;}
                if(uncertain)AnatomyMovement.suspend(body,support);
            }
        }
        /**
         * Rejecting a simultaneous plan does not roll material geometry back. Any retained
         * relation whose own event has a certified after-frame must still be valid there.
         * This is deliberately separate from conflict attribution: a stable support survives
         * another support's conflict, while an independently stale face is released locally.
         */
        private void revalidateRetainedContacts(List<MaterialEventDispatcher.Candidate<Entity>> candidates,
                List<MaterialEventDispatcher.Event<Entity>> events) {
            for(var candidate:candidates) {
                var body=candidate.body();var retained=AnatomyMovement.contact(body);if(retained==null)continue;
                MaterialEventDispatcher.Event<Entity> own=null;boolean ambiguous=false;
                for(var event:events)if(event.support()==retained.support()) {
                    if(own!=null) {ambiguous=true;break;}
                    own=event;
                }
                if(ambiguous) {AnatomyMovement.clear(body);continue;}
                if(own!=null && own.support() instanceof LivingEntity support
                        && !retainedContactValid(body,support,own.interval().handle().after()))
                    AnatomyMovement.clear(body);
            }
        }
        private boolean retainedContactValid(Entity body,LivingEntity support,GeometryProvider.QueryFrame after) {
            var retained=AnatomyMovement.contact(body);var surface=AnatomyMovement.surface(body);
            if(retained==null || retained.support()!=support || surface==null || !Platforms.eligible(body,support))return false;
            var identity=after.identity();
            if(!identity.matches(support)
                    || identity.localRegistrationGeneration()!=AnatomyMovement.registrationGeneration(support)
                    || identity.revision()!=retained.revision()
                    || after.snapshot().revision()!=retained.revision()
                    || !surface.support().equals(support.getUUID())
                    || surface.revision()!=retained.revision()
                    || !surface.piece().equals(retained.piece())
                    || surface.face()<0 || surface.face()>5)return false;
            var piece=after.snapshot().pieces().get(retained.piece());if(piece==null)return false;
            var separation=piece.separation(body.getBoundingBox());
            if(Math.abs(separation.gap())>.025)return false;
            int face=piece.closestFace(separation.normal());
            if(face!=surface.face())return false;
            return AnatomyMovement.gravity(body).supports(piece.faceNormal(face));
        }
        private java.util.function.BiFunction<AABB,Vec3,Vec3> clip(Entity body) {
            return (box,delta)->Entity.collideBoundingBox(body,delta,box,level,level.getEntityCollisions(body,box.expandTowards(delta)));
        }
        private MaterialEventDispatcher.Outcome fail(List<MaterialEventDispatcher.Event<Entity>> events,MaterialEventDispatcher.Reason reason) {
            if(reason!=MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED)
                for(var event:events)if(event.support() instanceof LivingEntity support)AnatomyMovement.invalidateSupport(support);
            record(level,0,0,0,events.size(),reason==MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED?1:0);
            return MaterialEventDispatcher.Outcome.quarantined(reason);
        }
        private MaterialEventDispatcher.BatchResolution<Entity> batchFailure(List<MaterialEventDispatcher.Event<Entity>> events,MaterialEventDispatcher.Reason reason) {
            var outcome=fail(events,reason);var outcomes=new ArrayList<MaterialEventDispatcher.Outcome>(events.size());
            for(int i=0;i<events.size();i++)outcomes.add(outcome);
            return new MaterialEventDispatcher.BatchResolution<>(outcomes,List.of());
        }
        private void apply(List<Plan> plans,List<MaterialEventDispatcher.Event<Entity>> events) {
            for(var plan:plans) {
                if(plan.displacement().lengthSqr()>1e-20)plan.body().setPos(plan.body().position().add(plan.displacement()));
                AnatomyMovement.afterMove(plan.body());
                if(AnatomyMovement.contact(plan.body())!=null)continue;
                for(var event:events) {
                    if(!(event.support() instanceof LivingEntity support) || !Platforms.eligible(plan.body(),support))continue;
                    var allowed=contactPieces(event,plan.contactPieces());if(allowed.isEmpty())continue;
                    if(establish(plan.body(),support,event.interval().handle().after(),allowed))break;
                }
            }
        }
        private Set<String> contactPieces(MaterialEventDispatcher.Event<Entity> event,Set<String> evidence) {
            String prefix=prefix(event);var pieces=new TreeSet<String>();
            for(var key:evidence)if(key.startsWith(prefix))pieces.add(key.substring(prefix.length()));
            return pieces;
        }
        private boolean establish(Entity body,LivingEntity support,GeometryProvider.QueryFrame after,Set<String> allowedPieces) {
            var identity=after.identity();
            if(!identity.matches(support) || identity.localRegistrationGeneration()!=AnatomyMovement.registrationGeneration(support))return false;
            for(var id:new TreeSet<>(allowedPieces)) {
                var piece=after.snapshot().pieces().get(id);if(piece==null)continue;
                var separation=piece.separation(body.getBoundingBox());
                if(Math.abs(separation.gap())>.025)continue;
                int face=piece.closestFace(separation.normal());var normal=piece.faceNormal(face);
                if(!AnatomyMovement.gravity(body).supports(normal))continue;
                Vec3 local=piece.facePoint(face,body.getBoundingBox().getCenter());
                SurfaceContact contact;
                try {contact=new SurfaceContact(support.getUUID(),identity.revision(),id,face,local,normal,after.authorityTick());}
                catch(RuntimeException rejected){continue;}
                if(AnatomyMovement.confirm(body,support,contact)) {
                    body.setOnGround(true);body.verticalCollisionBelow=true;return true;
                }
            }
            return false;
        }
        private record BodyConflict(int first,int second) {}
        private List<BodyConflict> bodyOverlapPairs(List<Plan> plans,List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var conflicts=new ArrayList<BodyConflict>();
            for(int i=0;i<plans.size();i++)for(int j=i+1;j<plans.size();j++) {
                var beforeA=candidates.get(i).bounds();var beforeB=candidates.get(j).bounds();
                var afterA=beforeA.move(plans.get(i).displacement());var afterB=beforeB.move(plans.get(j).displacement());
                if(overlapVolume(afterA,afterB)>overlapVolume(beforeA,beforeB)+OVERLAP_EPS)
                    conflicts.add(new BodyConflict(i,j));
            }
            return List.copyOf(conflicts);
        }
        private Set<Integer> bodyOverlapConflicts(List<Plan> plans,List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var conflicts=new TreeSet<Integer>();
            for(var conflict:bodyOverlapPairs(plans,candidates)) {
                conflicts.add(conflict.first());conflicts.add(conflict.second());
            }
            return conflicts;
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
