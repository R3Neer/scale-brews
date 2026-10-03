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
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Receiver holdout: UNAVAILABLE is a material barrier even when binding/tracking stay unchanged. */
public final class S29AdversarialUnavailableReplayProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        final boolean gravityRoundTrip=Boolean.getBoolean("scalebrews.s29GravityRoundTrip");
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var generation=new AtomicLong();
        var latest=new AtomicReference<GeometryProvider.PublishedFrame>();var certificate=new AtomicReference<AnatomyMaterialIntervalPayload>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("S29 fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_replay",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                var prior=AnatomyMovement.publishedFrame(cow).orElseThrow();
                generation.set(AnatomyRuntime.trackingGeneration(player,cow));
                if(generation.get()<1)throw new AssertionError("S29 fixture not tracked");
                AnatomyRuntime.stop(server);
                var a=advance(prior,true,0);var b=advance(a,true,.25);latest.set(b);
                var identity=b.identity();var first=a.endpoint();var last=b.endpoint();
                var token=new AnatomyMaterialIntervalPayload(identity.epoch(),identity.revision(),identity.dimension().identifier(),identity.entityId(),identity.support(),
                    identity.bindingGeneration(),generation.get(),100000,first.frameSerial(),last.frameSerial(),first.authorityTick(),last.authorityTick(),first.jointSampleTick(),last.jointSampleTick());
                certificate.set(token);
                AnatomyNetworking.sendPose(player,a,generation.get());AnatomyNetworking.sendPose(player,b,generation.get());AnatomyNetworking.sendMaterialInterval(player,token);
            });
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationInterval(cow,100000,.5).isPresent(),120);
            world.getServer().runOnServer(server->{
                var unavailable=gravityRoundTrip?advance(latest.get(),true,0,net.minecraft.core.Direction.WEST):advance(latest.get(),false,0);latest.set(unavailable);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),unavailable,generation.get());
            });
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow
                && (gravityRoundTrip?AnatomyClientNetworking.presentationFrame(cow)
                    .map(frame->frame.after().frameSerial()==latest.get().endpoint().frameSerial()).orElse(false)
                    :AnatomyClientNetworking.presentationFrame(cow).isEmpty()),120);
            world.getServer().runOnServer(server->{
                var recovered=gravityRoundTrip?advance(latest.get(),true,0,net.minecraft.core.Direction.DOWN):advance(latest.get(),true,0);latest.set(recovered);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),recovered,generation.get());
            });
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).map(frame->frame.after().frameSerial()==latest.get().endpoint().frameSerial()).orElse(false),120);
            world.getServer().runOnServer(server->AnatomyNetworking.sendMaterialInterval(server.getPlayerList().getPlayers().getFirst(),certificate.get()));
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                boolean revived=AnatomyClientNetworking.presentationInterval(cow,100000,.5).isPresent();
                System.out.println("S29 material barrier gravityRoundTrip="+gravityRoundTrip+" old-certificate replay authoritative="+revived);
                if(revived)throw new AssertionError("S29 lifecycle: pre-UNAVAILABLE material interval revived after recovery by replaying its certificate");
            });
        } finally {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
    }
    private static GeometryProvider.PublishedFrame advance(GeometryProvider.PublishedFrame prior,boolean available,double dx) {
        return advance(prior,available,dx,prior.endpoint().sample().gravity().down());
    }
    private static GeometryProvider.PublishedFrame advance(GeometryProvider.PublishedFrame prior,boolean available,double dx,net.minecraft.core.Direction direction) {
        var old=prior.endpoint();var sample=old.sample();var origin=sample.origin().add(dx,0,0);
        var gravity=new io.github.r3neer.scalebrews.collision.api.GravityFrame(direction);
        var nextSample=new AnatomyPoseHistory.Sample(sample.inputs(),origin,sample.yaw(),sample.scale(),gravity);
        var root=new RootFrame(old.root().sequence()+1,old.root().tick()+1,origin,sample.yaw(),sample.scale(),gravity);
        var endpoint=new GeometryProvider.CausalEndpoint(old.frameSerial()+1,old.authorityTick()+1,old.jointSampleTick(),root,nextSample,
            available?GeometryProvider.Availability.AVAILABLE:GeometryProvider.Availability.UNAVAILABLE);
        return new GeometryProvider.PublishedFrame(prior.identity(),endpoint);
    }
}
