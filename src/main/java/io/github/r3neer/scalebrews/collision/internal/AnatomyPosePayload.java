package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.pose.PoseProvider;

import java.util.UUID;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Authoritative pose channels, not renderer output. Identity/revision checks precede evaluation. */
public record AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
        Identifier model,Identifier provider,long frameSerial,long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick,long bindingGeneration,boolean available,
        PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity) implements CustomPacketPayload {
    /** Fixture/source compatibility only. Production uses the causal v4 constructor. */
    public AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,Identifier model,Identifier provider,long tick,PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale) {
        this(epoch,revision,dimension,entityId,entity,model,provider,tick,tick,tick,tick,tick,1,true,inputs,origin,yaw,scale,net.minecraft.core.Direction.DOWN);
    }
    /** Fixture/source compatibility only. Production uses the causal v4 constructor. */
    public AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,Identifier model,Identifier provider,long tick,PoseProvider.Inputs inputs,Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity) {
        this(epoch,revision,dimension,entityId,entity,model,provider,tick,tick,tick,tick,tick,1,true,inputs,origin,yaw,scale,gravity);
    }
    public AnatomyPosePayload {
        if(epoch==null || entity==null || dimension==null || model==null || provider==null || inputs==null || gravity==null || revision<0 || frameSerial<1 || authorityTick<0
            || jointSampleTick<0 || rootFrameSequence<0 || rootFrameTick<0 || bindingGeneration<1 || entityId<0
            || origin==null || !Double.isFinite(origin.lengthSqr()) || !Float.isFinite(yaw+scale) || scale<=0 || scale>1024)
            throw new IllegalArgumentException("Invalid anatomical pose frame");
    }
    /** Incompatible with v3: every frame now has independently ordered joint/root provenance. */
    public static final Type<AnatomyPosePayload> TYPE=new Type<>(ScaleBrews.id("anatomy_pose_v4"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyPosePayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.epoch);b.writeVarLong(p.revision);b.writeIdentifier(p.dimension);b.writeVarInt(p.entityId);b.writeUUID(p.entity);
        b.writeIdentifier(p.model);b.writeIdentifier(p.provider);b.writeVarLong(p.frameSerial);b.writeVarLong(p.authorityTick);b.writeVarLong(p.jointSampleTick);
        b.writeVarLong(p.rootFrameSequence);b.writeVarLong(p.rootFrameTick);b.writeVarLong(p.bindingGeneration);b.writeBoolean(p.available);
        b.writeFloat(p.inputs.walkPhase());b.writeFloat(p.inputs.walkAmount());b.writeFloat(p.inputs.age());b.writeFloat(p.inputs.headYaw());b.writeFloat(p.inputs.headPitch());b.writeBoolean(p.inputs.ordinary());
        b.writeVarInt(p.inputs.channels().size());p.inputs.channels().forEach((name,value)->{b.writeUtf(name,64);b.writeFloat(value);});
        b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);b.writeFloat(p.yaw);b.writeFloat(p.scale);b.writeEnum(p.gravity);
    },b->{
        var epoch=b.readUUID();long revision=b.readVarLong();var dimension=b.readIdentifier();int entityId=b.readVarInt();var entity=b.readUUID();
        var model=b.readIdentifier();var provider=b.readIdentifier();long frameSerial=b.readVarLong(),authorityTick=b.readVarLong(),jointSampleTick=b.readVarLong(),rootFrameSequence=b.readVarLong(),rootFrameTick=b.readVarLong(),bindingGeneration=b.readVarLong();boolean available=b.readBoolean();float walkPhase=b.readFloat(),walkAmount=b.readFloat(),age=b.readFloat(),headYaw=b.readFloat(),headPitch=b.readFloat();boolean ordinary=b.readBoolean();
        int count=b.readVarInt();if(count<0 || count>64)throw new IllegalArgumentException("Invalid pose channel count");
        var channels=new java.util.TreeMap<String,Float>();for(int i=0;i<count;i++)if(channels.put(b.readUtf(64),b.readFloat())!=null)throw new IllegalArgumentException("Duplicate pose channel");
        return new AnatomyPosePayload(epoch,revision,dimension,entityId,entity,model,provider,frameSerial,authorityTick,jointSampleTick,rootFrameSequence,rootFrameTick,bindingGeneration,available,new PoseProvider.Inputs(walkPhase,walkAmount,age,headYaw,headPitch,ordinary,channels),
            new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readFloat(),b.readEnum(net.minecraft.core.Direction.class));
    });
    /** Joint history clock; root-only updates may repeat it while frame serial advances. */
    @Deprecated public long tick(){return jointSampleTick;}
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
