package io.github.r3neer.scalebrews.platform.anatomy;

import io.github.r3neer.scalebrews.ScaleBrews;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Server-confirmed contact identity. Geometry remains server-owned through the catalog revision. */
public record AnatomyContactPayload(UUID epoch,long revision,Identifier dimension,int bodyId,UUID body,long trackingGeneration,long sequence,long tick,
        int supportId,UUID support,String piece,int face,Vec3 localPoint,Vec3 normal) implements CustomPacketPayload {
    public AnatomyContactPayload {
        if(epoch==null || dimension==null || body==null || revision<0 || trackingGeneration<1 || sequence<0 || tick<0 || bodyId<0)
            throw new IllegalArgumentException("Invalid anatomical contact identity");
        boolean clear=supportId<0;
        if(clear) {
            if(support!=null || piece!=null || localPoint!=null || normal!=null || face!=-1)
                throw new IllegalArgumentException("Clear contact carries surface data");
        } else if(support==null || piece==null || piece.isBlank() || face<0 || face>5 || localPoint==null || normal==null
                || !Double.isFinite(localPoint.lengthSqr()) || !Double.isFinite(normal.lengthSqr()) || normal.lengthSqr()<.99 || normal.lengthSqr()>1.01) {
            throw new IllegalArgumentException("Invalid anatomical contact surface");
        }
    }
    public static AnatomyContactPayload clear(UUID epoch,long revision,Identifier dimension,int bodyId,UUID body,long trackingGeneration,long sequence,long tick) {
        return new AnatomyContactPayload(epoch,revision,dimension,bodyId,body,trackingGeneration,sequence,tick,-1,null,null,-1,null,null);
    }
    public boolean present(){return supportId>=0;}
    public static final Type<AnatomyContactPayload> TYPE=new Type<>(ScaleBrews.id("anatomy_contact_v5"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyContactPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.epoch);b.writeVarLong(p.revision);b.writeIdentifier(p.dimension);b.writeVarInt(p.bodyId);b.writeUUID(p.body);
        b.writeVarLong(p.trackingGeneration);b.writeVarLong(p.sequence);b.writeVarLong(p.tick);b.writeBoolean(p.present());
        if(p.present()) {
            b.writeVarInt(p.supportId);b.writeUUID(p.support);b.writeUtf(p.piece,512);b.writeByte(p.face);
            write(b,p.localPoint);write(b,p.normal);
        }
    },b->{
        UUID epoch=b.readUUID();long revision=b.readVarLong();var dimension=b.readIdentifier();int bodyId=b.readVarInt();UUID body=b.readUUID();
        long generation=b.readVarLong(),sequence=b.readVarLong(),tick=b.readVarLong();
        if(!b.readBoolean())return clear(epoch,revision,dimension,bodyId,body,generation,sequence,tick);
        return new AnatomyContactPayload(epoch,revision,dimension,bodyId,body,generation,sequence,tick,b.readVarInt(),b.readUUID(),b.readUtf(512),b.readUnsignedByte(),read(b),read(b));
    });
    private static void write(RegistryFriendlyByteBuf b,Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
    private static Vec3 read(RegistryFriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
