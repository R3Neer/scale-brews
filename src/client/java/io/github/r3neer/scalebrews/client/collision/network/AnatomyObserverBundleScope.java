package io.github.r3neer.scalebrews.client.collision.network;

import io.github.r3neer.scalebrews.client.collision.presentation.CausalRemoteResidual;
import io.github.r3neer.scalebrews.client.mixin.AnatomyInterpolationAccess;
import io.github.r3neer.scalebrews.collision.internal.AnatomyObserverCarryPayload;
import java.util.Objects;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.phys.Vec3;

/** Packet-object scope, independent of Fabric custom-payload callback scheduling. */
public final class AnatomyObserverBundleScope {
    public record NativeState(Vec3 bodyPosition,Vec3 codecBase,boolean interpolating,
            Vec3 interpolationTarget,float targetYRot,float targetXRot,int interpolationLength) {
        public static NativeState capture(Entity body) {
            if(body==null)return null;
            InterpolationHandler handler=body.getInterpolation();
            return new NativeState(body.position(),body.getPositionCodec().getBase(),
                handler!=null && handler.hasActiveInterpolation(),handler==null?null:handler.position(),
                handler==null?0:handler.yRot(),handler==null?0:handler.xRot(),
                handler==null?0:((AnatomyInterpolationAccess)handler).scalebrews$interpolationLength());
        }
    }
    public record NativeEvent(ClientPacketListener listener,Entity body,Packet<?> packet,
            AnatomyObserverCarryPayload.Kind kind,Vec3 decodedTarget,NativeState before,NativeState after) {
        public boolean restarted() {
            return before!=null && after!=null && after.interpolating()
                && (!before.interpolating() || !Objects.equals(before.interpolationTarget(),after.interpolationTarget())
                    || before.targetYRot()!=after.targetYRot() || before.targetXRot()!=after.targetXRot());
        }
    }
    private static final ThreadLocal<Scope> ACTIVE=new ThreadLocal<>();
    private AnatomyObserverBundleScope() {}

    public static final class Scope {
        private final ClientPacketListener listener;
        private final Packet<?> nativePacket;
        private final AnatomyObserverCarryPayload attestation;
        private NativeEvent event;
        private int handled;
        private Scope(ClientPacketListener listener,Packet<?> nativePacket,AnatomyObserverCarryPayload attestation) {
            this.listener=listener;this.nativePacket=nativePacket;this.attestation=attestation;
        }
    }
    public static Scope open(ClientPacketListener listener,ClientboundBundlePacket bundle) {
        if(ACTIVE.get()!=null || listener==null || bundle==null)return null;
        var packets=bundle.subPackets().iterator();
        if(!packets.hasNext())return null;
        var first=packets.next();
        if(!packets.hasNext())return null;
        var second=packets.next();
        if(packets.hasNext() || !(second instanceof ClientboundCustomPayloadPacket custom)
                || !(custom.payload() instanceof AnatomyObserverCarryPayload attestation)
                || !(first instanceof ClientboundMoveEntityPacket || first instanceof ClientboundEntityPositionSyncPacket))return null;
        var scope=new Scope(listener,first,attestation);
        ACTIVE.set(scope);
        return scope;
    }
    public static void handled(NativeEvent event) {
        if(event==null || event.body()==null)return;
        var scope=ACTIVE.get();
        if(scope!=null && scope.listener==event.listener() && scope.nativePacket==event.packet()) {
            scope.handled++;
            if(scope.handled==1)scope.event=event;
            return;
        }
        CausalRemoteResidual.unattested(event);
    }
    public static void close(Scope scope,boolean completed) {
        if(scope==null)return;
        if(ACTIVE.get()==scope)ACTIVE.remove();
        if(!completed || scope.event==null)return;
        var event=scope.event;
        if(scope.handled==1 && event.packet()==scope.nativePacket && event.kind()==scope.attestation.nativeKind()
                && event.decodedTarget()!=null && event.decodedTarget().distanceToSqr(scope.attestation.nativeTarget())<=1e-12
                && event.after()!=null && event.after().codecBase().distanceToSqr(event.decodedTarget())<=1e-12)
            CausalRemoteResidual.attested(event,scope.attestation);
        else CausalRemoteResidual.unattested(event);
    }
}
