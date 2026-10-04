package io.github.r3neer.scalebrews.test;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** Two native Minecraft client processes on one real dedicated server socket. */
public final class S32AdversarialNativeBundlePairProof implements FabricClientGameTest {
    private Path root;
    private void write(String name,String value){try{Files.writeString(root.resolve(name),value);}catch(java.io.IOException e){throw new AssertionError(e);}}
    private String read(String name){try{return Files.readString(root.resolve(name));}catch(java.io.IOException e){throw new AssertionError(e);}}
    private void await(ClientGameTestContext context,String name){context.waitFor(c->Files.isRegularFile(root.resolve(name)),1200);}
    @Override public void runTest(ClientGameTestContext context) {
        root=Path.of(System.getProperty("scalebrews.s32NativePairDir"));
        S32AdversarialNativeBundleProof.prepare(context);
        try {if(System.getProperty("scalebrews.s32NativePairRole").equals("host"))host(context);else peer(context);}
        finally{S32AdversarialNativeBundleProof.disarm();}
    }
    private void host(ClientGameTestContext context) {
        var properties=new Properties();properties.setProperty("server-port","28232");properties.setProperty("server-ip","127.0.0.1");properties.setProperty("allow-flight","false");properties.setProperty("max-players","2");
        var id=new AtomicInteger();
        try(var server=context.worldBuilder().createServer(properties);var connection=server.connect()) {
            server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();var body=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);if(body==null)throw new AssertionError("No Cow");body.setNoAi(true);body.setNoGravity(true);body.setInvulnerable(true);body.setPersistenceRequired();body.setPos(player.getX()+2,player.getY(),player.getZ());s.overworld().addFreshEntity(body);id.set(body.getId());});
            context.waitFor(c->c.level.getEntity(id.get()) instanceof LivingEntity body && !body.isLocalInstanceAuthoritative() && body.getInterpolation()!=null,360);
            var base=context.computeOnClient(c->c.level.getEntity(id.get()).getPositionCodec().getBase());
            int port=server.computeOnServer(s->s.getPort());
            write("server.properties","port="+port+"\nbody="+id.get()+"\nx="+base.x+"\ny="+base.y+"\nz="+base.z+"\n");
            await(context,"peer-ready");
            server.runOnServer(s->{var players=s.getPlayerList().getPlayers();if(players.size()!=2 || players.get(0).getUUID().equals(players.get(1).getUUID()))throw new AssertionError("Two distinct actual native recipients required");System.out.println("S32 ADVERSARY actual dedicated recipients "+players.stream().map(p->p.getName().getString()+":"+p.getUUID()).toList());});
            server.runOnServer(s->{for(var p:s.getPlayerList().getPlayers()){int shift=p.getName().getString().equals("S32Peer")?100:0;
                S32AdversarialNativeBundleProof.send(p,new ClientboundMoveEntityPacket.Pos(id.get(),(short)3072,(short)0,(short)0,false),new S32AdversarialNativeBundleProof.Stamp(shift+1,id.get(),base.x+.75));
                S32AdversarialNativeBundleProof.send(p,new ClientboundMoveEntityPacket.Rot(id.get(),(byte)16,(byte)0,false),new S32AdversarialNativeBundleProof.Stamp(shift+2,id.get(),base.x+.75));
                S32AdversarialNativeBundleProof.send(p,new ClientboundMoveEntityPacket.Pos(id.get(),(short)0,(short)0,(short)0,false),new S32AdversarialNativeBundleProof.Stamp(shift+3,id.get(),base.x+.75));
            }});
            context.waitFor(c->S32AdversarialNativeBundleProof.events().size()==3,360);await(context,"peer-stage3");
            sendSync(server,id.get(),base,4,1.5);context.waitFor(c->S32AdversarialNativeBundleProof.events().size()==4,360);await(context,"peer-stage4");
            sendSync(server,id.get(),base,5,100);context.waitFor(c->S32AdversarialNativeBundleProof.events().size()==5,360);await(context,"peer-done");
            validate(base,0);write("host-done","PASS");
            System.out.println("S32 ADVERSARY paired native scope PASS host + peer, same dedicated server port="+port+", allow-flight=false; no S32 carry production claim");
        }
    }
    private void sendSync(TestServerContext server,int id,Vec3 base,int nonce,double dx) {
        server.runOnServer(s->{for(var p:s.getPlayerList().getPlayers()){int shift=p.getName().getString().equals("S32Peer")?100:0;
            S32AdversarialNativeBundleProof.send(p,new ClientboundEntityPositionSyncPacket(id,new PositionMoveRotation(base.add(dx,0,0),Vec3.ZERO,22.5f,0),false),new S32AdversarialNativeBundleProof.Stamp(shift+nonce,id,base.x+dx));}});
    }
    private void peer(ClientGameTestContext context) {
        await(context,"server.properties");var manifest=new Properties();try{manifest.load(new java.io.StringReader(read("server.properties")));}catch(java.io.IOException e){throw new AssertionError(e);}
        int id=Integer.parseInt(manifest.getProperty("body"));var base=new Vec3(Double.parseDouble(manifest.getProperty("x")),Double.parseDouble(manifest.getProperty("y")),Double.parseDouble(manifest.getProperty("z")));
        String address="127.0.0.1:"+manifest.getProperty("port");
        context.runOnClient(c->{if(!c.getUser().getName().equals("S32Peer"))throw new AssertionError("Peer process must use its own native username");ConnectScreen.startConnecting(c.gui.screen(),c,ServerAddress.parseString(address),new ServerData("S32 native probe",address,ServerData.Type.OTHER),false,null);});
        context.waitFor(c->c.level!=null && c.player!=null && c.level.getEntity(id) instanceof LivingEntity body && !body.isLocalInstanceAuthoritative() && body.getInterpolation()!=null,1200);
        context.runOnClient(c->{if(c.level.getEntity(id).getPositionCodec().getBase().distanceToSqr(base)>1e-10)throw new AssertionError("Peer did not receive the same vanilla spawn base");});
        write("peer-ready","PASS");
        context.waitFor(c->S32AdversarialNativeBundleProof.events().size()==3,1200);write("peer-stage3","PASS");
        context.waitFor(c->S32AdversarialNativeBundleProof.events().size()==4,1200);write("peer-stage4","PASS");
        context.waitFor(c->S32AdversarialNativeBundleProof.events().size()==5,1200);validate(base,100);write("peer-done","PASS");
        await(context,"host-done");context.waitFor(c->c.level==null,1200);
        context.runOnClient(c->c.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
        System.out.println("S32 ADVERSARY paired native scope PASS peer actual socket/native handler");
    }
    private void validate(Vec3 base,int shift) {
        var events=S32AdversarialNativeBundleProof.events();if(events.size()!=5)throw new AssertionError("Five actual native occurrences required");
        for(int i=0;i<5;i++){var e=events.get(i);if(e.nonce()!=shift+i+1 || !e.sameObject())throw new AssertionError("Recipient-specific nonce or packet identity changed");System.out.println("S32 ADVERSARY paired native bundle "+e);}
        var first=events.get(0);var rot=events.get(1);var noop=events.get(2);
        if(first.after().steps()!=3 || Math.abs(first.after().target().x-base.x-.75)>1e-6)throw new AssertionError("Relative positive native interpolation missing");
        if(rot.after().steps()!=3 || rot.before().targetYaw()==rot.after().targetYaw())throw new AssertionError("Actual changed-yaw restart missing");
        if(noop.before().steps()<1 || noop.before().steps()!=noop.after().steps())throw new AssertionError("Actual identical-target no-op missing");
        if(events.get(3).after().steps()!=3 || Math.abs(events.get(3).after().target().x-base.x-1.5)>1e-6)throw new AssertionError("Near sync interpolation missing");
        if(Math.abs(events.get(4).after().position().x-base.x-100)>1e-6)throw new AssertionError("Far sync snap missing");
    }
}
