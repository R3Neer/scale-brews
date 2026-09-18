package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Metadata-only cursor for the immediately following vanilla movement packet.
 * The server derives the body from the connection and resolves this server-issued
 * support endpoint only through an already-confirmed recipient/body receipt.
 */
public record AnatomyMoveReferencePayload(boolean vehicle,UUID support,long supportFrameSerial)
        implements CustomPacketPayload {
    public AnatomyMoveReferencePayload {
        if(support==null || supportFrameSerial<1)
            throw new IllegalArgumentException("Invalid anatomical movement reference");
    }

    public static final Type<AnatomyMoveReferencePayload> TYPE=
        new Type<>(ScaleBrews.id("anatomy_move_reference_v1"));

    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyMoveReferencePayload> CODEC=StreamCodec.of((buf,payload)->{
        buf.writeBoolean(payload.vehicle);
        buf.writeUUID(payload.support);
        buf.writeVarLong(payload.supportFrameSerial);
    },buf->new AnatomyMoveReferencePayload(
        buf.readBoolean(),
        buf.readUUID(),
        buf.readVarLong()
    ));

    @Override
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
