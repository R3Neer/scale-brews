package io.github.r3neer.scalebrews.test;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** Tests only the native wire/handler contract, never S32 production behavior. */
public final class S32AdversarialNativeBundleProof implements FabricClientGameTest {
    public record Stamp(long nonce,int body,double targetX) implements CustomPacketPayload {
        public static final Type<Stamp> TYPE=new Type<>(Identifier.parse("scalebrews_test:s32_native_scope"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Stamp> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarLong(p.nonce);b.writeVarInt(p.body);b.writeDouble(p.targetX);},
            b->new Stamp(b.readVarLong(),b.readVarInt(),b.readDouble()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record State(Vec3 position,Vec3 oldPosition,Vec3 codecBase,Vec3 target,float targetYaw,int steps,int tick) {}
    public record Event(long nonce,String kind,boolean sameObject,State before,State after,List<String> order) {}
    private static final List<Event> EVENTS=new CopyOnWriteArrayList<>();
    private static final ThreadLocal<Scope> SCOPE=new ThreadLocal<>();
    private static boolean registered;
    private static volatile boolean armed;
    private static final class Scope {
        final Packet<?> packet;final Stamp stamp;final List<String> order=new ArrayList<>();
        State before,after;String kind;int nativeCalls;
        Scope(Packet<?> packet,Stamp stamp){this.packet=packet;this.stamp=stamp;order.add("bundle-enter");}
    }
    public static boolean begin(ClientboundBundlePacket bundle) {
        if(!armed || !Minecraft.getInstance().packetProcessor().isSameThread())return false;
        var list=new ArrayList<Packet<?>>();bundle.subPackets().forEach(list::add);
        if(list.size()!=2 || !(list.get(1) instanceof ClientboundCustomPayloadPacket c) || !(c.payload() instanceof Stamp stamp))return false;
        if(SCOPE.get()!=null)throw new AssertionError("Nested native probe scope");
        SCOPE.set(new Scope(list.getFirst(),stamp));return true;
    }
    public static void before(Packet<?> packet) {
        var s=SCOPE.get();if(s==null)return;
        if(packet!=s.packet)throw new AssertionError("Native handler packet object differs from decoded bundle");
        s.nativeCalls++;s.kind=packet.getClass().getSimpleName();s.before=state(s.stamp.body);s.order.add("native-enter");
    }
    public static void after(Packet<?> packet) {
        var s=SCOPE.get();if(s==null)return;
        if(packet!=s.packet)throw new AssertionError("Native handler replaced packet identity");
        s.after=state(s.stamp.body);s.order.add("native-return");
    }
    public static void finish(boolean completed) {
        var s=SCOPE.get();SCOPE.remove();if(s==null)return;
        if(!completed)return;
        s.order.add("bundle-return");
        if(s.nativeCalls!=1 || s.after==null)throw new AssertionError("Exactly one real native handler event required");
        if(Math.abs(s.after.codecBase.x-s.stamp.targetX)>1e-6)throw new AssertionError("Wire codec target differs from scoped attestation");
        EVENTS.add(new Event(s.stamp.nonce,s.kind,true,s.before,s.after,List.copyOf(s.order)));
    }
    private static State state(int id) {
        var body=Minecraft.getInstance().level.getEntity(id);if(body==null)throw new AssertionError("Tracked body absent");
        var h=body.getInterpolation();int steps=0;
        if(h!=null)try {var f=InterpolationHandler.class.getDeclaredField("interpolationData");f.setAccessible(true);var data=f.get(h);var n=data.getClass().getDeclaredField("steps");n.setAccessible(true);steps=n.getInt(data);}
        catch(ReflectiveOperationException e){throw new AssertionError("Native observation fixture",e);}
        return new State(body.position(),body.oldPosition(),body.getPositionCodec().getBase(),h==null?body.position():h.position(),h==null?body.getYRot():h.yRot(),steps,body.tickCount);
    }
    private static synchronized void register() {
        if(registered)return;
        PayloadTypeRegistry.clientboundPlay().register(Stamp.TYPE,Stamp.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Stamp.TYPE,(p,c)->{var s=SCOPE.get();if(s!=null){if(p.nonce!=s.stamp.nonce)throw new AssertionError("Stamp callback changed nonce");s.order.add("fabric-payload");}});
        registered=true;
    }
    public static void prepare(ClientGameTestContext context) {context.runOnClient(c->register());EVENTS.clear();armed=true;}
    public static void disarm(){armed=false;SCOPE.remove();}
    public static List<Event> events(){return List.copyOf(EVENTS);}
    public static void send(net.minecraft.server.level.ServerPlayer player,Packet<? super ClientGamePacketListener> packet,Stamp stamp) {
        player.connection.send(new ClientboundBundlePacket(List.of(packet,new ClientboundCustomPayloadPacket(stamp))));
    }
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(c->register());EVENTS.clear();var id=new AtomicInteger();var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{var cow=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);if(cow==null)throw new AssertionError("No Cow");cow.setNoAi(true);cow.setNoGravity(true);cow.setInvulnerable(true);cow.setPos(2,4,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());});
            context.waitFor(c->c.level.getEntity(id.get()) instanceof LivingEntity body && !body.isLocalInstanceAuthoritative() && body.getInterpolation()!=null,120);
            var base=context.computeOnClient(c->c.level.getEntity(id.get()).getPositionCodec().getBase());
            armed=true;
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();
                send(p,new ClientboundMoveEntityPacket.Pos(id.get(),(short)3072,(short)0,(short)0,false),new Stamp(1,id.get(),base.x+.75));
                send(p,new ClientboundMoveEntityPacket.Rot(id.get(),(byte)16,(byte)0,false),new Stamp(2,id.get(),base.x+.75));
                send(p,new ClientboundMoveEntityPacket.Pos(id.get(),(short)0,(short)0,(short)0,false),new Stamp(3,id.get(),base.x+.75));
            });
            context.waitFor(c->EVENTS.size()==3,120);
            var a=EVENTS.get(0);var r=EVENTS.get(1);var n=EVENTS.get(2);
            if(a.after.steps!=3 || Math.abs(a.after.target.x-(base.x+.75))>1e-6)throw new AssertionError("Relative native interpolation positive control missing");
            if(r.after.steps!=3 || r.before.targetYaw==r.after.targetYaw)throw new AssertionError("Changed-yaw Rot did not restart real native handler");
            if(n.before.steps<1 || n.before.steps!=n.after.steps)throw new AssertionError("Same-target same-rotation event restarted instead of native no-op");
            for(var event:EVENTS)System.out.println("S32 ADVERSARY native bundle "+event);
            context.waitTicks(1);
            world.getServer().runOnServer(server->send(server.getPlayerList().getPlayers().getFirst(),new ClientboundEntityPositionSyncPacket(id.get(),new PositionMoveRotation(new Vec3(base.x+1.5,base.y,base.z),Vec3.ZERO,22.5f,0),false),new Stamp(4,id.get(),base.x+1.5)));
            context.waitFor(c->EVENTS.size()==4,120);
            if(EVENTS.get(3).after.steps!=3)throw new AssertionError("Near position-sync did not use native interpolation");
            world.getServer().runOnServer(server->send(server.getPlayerList().getPlayers().getFirst(),new ClientboundEntityPositionSyncPacket(id.get(),new PositionMoveRotation(new Vec3(base.x+100,base.y,base.z),Vec3.ZERO,22.5f,0),false),new Stamp(5,id.get(),base.x+100)));
            context.waitFor(c->EVENTS.size()==5,120);
            if(Math.abs(EVENTS.get(4).after.position.x-(base.x+100))>1e-6)throw new AssertionError("Far position-sync did not snap body natively");
            for(int i=3;i<EVENTS.size();i++)System.out.println("S32 ADVERSARY native bundle "+EVENTS.get(i));
            System.out.println("S32 ADVERSARY native scope PASS 5 actual encoded bundles; integrated recipient only, no production carry claim");
        } finally {armed=false;SCOPE.remove();world.close();}
    }
}
