package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalBodyResidual;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyContactPayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyNetworking;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** A real local carry must close the vanilla render interpolation gap at the accepted Cow ROOT. */
public final class S31ImplementerResidualClientProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);
        var tracking=new AtomicLong();
        var initial=new AtomicReference<GeometryProvider.PublishedFrame>();
        var seen=new AtomicReference<Vec3>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("S31 Cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                server.overworld().addFreshEntity(cow);cowId.set(cow.getId());
                var player=server.getPlayerList().getPlayers().getFirst();
                player.setNoGravity(true);
                player.getAttribute(Attributes.SCALE).setBaseValue(.2);player.refreshDimensions();
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s31_cow",profile));
            });
            context.waitFor(client->client.level!=null && client.player!=null && Math.abs(client.player.getScale()-.2)<.001
                && client.level.getEntity(cowId.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),160);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(cowId.get());
                var player=server.getPlayerList().getPlayers().getFirst();
                var frame=AnatomyMovement.publishedFrame(cow).orElseThrow();
                var box=AnatomyMovement.queryFrame(cow).orElseThrow().snapshot().pieces().get("root/body/cube_0");
                if(box==null)throw new AssertionError("S31 material body cuboid absent");
                int face=box.closestFace(new Vec3(0,1,0));
                var local=box.facePoint(face,box.bounds().getCenter());
                var point=box.point(local);
                player.teleportTo(point.x,point.y,point.z);
                long generation=AnatomyRuntime.trackingGeneration(player,cow);
                if(generation<1)throw new AssertionError("S31 Cow not tracked");
                tracking.set(generation);initial.set(frame);
                ServerPlayNetworking.send(player,new AnatomyContactPayload(AnatomyNetworking.epoch(server),
                    AnatomyNetworking.revision(server),server.overworld().dimension().identifier(),player.getId(),
                    player.getUUID(),1,10_000,server.overworld().getGameTime(),cow.getId(),cow.getUUID(),
                    frame.identity().bindingGeneration(),"root/body/cube_0",face,local,box.faceNormal(face)));
                AnatomyRuntime.stop(server);
            });
            context.waitFor(client->AnatomyClientNetworking.presentationContact(client.player,client.level.getGameTime()).isPresent()
                && AnatomyMovement.contact(client.player)!=null,160);
            var accepted=context.computeOnClient(client->AnatomyClientNetworking.presentationFrame(
                (Cow)client.level.getEntity(cowId.get())).orElseThrow());
            world.getServer().runOnServer(server->{
                var next=rootOnly(new GeometryProvider.PublishedFrame(initial.get().identity(),accepted.after()),.2);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),next,tracking.get());
            });
            context.waitFor(client->{
                var cow=client.level==null?null:client.level.getEntity(cowId.get());
                if(!(cow instanceof Cow living) || AnatomyClientNetworking.presentationFrame(living)
                    .map(frame->frame.after().frameSerial()==accepted.after().frameSerial()+1).orElse(false)==false)
                    return false;
                var transport=AnatomyMovement.transport(client.player);
                if(transport==null || Math.abs(transport.appliedDelta().x-.2)>.001)return false;
                var position=client.player.position();
                var bounds=client.player.getBoundingBox();
                var velocity=client.player.getDeltaMovement();
                for(float partial:new float[]{0f,.25f,.5f,1f}) {
                    double expected=.2*(1-partial);
                    // Camera-first and renderer-first reads must agree without consuming the sample.
                    var cameraFirst=CausalBodyResidual.cameraOffset(client.player,partial);
                    var renderer=CausalBodyResidual.rendererOffset(client.player,partial);
                    var cameraAgain=CausalBodyResidual.cameraOffset(client.player,partial);
                    if(Math.abs(renderer.x-expected)>.005 || Math.abs(renderer.y)>1e-8
                            || Math.abs(renderer.z)>1e-8 || !cameraFirst.equals(renderer)
                            || !cameraAgain.equals(renderer))return false;
                }
                if(!client.player.position().equals(position) || !client.player.getBoundingBox().equals(bounds)
                        || !client.player.getDeltaMovement().equals(velocity))
                    throw new AssertionError("S31 visual reads changed physical state");
                seen.set(CausalBodyResidual.rendererOffset(client.player,.5f));return true;
            },120);
            if(seen.get()==null)throw new AssertionError("S31 did not observe an interpolated local carry residual");
            System.out.println("S31 IMPLEMENTER RESIDUAL PASS accepted ROOT carry and draw/camera fraction");
        } finally {
            world.getServer().runOnServer(AnatomyRuntime::stop);
            world.close();
        }
    }
    private static GeometryProvider.PublishedFrame rootOnly(GeometryProvider.PublishedFrame prior,double dx) {
        var old=prior.endpoint();var sample=old.sample();var origin=old.root().origin().add(dx,0,0);
        var nextSample=new io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory.Sample(
            sample.inputs(),origin,sample.yaw(),sample.scale(),sample.gravity());
        var root=new RootFrame(old.root().sequence()+1,old.root().tick(),origin,sample.yaw(),sample.scale(),sample.gravity());
        var endpoint=new GeometryProvider.CausalEndpoint(old.frameSerial()+1,old.authorityTick()+1,old.jointSampleTick(),
            root,nextSample,GeometryProvider.Availability.AVAILABLE);
        return new GeometryProvider.PublishedFrame(prior.identity(),endpoint);
    }
}
