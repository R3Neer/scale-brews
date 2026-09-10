package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.test.mixin.TestConnectionAccess;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;

/** Test-only FIFO packet delay on one real dedicated-server connection. Never packaged in the mod. */
public final class PlatformTestLatency extends ChannelDuplexHandler {
    public enum Flow { CLIENT_TO_SERVER, SERVER_TO_CLIENT }
    /** Actual Netty observation; it never claims a vanilla movement was accepted. */
    public record PacketTrace(long sequence,long flowSequence,Flow flow,String packetType,String detail,int configuredRttMillis,long observedNanos,long scheduledDelayNanos) {}
    /** A per-direction delivery-prefix boundary captured atomically on Netty's event loop. */
    public record Fence(long globalSequence,long inboundSequence,long outboundSequence) {}
    public record Baseline(long playerMoves,long vehicleMoves,long playerCorrections,long vehicleCorrections,
            long pendingInbound,long pendingOutbound,long droppedTraceEvents,long peakTraceEntries,Map<String,Long> packetTotals,List<PacketTrace> trace) {}

    private static final int MAX_RELEVANT_TRACE=512;
    private final AtomicInteger playerCorrections=new AtomicInteger();
    private final AtomicInteger vehicleCorrections=new AtomicInteger();
    private final AtomicLong playerMoves=new AtomicLong();
    private final AtomicLong vehicleMoves=new AtomicLong();
    private final AtomicLong eventSequence=new AtomicLong();
    private final AtomicLong droppedTraceEvents=new AtomicLong();
    private final AtomicLong peakTraceEntries=new AtomicLong();
    private final AtomicLong pendingInbound=new AtomicLong();
    private final AtomicLong pendingOutbound=new AtomicLong();
    private final AtomicLong inboundSequence=new AtomicLong();
    private final AtomicLong outboundSequence=new AtomicLong();
    /** Prefixes, never maxima of independently scheduled deliveries. */
    private final AtomicLong deliveredInbound=new AtomicLong();
    private final AtomicLong deliveredOutbound=new AtomicLong();
    // The following structures are owned exclusively by the channel event loop.
    private final ArrayDeque<PacketTrace> relevantTrace=new ArrayDeque<>();
    private final Map<String,Long> packetTotals=new LinkedHashMap<>();
    private final FlowQueue inbound=new FlowQueue(Flow.SERVER_TO_CLIENT);
    private final FlowQueue outbound=new FlowQueue(Flow.CLIENT_TO_SERVER);
    private final AtomicBoolean closed=new AtomicBoolean();
    private volatile Channel channel;
    private int roundTripMillis;

    public int corrections(){return playerCorrections.get()+vehicleCorrections.get();}
    public int playerCorrections(){return playerCorrections.get();}
    public int vehicleCorrections(){return vehicleCorrections.get();}

    public static PlatformTestLatency install(Minecraft client) {
        var handler=new PlatformTestLatency();
        var channel=((TestConnectionAccess)client.getConnection().getConnection()).test$channel();
        channel.eventLoop().submit(()->{
            channel.pipeline().addBefore("packet_handler","scalebrews_test_latency",handler);
            handler.channel=channel;
        }).syncUninterruptibly();
        return handler;
    }

    /** Event-loop barrier: only later observations use this RTT. */
    public void latency(int millis) {
        if(millis<0)throw new IllegalArgumentException("RTT must be non-negative");
        onEventLoop(()->roundTripMillis=millis);
    }

    /** Event-loop barrier after setup traffic; already delayed packets remain accounted as pending. */
    public void resetBaseline() {
        onEventLoop(()->{
            playerCorrections.set(0);vehicleCorrections.set(0);playerMoves.set(0);vehicleMoves.set(0);
            eventSequence.set(0);droppedTraceEvents.set(0);peakTraceEntries.set(0);relevantTrace.clear();packetTotals.clear();
        });
    }

    public Baseline baseline() {
        return callOnEventLoop(()->new Baseline(playerMoves.get(),vehicleMoves.get(),playerCorrections.get(),vehicleCorrections.get(),
            pendingInbound.get(),pendingOutbound.get(),droppedTraceEvents.get(),peakTraceEntries.get(),Map.copyOf(packetTotals),List.copyOf(relevantTrace)));
    }

    /** Capture observed input/output prefixes atomically without injecting traffic. */
    public Fence fence(){return callOnEventLoop(()->new Fence(eventSequence.get(),inboundSequence.get(),outboundSequence.get()));}
    /** True only after both strict delivery prefixes reached the captured frontier. */
    public boolean passed(Fence fence){return fence!=null && deliveredInbound.get()>=fence.inboundSequence() && deliveredOutbound.get()>=fence.outboundSequence();}
    public boolean quiescent(){return pendingInbound.get()==0 && pendingOutbound.get()==0;}

    /** Remove the handler and release queued packets if fixture cleanup begins. */
    public void close(Minecraft client) {
        if(!closed.compareAndSet(false,true))return;
        var current=channel;if(current==null)return;
        onEventLoop(()->{
            drain(inbound);drain(outbound);
            if(current.pipeline().context(this)!=null)current.pipeline().remove(this);
        });
    }

    @Override public void channelRead(ChannelHandlerContext ctx,Object message) {
        if(closed.get()){ctx.fireChannelRead(message);return;}
        if(message instanceof ClientboundPlayerPositionPacket)playerCorrections.incrementAndGet();
        if(message.getClass().getSimpleName().equals("ClientboundMoveVehiclePacket"))vehicleCorrections.incrementAndGet();
        long flowSequence=inboundSequence.incrementAndGet(),due=reserve(inbound);
        observe(inbound,message,flowSequence,due);
        enqueue(inbound,new DelayedPacket(ctx,message,null,flowSequence,due));
    }

