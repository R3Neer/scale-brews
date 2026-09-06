package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** Coordinate reference for the immediately following vanilla movement packet, not movement authority. */
public record PlatformMovePayload(int body,int support,Vec3 absolute,Vec3 local) implements CustomPacketPayload {
    public static final Type<PlatformMovePayload> TYPE=new Type<>(ScaleBrews.id("platform_move_reference"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PlatformMovePayload> CODEC=StreamCodec.of((buf,p)->{
        buf.writeVarInt(p.body); buf.writeVarInt(p.support);
        buf.writeDouble(p.absolute.x); buf.writeDouble(p.absolute.y); buf.writeDouble(p.absolute.z);
        buf.writeDouble(p.local.x); buf.writeDouble(p.local.y); buf.writeDouble(p.local.z);
    },buf->new PlatformMovePayload(buf.readVarInt(),buf.readVarInt(),
        new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble()),
        new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble())));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public boolean finite() { return Double.isFinite(absolute.lengthSqr()) && Double.isFinite(local.lengthSqr()); }
}
