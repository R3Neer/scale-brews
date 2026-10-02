package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-issued certificate naming one Q2 material interval whose motion was
 * accepted by the authoritative interval provider. It contains identity only:
 * no geometry, pose channels, matrices, contact or movement delta.
 */
public record AnatomyMaterialIntervalPayload(UUID epoch,long revision,Identifier dimension,
        int supportId,UUID support,long bindingGeneration,long trackingGeneration,long materialSerial,
        long beforeFrameSerial,long afterFrameSerial,long beforeAuthorityTick,long afterAuthorityTick,
        long beforeJointSampleTick,long afterJointSampleTick) implements CustomPacketPayload {
    public AnatomyMaterialIntervalPayload {
        if(epoch==null || revision<0 || dimension==null || supportId<0 || support==null
                || bindingGeneration<1 || trackingGeneration<1 || materialSerial<1
                || beforeFrameSerial<1 || beforeFrameSerial==Long.MAX_VALUE
                || afterFrameSerial!=beforeFrameSerial+1
                || beforeAuthorityTick<0 || afterAuthorityTick<beforeAuthorityTick
                || beforeJointSampleTick<0 || afterJointSampleTick<beforeJointSampleTick)
            throw new IllegalArgumentException("Invalid anatomical material interval certificate");
    }

    public static final Type<AnatomyMaterialIntervalPayload> TYPE=
        new Type<>(ScaleBrews.id("anatomy_material_interval_v1"));

    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyMaterialIntervalPayload> CODEC=StreamCodec.of((buf,p)->{
        buf.writeUUID(p.epoch);buf.writeVarLong(p.revision);buf.writeIdentifier(p.dimension);
        buf.writeVarInt(p.supportId);buf.writeUUID(p.support);buf.writeVarLong(p.bindingGeneration);
        buf.writeVarLong(p.trackingGeneration);buf.writeVarLong(p.materialSerial);
        buf.writeVarLong(p.beforeFrameSerial);buf.writeVarLong(p.afterFrameSerial);
        buf.writeVarLong(p.beforeAuthorityTick);buf.writeVarLong(p.afterAuthorityTick);
        buf.writeVarLong(p.beforeJointSampleTick);buf.writeVarLong(p.afterJointSampleTick);
    },buf->new AnatomyMaterialIntervalPayload(
        buf.readUUID(),buf.readVarLong(),buf.readIdentifier(),buf.readVarInt(),buf.readUUID(),
        buf.readVarLong(),buf.readVarLong(),buf.readVarLong(),buf.readVarLong(),buf.readVarLong(),
        buf.readVarLong(),buf.readVarLong(),buf.readVarLong(),buf.readVarLong()
    ));

    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