    @Override public void write(ChannelHandlerContext ctx,Object message,ChannelPromise promise) {
        if(closed.get()){ctx.write(message,promise);return;}
        if(message instanceof ServerboundMovePlayerPacket)playerMoves.incrementAndGet();
        if(message instanceof ServerboundMoveVehiclePacket)vehicleMoves.incrementAndGet();
        long flowSequence=outboundSequence.incrementAndGet(),due=reserve(outbound);
        observe(outbound,message,flowSequence,due);
        enqueue(outbound,new DelayedPacket(ctx,message,promise,flowSequence,due));
    }

    private void observe(FlowQueue flow,Object message,long flowSequence,long dueNanos) {
        long sequence=eventSequence.incrementAndGet();String type=message.getClass().getName();
        packetTotals.merge(flow.flow+":"+type,1L,Long::sum);
        if(!relevant(message))return;
        if(relevantTrace.size()==MAX_RELEVANT_TRACE){relevantTrace.removeFirst();droppedTraceEvents.incrementAndGet();}
        relevantTrace.addLast(new PacketTrace(sequence,flowSequence,flow.flow,type,detail(message),roundTripMillis,System.nanoTime(),Math.max(0,dueNanos-System.nanoTime())));
        peakTraceEntries.accumulateAndGet(relevantTrace.size(),Math::max);
    }

    private static boolean relevant(Object message) {
        return message instanceof ServerboundMovePlayerPacket || message instanceof ServerboundMoveVehiclePacket
            || message instanceof ClientboundPlayerPositionPacket || message.getClass().getSimpleName().equals("ClientboundMoveVehiclePacket");
    }

    /** Positions are diagnostic-only and make a vehicle correction causal in the N2 trace. */
    private static String detail(Object message) {
        if(message instanceof ServerboundMoveVehiclePacket packet)
            return "target="+packet.position()+",yaw="+packet.yRot()+",pitch="+packet.xRot()+",ground="+packet.onGround();
        if(message instanceof ClientboundMoveVehiclePacket packet)
            return "correction="+packet.position()+",yaw="+packet.yRot()+",pitch="+packet.xRot();
        return "";
    }

    /** Reserve due time now; a single queue pump, not same-time timers, enforces FIFO delivery. */
    private long reserve(FlowQueue queue) {
        long now=System.nanoTime(),half=TimeUnit.MILLISECONDS.toNanos(roundTripMillis)/2;
        long due=Math.max(now+half,queue.notBeforeNanos);queue.notBeforeNanos=due;return due;
    }

    private void enqueue(FlowQueue queue,DelayedPacket packet) {
        queue.entries.addLast(packet);
        if(queue.flow==Flow.SERVER_TO_CLIENT)pendingInbound.incrementAndGet();else pendingOutbound.incrementAndGet();
        pump(queue);
    }

    /** Exactly one pending timer per direction: equal deadlines retain insertion order. */
    private void pump(FlowQueue queue) {
        if(queue.scheduled || queue.entries.isEmpty() || closed.get())return;
        queue.scheduled=true;var first=queue.entries.getFirst();
        first.context.executor().schedule(()->deliver(queue),Math.max(0,first.dueNanos-System.nanoTime()),TimeUnit.NANOSECONDS);
    }

    private void deliver(FlowQueue queue) {
        queue.scheduled=false;var packet=queue.entries.pollFirst();if(packet==null)return;
        try {
            if(!closed.get() && packet.context.channel().isActive()) {
                if(queue.flow==Flow.SERVER_TO_CLIENT)packet.context.fireChannelRead(packet.message);
                else packet.context.writeAndFlush(packet.message,packet.promise);
            } else release(packet);
        } finally {
            if(queue.flow==Flow.SERVER_TO_CLIENT){deliveredInbound.set(packet.sequence);pendingInbound.decrementAndGet();}
            else {deliveredOutbound.set(packet.sequence);pendingOutbound.decrementAndGet();}
            pump(queue);
        }
    }

    private void drain(FlowQueue queue) {
        queue.scheduled=false;
        while(!queue.entries.isEmpty()) {
            var packet=queue.entries.removeFirst();release(packet);
            if(queue.flow==Flow.SERVER_TO_CLIENT){deliveredInbound.set(packet.sequence);pendingInbound.decrementAndGet();}
            else {deliveredOutbound.set(packet.sequence);pendingOutbound.decrementAndGet();}
        }
    }

    private static void release(DelayedPacket packet) {
        ReferenceCountUtil.release(packet.message);
        if(packet.promise!=null)packet.promise.tryFailure(new ClosedChannelException());
    }

    private void onEventLoop(Runnable action) {
        var current=channel;if(current==null)throw new IllegalStateException("Latency handler is not installed");
        if(current.eventLoop().inEventLoop())action.run();else current.eventLoop().submit(action).syncUninterruptibly();
    }

    private <T> T callOnEventLoop(java.util.function.Supplier<T> action) {
        var current=channel;if(current==null)throw new IllegalStateException("Latency handler is not installed");
        if(current.eventLoop().inEventLoop())return action.get();
        var result=new AtomicReference<T>();current.eventLoop().submit(()->result.set(action.get())).syncUninterruptibly();return result.get();
    }

    private static final class FlowQueue {
        final Flow flow;final ArrayDeque<DelayedPacket> entries=new ArrayDeque<>();
        long notBeforeNanos;boolean scheduled;
        FlowQueue(Flow flow){this.flow=flow;}
    }
    private record DelayedPacket(ChannelHandlerContext context,Object message,ChannelPromise promise,long sequence,long dueNanos) {}
}
