package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-issued metadata for one already-applied transport receipt.
 * This carries no geometry, contact shape, pose or client-authored movement.
 */
public record AnatomyTransportReceiptPayload(UUID epoch,long revision,Identifier dimension,
        int bodyId,UUID body,long trackingGeneration,UUID support,long supportFrameSerial,
        long receiptSequence,long tick) implements CustomPacketPayload {
    public AnatomyTransportReceiptPayload {
        if(epoch==null || revision<0 || dimension==null || bodyId<0 || body==null || trackingGeneration<1
                || support==null || supportFrameSerial<1 || receiptSequence<1 || tick<0)
            throw new IllegalArgumentException("Invalid anatomical transport receipt token");
    }

    public static final Type<AnatomyTransportReceiptPayload> TYPE=
        new Type<>(ScaleBrews.id("anatomy_transport_receipt_v1"));

    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyTransportReceiptPayload> CODEC=StreamCodec.of((buf,p)->{
        buf.writeUUID(p.epoch);buf.writeVarLong(p.revision);buf.writeIdentifier(p.dimension);
        buf.writeVarInt(p.bodyId);buf.writeUUID(p.body);buf.writeVarLong(p.trackingGeneration);
        buf.writeUUID(p.support);buf.writeVarLong(p.supportFrameSerial);buf.writeVarLong(p.receiptSequence);buf.writeVarLong(p.tick);
    },buf->new AnatomyTransportReceiptPayload(
        buf.readUUID(),buf.readVarLong(),buf.readIdentifier(),buf.readVarInt(),buf.readUUID(),buf.readVarLong(),
        buf.readUUID(),buf.readVarLong(),buf.readVarLong(),buf.readVarLong()
    ));

    @Override
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
