package io.github.r3neer.scalebrews.client.collision.presentation;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking.PresentationFrame;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking.PresentationIdentity;
import io.github.r3neer.scalebrews.client.platform.PlatformCamera;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.internal.AnatomyContactPayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.integration.gravity.GravityFrames;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** One locally controlled body's visual relation to accepted causal support material. */
public final class CausalBodyResidual {
    private record Key(UUID body,int bodyId,UUID support,int supportId,PresentationIdentity identity,
            long tracking,GravityFrame bodyGravity,GravityFrame supportGravity,String piece,int face) {}
    private record DrawSample(long bodyTick,Vec3 oldPosition,Vec3 supportDisplacement,Vec3 appliedCarry) {}
    private static final class Relation {
        final Entity body;
        final LivingEntity support;
        final Key key;
        final GravityFrames.Observation bodyGravityObservation,supportGravityObservation;
        final AnatomyClientNetworking.PredictionObservation predictionObservation;
        long bodyTrackingGeneration;
        PresentationFrame current,pendingBefore,pendingAfter;
        Vec3 confirmedPoint;
        long lastTransportSequence;
        AnatomyMovement.ClientTransportCursor lastCursor;
        volatile DrawSample draw;
        Relation(Entity body,LivingEntity support,Key key,PresentationFrame frame,Vec3 confirmedPoint) {
            this.body=body;this.support=support;this.key=key;current=frame;this.confirmedPoint=confirmedPoint;
            bodyGravityObservation=GravityFrames.observe(body);
            supportGravityObservation=GravityFrames.observe(support);
            predictionObservation=AnatomyClientNetworking.observePredictionBody();
        }
    }
    private static volatile Relation relation;
    private CausalBodyResidual() {}

