package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.test.mixin.TestConnectionAccess;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.client.Minecraft;
import java.util.concurrent.TimeUnit;

/** Test-only packet delay on a real dedicated-server connection. Never packaged in the mod. */
public final class PlatformTestLatency extends ChannelDuplexHandler {
    private volatile int roundTripMillis;
    private volatile int corrections;
    public int corrections() { return corrections; }
    public static PlatformTestLatency install(Minecraft client) {
        var handler=new PlatformTestLatency();
        var channel=((TestConnectionAccess)client.getConnection().getConnection()).test$channel();
        channel.eventLoop().submit(()->channel.pipeline().addBefore("packet_handler","scalebrews_test_latency",handler)).syncUninterruptibly();
        return handler;
    }
    public void latency(int millis) { roundTripMillis=millis; }
    public void channelRead(ChannelHandlerContext ctx,Object message) {
        if(message instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket) corrections++;
        ctx.executor().schedule(()->{
            if(ctx.channel().isActive()) ctx.fireChannelRead(message); else ReferenceCountUtil.release(message);
        },roundTripMillis/2,TimeUnit.MILLISECONDS);
    }
    public void write(ChannelHandlerContext ctx,Object message,ChannelPromise promise) {
        ctx.executor().schedule(()->{
            if(ctx.channel().isActive()) ctx.writeAndFlush(message,promise);
            else { ReferenceCountUtil.release(message); promise.tryFailure(new java.nio.channels.ClosedChannelException()); }
        },roundTripMillis/2,TimeUnit.MILLISECONDS);
    }
}
