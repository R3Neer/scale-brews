package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record PlatformPayload(int body, int support, String surface, Vec3 contact, long sequence) implements CustomPacketPayload {
    public static final Type<PlatformPayload> TYPE=new Type<>(ScaleBrews.id("platform_support"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PlatformPayload> CODEC=StreamCodec.of((buf,p)->{
        buf.writeVarInt(p.body); buf.writeVarInt(p.support); buf.writeUtf(p.surface,128);
        buf.writeDouble(p.contact.x); buf.writeDouble(p.contact.y); buf.writeDouble(p.contact.z);
        buf.writeVarLong(p.sequence);
    },buf->new PlatformPayload(buf.readVarInt(),buf.readVarInt(),buf.readUtf(128),
        new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble()),buf.readVarLong()));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
