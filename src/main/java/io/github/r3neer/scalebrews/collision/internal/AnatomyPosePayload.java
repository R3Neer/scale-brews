package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import io.github.r3neer.scalebrews.collision.api.spi.RootTransformProvider;
import java.util.UUID;
import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** Authoritative joint channels plus an independently transported root DTO and recipient tracking generation. */
public record AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
        Identifier model,Identifier provider,Identifier rootProvider,long frameSerial,long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick,
        long bindingGeneration,long trackingGeneration,boolean available,PoseEngine.Inputs inputs,Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity,
        RootTransformProvider.RootTransform rootTransform) implements CustomPacketPayload {
    /** Fixture/source compatibility only. Production uses the causal constructor. */
    public AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,Identifier model,Identifier provider,long tick,PoseEngine.Inputs inputs,Vec3 origin,float yaw,float scale) {
        this(epoch,revision,dimension,entityId,entity,model,provider,GeometryProvider.DEFAULT_ROOT_PROVIDER,tick,tick,tick,tick,tick,1,1,true,inputs,origin,yaw,scale,
            net.minecraft.core.Direction.DOWN,legacyRoot(origin,yaw,scale,net.minecraft.core.Direction.DOWN));
    }
    /** Fixture/source compatibility only. Production uses the causal constructor. */
    public AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,Identifier model,Identifier provider,long tick,PoseEngine.Inputs inputs,Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity) {
        this(epoch,revision,dimension,entityId,entity,model,provider,GeometryProvider.DEFAULT_ROOT_PROVIDER,tick,tick,tick,tick,tick,1,1,true,inputs,origin,yaw,scale,gravity,
            legacyRoot(origin,yaw,scale,gravity));
    }
    /** Source-compatible pre-S21 causal constructor; it carries the exact former entity-root authority. */
    public AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
            Identifier model,Identifier provider,long frameSerial,long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick,long bindingGeneration,boolean available,
            PoseEngine.Inputs inputs,Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity) {
        this(epoch,revision,dimension,entityId,entity,model,provider,GeometryProvider.DEFAULT_ROOT_PROVIDER,frameSerial,authorityTick,jointSampleTick,
            rootFrameSequence,rootFrameTick,bindingGeneration,1,available,inputs,origin,yaw,scale,gravity,legacyRoot(origin,yaw,scale,gravity));
    }
    /** Source-compatible pre-S24 constructor. Tracking generation 1 is fixture-only; live sends provide it explicitly. */
    public AnatomyPosePayload(UUID epoch,long revision,Identifier dimension,int entityId,UUID entity,
            Identifier model,Identifier provider,Identifier rootProvider,long frameSerial,long authorityTick,long jointSampleTick,long rootFrameSequence,long rootFrameTick,
            long bindingGeneration,boolean available,PoseEngine.Inputs inputs,Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity,
            RootTransformProvider.RootTransform rootTransform) {
        this(epoch,revision,dimension,entityId,entity,model,provider,rootProvider,frameSerial,authorityTick,jointSampleTick,rootFrameSequence,rootFrameTick,
            bindingGeneration,1,available,inputs,origin,yaw,scale,gravity,rootTransform);
    }
    public AnatomyPosePayload {
        if(epoch==null || entity==null || dimension==null || model==null || provider==null || rootProvider==null || inputs==null || gravity==null || rootTransform==null
            || revision<0 || frameSerial<1 || authorityTick<0 || jointSampleTick<0 || jointSampleTick>authorityTick || rootFrameSequence<0 || rootFrameTick<0
            || rootFrameTick>authorityTick || bindingGeneration<1 || trackingGeneration<1 || entityId<0 || origin==null || !Double.isFinite(origin.lengthSqr())
            || !Float.isFinite(yaw+scale) || scale<=0 || scale>1024 || !rootTransform.origin().equals(origin) || Float.compare(rootTransform.scale(),scale)!=0)
            throw new IllegalArgumentException("Invalid anatomical pose frame");
    }
    /** S24 pose/root packet. Catalog protocol versioning remains independent. */
    public static final Type<AnatomyPosePayload> TYPE=new Type<>(ScaleBrews.id("anatomy_pose_v6"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AnatomyPosePayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.epoch);b.writeVarLong(p.revision);b.writeIdentifier(p.dimension);b.writeVarInt(p.entityId);b.writeUUID(p.entity);
        b.writeIdentifier(p.model);b.writeIdentifier(p.provider);b.writeIdentifier(p.rootProvider);b.writeVarLong(p.frameSerial);b.writeVarLong(p.authorityTick);b.writeVarLong(p.jointSampleTick);
        b.writeVarLong(p.rootFrameSequence);b.writeVarLong(p.rootFrameTick);b.writeVarLong(p.bindingGeneration);b.writeVarLong(p.trackingGeneration);b.writeBoolean(p.available);
        b.writeFloat(p.inputs.walkPhase());b.writeFloat(p.inputs.walkAmount());b.writeFloat(p.inputs.age());b.writeFloat(p.inputs.headYaw());b.writeFloat(p.inputs.headPitch());b.writeBoolean(p.inputs.ordinary());
        b.writeVarInt(p.inputs.channels().size());p.inputs.channels().forEach((name,value)->{b.writeUtf(name,64);b.writeFloat(value);});
        b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);b.writeFloat(p.yaw);b.writeFloat(p.scale);b.writeEnum(p.gravity);
        var root=p.rootTransform;var q=root.quaternion();
        b.writeDouble(root.origin().x);b.writeDouble(root.origin().y);b.writeDouble(root.origin().z);
        b.writeFloat(q.x);b.writeFloat(q.y);b.writeFloat(q.z);b.writeFloat(q.w);b.writeFloat(root.scale());
    },b->{
        var epoch=b.readUUID();long revision=b.readVarLong();var dimension=b.readIdentifier();int entityId=b.readVarInt();var entity=b.readUUID();
        var model=b.readIdentifier();var provider=b.readIdentifier();var rootProvider=b.readIdentifier();
        long frameSerial=b.readVarLong(),authorityTick=b.readVarLong(),jointSampleTick=b.readVarLong(),rootFrameSequence=b.readVarLong(),rootFrameTick=b.readVarLong(),bindingGeneration=b.readVarLong(),trackingGeneration=b.readVarLong();boolean available=b.readBoolean();
        float walkPhase=b.readFloat(),walkAmount=b.readFloat(),age=b.readFloat(),headYaw=b.readFloat(),headPitch=b.readFloat();boolean ordinary=b.readBoolean();
        int count=b.readVarInt();if(count<0 || count>64)throw new IllegalArgumentException("Invalid pose channel count");
        var channels=new java.util.TreeMap<String,Float>();for(int i=0;i<count;i++)if(channels.put(b.readUtf(64),b.readFloat())!=null)throw new IllegalArgumentException("Duplicate pose channel");
        var origin=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());float yaw=b.readFloat(),scale=b.readFloat();var gravity=b.readEnum(net.minecraft.core.Direction.class);
        var rootOrigin=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());
        var rootTransform=new RootTransformProvider.RootTransform(rootOrigin,new Quaternionf(b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat()),b.readFloat());
        return new AnatomyPosePayload(epoch,revision,dimension,entityId,entity,model,provider,rootProvider,frameSerial,authorityTick,jointSampleTick,rootFrameSequence,rootFrameTick,
            bindingGeneration,trackingGeneration,available,new PoseEngine.Inputs(walkPhase,walkAmount,age,headYaw,headPitch,ordinary,channels),origin,yaw,scale,gravity,rootTransform);
    });
    private static RootTransformProvider.RootTransform legacyRoot(Vec3 origin,float yaw,float scale,net.minecraft.core.Direction gravity) {
        if(origin==null || gravity==null)throw new IllegalArgumentException("Missing legacy root authority");
        Quaternionf rotation=new Quaternionf().setFromNormalized(new Matrix3f(new GravityFrame(gravity).matrix()))
            .rotateY((float)Math.toRadians(180.0-yaw));
        return new RootTransformProvider.RootTransform(origin,rotation,scale);
    }
    /** Joint history clock; root-only updates may repeat it while frame serial advances. */
    @Deprecated public long tick(){return jointSampleTick;}
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
