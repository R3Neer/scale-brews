package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Metadata-only cursor for the immediately following vanilla movement packet.
 * The server derives the body from the connection and accepts this cursor only
 * when it names an exact server-issued transport receipt.
 */
public record AnatomyMoveReferencePayload(boolean vehicle,long transportTick,long transportSequence,long rootFrameSequence)
        implements CustomPacketPayload {
    public AnatomyMoveReferencePayload {
        if(transportTick<0 || transportSequence<1 || rootFrameSequence<0)
            throw new IllegalArgumentException("Invalid anatomical movement reference");
    }

    public static final Type<AnatomyMoveReferencePayload> TYPE=
        new Type<>(ScaleBrews.id("anatomy_move_reference_v1"));

    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyMoveReferencePayload> CODEC=StreamCodec.of((buf,payload)->{
        buf.writeBoolean(payload.vehicle);
        buf.writeVarLong(payload.transportTick);
        buf.writeVarLong(payload.transportSequence);
        buf.writeVarLong(payload.rootFrameSequence);
    },buf->new AnatomyMoveReferencePayload(
        buf.readBoolean(),
        buf.readVarLong(),
        buf.readVarLong(),
        buf.readVarLong()
    ));

    @Override
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
