package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalBodyResidual;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Wire-confirmed local contact -> real carry callback -> positive causal renderer residual. */
public final class S31AdversarialControlledVehicleProof implements FabricClientGameTest {
    private record Observation(AnatomyMovement.ClientTransportCursor cursor,Vec3 applied,Vec3 draw) {}
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var bodyId=new AtomicInteger();var id=new AtomicInteger();var saved=new AtomicReference<GeometryProvider.PublishedFrame>();var tracking=new AtomicLong();var bodyTracking=new AtomicLong();
        var observation=new AtomicReference<Observation>();var observer=new AtomicReference<BiConsumer<Entity,AnatomyMovement.ClientTransportCursor>>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(s->{var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("No Cow");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                cow.getAttribute(Attributes.SCALE).addPermanentModifier(new AttributeModifier(Identifier.parse("s31_adversary:large"),9,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));cow.refreshDimensions();
                s.overworld().addFreshEntity(cow);id.set(cow.getId());var player=s.getPlayerList().getPlayers().getFirst();
                player.getAttribute(Attributes.SCALE).addPermanentModifier(new AttributeModifier(Identifier.parse("s31_adversary:scale"),-.8,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                player.refreshDimensions();player.setNoGravity(true);player.teleportTo(2,3,-2);
                var boat=EntityTypes.OAK_BOAT.create(s.overworld(),EntitySpawnReason.COMMAND);if(boat==null)throw new AssertionError("No boat");
                boat.setNoGravity(true);boat.setPos(2,3,-2);s.overworld().addFreshEntity(boat);bodyId.set(boat.getId());
                if(!player.startRiding(boat,true,true))throw new AssertionError("Real control acquisition failed");
                AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("s31_adversary:local",profile));});
            context.waitTicks(5);
            world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(id.get());var player=s.getPlayerList().getPlayers().getFirst();
                saved.set(AnatomyMovement.publishedFrame(cow).orElseThrow());tracking.set(AnatomyRuntime.trackingGeneration(player,cow));bodyTracking.set(AnatomyRuntime.trackingGeneration(player,s.overworld().getEntity(bodyId.get())));
                AnatomyRuntime.stop(s);AnatomyNetworking.sendPose(player,saved.get(),tracking.get());});
            context.waitFor(c->c.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                && c.player.getRootVehicle().getId()==bodyId.get() && c.player.getRootVehicle().isLocalInstanceAuthoritative() && AnatomyMovement.queryFrame(cow).isPresent(),160);
            var contact=context.computeOnClient(c->{var cow=(Cow)c.level.getEntity(id.get());var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                String piece="root/body/cube_0";var box=frame.evaluated().pieces().get(piece);var bounds=box.bounds();
                int face=-1;for(int i=0;i<6;i++)if(AnatomyMovement.gravity(c.player.getRootVehicle()).supports(box.faceNormal(i))){face=i;break;}
                if(face<0)throw new AssertionError("No supported material face");
                var worldPoint=new Vec3(bounds.getCenter().x,bounds.maxY,bounds.getCenter().z);var local=box.facePoint(face,worldPoint);
                c.player.getRootVehicle().setNoGravity(true);c.player.getRootVehicle().setPos(worldPoint);c.player.getRootVehicle().setOldPosAndRot();
                return new AnatomyContactPayload(frame.identity().epoch(),frame.identity().revision(),c.level.dimension().identifier(),c.player.getRootVehicle().getId(),c.player.getRootVehicle().getUUID(),
                    bodyTracking.get(),1,c.level.getGameTime(),cow.getId(),cow.getUUID(),frame.identity().bindingGeneration(),piece,face,local,box.faceNormal(face));});
            world.getServer().runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),contact));
            context.waitFor(c->AnatomyClientNetworking.presentationContact(c.player.getRootVehicle(),c.level.getGameTime()).isPresent() && AnatomyMovement.supported(c.player.getRootVehicle()),120);
            context.runOnClient(c->{
                try {var f=AnatomyMovement.class.getDeclaredField("CLIENT_TRANSPORT_OBSERVER");f.setAccessible(true);
                    @SuppressWarnings("unchecked") var old=(BiConsumer<Entity,AnatomyMovement.ClientTransportCursor>)f.get(null);observer.set(old);
                    AnatomyMovement.installClientTransportObserver((body,cursor)->{old.accept(body,cursor);
                        if(body==c.player.getRootVehicle() && cursor.support().equals(contact.support()))observation.compareAndSet(null,new Observation(cursor,
                            AnatomyMovement.transport(body).appliedDelta(),verifiedVehicleDraw(c,body)));});
                } catch(ReflectiveOperationException failure){throw new AssertionError("Observer fixture setup failed",failure);}
                CausalBodyResidual.refresh(c.player.getRootVehicle());
            });
            // A quiet accepted endpoint advances physics provenance without moving the material.
            world.getServer().runOnServer(s->AnatomyNetworking.sendPose(s.getPlayerList().getPlayers().getFirst(),next(saved.get(),Vec3.ZERO),tracking.get()));
            context.waitTicks(3);
            world.getServer().runOnServer(s->AnatomyNetworking.sendPose(s.getPlayerList().getPlayers().getFirst(),next(next(saved.get(),Vec3.ZERO),new Vec3(.2,0,0)),tracking.get()));
            context.waitFor(c->observation.get()!=null,120);
            var actual=observation.get();System.out.println("S31 ADVERSARY local contact actual carry beforeSerial="+actual.cursor().beforeSupportFrameSerial()
                +" afterSerial="+actual.cursor().supportFrameSerial()+" applied="+actual.applied()+" rendererPartialHalf="+actual.draw());
            if(Math.abs(actual.applied().x-.2)>1e-5 || actual.applied().lengthSqr()<.01)throw new AssertionError("Nonzero fully-applied ROOT carry control missing");
            if(actual.draw().distanceToSqr(actual.applied().scale(.5))>1e-10)throw new AssertionError("Wire-confirmed local ROOT carry lost its nonzero draw residual after a quiet endpoint");
        } finally {context.runOnClient(c->{if(observer.get()!=null)AnatomyMovement.installClientTransportObserver(observer.get());CausalBodyResidual.reset();});
            world.getServer().runOnServer(AnatomyRuntime::stop);context.waitTicks(2);world.close();}
    }
    private static Vec3 verifiedVehicleDraw(net.minecraft.client.Minecraft c,Entity vehicle) {
        Vec3 value=CausalBodyResidual.rendererOffset(vehicle,.5f);
        if(CausalBodyResidual.rendererOffset(c.player,.5f).lengthSqr()!=0)throw new AssertionError("Passenger avatar received a second vehicle correction");
        if(CausalBodyResidual.cameraOffset(c.player,.5f).distanceToSqr(value)>1e-12)throw new AssertionError("Vehicle owner correction did not reach rider camera");
        return value;
    }
    private static GeometryProvider.PublishedFrame next(GeometryProvider.PublishedFrame prior,Vec3 delta) {
        var e=prior.endpoint();var sample=e.sample();var origin=sample.origin().add(delta);
        var nextSample=new AnatomyPoseHistory.Sample(sample.inputs(),origin,sample.yaw(),sample.scale(),sample.gravity());
        var root=new RootFrame(e.root().sequence()+1,e.root().tick()+1,origin,sample.yaw(),sample.scale(),sample.gravity());
        return new GeometryProvider.PublishedFrame(prior.identity(),new GeometryProvider.CausalEndpoint(e.frameSerial()+1,e.authorityTick()+1,e.jointSampleTick(),root,nextSample,GeometryProvider.Availability.AVAILABLE));
    }
}
