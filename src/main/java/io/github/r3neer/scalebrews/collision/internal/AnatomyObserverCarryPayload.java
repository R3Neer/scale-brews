package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.ScaleBrews;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** An occurrence-scoped server attestation for already-applied observer carry, never a movement command. */
public record AnatomyObserverCarryPayload(UUID epoch,long revision,Identifier dimension,
        int bodyId,UUID body,long bodyTrackingGeneration,int supportId,UUID support,
        long supportBindingGeneration,long supportTrackingGeneration,
        long nonce,Kind nativeKind,Vec3 nativeTarget,List<CarryMark> carries) implements CustomPacketPayload {
    public static final int MAX_CARRIES=16;
    public enum Kind {RELATIVE_POS,RELATIVE_POS_ROT,POSITION_SYNC}
    public record CarryMark(long materialSerial,long beforeSupportFrameSerial,long supportFrameSerial,long transportSequence,
            String piece,int face,Vec3 localPoint,Vec3 bodyBefore,Vec3 bodyAfter,Vec3 appliedDelta) {
        public CarryMark {
            if(materialSerial<1 || beforeSupportFrameSerial<1 || supportFrameSerial<=beforeSupportFrameSerial || transportSequence<1
                    || piece==null || piece.isBlank() || piece.length()>128 || face<0 || face>5
                    || !finite(localPoint) || !finite(bodyBefore) || !finite(bodyAfter) || !finite(appliedDelta)
                    || bodyAfter.distanceToSqr(bodyBefore.add(appliedDelta))>1e-12)
                throw new IllegalArgumentException("Invalid observer carry mark");
        }
    }
    public AnatomyObserverCarryPayload {
        if(epoch==null || revision<0 || dimension==null || bodyId<0 || body==null || bodyTrackingGeneration<1
                || supportId<0 || support==null || supportBindingGeneration<1 || supportTrackingGeneration<1
                || nonce<1 || nativeKind==null
                || !finite(nativeTarget) || carries==null || carries.isEmpty() || carries.size()>MAX_CARRIES)
            throw new IllegalArgumentException("Invalid observer carry attestation");
        carries=List.copyOf(carries);
        long previous=0;
        for(var mark:carries) {
            if(mark==null || mark.transportSequence()<=previous)throw new IllegalArgumentException("Unordered observer carries");
            previous=mark.transportSequence();
        }
    }
    public static final Type<AnatomyObserverCarryPayload> TYPE=
        new Type<>(ScaleBrews.id("anatomy_observer_carry_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyObserverCarryPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.epoch);b.writeVarLong(p.revision);b.writeIdentifier(p.dimension);
        b.writeVarInt(p.bodyId);b.writeUUID(p.body);b.writeVarLong(p.bodyTrackingGeneration);
        b.writeVarInt(p.supportId);b.writeUUID(p.support);b.writeVarLong(p.supportBindingGeneration);
        b.writeVarLong(p.supportTrackingGeneration);
        b.writeVarLong(p.nonce);b.writeEnum(p.nativeKind);write(b,p.nativeTarget);
        b.writeVarInt(p.carries.size());
        for(var mark:p.carries) {
            b.writeVarLong(mark.materialSerial);b.writeVarLong(mark.beforeSupportFrameSerial);b.writeVarLong(mark.supportFrameSerial);
            b.writeVarLong(mark.transportSequence);b.writeUtf(mark.piece,128);b.writeByte(mark.face);
            write(b,mark.localPoint);write(b,mark.bodyBefore);write(b,mark.bodyAfter);write(b,mark.appliedDelta);
        }
    },b->{
        UUID epoch=b.readUUID();long revision=b.readVarLong();Identifier dimension=b.readIdentifier();
        int bodyId=b.readVarInt();UUID body=b.readUUID();long generation=b.readVarLong();
        int supportId=b.readVarInt();UUID support=b.readUUID();long binding=b.readVarLong();long supportTracking=b.readVarLong();
        long nonce=b.readVarLong();Kind kind=b.readEnum(Kind.class);Vec3 target=read(b);
        int count=b.readVarInt();if(count<1 || count>MAX_CARRIES)throw new IllegalArgumentException("Invalid observer carry count");
        var marks=new ArrayList<CarryMark>(count);
        for(int i=0;i<count;i++)marks.add(new CarryMark(b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),
            b.readUtf(128),b.readUnsignedByte(),read(b),read(b),read(b),read(b)));
        return new AnatomyObserverCarryPayload(epoch,revision,dimension,bodyId,body,generation,supportId,support,
            binding,supportTracking,nonce,kind,target,marks);
    });
    private static void write(RegistryFriendlyByteBuf b,Vec3 point) {
        b.writeDouble(point.x);b.writeDouble(point.y);b.writeDouble(point.z);
    }
    private static Vec3 read(RegistryFriendlyByteBuf b) {
        return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());
    }
    private static boolean finite(Vec3 point) {
        return point!=null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