    /** Hard barriers also retire the camera's retained smoothing, including short teleports. */
    public static void reset() {
        if(relation!=null)relation=null;
        PlatformCamera.reset();
    }
    public static void retireSupport(UUID support) {
        if(relation!=null && relation.key.support().equals(support))reset();
    }
    public static void retireBody(Entity body) {
        if(relation!=null && relation.body==body)reset();
    }
    /** Riding topology changes retire the old prediction relation at the event, even without a visual query. */
    public static void ridingTopologyChanged(Entity passenger,Entity vehicle) {
        if(passenger==null || !passenger.level().isClientSide())return;
        var r=relation;
        if(r!=null && (passenger==r.body || vehicle==r.body
                || passenger==Minecraft.getInstance().player))reset();
    }
    public static void contactPacket(AnatomyContactPayload packet) {
        var r=relation;
        if(r==null || !r.key.body().equals(packet.body()))return;
        if(!packet.present() || packet.bodyId()!=r.key.bodyId() || !packet.support().equals(r.key.support())
                || packet.supportId()!=r.key.supportId() || !packet.piece().equals(r.key.piece())
                || packet.face()!=r.key.face() || packet.revision()!=r.key.identity().revision()
                || packet.supportBindingGeneration()!=r.key.identity().bindingGeneration()
                || packet.trackingGeneration()!=r.bodyTrackingGeneration)reset();
    }
    public static void refresh(Entity body) {
        if(body==null || body.level()!=Minecraft.getInstance().level
                || !AnatomyClientNetworking.isCurrentPredictionBody(body)) {
            if(relation!=null)reset();
            return;
        }
        var contact=AnatomyClientNetworking.presentationContact(body,body.level().getGameTime()).orElse(null);
        if(contact==null) {if(relation!=null)reset();return;}
        var support=contact.support();
        var frame=AnatomyClientNetworking.presentationFrame(support).orElse(null);
        if(frame==null || frame.identity().revision()!=contact.surface().revision()
                || !frame.evaluated().pieces().containsKey(contact.surface().piece())) {
            if(relation!=null)reset();return;
        }
        var key=new Key(body.getUUID(),body.getId(),support.getUUID(),support.getId(),frame.identity(),
            frame.trackingGeneration(),AnatomyMovement.gravity(body),AnatomyMovement.gravity(support),
            contact.surface().piece(),contact.surface().face());
        var r=relation;
        if(r==null || r.body!=body || r.support!=support || !r.key.equals(key)
                || !authorityMatches(r) || !gravityMatches(r)
                || r.bodyTrackingGeneration!=contact.bodyTrackingGeneration()) {
            if(r!=null)reset();
            relation=new Relation(body,support,key,frame,contact.surface().localPoint());
            relation.bodyTrackingGeneration=contact.bodyTrackingGeneration();
            return;
        }
        r.confirmedPoint=contact.surface().localPoint();
        if(!accept(r,frame))reset();
    }
    /** Packet acceptance, rather than a render query, advances the endpoint pair. */
    public static void acceptedEndpoint(LivingEntity support) {
        var r=relation;
        if(r==null || r.support!=support)return;
        var frame=AnatomyClientNetworking.presentationFrame(support).orElse(null);
        if(frame==null || !accept(r,frame))reset();
    }
    private static boolean accept(Relation r,PresentationFrame frame) {
        if(!r.key.identity().equals(frame.identity()) || r.key.tracking()!=frame.trackingGeneration())return false;
        long serial=frame.after().frameSerial(),last=r.current.after().frameSerial();
        if(serial==last)return true;
        if(serial!=last+1)return false;
        if(r.pendingBefore==null)r.pendingBefore=r.current;
        r.pendingAfter=frame;
        r.current=frame;
        return true;
    }
    private static boolean gravityMatches(Relation r) {
        var body=GravityFrames.observe(r.body);
        var support=GravityFrames.observe(r.support);
        return r.bodyGravityObservation.generation()==body.generation()
            && r.supportGravityObservation.generation()==support.generation()
            && r.key.bodyGravity().equals(body.frame()) && r.key.supportGravity().equals(support.frame());
    }
    private static boolean authorityMatches(Relation r) {
        var observed=AnatomyClientNetworking.observePredictionBody();
        return observed.body()==r.body && observed.generation()==r.predictionObservation.generation();
    }
    /** Retire an accepted frame only when the physical anchor confirms no material displacement. */
    public static void quietFrame(Entity body,AnatomyMovement.ClientQuietFrameCursor cursor) {
        var r=relation;
        if(r==null || r.body!=body)return;
        if(!r.key.support().equals(cursor.support()) || !r.key.piece().equals(cursor.piece())
                || r.key.face()!=cursor.face() || r.pendingBefore==null || r.pendingAfter==null
                || r.pendingBefore.after().frameSerial()!=cursor.beforeSupportFrameSerial()
                || r.pendingAfter.after().frameSerial()!=cursor.supportFrameSerial()) {reset();return;}
        var before=r.pendingBefore.evaluated().pieces().get(r.key.piece());
        var after=r.pendingAfter.evaluated().pieces().get(r.key.piece());
        if(before==null || after==null) {reset();return;}
        Vec3 beforePoint=before.point(cursor.localPoint()),afterPoint=after.point(cursor.localPoint());
        if(beforePoint.distanceToSqr(cursor.beforeMaterialPoint())>1e-8
                || afterPoint.distanceToSqr(cursor.afterMaterialPoint())>1e-8
                || beforePoint.distanceToSqr(afterPoint)>1e-16) {reset();return;}
        r.pendingBefore=null;r.pendingAfter=null;
    }
    /** The physical callback provides the point and before/after serial actually used by carry. */
    public static void carried(Entity body,AnatomyMovement.ClientTransportCursor cursor) {
        var r=relation;
        if(r!=null && r.body==body && cursor.localTransportSequence()==r.lastTransportSequence
                && cursor.equals(r.lastCursor))return;
        if(r==null || r.body!=body || !r.key.support().equals(cursor.support())
                || !r.key.piece().equals(cursor.piece()) || r.key.face()!=cursor.face()
                || r.pendingBefore==null || r.pendingAfter==null
                || r.pendingBefore.after().frameSerial()!=cursor.beforeSupportFrameSerial()
                || r.pendingAfter.after().frameSerial()!=cursor.supportFrameSerial()
                || cursor.localTransportSequence()<=r.lastTransportSequence) {
            if(r!=null && r.body==body)reset();return;
        }
        var transport=AnatomyMovement.transport(body);
        var before=r.pendingBefore.evaluated().pieces().get(r.key.piece());
        var after=r.pendingAfter.evaluated().pieces().get(r.key.piece());
        if(transport==null || transport.sequence()!=cursor.localTransportSequence() || before==null || after==null) {reset();return;}
        Vec3 physicalPoint=cursor.localPoint();
        Vec3 beforePoint=before.point(physicalPoint),afterPoint=after.point(physicalPoint);
        if(beforePoint.distanceToSqr(cursor.beforeMaterialPoint())>1e-8
                || afterPoint.distanceToSqr(cursor.afterMaterialPoint())>1e-8) {reset();return;}
        var prior=r.draw;
        if(prior!=null && prior.bodyTick()==cursor.bodyTick()
                && !prior.oldPosition().equals(cursor.bodyOldPosition())) {reset();return;}
        boolean sameBaseline=prior!=null && prior.bodyTick()==cursor.bodyTick()
            && Objects.equals(prior.oldPosition(),cursor.bodyOldPosition());
        Vec3 supportDisplacement=(sameBaseline?prior.supportDisplacement():Vec3.ZERO)
            .add(afterPoint.subtract(beforePoint));
        Vec3 appliedCarry=(sameBaseline?prior.appliedCarry():Vec3.ZERO).add(transport.appliedDelta());
        r.draw=new DrawSample(cursor.bodyTick(),cursor.bodyOldPosition(),supportDisplacement,appliedCarry);
        r.lastTransportSequence=cursor.localTransportSequence();
        r.lastCursor=cursor;
        r.pendingBefore=null;r.pendingAfter=null;
    }
    /** Pure read: it never samples model geometry or advances a relation. */
    public static Vec3 rendererOffset(Entity body,float partial) {
        var draw=activeDraw(body);
        if(draw==null || !Float.isFinite(partial) || partial<0 || partial>1)return Vec3.ZERO;
        return draw.supportDisplacement().subtract(draw.appliedCarry().scale(partial));
    }
    /** A dual-role local actor must keep vanilla's interpolated root beneath its causal model pose. */
    public static boolean hasActiveLocalTransport(Entity body) {return activeDraw(body)!=null;}
    private static DrawSample activeDraw(Entity body) {
        var r=relation;
        var draw=r==null?null:r.draw;
        return draw!=null && r.body==body && authorityMatches(r) && r.pendingBefore==null
            && gravityMatches(r) && body.tickCount==draw.bodyTick()
            && body.oldPosition().equals(draw.oldPosition()) && body.level()==Minecraft.getInstance().level
            ?draw:null;
    }
    /** A replaced or naturally incorporated vanilla interpolation baseline is a hard camera barrier. */
    public static boolean unexpectedCameraBaseline(Entity cameraEntity) {
        var r=relation;
        var draw=r==null?null:r.draw;
        return r!=null && (!authorityMatches(r)
            || (cameraEntity!=null && cameraEntity.getRootVehicle()==r.body
                && (!gravityMatches(r) || (draw!=null && (draw.bodyTick()!=r.body.tickCount
                    || !r.body.oldPosition().equals(draw.oldPosition()))))));
    }
    public static Vec3 cameraOffset(Entity cameraEntity,float partial) {
        var r=relation;
        if(r==null || cameraEntity==null)return Vec3.ZERO;
        var owner=cameraEntity.getRootVehicle();
        return owner==r.body?rendererOffset(r.body,partial):Vec3.ZERO;
    }
}
