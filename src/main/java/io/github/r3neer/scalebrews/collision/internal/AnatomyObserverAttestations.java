package io.github.r3neer.scalebrews.collision.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Recipient-specific binding between real native position sends and recent physical carry evidence. */
public final class AnatomyObserverAttestations {
    private static final class Cursor {
        final Entity instance;
        final int bodyId;
        final long tracking;
        long transportGeneration,lastSequence,nextNonce=1,pendingNonce;
        Vec3 nativeBase;
        Cursor(Entity instance,long tracking,long transportGeneration,long lastSequence,Vec3 nativeBase) {
            this.instance=instance;this.bodyId=instance.getId();this.tracking=tracking;this.transportGeneration=transportGeneration;
            this.lastSequence=lastSequence;
            this.nativeBase=nativeBase;
        }
    }
    private static final Map<ServerPlayerConnection,Map<UUID,Cursor>> CURSORS=Collections.synchronizedMap(
        new com.google.common.collect.MapMaker().weakKeys().makeMap());
    private AnatomyObserverAttestations() {}

    /** START_TRACKING establishes a new baseline; spawn position already includes all earlier carries. */
    public static synchronized void startTracking(ServerPlayer recipient,Entity body) {
        if(recipient==null || body==null || recipient.connection==null)return;
        long generation=AnatomyRuntime.trackingGeneration(recipient,body);
        if(generation<1)return;
        var transport=AnatomyMovement.transport(body);
        long sequence=transport==null?0:transport.sequence();
        var active=CURSORS.computeIfAbsent(recipient.connection,ignored->new HashMap<>());
        if(active.size()>=TrackingGenerationLedger.MAX_ACTIVE && !active.containsKey(body.getUUID()))return;
        var prior=active.get(body.getUUID());
        // A catalog rebind does not restart vanilla's quantized position codec.
        // Preserve its recipient baseline only for the same live entity instance.
        var base=prior!=null && prior.instance==body?prior.nativeBase:body.trackingPosition();
        active.put(body.getUUID(),new Cursor(body,generation,AnatomyMovement.transportGeneration(body),sequence,base));
    }
    public static synchronized void stopTracking(ServerPlayer recipient,Entity body) {
        if(recipient==null || body==null || recipient.connection==null)return;
        var map=CURSORS.get(recipient.connection);
        if(map!=null) {map.remove(body.getUUID());if(map.isEmpty())CURSORS.remove(recipient.connection);}
    }
    public static synchronized void disconnect(ServerPlayer recipient) {
        if(recipient!=null && recipient.connection!=null)CURSORS.remove(recipient.connection);
    }
    /** Vanilla relative deltas are decoded against each recipient's last decoded target,
     * not ServerEntity's exact physical base (which loses codec rounding history). */
    public static synchronized Vec3 nativeBase(ServerPlayerConnection connection,Entity body) {
        var cursor=cursor(connection,body);
        return cursor==null?null:cursor.nativeBase;
    }
    /** Advance only after the exact native packet or certified bundle was sent. */
    public static synchronized void nativePositionSent(ServerPlayerConnection connection,Entity body,Vec3 target) {
        var cursor=cursor(connection,body);
        if(cursor!=null)cursor.nativeBase=target!=null && finite(target)?target:null;
    }
    /** A failed/unattested native send retires older carried components for that recipient. */
    public static synchronized void nativeWithoutAttestation(ServerPlayerConnection connection,Entity body) {
        var cursor=cursor(connection,body);
        if(cursor==null)return;
        var transport=AnatomyMovement.transport(body);
        if(transport!=null)cursor.lastSequence=Math.max(cursor.lastSequence,transport.sequence());
        cursor.pendingNonce=0;
    }
    public static synchronized AnatomyObserverCarryPayload prepare(ServerPlayerConnection connection,Entity body,
            AnatomyObserverCarryPayload.Kind kind,Vec3 nativeTarget) {
        var cursor=cursor(connection,body);
        if(cursor==null)return null;
        if(kind==null || nativeTarget==null || !finite(nativeTarget))return reject(connection,body);
        var recipient=connection.getPlayer();
        if(recipient==null || recipient==body || body.hasIndirectPassenger(recipient)
                || recipient.level()!=body.level() || cursor.tracking!=AnatomyRuntime.trackingGeneration(recipient,body))
            return reject(connection,body);
        var history=AnatomyObserverCarries.snapshot(body);
        if(history.isEmpty())return reject(connection,body);
        var selected=new ArrayList<AnatomyObserverCarries.Carry>();
        for(var carry:history)if(carry.transportSequence()>cursor.lastSequence)selected.add(carry);
        if(selected.isEmpty() || selected.size()>AnatomyObserverCarryPayload.MAX_CARRIES) {
            return reject(connection,body);
        }
        var first=selected.getFirst();var last=selected.getLast();
        if(first.transportSequence()!=cursor.lastSequence+1 || !sameIdentity(selected)
                || last.bodyAfter().distanceToSqr(body.position())>1e-6
                || nativeTarget.distanceToSqr(body.trackingPosition())>1e-6
                || cursor.nextNonce==Long.MAX_VALUE) {
            return reject(connection,body);
        }
        var support=recipient.level().getEntity(first.supportId());
        if(!(support instanceof LivingEntity living) || !support.getUUID().equals(first.support())) {
            return reject(connection,body);
        }
        long supportTracking=AnatomyRuntime.trackingGeneration(recipient,support);
        var descriptor=AnatomyBindingState.descriptor(living);
        if(supportTracking<1 || descriptor==null || descriptor.bindingGeneration()!=first.supportBindingGeneration()) {
            return reject(connection,body);
        }
        var marks=new ArrayList<AnatomyObserverCarryPayload.CarryMark>(selected.size());
        for(var carry:selected)marks.add(new AnatomyObserverCarryPayload.CarryMark(
            carry.materialSerial(),carry.beforeSupportFrameSerial(),carry.supportFrameSerial(),carry.transportSequence(),
            carry.piece(),carry.face(),carry.localPoint(),carry.bodyBefore(),carry.bodyAfter(),carry.appliedDelta()));
        long nonce=cursor.nextNonce++;
        cursor.pendingNonce=nonce;
        return new AnatomyObserverCarryPayload(first.epoch(),first.revision(),first.dimension(),body.getId(),body.getUUID(),
            cursor.tracking,first.supportId(),first.support(),first.supportBindingGeneration(),supportTracking,
            nonce,kind,nativeTarget,marks);
    }
    public static synchronized void sent(ServerPlayerConnection connection,Entity body,AnatomyObserverCarryPayload packet) {
        var cursor=cursor(connection,body);
        if(cursor==null || packet==null || cursor.pendingNonce!=packet.nonce())return;
        cursor.lastSequence=packet.carries().getLast().transportSequence();
        cursor.nativeBase=packet.nativeTarget();
        cursor.pendingNonce=0;
    }
    private static AnatomyObserverCarryPayload reject(ServerPlayerConnection connection,Entity body) {
        nativeWithoutAttestation(connection,body);
        return null;
    }
    private static Cursor cursor(ServerPlayerConnection connection,Entity body) {
        if(connection==null || body==null)return null;
        var map=CURSORS.get(connection);
        var cursor=map==null?null:map.get(body.getUUID());
        if(cursor==null || cursor.bodyId!=body.getId() || cursor.instance!=body)return null;
        long physicalGeneration=AnatomyMovement.transportGeneration(body);
        if(cursor.transportGeneration!=physicalGeneration) {
            cursor.transportGeneration=physicalGeneration;
            cursor.lastSequence=0;
            cursor.pendingNonce=0;
        }
        return cursor;
    }
    private static boolean sameIdentity(ArrayList<AnatomyObserverCarries.Carry> carries) {
        var first=carries.getFirst();long previous=first.transportSequence()-1;
        long previousFrame=0;
        for(var carry:carries) {
            if(carry.transportSequence()!=previous+1 || !carry.epoch().equals(first.epoch())
                    || carry.revision()!=first.revision() || !carry.dimension().equals(first.dimension())
                    || !carry.body().equals(first.body()) || carry.bodyId()!=first.bodyId()
                    || !carry.support().equals(first.support()) || carry.supportId()!=first.supportId()
                    || carry.supportBindingGeneration()!=first.supportBindingGeneration()
                    || previousFrame!=0 && carry.beforeSupportFrameSerial()!=previousFrame)return false;
            previous=carry.transportSequence();
            previousFrame=carry.supportFrameSerial();
        }
        return true;
    }
    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
