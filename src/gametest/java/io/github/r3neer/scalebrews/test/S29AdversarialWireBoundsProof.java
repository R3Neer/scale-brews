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
import java.util.UUID;
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

/** Wire-bound oracle: exact cap, fail-closed overflow, sticky scope and retrack recovery. */
public final class S29AdversarialWireBoundsProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var generation=new AtomicLong();var latest=new AtomicReference<GeometryProvider.PublishedFrame>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("S29 fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_identity",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                latest.set(AnatomyMovement.publishedFrame(cow).orElseThrow());generation.set(AnatomyRuntime.trackingGeneration(player,cow));
                if(generation.get()<1)throw new AssertionError("S29 fixture not tracked");AnatomyRuntime.stop(server);
            });
            var before=advance(latest.get(),.1);var after=advance(before,.2);
            context.runOnClient(client->inbox().clear());
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                AnatomyNetworking.sendPose(player,before,generation.get());AnatomyNetworking.sendPose(player,after,generation.get());
                for(int n=0;n<128;n++)AnatomyNetworking.sendMaterialInterval(player,token(before,after,generation.get(),90000+n));
            });
            context.waitFor(client->AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(id.get()),90127,.5).isPresent(),120);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                for(int n=0;n<128;n++)if(AnatomyClientNetworking.presentationInterval(cow,90000+n,.5).isEmpty())
                    throw new AssertionError("Within-bound certificate lost at "+n);
                System.out.println("S29 wire inbox exact boundary 128 PASS");
            });
            world.getServer().runOnServer(server->AnatomyNetworking.sendMaterialInterval(server.getPlayerList().getPlayers().getFirst(),token(before,after,generation.get(),90128)));
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                for(int n=0;n<129;n++)if(AnatomyClientNetworking.presentationInterval(cow,90000+n,.5).isPresent())
                    throw new AssertionError("Overflow inbox retained authoritative interval "+n);
            });
            world.getServer().runOnServer(server->AnatomyNetworking.sendMaterialInterval(server.getPlayerList().getPlayers().getFirst(),token(before,after,generation.get(),90000)));
            context.waitTicks(2);
            context.runOnClient(client->{
                if(AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(id.get()),90000,.5).isPresent())
                    throw new AssertionError("Overflow recovered by same-window replay");
            });
            generation.incrementAndGet();
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                AnatomyNetworking.sendMaterialInterval(player,token(before,after,generation.get(),91000));
                AnatomyNetworking.sendPose(player,before,generation.get());AnatomyNetworking.sendPose(player,after,generation.get());
            });
            context.waitFor(client->AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(id.get()),91000,.5).isPresent(),120);
            context.runOnClient(client->{
                if(AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(id.get()),90000,.5).isPresent())
                    throw new AssertionError("New tracking revived old saturated material");
                System.out.println("S29 wire inbox overflow fail-closed/sticky and new tracking recovery PASS");
            });
        } finally {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
    }
    private static GeometryProvider.PublishedFrame advance(GeometryProvider.PublishedFrame prior,double dx) {
        var old=prior.endpoint();var sample=old.sample();var origin=sample.origin().add(dx,0,0);
        var nextSample=new AnatomyPoseHistory.Sample(sample.inputs(),origin,sample.yaw(),sample.scale(),sample.gravity());
        var root=new RootFrame(old.root().sequence()+1,old.root().tick()+1,origin,sample.yaw(),sample.scale(),sample.gravity());
        return new GeometryProvider.PublishedFrame(prior.identity(),new GeometryProvider.CausalEndpoint(old.frameSerial()+1,old.authorityTick()+1,old.jointSampleTick(),root,nextSample,GeometryProvider.Availability.AVAILABLE));
    }
    private static AnatomyMaterialIntervalPayload token(GeometryProvider.PublishedFrame a,GeometryProvider.PublishedFrame b,long tracking,long serial) {
        var i=b.identity();var before=a.endpoint();var after=b.endpoint();
        return new AnatomyMaterialIntervalPayload(i.epoch(),i.revision(),i.dimension().identifier(),i.entityId(),i.support(),i.bindingGeneration(),tracking,serial,
            before.frameSerial(),after.frameSerial(),before.authorityTick(),after.authorityTick(),before.jointSampleTick(),after.jointSampleTick());
    }
    private static AnatomyCertifiedIntervalInbox inbox() {
        try {var field=AnatomyClientNetworking.class.getDeclaredField("certifiedIntervals");field.setAccessible(true);return (AnatomyCertifiedIntervalInbox)field.get(null);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
}
