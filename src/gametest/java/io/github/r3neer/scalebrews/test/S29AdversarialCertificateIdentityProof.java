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

/** Server-issued certificate permutations, no-inference and exact receiver lifecycle fences. */
public final class S29AdversarialCertificateIdentityProof implements FabricClientGameTest {
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
            for(int order=0;order<3;order++) {
                final long serial=50000+order;final int permutation=order;
                var before=advance(latest.get(),.1);var after=advance(before,.2);latest.set(after);
                var token=token(before,after,generation.get(),serial);
                world.getServer().runOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();
                    if(permutation==0)AnatomyNetworking.sendMaterialInterval(player,token);
                    AnatomyNetworking.sendPose(player,before,generation.get());
                    if(permutation==1)AnatomyNetworking.sendMaterialInterval(player,token);
                    AnatomyNetworking.sendPose(player,after,generation.get());
                });
                context.waitFor(client->AnatomyClientNetworking.presentationFrame((Cow)client.level.getEntity(id.get()))
                    .map(frame->frame.after().frameSerial()==after.endpoint().frameSerial()).orElse(false),120);
                if(order==2) {
                    context.runOnClient(client->{
                        var cow=(Cow)client.level.getEntity(id.get());
                        if(AnatomyClientNetworking.presentationInterval(cow,serial,.5).isPresent())throw new AssertionError("Consecutive endpoint serials inferred an interval without a server certificate");
                    });
                    world.getServer().runOnServer(server->AnatomyNetworking.sendMaterialInterval(server.getPlayerList().getPlayers().getFirst(),token));
                }
                context.waitFor(client->AnatomyClientNetworking.presentationInterval((Cow)client.level.getEntity(id.get()),serial,.5).isPresent(),120);
                context.runOnClient(client->{
                    var cow=(Cow)client.level.getEntity(id.get());
                    var first=AnatomyClientNetworking.presentationInterval(cow,serial,.5).orElseThrow();
                    if(first!=AnatomyClientNetworking.presentationInterval(cow,serial,.5).orElseThrow())throw new AssertionError("Identical interval query did not reuse its frame cache");
                    for(double f:new double[]{0,.25,.5,.75,1}) {
                        var frame=AnatomyClientNetworking.presentationInterval(cow,serial,f).orElseThrow();
                        var expected=before.endpoint().sample().origin().lerp(after.endpoint().sample().origin(),f);
                        if(frame.kind()!=AnatomyClientNetworking.PresentationKind.CERTIFIED_INTERVAL
                                || frame.before().frameSerial()!=token.beforeFrameSerial() || frame.after().frameSerial()!=token.afterFrameSerial()
                                || frame.evaluated().origin().distanceToSqr(expected)>1e-18)
                            throw new AssertionError("Certified interval changed material endpoints or root trajectory at "+f);
                    }
                    for(double f:new double[]{-.0001,1.0001,Double.NaN,Double.POSITIVE_INFINITY})
                        if(AnatomyClientNetworking.presentationInterval(cow,serial,f).isPresent())throw new AssertionError("Invalid interpolation fraction accepted "+f);
                    if(AnatomyClientNetworking.presentationInterval(cow,serial+1000,.5).isPresent())throw new AssertionError("Missing serial silently selected another interval");
                    System.out.println("S29 CERTIFICATE permutation="+permutation+" identity/cache/fractions PASS");
                });
            }
            var before=advance(latest.get(),.1);var after=advance(before,.2);latest.set(after);
            world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();AnatomyNetworking.sendPose(player,before,generation.get());AnatomyNetworking.sendPose(player,after,generation.get());});
            context.waitFor(client->AnatomyClientNetworking.presentationFrame((Cow)client.level.getEntity(id.get()))
                .map(frame->frame.after().frameSerial()==after.endpoint().frameSerial()).orElse(false),120);
            var valid=token(before,after,generation.get(),60000);
            for(int axis=0;axis<7;axis++) {
                final int changed=axis;final long serial=60000+axis;
                // Isolate each negative case without changing endpoints or session authority.
                context.runOnClient(client->inbox().clear());
                var invalid=wrongIdentity(valid,axis,serial);
                world.getServer().runOnServer(server->ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),invalid));
                context.waitTicks(2);
                context.runOnClient(client->{
                    var cow=(Cow)client.level.getEntity(id.get());
                    if(AnatomyClientNetworking.presentationInterval(cow,serial,.5).isPresent())throw new AssertionError("Mismatched certificate axis "+changed+" became authoritative");
                    System.out.println("S29 CERTIFICATE negative identity axis="+changed+" PASS");
                });
            }
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
    private static AnatomyMaterialIntervalPayload wrongIdentity(AnatomyMaterialIntervalPayload p,int axis,long serial) {
        return new AnatomyMaterialIntervalPayload(axis==0?new UUID(42,43):p.epoch(),p.revision()+(axis==1?1:0),axis==2?Identifier.parse("minecraft:the_nether"):p.dimension(),
            p.supportId()+(axis==3?1000:0),axis==4?new UUID(44,45):p.support(),p.bindingGeneration()+(axis==5?1:0),p.trackingGeneration()+(axis==6?1:0),serial,
            p.beforeFrameSerial(),p.afterFrameSerial(),p.beforeAuthorityTick(),p.afterAuthorityTick(),p.beforeJointSampleTick(),p.afterJointSampleTick());
    }
    private static AnatomyCertifiedIntervalInbox inbox() {
        try {var field=AnatomyClientNetworking.class.getDeclaredField("certifiedIntervals");field.setAccessible(true);return (AnatomyCertifiedIntervalInbox)field.get(null);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
}
