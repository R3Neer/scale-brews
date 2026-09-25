package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * V2 movement reference: returns only a server-issued receipt token.
 * The server derives player/controlled-body identity from the connection.
 */
public record AnatomyMoveReferenceV2Payload(boolean vehicle,long receiptSequence)
        implements CustomPacketPayload {
    public AnatomyMoveReferenceV2Payload {
        if(receiptSequence<1)throw new IllegalArgumentException("Invalid anatomical receipt reference");
    }

    public static final Type<AnatomyMoveReferenceV2Payload> TYPE=
        new Type<>(ScaleBrews.id("anatomy_move_reference_v2"));

    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyMoveReferenceV2Payload> CODEC=StreamCodec.of((buf,p)->{
        buf.writeBoolean(p.vehicle);buf.writeVarLong(p.receiptSequence);
    },buf->new AnatomyMoveReferenceV2Payload(buf.readBoolean(),buf.readVarLong()));

    @Override
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
