package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.platform.PlatformConnection;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** Real integrated client/server objects share a JVM, but never receipt ownership. */
public final class S00ClientBoundaryTests implements FabricClientGameTest {
    private static final Vec3 UP=new Vec3(0,1,0),POINT=new Vec3(.5,1,.5),DELTA=new Vec3(.125,0,0);
    @Override public void runTest(ClientGameTestContext context) {
        var world=context.worldBuilder().create();var supportId=new AtomicInteger();
        var before=new AtomicReference<List<AnatomyTransportReceipts.Receipt>>();var problems=new ArrayList<String>();
        try {
            world.getServer().runOnServer(server->{
                var support=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(support==null)throw new AssertionError("Could not create receipt support");
                support.setNoAi(true);support.setNoGravity(true);support.setPos(3,4,3);server.overworld().addFreshEntity(support);supportId.set(support.getId());
            });
            context.waitFor(client->client.level!=null && client.player!=null && client.level.getEntity(supportId.get()) instanceof LivingEntity,100);
            world.getServer().runOnServer(server->{
                var body=server.getPlayerList().getPlayers().getFirst();var support=(LivingEntity)server.overworld().getEntity(supportId.get());
                AnatomyTransportReceipts.invalidate(body);
                // Recorder fixture obeys its post-apply contract: move and baseline
                // the real server body first. This is NOT a proof of the carry solver.
                var old=body.position();body.setPos(old.add(DELTA));((PlatformConnection)body.connection).scalebrews$transportBaseline(body,DELTA);
                record(body,support,1,DELTA);var saved=AnatomyTransportReceipts.history(body,body.getUUID());
                check(saved.size()==1 && saved.getFirst().bodyBefore().distanceToSqr(old)<1e-16,"Fixture did not record the actual applied delta");before.set(saved);
            });
            context.runOnClient(client->{
                check(client.player.getUUID().equals(before.get().getFirst().body()),"Client and server do not share body identity in this fixture");
                check(client.player.level().isClientSide(),"Attack target is not a client replica");
                AnatomyMovement.clear(client.player);
            });
            world.getServer().runOnServer(server->{
                var body=server.getPlayerList().getPlayers().getFirst();
                if(!AnatomyTransportReceipts.history(body,body.getUUID()).equals(before.get()))problems.add("Client clear deleted authoritative receipts");
                var support=(LivingEntity)server.overworld().getEntity(supportId.get());
                AnatomyTransportReceipts.invalidate(body);record(body,support,2,Vec3.ZERO);before.set(AnatomyTransportReceipts.history(body,body.getUUID()));
            });
            context.runOnClient(client->{
                var support=(LivingEntity)client.level.getEntity(supportId.get());var body=client.player;
                long tick=client.level.getGameTime();var material=box(UNIT).move(support.position());
                var contact=new AnatomyMovement.Contact(support,"piece",1,UP,1);
                var surface=new SurfaceContact(support.getUUID(),1,"piece",3,POINT,UP,tick);
                var root=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
                AnatomyTransportReceipts.record(body,contact,surface,root,new SupportTransport(tick,999,1,Vec3.ZERO,Vec3.ZERO),material,material);
            });
            world.getServer().runOnServer(server->{
                var body=server.getPlayerList().getPlayers().getFirst();
                if(!AnatomyTransportReceipts.history(body,body.getUUID()).equals(before.get()))problems.add("Client record changed authoritative receipts");
                var old=body.position();AnatomyTransportReceipts.invalidate(body);
                check(AnatomyTransportReceipts.history(body,body.getUUID()).isEmpty() && body.position().equals(old),"Server release failed or reapplied transport");
            });
            if(!problems.isEmpty())throw new AssertionError(String.join("; ",problems));
            System.out.println("S00_CLIENT_RECEIPT_AUTHORITY PASS real client clear/record isolation and server invalidation");
        }finally{world.getServer().runOnServer(AnatomyTransportReceipts::clear);world.close();}
    }
    private static void record(ServerPlayer body,LivingEntity support,long sequence,Vec3 delta) {
        long tick=body.level().getGameTime();var material=box(UNIT).move(support.position());
        var contact=new AnatomyMovement.Contact(support,"piece",1,UP,1);
        var surface=new SurfaceContact(support.getUUID(),1,"piece",3,POINT,UP,tick);
        var root=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        AnatomyTransportReceipts.record(body,contact,surface,root,new SupportTransport(tick,sequence,1,delta,delta),material,material.move(delta));
    }
}
