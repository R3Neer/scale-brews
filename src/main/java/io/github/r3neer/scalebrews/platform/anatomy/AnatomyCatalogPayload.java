package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.UUID;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-to-client only, bounded fragments; never accepts a player's geometry on the server. */
public record AnatomyCatalogPayload(UUID epoch,long revision,int index,int count,int totalBytes,String digest,byte[] fragment) implements CustomPacketPayload {
    public static final int CHUNK=16384,MAX_BYTES=16*1024*1024;
    public AnatomyCatalogPayload {
        if(epoch==null || revision<0 || totalBytes<1 || totalBytes>MAX_BYTES || count!=(totalBytes+CHUNK-1)/CHUNK || index<0 || index>=count
            || digest==null || !digest.matches("[0-9a-f]{64}") || fragment==null || fragment.length!=Math.min(CHUNK,totalBytes-index*CHUNK))
            throw new IllegalArgumentException("Invalid anatomy catalog fragment");
        fragment=fragment.clone();
    }
    @Override public byte[] fragment(){return fragment.clone();}
    public static final Type<AnatomyCatalogPayload> TYPE=new Type<>(ScaleBrews.id("anatomy_catalog_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyCatalogPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.epoch);b.writeVarLong(p.revision);b.writeVarInt(p.index);b.writeVarInt(p.count);b.writeVarInt(p.totalBytes);b.writeUtf(p.digest,64);b.writeByteArray(p.fragment);
    },b->new AnatomyCatalogPayload(b.readUUID(),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(64),b.readByteArray(CHUNK)));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
