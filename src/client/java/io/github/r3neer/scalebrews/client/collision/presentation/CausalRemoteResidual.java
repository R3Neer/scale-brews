package io.github.r3neer.scalebrews.client.collision.presentation;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyObserverBundleScope.NativeEvent;
import io.github.r3neer.scalebrews.collision.internal.AnatomyObserverCarryPayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyContactPayload;
import io.github.r3neer.scalebrews.collision.internal.BuiltInRootTransformProviders;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Visual-only remote body correction derived from attested native interpolation and certified material. */
public final class CausalRemoteResidual {
    private static final int MAX_BODIES=4096,MAX_PENDING_PACKETS=8,MAX_TOTAL_PENDING=512,PENDING_TICKS=8;
    private static final double MATERIAL_TOLERANCE=1e-6;
    private static final Map<Entity,Relation> RELATIONS=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().makeMap());
    private record Watermark(java.util.UUID epoch,long revision,long tracking,long nonce) {}
    /** Compact replay watermark survives visual retirement until this tracked entity unloads. */
    private static final Map<Entity,Watermark> WATERMARKS=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().makeMap());
    private static int pendingCount;
    private static final class Relation {
        AnatomyObserverCarryPayload identity;
        final GravityFrames.Observation bodyGravity,supportGravity;
        final InterpolationHandler handler;
        long lastNonce,lastSequence,pendingSince,baselineGeneration,acceptedBaselineGeneration;
        int stepsTotal,remaining;
        Vec3 oldResidual=Vec3.ZERO,currentResidual=Vec3.ZERO,basis=Vec3.ZERO;
        Vec3 nativeTarget,expectedPosition,expectedOldPosition;
        AnatomyObserverCarryPayload pending;
        final ArrayDeque<AnatomyObserverCarryPayload> pendingPackets=new ArrayDeque<>();
        boolean verified,allowOriginalRoot=true;
        Relation(Entity body,LivingEntity support,AnatomyObserverCarryPayload packet) {
            identity=packet;handler=body.getInterpolation();
            bodyGravity=GravityFrames.observe(body);supportGravity=GravityFrames.observe(support);
            lastNonce=packet.nonce();
        }
    }
    private CausalRemoteResidual() {}

    public static void reset(){RELATIONS.clear();WATERMARKS.clear();pendingCount=0;}
    public static void retire(Entity body) {
        if(body==null)return;
        var removed=RELATIONS.remove(body);
        if(removed!=null)pendingCount-=removed.pendingPackets.size();
    }
    public static void forget(Entity body){retire(body);if(body!=null)WATERMARKS.remove(body);}
    /** A synchronized map still requires its monitor while copying a collection view. */
    private static java.util.List<Map.Entry<Entity,Relation>> relationSnapshot() {
        synchronized(RELATIONS) {
            var snapshot=new java.util.ArrayList<Map.Entry<Entity,Relation>>(RELATIONS.size());
            RELATIONS.forEach((body,relation)->snapshot.add(Map.entry(body,relation)));
            return snapshot;
        }
    }
    /** Native old-position writes are events, so restoring the same value cannot revive a retired baseline. */
    public static void nativeBaselineChanged(Entity body) {
        if(body==null || !body.level().isClientSide())return;
        var relation=RELATIONS.get(body);
        if(relation==null)return;
        if(relation.baselineGeneration==Long.MAX_VALUE){retire(body);return;}
        relation.baselineGeneration++;
    }
    public static void retireSupport(java.util.UUID support) {
        if(support==null)return;
        for(var entry:relationSnapshot())
            if(entry.getValue().identity.support().equals(support))retire(entry.getKey());
    }
    public static void acceptedOwnFrame(LivingEntity body) {
        var relation=RELATIONS.get(body);
        if(relation!=null)relation.allowOriginalRoot=originalRootAllowed(body);
    }
    public static void contactPacket(Entity body,AnatomyContactPayload packet) {
        var relation=RELATIONS.get(body);
        if(relation==null)return;
        var identity=relation.identity;
        if(!packet.present() || packet.bodyId()!=identity.bodyId() || !packet.body().equals(identity.body())
                || packet.trackingGeneration()!=identity.bodyTrackingGeneration()
                || packet.supportId()!=identity.supportId() || !packet.support().equals(identity.support())
                || packet.supportBindingGeneration()!=identity.supportBindingGeneration()
                || !packet.piece().equals(identity.carries().getLast().piece())
                || packet.face()!=identity.carries().getLast().face()
                || packet.revision()!=identity.revision())retire(body);
    }

    /** Called only after the native handler returned inside the exact decoded bundle scope. */
    public static void attested(NativeEvent event,AnatomyObserverCarryPayload packet) {
        Entity body=event.body();var level=Minecraft.getInstance().level;
        if(body==null || level==null || body.level()!=level || body.isLocalInstanceAuthoritative()
                || packet.bodyId()!=body.getId() || !packet.body().equals(body.getUUID())
                || !packet.dimension().equals(level.dimension().identifier()) || !event.restarted()
                || event.after()==null || !event.after().interpolating()
                || event.after().interpolationLength()<1 || event.after().interpolationLength()>20
                || event.after().interpolationTarget()==null
                || event.after().interpolationTarget().distanceToSqr(packet.nativeTarget())>MATERIAL_TOLERANCE) {
            retire(body);return;
        }
        var watermark=WATERMARKS.get(body);
        if(watermark!=null && (packet.bodyTrackingGeneration()<watermark.tracking()
                || packet.bodyTrackingGeneration()==watermark.tracking()
                    && (!packet.epoch().equals(watermark.epoch()) || packet.revision()!=watermark.revision()
                        || packet.nonce()<=watermark.nonce())))return;
        if(watermark==null && WATERMARKS.size()>=MAX_BODIES)return;
        WATERMARKS.put(body,new Watermark(packet.epoch(),packet.revision(),packet.bodyTrackingGeneration(),packet.nonce()));
        var support=level.getEntity(packet.supportId());
        if(!(support instanceof LivingEntity living) || !support.getUUID().equals(packet.support())) {
            retire(body);return;
        }
        var prior=RELATIONS.get(body);
        // A missing material certificate cannot be recovered after vanilla has fully
        // incorporated its packet. Drop that expired chain so a later, independently
        // certified carry can start from the now-quiet native baseline.
        if(prior!=null && prior.pending!=null && quietBefore(event)) {
            retire(body);
            prior=null;
        }
        if(prior!=null && packet.nonce()<=prior.lastNonce)return;
        if(prior!=null && (!sameIdentity(prior.identity,packet)
                || packet.carries().getFirst().transportSequence()!=prior.lastSequence+1)) {
            retire(body);prior=null;
        }
        if(prior==null && RELATIONS.size()>=MAX_BODIES)return;
        var relation=prior==null?new Relation(body,living,packet):prior;
        if(relation.pendingPackets.size()>=MAX_PENDING_PACKETS || pendingCount>=MAX_TOTAL_PENDING) {
            retire(body);return;
        }
        if(prior!=null && (!gravityMatches(relation,body,living) || relation.handler!=body.getInterpolation())) {
            retire(body);return;
        }
        settleAtBreak(event,relation);
        settleIfQuiet(body,relation);
        Vec3 applied=Vec3.ZERO;
        for(var mark:packet.carries())applied=applied.add(mark.appliedDelta());
        relation.oldResidual=relation.oldResidual.add(applied);
        relation.currentResidual=relation.currentResidual.add(applied);
        relation.basis=relation.currentResidual;
        relation.stepsTotal=event.after().interpolationLength();
        relation.remaining=relation.stepsTotal;
        relation.nativeTarget=event.after().interpolationTarget();
        relation.expectedPosition=body.position();
        relation.expectedOldPosition=body.oldPosition();
        relation.acceptedBaselineGeneration=relation.baselineGeneration;
        relation.lastNonce=packet.nonce();
        relation.lastSequence=packet.carries().getLast().transportSequence();
        if(relation.pendingPackets.isEmpty())relation.pendingSince=level.getGameTime();
        relation.pendingPackets.addLast(packet);
        pendingCount++;
        relation.pending=packet;
        relation.verified=false;
        relation.identity=packet;
        RELATIONS.put(body,relation);
        activate(body,relation);
    }

    /** Rotation-only native restarts transfer proved outstanding carry; unproven positions retire it. */
    public static void unattested(NativeEvent event) {
        var relation=RELATIONS.get(event.body());
        if(relation==null)return;
        settleAtBreak(event,relation);
        settleIfQuiet(event.body(),relation);
        if(event.kind()!=null || event.after()==null || !event.after().interpolating()) {
            retire(event.body());return;
        }
        if(!event.restarted())return;
        if(event.after().interpolationLength()<1 || event.after().interpolationLength()>20) {
            retire(event.body());return;
        }
        relation.basis=relation.currentResidual;
        relation.stepsTotal=event.after().interpolationLength();relation.remaining=relation.stepsTotal;
        relation.nativeTarget=event.after().interpolationTarget();
        relation.expectedPosition=event.body().position();
        relation.expectedOldPosition=event.body().oldPosition();
    }

    /** Native interpolation tick, after vanilla changed the physical client entity. */
    public static void interpolated(Entity body,InterpolationHandler handler,Vec3 beforeTarget,boolean beforeActive) {
        var relation=RELATIONS.get(body);
        if(relation==null || relation.handler!=handler)return;
        if(relation.baselineGeneration-relation.acceptedBaselineGeneration>1){retire(body);return;}
        if(relation.remaining<1 || !beforeActive) {
            if(!beforeActive) {
                relation.oldResidual=Vec3.ZERO;relation.currentResidual=Vec3.ZERO;
                relation.expectedPosition=body.position();
                relation.expectedOldPosition=body.oldPosition();
                relation.acceptedBaselineGeneration=relation.baselineGeneration;
            }
            return;
        }
        if(relation.stepsTotal<1 || !handler.position().equals(beforeTarget)
                || !handler.position().equals(relation.nativeTarget)) {retire(body);return;}
        relation.oldResidual=relation.currentResidual;
        relation.remaining--;
        relation.currentResidual=relation.basis.scale((double)relation.remaining/relation.stepsTotal);
        relation.expectedPosition=body.position();
        relation.expectedOldPosition=body.oldPosition();
        relation.acceptedBaselineGeneration=relation.baselineGeneration;
        if(relation.pending!=null && body.level().getGameTime()-relation.pendingSince>PENDING_TICKS)retire(body);
    }

    /** Runs after contact/endpoint/interval acceptance, never from a render query. */
    public static void endTick() {
        var level=Minecraft.getInstance().level;
        if(level==null){reset();return;}
        for(var entry:relationSnapshot()) {
            Entity body=entry.getKey();Relation relation=entry.getValue();
            if(body==null || body.level()!=level || body.isRemoved()
                    || relation.baselineGeneration!=relation.acceptedBaselineGeneration
                    || relation.expectedOldPosition!=null && !body.oldPosition().equals(relation.expectedOldPosition)
                    || body.level().getGameTime()-relation.pendingSince>PENDING_TICKS && relation.pending!=null) {
                retire(body);continue;
            }
            if(relation.pending!=null) {
                if(quiet(body,relation))retire(body);
                else activate(body,relation);
            }
            else if(!contactMatches(body,relation) || !gravityMatches(relation,body,support(body,relation.identity)))retire(body);
            else {
                settleIfQuiet(body,relation);
                relation.allowOriginalRoot=originalRootAllowed(body);
            }
        }
    }

    private static void activate(Entity body,Relation relation) {
        var packet=relation.pending;
        if(packet==null)return;
        var support=support(body,packet);
        if(support==null)return;
        var contact=AnatomyClientNetworking.presentationContact(body,body.level().getGameTime()).orElse(null);
        if(contact==null)return;
        var supportFrame=AnatomyClientNetworking.presentationFrame(support).orElse(null);
        if(contact.bodyTrackingGeneration()!=packet.bodyTrackingGeneration()
                || contact.support()!=support || !contact.surface().support().equals(packet.support())
                || contact.surface().revision()!=packet.revision()
                || supportFrame==null || !frameMatches(supportFrame,packet)
                || !gravityMatches(relation,body,support)) {retire(body);return;}
        Vec3 material=Vec3.ZERO,applied=Vec3.ZERO;
        for(var pendingPacket:relation.pendingPackets)for(var mark:pendingPacket.carries()) {
            if(!contact.surface().piece().equals(mark.piece()) || contact.surface().face()!=mark.face()) {
                retire(body);return;
            }
            var before=AnatomyClientNetworking.presentationInterval(support,mark.materialSerial(),0).orElse(null);
            var after=AnatomyClientNetworking.presentationInterval(support,mark.materialSerial(),1).orElse(null);
            if(before==null || after==null)return;
            if(before.before().frameSerial()!=mark.beforeSupportFrameSerial()
                    || after.after().frameSerial()!=mark.supportFrameSerial()
                    || !frameMatches(before,pendingPacket) || !frameMatches(after,pendingPacket)) {retire(body);return;}
            var boxBefore=before.evaluated().pieces().get(mark.piece());
            var boxAfter=after.evaluated().pieces().get(mark.piece());
            if(boxBefore==null || boxAfter==null) {retire(body);return;}
            var step=boxAfter.point(mark.localPoint()).subtract(boxBefore.point(mark.localPoint()));
            if(step.distanceToSqr(mark.appliedDelta())>MATERIAL_TOLERANCE) {retire(body);return;}
            material=material.add(step);
            applied=applied.add(mark.appliedDelta());
        }
        if(material.distanceToSqr(applied)>MATERIAL_TOLERANCE) {retire(body);return;}
        relation.pending=null;
        pendingCount-=relation.pendingPackets.size();
        relation.pendingPackets.clear();
        relation.verified=true;
        relation.allowOriginalRoot=originalRootAllowed(body);
    }

    private static boolean frameMatches(AnatomyClientNetworking.PresentationFrame frame,AnatomyObserverCarryPayload packet) {
        var id=frame.identity();
        return id.epoch().equals(packet.epoch()) && id.revision()==packet.revision()
            && id.dimension().equals(packet.dimension()) && id.entityId()==packet.supportId()
            && id.support().equals(packet.support()) && id.bindingGeneration()==packet.supportBindingGeneration()
            && frame.trackingGeneration()==packet.supportTrackingGeneration();
    }
    private static LivingEntity support(Entity body,AnatomyObserverCarryPayload packet) {
        if(body==null || body.level()!=Minecraft.getInstance().level)return null;
        var entity=body.level().getEntity(packet.supportId());
        return entity instanceof LivingEntity living && living.getUUID().equals(packet.support())?living:null;
    }
    private static boolean contactMatches(Entity body,Relation relation) {
        var contact=AnatomyClientNetworking.presentationContact(body,body.level().getGameTime()).orElse(null);
        var frame=contact==null?null:AnatomyClientNetworking.presentationFrame(contact.support()).orElse(null);
        return contact!=null && contact.bodyTrackingGeneration()==relation.identity.bodyTrackingGeneration()
            && contact.support().getUUID().equals(relation.identity.support())
            && contact.support().getId()==relation.identity.supportId()
            && frame!=null && frameMatches(frame,relation.identity)
            && contact.surface().piece().equals(relation.identity.carries().getLast().piece())
            && contact.surface().face()==relation.identity.carries().getLast().face();
    }
    private static boolean gravityMatches(Relation relation,Entity body,LivingEntity support) {
        if(support==null)return false;
        var bodyNow=GravityFrames.observe(body);var supportNow=GravityFrames.observe(support);
        return bodyNow.generation()==relation.bodyGravity.generation()
            && supportNow.generation()==relation.supportGravity.generation()
            && bodyNow.frame().equals(relation.bodyGravity.frame())
            && supportNow.frame().equals(relation.supportGravity.frame());
    }
    private static boolean sameIdentity(AnatomyObserverCarryPayload before,AnatomyObserverCarryPayload after) {
        return before.epoch().equals(after.epoch()) && before.revision()==after.revision()
            && before.dimension().equals(after.dimension()) && before.bodyId()==after.bodyId()
            && before.body().equals(after.body()) && before.bodyTrackingGeneration()==after.bodyTrackingGeneration()
            && before.supportId()==after.supportId() && before.support().equals(after.support())
            && before.supportBindingGeneration()==after.supportBindingGeneration()
            && before.supportTrackingGeneration()==after.supportTrackingGeneration();
    }
    private static void settleIfQuiet(Entity body,Relation relation) {
        if(quiet(body,relation)) {
            relation.oldResidual=Vec3.ZERO;
            relation.currentResidual=Vec3.ZERO;
            relation.basis=Vec3.ZERO;
            relation.remaining=0;
            relation.expectedPosition=body.position();
            relation.expectedOldPosition=body.oldPosition();
        }
    }
    private static void settleAtBreak(NativeEvent event,Relation relation) {
        if(quietBefore(event)) {
            relation.oldResidual=Vec3.ZERO;
            relation.currentResidual=Vec3.ZERO;
            relation.basis=Vec3.ZERO;
            relation.remaining=0;
        }
    }
    private static boolean quiet(Entity body,Relation relation) {
        return relation.handler!=null && !relation.handler.hasActiveInterpolation()
            && body.oldPosition().equals(body.position());
    }
    private static boolean quietBefore(NativeEvent event) {
        return event.before()!=null && !event.before().interpolating()
            && event.body().oldPosition().equals(event.before().bodyPosition());
    }
    private static boolean originalRootAllowed(Entity body) {
        if(!(body instanceof LivingEntity living))return true;
        var own=AnatomyClientNetworking.presentationFrame(living).orElse(null);
        return own==null || own.identity().rootProvider().equals(BuiltInRootTransformProviders.ENTITY_ROOT);
    }
    /** Distinguishes equal visual vectors produced by different certified packet occurrences. */
    public static long renderNonce(Entity body) {
        var relation=body==null?null:RELATIONS.get(body);
        return relation==null?0:relation.lastNonce;
    }
    /** Pure presentation read. A remote residual never contributes to local camera smoothing. */
    public static Vec3 rendererOffset(Entity body,float partial) {
        if(body==null || !Float.isFinite(partial) || partial<0 || partial>1)return Vec3.ZERO;
        var relation=RELATIONS.get(body);
        if(relation==null || !relation.verified || relation.pending!=null
                || body.level()!=Minecraft.getInstance().level || body.isLocalInstanceAuthoritative()
                || !body.position().equals(relation.expectedPosition)
                || !body.oldPosition().equals(relation.expectedOldPosition)
                || relation.baselineGeneration!=relation.acceptedBaselineGeneration || !relation.allowOriginalRoot
                || !gravityMatches(relation,body,support(body,relation.identity))
                || relation.handler!=null && !relation.handler.hasActiveInterpolation()
                    && body.oldPosition().equals(body.position()))return Vec3.ZERO;
        return relation.oldResidual.lerp(relation.currentResidual,partial);
    }
}
