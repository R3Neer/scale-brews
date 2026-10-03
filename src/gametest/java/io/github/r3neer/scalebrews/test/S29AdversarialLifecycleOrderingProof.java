package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Independent ordering/replay proof across recipient-tracking and binding restarts. */
public final class S29AdversarialLifecycleOrderingProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var tracking=new AtomicLong();var last=new AtomicReference<GeometryProvider.PublishedFrame>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("S29 lifecycle fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_lifecycle",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                last.set(AnatomyMovement.publishedFrame(cow).orElseThrow());tracking.set(AnatomyRuntime.trackingGeneration(player,cow));
                if(tracking.get()<1)throw new AssertionError("Fixture lacks tracking authority");AnatomyRuntime.stop(server);
            });
            GeometryProvider.PublishedFrame oldA=null,oldB=null;AnatomyMaterialIntervalPayload oldToken=null;long oldTracking=tracking.get();
            for(int restart=0;restart<4;restart++) {
                long binding=last.get().identity().bindingGeneration()+(restart==1 || restart==3?1:0);
                long generation=tracking.get()+(restart==2 || restart==3?1:0);tracking.set(generation);
                var a=advance(last.get(),binding,.1);var b=advance(a,binding,.2);last.set(b);
                var certificate=certificate(a,b,generation,70000+restart);
                world.getServer().runOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();
                    // The new lifecycle is first observed in the interval stream, not the pose stream.
                    ServerPlayNetworking.send(player,certificate);
                });
                context.waitTicks(2);
                context.runOnClient(client->{var cow=(Cow)client.level.getEntity(id.get());
                    if(AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),.5).isPresent())
                        throw new AssertionError("Certificate materialized before its new lifecycle endpoints arrived");});
                world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();
                    AnatomyNetworking.sendPose(player,a,generation);AnatomyNetworking.sendPose(player,b,generation);});
                context.waitFor(client->AnatomyClientNetworking.presentationFrame((Cow)client.level.getEntity(id.get()))
                    .map(frame->frame.after().frameSerial()==b.endpoint().frameSerial()).orElse(false),120);
                context.runOnClient(client->{var cow=(Cow)client.level.getEntity(id.get());
                    var interval=AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),.5).orElseThrow(
                        ()->new AssertionError("Exact certificate staged before lifecycle restart was discarded: "+certificate));
                    if(interval.identity().bindingGeneration()!=binding || interval.before().frameSerial()!=a.endpoint().frameSerial()
                            || interval.after().frameSerial()!=b.endpoint().frameSerial())throw new AssertionError("New lifecycle mixed old endpoint identity");
                    System.out.println("S29 RESTART binding="+binding+" tracking="+generation+" early certificate PASS");});
                if(oldToken!=null) {
                    var stale=oldToken;var priorA=oldA;var priorB=oldB;long priorGeneration=oldTracking;
                    world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();
                        ServerPlayNetworking.send(player,stale);AnatomyNetworking.sendPose(player,priorA,priorGeneration);AnatomyNetworking.sendPose(player,priorB,priorGeneration);});
                    context.waitTicks(2);
                    context.runOnClient(client->{var cow=(Cow)client.level.getEntity(id.get());
                        if(AnatomyClientNetworking.presentationInterval(cow,stale.materialSerial(),.5).isPresent())throw new AssertionError("Retired lifecycle certificate revived after restart");
                        if(AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),.5).isEmpty())throw new AssertionError("Stale replay evicted the new lifecycle interval");});
                }
                oldA=a;oldB=b;oldToken=certificate;oldTracking=generation;
            }
            // A named before endpoint absent from exact history must not use the prior/nearest frame.
            var missing=advance(last.get(),last.get().identity().bindingGeneration(),.1);
            var after=advance(missing,missing.identity().bindingGeneration(),.2);last.set(after);
            long generation=tracking.get();var gap=certificate(missing,after,generation,80000);
            world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();
                AnatomyNetworking.sendPose(player,after,generation);ServerPlayNetworking.send(player,gap);});
            context.waitFor(client->AnatomyClientNetworking.presentationFrame((Cow)client.level.getEntity(id.get()))
                .map(frame->frame.after().frameSerial()==after.endpoint().frameSerial()).orElse(false),120);
            context.runOnClient(client->{var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyClientNetworking.presentationInterval(cow,80000,.5).isPresent())throw new AssertionError("Gapped before endpoint substituted a nearby frame");});
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),missing,generation));
            context.waitTicks(2);
            context.runOnClient(client->{var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyClientNetworking.presentationInterval(cow,80000,.5).isPresent())throw new AssertionError("Stale late pose packet revived an interval missing an accepted before endpoint");
                System.out.println("S29 missing/stale before endpoint remains fail-closed PASS");});
        } finally {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
    }
    private static GeometryProvider.PublishedFrame advance(GeometryProvider.PublishedFrame prior,long binding,double dx) {
        var old=prior.endpoint();var sample=old.sample();var origin=sample.origin().add(dx,0,0);
        var next=new AnatomyPoseHistory.Sample(sample.inputs(),origin,sample.yaw(),sample.scale(),sample.gravity());
        var root=new RootFrame(old.root().sequence()+1,old.root().tick()+1,origin,next.yaw(),next.scale(),next.gravity());
        var i=prior.identity();var identity=new GeometryProvider.GeometryIdentity(i.dimension(),i.support(),i.entityId(),i.epoch(),i.revision(),i.model(),i.poseProvider(),i.rootProvider(),binding,i.localRegistrationGeneration());
        return new GeometryProvider.PublishedFrame(identity,new GeometryProvider.CausalEndpoint(old.frameSerial()+1,old.authorityTick()+1,old.jointSampleTick(),root,next,GeometryProvider.Availability.AVAILABLE));
    }
    private static AnatomyMaterialIntervalPayload certificate(GeometryProvider.PublishedFrame a,GeometryProvider.PublishedFrame b,long tracking,long serial) {
        var i=b.identity();var before=a.endpoint();var after=b.endpoint();
        return new AnatomyMaterialIntervalPayload(i.epoch(),i.revision(),i.dimension().identifier(),i.entityId(),i.support(),i.bindingGeneration(),tracking,serial,
            before.frameSerial(),after.frameSerial(),before.authorityTick(),after.authorityTick(),before.jointSampleTick(),after.jointSampleTick());
    }
}
