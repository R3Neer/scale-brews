package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.platform.anatomy.GeometryExtractor;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyDefinition;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyFilter;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyNetworking;
import io.github.r3neer.scalebrews.platform.anatomy.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.anatomy.GeometryProvider;
import io.github.r3neer.scalebrews.platform.anatomy.GravityFrame;
import io.github.r3neer.scalebrews.platform.anatomy.ModelGeometry;
import io.github.r3neer.scalebrews.platform.anatomy.PoseProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/**
 * T2-only client receiver proof. It intentionally exercises full accepted
 * endpoint lifecycle rather than rendering state or PoseHistory interpolation.
 * Register in a dedicated future client selector, never ordinary physics A/B.
 */
public final class AnatomyPresentationFrameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicReference<Integer>();var latest=new AtomicReference<GeometryProvider.PublishedFrame>();
        var rootUpdate=new AtomicReference<GeometryProvider.PublishedFrame>();
        var unavailableUpdate=new AtomicReference<GeometryProvider.PublishedFrame>();
        var recoveredUpdate=new AtomicReference<GeometryProvider.PublishedFrame>();
        var world=context.worldBuilder().create();Throwable failure=null;
        try {
            world.getServer().runOnServer(server->boot(server,geometry,profile,cowId));
            context.waitFor(client->{
                var entity=client.level==null?null:client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },100);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                var first=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                var repeat=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                if(first!=repeat || first.kind()!=AnatomyClientNetworking.PresentationKind.CURRENT_ENDPOINT || first.fraction()!=1
                        || first.before()!=first.after() || !first.evaluated().origin().equals(first.before().sample().origin()))
                    throw new AssertionError("Presentation frame did not cache one exact current endpoint");
                assertImmutable(first);
            });
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(cowId.get());
                var published=AnatomyMovement.publishedFrame(cow).orElseThrow();latest.set(published);
                // Stop only after capturing the core-produced full frame. It
                // prevents the live publisher from racing the deliberately
                // ordered receiver inputs below; every injected packet still
                // uses the real PublishedFrame codec, never render state.
                AnatomyRuntime.stop(server);
                var next=rootOnly(published,published.endpoint().frameSerial()+1,new Vec3(7,3,-2),33);rootUpdate.set(next);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),next);
            });
            context.waitTicks(5);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                if(frame.before().frameSerial()!=latest.get().endpoint().frameSerial()+1 || frame.before().jointSampleTick()!=latest.get().endpoint().jointSampleTick()
                        || !frame.evaluated().origin().equals(new Vec3(7,3,-2)))
                    throw new AssertionError("Root-only same-joint endpoint mixed an old root or interpolated PoseHistory");
            });
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),latest.get()));
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                if(frame.before().frameSerial()!=latest.get().endpoint().frameSerial()+1)throw new AssertionError("Old full endpoint displaced newer presentation frame");
            });
            world.getServer().runOnServer(server->{
                var next=unavailable(rootUpdate.get(),latest.get().endpoint().frameSerial()+2);unavailableUpdate.set(next);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),next);
            });
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                if(AnatomyClientNetworking.presentationFrame(cow).isPresent())throw new AssertionError("Unavailable pose retained a drawable presentation frame");
            });
            world.getServer().runOnServer(server->{
                var next=availableAfterGap(unavailableUpdate.get(),latest.get().endpoint().frameSerial()+3,new Vec3(-3,5,1),-12);recoveredUpdate.set(next);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),next);
            });
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                if(frame.identity().bindingGeneration()!=latest.get().identity().bindingGeneration() || frame.before().frameSerial()!=latest.get().endpoint().frameSerial()+3
                        || frame.before().authorityTick()<=unavailableUpdate.get().endpoint().authorityTick()
                        || frame.before().jointSampleTick()!=unavailableUpdate.get().endpoint().jointSampleTick()
                        || !frame.evaluated().origin().equals(new Vec3(-3,5,1)))
                    throw new AssertionError("Available endpoint after an unavailable gap did not reacquire a fresh static frame");
            });
            long reboundBinding=latest.get().identity().bindingGeneration()+1;
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),rebound(recoveredUpdate.get(),latest.get().endpoint().frameSerial()+4,new Vec3(2,4,8),reboundBinding)));
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                if(frame.identity().bindingGeneration()!=reboundBinding || frame.before().frameSerial()!=latest.get().endpoint().frameSerial()+4 || !frame.evaluated().origin().equals(new Vec3(2,4,8)))
                    throw new AssertionError("New binding did not reacquire only its new static presentation endpoint");
            });
        } catch(Throwable error) {failure=error;throw error;}
        finally {
            try {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
            catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("Presentation proof cleanup failed",cleanup);}
        }
    }

    private static void boot(MinecraftServer server,ModelGeometry geometry,PlatformDefinition profile,AtomicReference<Integer> cowId) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create presentation cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);cowId.set(cow.getId());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:presentation_cow",profile));
    }

    private static GeometryProvider.PublishedFrame rootOnly(GeometryProvider.PublishedFrame prior,long serial,Vec3 origin,float yaw) {
        var old=prior.endpoint();var sample=old.sample();
        var nextSample=new io.github.r3neer.scalebrews.platform.anatomy.AnatomyPoseHistory.Sample(sample.inputs(),origin,yaw,sample.scale(),sample.gravity());
        var root=new AnatomyMovement.RootFrame(old.root().sequence()+1,old.root().tick(),origin,yaw,sample.scale(),sample.gravity());
        var endpoint=new GeometryProvider.CausalEndpoint(serial,old.authorityTick(),old.jointSampleTick(),root,nextSample,GeometryProvider.Availability.AVAILABLE);
        return new GeometryProvider.PublishedFrame(prior.identity(),endpoint);
    }

    private static GeometryProvider.PublishedFrame unavailable(GeometryProvider.PublishedFrame prior,long serial) {
        var old=prior.endpoint();var endpoint=new GeometryProvider.CausalEndpoint(serial,old.authorityTick()+1,old.jointSampleTick()+1,
            new AnatomyMovement.RootFrame(old.root().sequence()+1,old.root().tick()+1,old.root().origin(),old.root().yaw(),old.root().scale(),old.root().gravity()),
            old.sample(),GeometryProvider.Availability.UNAVAILABLE);
        return new GeometryProvider.PublishedFrame(prior.identity(),endpoint);
    }

    /** A new drawable endpoint may reacquire after a gap, but never interpolates through it. */
    private static GeometryProvider.PublishedFrame availableAfterGap(GeometryProvider.PublishedFrame prior,long serial,Vec3 origin,float yaw) {
        var old=prior.endpoint();var sample=old.sample();
        var nextSample=new io.github.r3neer.scalebrews.platform.anatomy.AnatomyPoseHistory.Sample(sample.inputs(),origin,yaw,sample.scale(),sample.gravity());
        var root=new AnatomyMovement.RootFrame(old.root().sequence()+1,old.root().tick()+1,origin,yaw,sample.scale(),sample.gravity());
        // The joint sample remains the last accepted one; only the endpoint
        // authority/root advances. This is a static reacquisition, not a
        // fabricated motion interval across the unavailable gap.
        var endpoint=new GeometryProvider.CausalEndpoint(serial,old.authorityTick()+1,old.jointSampleTick(),root,nextSample,GeometryProvider.Availability.AVAILABLE);
        return new GeometryProvider.PublishedFrame(prior.identity(),endpoint);
    }

    private static GeometryProvider.PublishedFrame rebound(GeometryProvider.PublishedFrame prior,long serial,Vec3 origin,long bindingGeneration) {
        var old=prior.endpoint();var identity=prior.identity();var sample=new io.github.r3neer.scalebrews.platform.anatomy.AnatomyPoseHistory.Sample(
            old.sample().inputs(),origin,old.sample().yaw(),old.sample().scale(),old.sample().gravity());
        var root=new AnatomyMovement.RootFrame(old.root().sequence()+1,old.root().tick()+1,origin,sample.yaw(),sample.scale(),sample.gravity());
        var endpoint=new GeometryProvider.CausalEndpoint(serial,old.authorityTick()+1,old.jointSampleTick(),root,sample,GeometryProvider.Availability.AVAILABLE);
        var rebound=new GeometryProvider.GeometryIdentity(identity.dimension(),identity.support(),identity.entityId(),identity.epoch(),identity.revision(),identity.model(),identity.poseProvider(),bindingGeneration,identity.localRegistrationGeneration()+1);
        return new GeometryProvider.PublishedFrame(rebound,endpoint);
    }

    private static void assertImmutable(AnatomyClientNetworking.PresentationFrame frame) {
        try {frame.evaluated().pieces().clear();throw new AssertionError("Presentation pieces map was mutable");}
        catch(UnsupportedOperationException expected) {}
        var first=frame.evaluated().rootTrs().matrix();first.m00(77);
        if(frame.evaluated().rootTrs().matrix().m00()==77)throw new AssertionError("Presentation affine matrix leaked mutable state");
    }
}
