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

/** Two live supports: a valid foreign certificate never grants authority to its neighbor. */
public final class S29AdversarialCrossSupportProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var otherId=new AtomicInteger(-1);var generation=new AtomicLong();var otherGeneration=new AtomicLong();var latest=new AtomicReference<GeometryProvider.PublishedFrame>();var otherLatest=new AtomicReference<GeometryProvider.PublishedFrame>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("S29 fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());
                var other=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(other==null)throw new AssertionError("Second support absent");
                other.setNoAi(true);other.setNoGravity(true);other.setPos(5,3,2);server.overworld().addFreshEntity(other);otherId.set(other.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_identity",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                && client.level.getEntity(otherId.get()) instanceof Cow other && AnatomyClientNetworking.presentationFrame(other).isPresent(),120);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                latest.set(AnatomyMovement.publishedFrame(cow).orElseThrow());generation.set(AnatomyRuntime.trackingGeneration(player,cow));
                var other=(Cow)server.overworld().getEntity(otherId.get());
                otherLatest.set(AnatomyMovement.publishedFrame(other).orElseThrow());otherGeneration.set(AnatomyRuntime.trackingGeneration(player,other));
                if(generation.get()<1 || otherGeneration.get()<1)throw new AssertionError("S29 fixture not tracked");AnatomyRuntime.stop(server);
            });
            var a=advance(latest.get(),.1);var b=advance(a,.2);
            var foreignA=advance(otherLatest.get(),.1);var foreignB=advance(foreignA,.2);
            context.waitTicks(2);context.runOnClient(client->inbox().clear());
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                AnatomyNetworking.sendPose(player,a,generation.get());AnatomyNetworking.sendPose(player,b,generation.get());
                AnatomyNetworking.sendPose(player,foreignA,otherGeneration.get());AnatomyNetworking.sendPose(player,foreignB,otherGeneration.get());
                AnatomyNetworking.sendMaterialInterval(player,token(foreignA,foreignB,otherGeneration.get(),92000));
            });
            context.waitFor(client->AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(otherId.get()),92000,.5).isPresent(),120);
            context.runOnClient(client->{
                if(AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(id.get()),92000,.5).isPresent())
                    throw new AssertionError("Another support's certified interval became authoritative for target");
                System.out.println("S29 cross-support: positive foreign certificate cannot satisfy target PASS");
            });
            world.getServer().runOnServer(server->AnatomyNetworking.sendMaterialInterval(server.getPlayerList().getPlayers().getFirst(),token(a,b,generation.get(),92000)));
            context.waitFor(client->AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(id.get()),92000,.5).isPresent(),120);
            context.runOnClient(client->{
                var target=(Cow)client.level.getEntity(id.get());var other=(Cow)client.level.getEntity(otherId.get());
                var targetFrame=AnatomyClientNetworking.presentationInterval(target,92000,.5).orElseThrow();
                var otherFrame=AnatomyClientNetworking.presentationInterval(other,92000,.5).orElseThrow();
                if(!targetFrame.identity().support().equals(target.getUUID()) || !otherFrame.identity().support().equals(other.getUUID())
                    || targetFrame.evaluated().origin().distanceToSqr(a.endpoint().sample().origin().lerp(b.endpoint().sample().origin(),.5))>1e-18
                    || otherFrame.evaluated().origin().distanceToSqr(foreignA.endpoint().sample().origin().lerp(foreignB.endpoint().sample().origin(),.5))>1e-18)
                    throw new AssertionError("Equal serials across supports aliased geometry or cache identity");
                System.out.println("S29 cross-support: same serial preserves distinct identity/geometry PASS");
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
