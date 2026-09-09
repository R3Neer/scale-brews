package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.UUID;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Authoritative pose channels, not renderer output. Identity/revision checks precede evaluation. */
public record AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
        Identifier model,Identifier provider,long tick,PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity) implements CustomPacketPayload {
    public AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,Identifier model,Identifier provider,long tick,PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale) {
        this(epoch,revision,dimension,entityId,entity,model,provider,tick,inputs,origin,yaw,scale,net.minecraft.core.Direction.DOWN);
    }
    public AnatomyPosePayload {
        if(epoch==null || entity==null || dimension==null || model==null || provider==null || inputs==null || gravity==null || revision<0 || tick<0 || entityId<0
            || origin==null || !Double.isFinite(origin.lengthSqr()) || !Float.isFinite(yaw+scale) || scale<=0 || scale>1024)
            throw new IllegalArgumentException("Invalid anatomical pose frame");
    }
    public static final Type<AnatomyPosePayload> TYPE=new Type<>(ScaleBrews.id("anatomy_pose_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyPosePayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.epoch);b.writeVarLong(p.revision);b.writeIdentifier(p.dimension);b.writeVarInt(p.entityId);b.writeUUID(p.entity);
        b.writeIdentifier(p.model);b.writeIdentifier(p.provider);b.writeVarLong(p.tick);
        b.writeFloat(p.inputs.walkPhase());b.writeFloat(p.inputs.walkAmount());b.writeFloat(p.inputs.age());b.writeFloat(p.inputs.headYaw());b.writeFloat(p.inputs.headPitch());b.writeBoolean(p.inputs.ordinary());
        b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);b.writeFloat(p.yaw);b.writeFloat(p.scale);b.writeEnum(p.gravity);
    },b->new AnatomyPosePayload(b.readUUID(),b.readVarLong(),b.readIdentifier(),b.readVarInt(),b.readUUID(),b.readIdentifier(),b.readIdentifier(),b.readVarLong(),
        new PoseProvider.Inputs(b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readFloat(),b.readEnum(net.minecraft.core.Direction.class)));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
