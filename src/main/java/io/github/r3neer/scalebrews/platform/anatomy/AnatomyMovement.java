package io.github.r3neer.scalebrews.platform.anatomy;

import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Shared movement integration under explicit proof activation until Gate 1 passes. */
public final class AnatomyMovement {
    private AnatomyMovement() {}
    private static final Set<Level> ACTIVE=Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    // Entity equality reuses network IDs across logical worlds in an integrated process.
    // Weak identity keys keep server and client instances strictly separate.
    private static <K,V> Map<K,V> entityMap(){return Collections.synchronizedMap(new com.google.common.collect.MapMaker().weakKeys().<K,V>makeMap());}
    private static final Map<LivingEntity,GeometryProvider> PROVIDERS=entityMap();
    private static final Map<Entity,GravityFrame> GRAVITY=entityMap();
    private static final Map<Entity,Contact> CONTACTS=entityMap();
    private static final Map<Entity,Anchor> ANCHORS=entityMap();
    private static final Map<Entity,SupportTransport> TRANSPORT=entityMap();
    private static final Map<Entity,SurfaceContact> SURFACES=entityMap();
    private static final Map<Entity,Set<UUID>> SUSPENDED=entityMap();
    public static boolean suspended(Entity body,LivingEntity support) {
        var suspended=SUSPENDED.get(body);
        if(suspended==null || !suspended.contains(support.getUUID()))return false;
        var provider=PROVIDERS.get(support);var shape=provider==null?null:provider.sample(support).orElse(null);
        if(shape!=null && shape.pieces().values().stream().anyMatch(p->p.overlaps(body.getBoundingBox())))return true;
        suspended.remove(support.getUUID());if(suspended.isEmpty())SUSPENDED.remove(body);return false;
    }
    private static void suspend(Entity body,LivingEntity support) {
        SUSPENDED.computeIfAbsent(body,e->ConcurrentHashMapHolder.newSet()).add(support.getUUID());
        var contact=contact(body);if(contact!=null && contact.support()==support)clear(body);
    }
    private static final class ConcurrentHashMapHolder {static Set<UUID> newSet(){return java.util.concurrent.ConcurrentHashMap.newKeySet();}}
    public record SweepMetrics(long tick,long queries,long pieces,long evaluations,long exhausted) {}
    private static final Map<Level,SweepMetrics> METRICS=Collections.synchronizedMap(new WeakHashMap<>());
    public static SweepMetrics sweepMetrics(Level level){return METRICS.getOrDefault(level,new SweepMetrics(level.getGameTime(),0,0,0,0));}
    private static void recordSweep(Level level,int pieces,int evaluations,int exhausted) {
        synchronized(METRICS) {
            var old=sweepMetrics(level);
            if(old.tick()!=level.getGameTime())old=new SweepMetrics(level.getGameTime(),0,0,0,0);
            METRICS.put(level,new SweepMetrics(old.tick(),old.queries()+1,old.pieces()+pieces,old.evaluations()+evaluations,old.exhausted()+exhausted));
        }
    }
    private record Anchor(Vec3 local,Vec3 previous,Vec3 supportOrigin,Vec3 bodyOrigin,GravityFrame bodyGravity,GravityFrame supportGravity) {}
    public record Contact(LivingEntity support,String piece,long revision,Vec3 normal) {}
    public static synchronized void activate(Level level){ACTIVE.add(level);}
    public static synchronized boolean active(Level level){return ACTIVE.contains(level);}
    public static synchronized boolean active(Entity e){return ACTIVE.contains(e.level());}
    /** Server simulates every body; a client predicts only entities it owns locally. */
    public static boolean simulates(Entity body){return !body.level().isClientSide() || body.isLocalInstanceAuthoritative();}
    public static synchronized void register(LivingEntity support,GeometryProvider provider){PROVIDERS.put(support,provider);}
    public static synchronized void gravity(Entity body,GravityFrame gravity){GRAVITY.put(body,gravity);}
    public static synchronized GravityFrame gravity(Entity body){return GRAVITY.containsKey(body)?GRAVITY.get(body):GravityFrames.get(body);}
    public static synchronized Contact contact(Entity body){return CONTACTS.get(body);}
    public static synchronized void clear(Entity body){CONTACTS.remove(body);ANCHORS.remove(body);SURFACES.remove(body);}
    public static synchronized SurfaceContact surface(Entity body){return SURFACES.get(body);}
    public static synchronized SupportTransport transport(Entity body){return TRANSPORT.get(body);}
    public static synchronized boolean confirm(Entity body,LivingEntity support,SurfaceContact surface) {
        if(!active(body) || surface==null || !support.getUUID().equals(surface.support()) || !Platforms.eligible(body,support))return false;
        var provider=PROVIDERS.get(support);var snapshot=provider==null?null:provider.sample(support).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(surface.piece());
        if(piece==null || snapshot.revision()!=surface.revision() || surface.face()<0 || surface.face()>5)return false;
        Vec3 point=piece.point(surface.localPoint()),normal=piece.faceNormal(surface.face());
        // The authoritative normal belongs to the packet tick. Recompute it from the newest
        // accepted pose instead of rejecting a valid contact while the support is animating.
        if(!gravity(body).supports(normal))return false;
        CONTACTS.put(body,new Contact(support,surface.piece(),surface.revision(),normal));
        SURFACES.put(body,new SurfaceContact(surface.support(),surface.revision(),surface.piece(),surface.face(),surface.localPoint(),normal,surface.tick()));
        ANCHORS.put(body,new Anchor(surface.localPoint(),point,support.position(),body.position(),gravity(body),gravity(support)));
        return true;
    }
    public static boolean supported(Entity body) {
        var c=contact(body);if(c==null || suspended(body,c.support()) || !Platforms.eligible(body,c.support()))return false;
        var provider=PROVIDERS.get(c.support());var snapshot=provider==null?null:provider.sample(c.support()).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(c.piece());
        if(piece==null || snapshot.revision()!=c.revision())return false;
        var separation=piece.separation(body.getBoundingBox());
        return Math.abs(separation.gap())<=.025 && gravity(body).supports(separation.normal());
    }
    public static boolean spaceClear(Entity body,AABB box) {
        for(var candidate:candidates(body,box))if(candidate.box.overlaps(box))return false;
        return true;
    }
    public static void tick(Level level) {
        if(!ACTIVE.contains(level))return;
        Map<LivingEntity,GeometryProvider> providers;List<Entity> bodies;
        synchronized(PROVIDERS){providers=new IdentityHashMap<>(PROVIDERS);}
        synchronized(CONTACTS){bodies=List.copyOf(CONTACTS.keySet());}
        for(var entry:providers.entrySet())if(entry.getKey().level()==level)
            entry.getValue().tick(entry.getKey(),level.getGameTime());
        for(var body:bodies)if(body.level()==level)carry(body);
    }
    /** Client-side pose cache update without moving server-owned observer entities. */
    public static void tickGeometry(Level level) {
        if(!ACTIVE.contains(level))return;
        Map<LivingEntity,GeometryProvider> providers;
        synchronized(PROVIDERS){providers=new IdentityHashMap<>(PROVIDERS);}
        for(var entry:providers.entrySet())if(entry.getKey().level()==level)
            entry.getValue().tick(entry.getKey(),level.getGameTime());
    }
    public static synchronized void deactivate(Level level){
        ACTIVE.remove(level);PROVIDERS.keySet().removeIf(e->e.level()==level);
        GRAVITY.keySet().removeIf(e->e.level()==level);CONTACTS.keySet().removeIf(e->e.level()==level);
        ANCHORS.keySet().removeIf(e->e.level()==level);TRANSPORT.keySet().removeIf(e->e.level()==level);
        SURFACES.keySet().removeIf(e->e.level()==level);
        SUSPENDED.keySet().removeIf(e->e.level()==level);
        METRICS.remove(level);
    }
    private record Candidate(LivingEntity entity,String id,long revision,ConvexBox box) {}
    public record SweepContact(LivingEntity support,String piece,long revision,ConservativeSweep.Result result) {}
    /** Dedicated temporal query, deliberately separate from navigation and vanilla noCollision. */
    public static SweepContact sweep(Entity body,Vec3 displacement,int iterations) {
        if(!active(body))return null;
        AABB swept=body.getBoundingBox().expandTowards(displacement);
        var supports=new ArrayList<>(body.level().getEntitiesOfClass(LivingEntity.class,
            swept.inflate(Platforms.searchMargin(body.level())+4),s->Platforms.eligible(body,s)));
        supports.sort(Comparator.comparingInt(Entity::getId));
        SweepContact best=null;
        int queried=0,evaluations=0,exhausted=0;
        for(var support:supports) {
            if(suspended(body,support))continue;
            var provider=PROVIDERS.get(support);
            var snapshot=provider==null?null:provider.motion(support).orElse(null);
            if(snapshot==null || snapshot.tick()!=body.level().getGameTime())continue;
            for(var id:new TreeSet<>(snapshot.pieces().keySet())) {
                var motion=snapshot.pieces().get(id);
                // Every point stays within speed*dt of its initial point; endpoint envelopes alone are unsafe.
                if(!motion.at().apply(0).bounds().inflate(motion.maxPointSpeed()+.001).intersects(swept))continue;
                var hit=ConservativeSweep.query(body.getBoundingBox(),displacement,motion,iterations);
                queried++;evaluations+=hit.evaluations();
                if(hit.status()==ConservativeSweep.Status.ITERATION_LIMIT)exhausted++;
                if(hit.status()==ConservativeSweep.Status.CLEAR)continue;
                if(best==null || hit.safeFraction()<best.result().safeFraction()-1e-9)
                    best=new SweepContact(support,id,snapshot.revision(),hit);
            }
        }
        recordSweep(body.level(),queried,evaluations,exhausted);
        return best;
    }
    public record Selection(SurfaceContact contact,Vec3 position,double fraction) {}
    public static Selection raycast(Entity observer,Vec3 start,Vec3 end) {
        if(!active(observer))return null;
        Selection best=null;
        for(var candidate:candidates(observer,new AABB(start,end).inflate(.001))) {
            var hit=candidate.box.raycast(start,end);
            if(hit==null || best!=null && hit.fraction()>=best.fraction()-1e-9)continue;
            var contact=new SurfaceContact(candidate.entity.getUUID(),candidate.revision,candidate.id,hit.face(),hit.localPoint(),hit.normal(),observer.level().getGameTime());
            best=new Selection(contact,start.add(end.subtract(start).scale(hit.fraction())),hit.fraction());
        }
        return best;
    }
    private static List<Candidate> candidates(Entity body,AABB swept) {
        List<Candidate> result=new ArrayList<>();
        for(var support:body.level().getEntitiesOfClass(LivingEntity.class,swept.inflate(Platforms.searchMargin(body.level())),s->Platforms.eligible(body,s))) {
            if(suspended(body,support))continue;
            GeometryProvider provider=PROVIDERS.get(support);
            if(provider==null)continue;
            provider.sample(support).ifPresent(snapshot->snapshot.pieces().forEach((id,box)->{
                if(box.bounds().inflate(.001).intersects(swept))result.add(new Candidate(support,id,snapshot.revision(),box));
            }));
        }
        result.sort(Comparator.comparingInt((Candidate c)->c.entity.getId()).thenComparing(Candidate::id));
        return result;
    }
    public static boolean replacesPair(Entity body,Entity other) {
        if(!(other instanceof LivingEntity support) || !Platforms.eligible(body,support))return false;
        var provider=PROVIDERS.get(support);
        return provider!=null && provider.sample(support).filter(s->!s.pieces().isEmpty()).isPresent();
    }
    /** Resweep after sliding, with block collision validation on every remaining segment. */
    public static Vec3 collide(Entity body,Vec3 requested) {
        if(gravity(body).vertical(requested)>1e-5)clear(body);
        AABB box=body.getBoundingBox();
        var existing=contact(body);
        Map<String,ConservativeSweep.Motion> motions=new TreeMap<>();Map<String,Candidate> identities=new HashMap<>();
        for(var support:body.level().getEntitiesOfClass(LivingEntity.class,box.expandTowards(requested).inflate(Platforms.searchMargin(body.level())+4),s->Platforms.eligible(body,s))) {
            if(suspended(body,support))continue;
            var provider=PROVIDERS.get(support);var snapshot=provider==null?null:provider.motion(support).orElse(null);
            if(snapshot==null || snapshot.tick()!=body.level().getGameTime())continue;
            snapshot.pieces().forEach((id,motion)->{
                String key=(existing!=null && existing.support()==support?"0/":"1/")+String.format(java.util.Locale.ROOT,"%010d",support.getId())+"/"+id;
                motions.put(key,motion);identities.put(key,new Candidate(support,id,snapshot.revision(),motion.at().apply(1)));
            });
        }
        java.util.function.BiFunction<AABB,Vec3,Vec3> clip=(bounds,delta)->
            Entity.collideBoundingBox(body,delta,bounds,body.level(),body.level().getEntityCollisions(body,bounds.expandTowards(delta)));
        var initial=AnatomySeparation.resolve(box,motions.values().stream().map(m->m.at().apply(0)).toList(),4,128,clip);
        Vec3 correction=initial.displacement();
        if(!initial.separated()) {
            final AABB overlapBox=box;
            Set<LivingEntity> blocked=new HashSet<>();
            motions.forEach((id,m)->{if(m.at().apply(0).overlaps(overlapBox))blocked.add(identities.get(id).entity);});
            blocked.forEach(s->suspend(body,s));motions.keySet().removeIf(id->blocked.contains(identities.get(id).entity));
        }
        var response=TemporalResponse.resolve(box.move(correction),requested,motions,32,256,clip);
        recordSweep(body.level(),motions.size(),response.evaluations(),response.status()==TemporalResponse.Status.ITERATION_LIMIT?1:0);
        for(var hit:response.contacts())if(gravity(body).supports(hit.normal())) {
            var best=identities.get(hit.piece());CONTACTS.put(body,new Contact(best.entity,best.id,best.revision,hit.normal()));
        }
        Vec3 moved=correction.add(response.displacement());AABB finalBox=box.move(moved);
        var recovery=AnatomySeparation.resolve(finalBox,motions.values().stream().map(m->m.at().apply(1)).toList(),4,128,clip);
        if(recovery.separated())moved=moved.add(recovery.displacement());
        else motions.forEach((id,m)->{if(m.at().apply(1).overlaps(finalBox))suspend(body,identities.get(id).entity);});
        return moved;
    }
    public static void afterMove(Entity body) {
        var c=contact(body);
        if(c==null)return;
        if(!Platforms.eligible(body,c.support)){clear(body);return;}
        var provider=PROVIDERS.get(c.support);
        var snapshot=provider==null?null:provider.sample(c.support).orElse(null);
        var piece=snapshot==null?null:snapshot.pieces().get(c.piece);
        if(piece==null || snapshot.revision()!=c.revision){clear(body);return;}
        var separation=piece.separation(body.getBoundingBox());
        if(Math.abs(separation.gap())>.025 || !gravity(body).supports(separation.normal())){clear(body);return;}
        int face=piece.closestFace(separation.normal());var normal=piece.faceNormal(face);
        if(!gravity(body).supports(normal)){clear(body);return;}
        Vec3 local=piece.facePoint(face,body.getBoundingBox().getCenter());
        SURFACES.put(body,new SurfaceContact(c.support.getUUID(),c.revision,c.piece,face,local,normal,body.level().getGameTime()));
        ANCHORS.put(body,new Anchor(local,piece.point(local),c.support.position(),body.position(),gravity(body),gravity(c.support)));
        body.setOnGround(true);
        body.verticalCollisionBelow=true;
    }
    public static void carry(Entity body){carry(body,Collections.newSetFromMap(new IdentityHashMap<>()));}
    private static void carry(Entity body,Set<Entity> visiting) {
        var c=contact(body);var anchor=ANCHORS.get(body);
        if(c==null || anchor==null)return;
        if(!visiting.add(body) || !Platforms.eligible(body,c.support)
            || !gravity(body).equals(anchor.bodyGravity) || !gravity(c.support).equals(anchor.supportGravity)){clear(body);return;}
        try {
            carry(c.support,visiting);
            var provider=PROVIDERS.get(c.support);
            var snapshot=provider==null?null:provider.sample(c.support).orElse(null);
            var piece=snapshot==null?null:snapshot.pieces().get(c.piece);
            if(piece==null || snapshot.revision()!=c.revision || c.support.position().distanceToSqr(anchor.supportOrigin)>16
                    || body.position().distanceToSqr(anchor.bodyOrigin)>16){clear(body);return;}
            var surface=SURFACES.get(body);
            if(surface==null || !gravity(body).supports(piece.faceNormal(surface.face()))){clear(body);return;}
            Vec3 now=piece.point(anchor.local),delta=now.subtract(anchor.previous);
            if(delta.lengthSqr()>16){clear(body);return;}
            if(delta.lengthSqr()>1e-16) {
                Entity previous=io.github.r3neer.scalebrews.platform.PlatformPhysics.enter(body);
                Vec3 allowed;
                try {
                    allowed=Entity.collideBoundingBox(body,delta,body.getBoundingBox(),body.level(),body.level().getEntityCollisions(body,body.getBoundingBox().expandTowards(delta)));
                } finally {io.github.r3neer.scalebrews.platform.PlatformPhysics.exit(previous);}
                body.setPos(body.position().add(allowed));
                positionPassengers(body);
                if(body instanceof net.minecraft.server.level.ServerPlayer player)
                    ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,allowed);
                for(var passenger:body.getIndirectPassengers())
                    if(passenger instanceof net.minecraft.server.level.ServerPlayer player)
                        ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,allowed);
                var before=TRANSPORT.get(body);long tick=body.level().getGameTime();
                TRANSPORT.put(body,new SupportTransport(tick,before==null?1:before.sequence()+1,before!=null && before.tick()==tick?before.displacement().add(allowed):allowed));
                if(allowed.distanceToSqr(delta)>1e-8){clear(body);return;}
            }
            ANCHORS.put(body,new Anchor(anchor.local,now,c.support.position(),body.position(),anchor.bodyGravity,anchor.supportGravity));
        } finally {visiting.remove(body);}
    }
    /** Root transport can run after passenger ticks; refresh native seats without another physics move. */
    private static void positionPassengers(Entity root) {
        var pending=new ArrayDeque<Entity>();var visited=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        pending.add(root);
        while(!pending.isEmpty()) {
            var vehicle=pending.removeFirst();if(!visited.add(vehicle))continue;
            for(var passenger:vehicle.getPassengers()) {
                vehicle.positionRider(passenger);
                pending.addLast(passenger);
            }
        }
    }
}
