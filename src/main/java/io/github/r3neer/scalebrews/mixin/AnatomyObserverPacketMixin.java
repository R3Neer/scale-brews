package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.scalebrews.collision.internal.AnatomyObserverAttestations;
import io.github.r3neer.scalebrews.collision.internal.AnatomyObserverCarryPayload;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Binds an observer certificate to the exact vanilla tracking send and recipient. */
@Mixin(targets="net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class AnatomyObserverPacketMixin {
    @Shadow @Final private Entity entity;

    @WrapOperation(method="sendToTrackingPlayers",at=@At(value="INVOKE",
        target="Lnet/minecraft/server/network/ServerPlayerConnection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void scalebrews$attestObserverCarry(ServerPlayerConnection receiver,Packet<? super ClientGamePacketListener> nativePacket,
            Operation<Void> original) {
        var kind=scalebrews$kind(nativePacket);
        var target=scalebrews$target(receiver,nativePacket,kind);
        if(kind==null || target==null) {
            // A teleport replaces the native baseline. Carries that were not sent before
            // it must never be attached to a later relative position update.
            if(nativePacket instanceof ClientboundTeleportEntityPacket teleport && teleport.id()==entity.getId())
                AnatomyObserverAttestations.nativeWithoutAttestation(receiver,entity);
            else if(kind!=null)AnatomyObserverAttestations.nativeWithoutAttestation(receiver,entity);
            original.call(receiver,nativePacket);
            if(nativePacket instanceof ClientboundTeleportEntityPacket teleport && teleport.id()==entity.getId())
                AnatomyObserverAttestations.nativePositionSent(receiver,entity,
                    teleport.relatives().isEmpty()?teleport.change().position():null);
            else if(kind!=null)AnatomyObserverAttestations.nativePositionSent(receiver,entity,null);
            return;
        }
        var recipient=receiver.getPlayer();
        var base=AnatomyObserverAttestations.nativeBase(receiver,entity);
        if(recipient==null || base!=null && target.distanceToSqr(base)<=1e-16
                || !ServerPlayNetworking.canSend(recipient,AnatomyObserverCarryPayload.TYPE)) {
            AnatomyObserverAttestations.nativeWithoutAttestation(receiver,entity);
            original.call(receiver,nativePacket);
            AnatomyObserverAttestations.nativePositionSent(receiver,entity,target);
            return;
        }
        var attestation=AnatomyObserverAttestations.prepare(receiver,entity,kind,target);
        if(attestation==null) {
            original.call(receiver,nativePacket);
            AnatomyObserverAttestations.nativePositionSent(receiver,entity,target);
            return;
        }
        var bundle=new ClientboundBundlePacket(List.<Packet<? super ClientGamePacketListener>>of(
            nativePacket,new ClientboundCustomPayloadPacket(attestation)));
        try {
            original.call(receiver,bundle);
            AnatomyObserverAttestations.sent(receiver,entity,attestation);
        } catch(RuntimeException failure) {
            AnatomyObserverAttestations.nativeWithoutAttestation(receiver,entity);
            throw failure;
        }
    }

    private AnatomyObserverCarryPayload.Kind scalebrews$kind(Packet<?> packet) {
        if(packet instanceof ClientboundMoveEntityPacket move && move.hasPosition()
                && move.getEntity(entity.level())==entity)
            return move.hasRotation()?AnatomyObserverCarryPayload.Kind.RELATIVE_POS_ROT
                :AnatomyObserverCarryPayload.Kind.RELATIVE_POS;
        if(packet instanceof ClientboundEntityPositionSyncPacket sync && sync.id()==entity.getId())
            return AnatomyObserverCarryPayload.Kind.POSITION_SYNC;
        return null;
    }

    private Vec3 scalebrews$target(ServerPlayerConnection receiver,Packet<?> packet,AnatomyObserverCarryPayload.Kind kind) {
        if(kind==null)return null;
        if(packet instanceof ClientboundEntityPositionSyncPacket sync)return sync.values().position();
        if(packet instanceof ClientboundMoveEntityPacket move) {
            var base=AnatomyObserverAttestations.nativeBase(receiver,entity);
            if(base==null)return null;
            var codec=new VecDeltaCodec();
            codec.setBase(base);
            return codec.decode(move.getXa(),move.getYa(),move.getZa());
        }
        return null;
    }
}
