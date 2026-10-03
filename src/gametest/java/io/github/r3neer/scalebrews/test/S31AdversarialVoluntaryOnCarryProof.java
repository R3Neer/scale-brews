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
public final class S31AdversarialVoluntaryOnCarryProof implements FabricClientGameTest {
    private static volatile java.util.function.Consumer<net.minecraft.client.Minecraft> checkpoint;
    private static boolean installed;
    private record Observation(AnatomyMovement.ClientTransportCursor cursor,Vec3 applied,Vec3 draw) {}
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger();var saved=new AtomicReference<GeometryProvider.PublishedFrame>();var tracking=new AtomicLong();var bodyTracking=new AtomicLong();
        var finished=new AtomicBoolean();var observation=new AtomicReference<Observation>();var observer=new AtomicReference<BiConsumer<Entity,AnatomyMovement.ClientTransportCursor>>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(s->{var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("No Cow");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                s.overworld().addFreshEntity(cow);id.set(cow.getId());var player=s.getPlayerList().getPlayers().getFirst();
                player.getAttribute(Attributes.SCALE).addPermanentModifier(new AttributeModifier(Identifier.parse("s31_adversary:scale"),-.8,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                player.refreshDimensions();player.setNoGravity(true);player.teleportTo(2,3,-2);
                AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("s31_adversary:local",profile));});
            context.waitTicks(5);
            world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(id.get());var player=s.getPlayerList().getPlayers().getFirst();
                saved.set(AnatomyMovement.publishedFrame(cow).orElseThrow());tracking.set(AnatomyRuntime.trackingGeneration(player,cow));bodyTracking.set(AnatomyRuntime.trackingGeneration(player,player));
                AnatomyRuntime.stop(s);saved.set(fixedBaseline(saved.get()));AnatomyNetworking.sendPose(player,saved.get(),tracking.get());});
            context.waitFor(c->c.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                && c.player.getScale()<.21 && AnatomyMovement.queryFrame(cow).isPresent(),160);
            var contact=context.computeOnClient(c->{var cow=(Cow)c.level.getEntity(id.get());var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                String piece="root/head/cube_0";var box=frame.evaluated().pieces().get(piece);var bounds=box.bounds();
                int face=-1;for(int i=0;i<6;i++)if(AnatomyMovement.gravity(c.player).supports(box.faceNormal(i))){face=i;break;}
                if(face<0)throw new AssertionError("No supported material face");
                var worldPoint=new Vec3(bounds.getCenter().x,bounds.maxY,bounds.getCenter().z);var local=box.facePoint(face,worldPoint);
                c.player.setNoGravity(true);c.player.refreshDimensions();c.player.setPos(worldPoint);c.player.setOldPosAndRot();
                return new AnatomyContactPayload(frame.identity().epoch(),frame.identity().revision(),c.level.dimension().identifier(),c.player.getId(),c.player.getUUID(),
                    bodyTracking.get(),1,c.level.getGameTime(),cow.getId(),cow.getUUID(),frame.identity().bindingGeneration(),piece,face,local,box.faceNormal(face));});
            world.getServer().runOnServer(s->ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(),contact));
            context.waitFor(c->AnatomyClientNetworking.presentationContact(c.player,c.level.getGameTime()).isPresent() && AnatomyMovement.supported(c.player),120);
            context.runOnClient(c->{
                try {var f=AnatomyMovement.class.getDeclaredField("CLIENT_TRANSPORT_OBSERVER");f.setAccessible(true);
                    @SuppressWarnings("unchecked") var old=(BiConsumer<Entity,AnatomyMovement.ClientTransportCursor>)f.get(null);observer.set(old);
                    AnatomyMovement.installClientTransportObserver((body,cursor)->{                        old.accept(body,cursor);
                        if(body==c.player && cursor.support().equals(contact.support()))observation.compareAndSet(null,new Observation(cursor,
                            AnatomyMovement.transport(body).appliedDelta(),Vec3.ZERO));});
                } catch(ReflectiveOperationException failure){throw new AssertionError("Observer fixture setup failed",failure);}
                if(!installed) {
                    net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{var task=checkpoint;if(task!=null)task.accept(client);});installed=true;
                }
                checkpoint=client->{var captured=observation.get();if(captured==null || finished.get())return;
                    if(client.player.tickCount!=captured.cursor().bodyTick())throw new AssertionError("Post-carry checkpoint missed original interpolation baseline");
                    var draw=actualRendererOffset(client,client.player,captured.applied());
                    observation.set(new Observation(captured.cursor(),captured.applied(),draw));finished.set(true);checkpoint=null;};
                CausalBodyResidual.refresh(c.player);
            });
            // A quiet accepted endpoint advances physics provenance without moving the material.
            world.getServer().runOnServer(s->AnatomyNetworking.sendPose(s.getPlayerList().getPlayers().getFirst(),next(saved.get(),Vec3.ZERO),tracking.get()));
            context.waitTicks(3);
            world.getServer().runOnServer(s->AnatomyNetworking.sendPose(s.getPlayerList().getPlayers().getFirst(),next(next(saved.get(),Vec3.ZERO),new Vec3(.2,0,0)),tracking.get()));
            context.waitFor(c->finished.get(),120);
            var actual=observation.get();System.out.println("S31 ADVERSARY local contact actual carry beforeSerial="+actual.cursor().beforeSupportFrameSerial()
                +" afterSerial="+actual.cursor().supportFrameSerial()+" applied="+actual.applied()+" rendererPartialHalf="+actual.draw());
            if(actual.applied().lengthSqr()<1e-6)throw new AssertionError("Nonzero fully-applied JOINT carry control missing");
            if(actual.draw().distanceToSqr(actual.applied().scale(.5))>1e-10)throw new AssertionError("Wire-confirmed local JOINT carry lost its nonzero draw residual after a quiet endpoint");
        } finally {context.runOnClient(c->{checkpoint=null;if(observer.get()!=null)AnatomyMovement.installClientTransportObserver(observer.get());CausalBodyResidual.reset();});
            world.getServer().runOnServer(AnatomyRuntime::stop);context.waitTicks(2);world.close();}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static Vec3 actualRendererOffset(net.minecraft.client.Minecraft client,Entity body,Vec3 appliedCarry) {
        var position=body.position();var velocity=body.getDeltaMovement();var old=body.oldPosition();
        var passive=CausalBodyResidual.rendererOffset(body,.5f);
        var voluntary=new Vec3(.025,0,0);
        body.move(MoverType.SELF,voluntary);
        if(body.position().subtract(position).distanceToSqr(voluntary)>1e-10)throw new AssertionError("Native voluntary move was clipped unexpectedly");
        var renderer=(net.minecraft.client.renderer.entity.EntityRenderer)client.getEntityRenderDispatcher().getRenderer(body);
        var state=(net.minecraft.client.renderer.entity.state.EntityRenderState)renderer.createRenderState();
        for(float fraction:new float[]{0,.25f,.5f,.75f,1}) {
            var physical=body.position();var bounds=body.getBoundingBox();var gravity=AnatomyMovement.gravity(body);
            renderer.extractRenderState(body,state,fraction);var offset=renderer.getRenderOffset(state);
            var ordinary=physical.subtract(old).subtract(appliedCarry);
            var expected=physical.subtract(ordinary.scale(1-fraction));var visible=new Vec3(state.x,state.y,state.z).add(offset);
            if(visible.distanceToSqr(expected)>1e-8)throw new AssertionError("Renderer erased or amplified voluntary movement on top of carry: "+fraction+" visible="+visible+" expected="+expected);
            if(!body.position().equals(physical) || !body.getBoundingBox().equals(bounds) || !AnatomyMovement.gravity(body).equals(gravity) || !body.getDeltaMovement().equals(velocity) || !body.oldPosition().equals(old))throw new AssertionError("Draw query changed physical state");
        }
        if(CausalBodyResidual.rendererOffset(body,.5f).distanceToSqr(passive)>1e-12)throw new AssertionError("Voluntary move contaminated passive residual");
        System.out.println("S31 ADVERSARY voluntary-on-carry native move="+voluntary+" passive="+passive);
        return passive;
    }
    private static GeometryProvider.PublishedFrame fixedBaseline(GeometryProvider.PublishedFrame prior) {
        var e=prior.endpoint();var sample=e.sample();
        var inputs=new io.github.r3neer.scalebrews.collision.api.spi.PoseEngine.Inputs(0,0,0,0,0,true,Map.of());
        var normalized=new AnatomyPoseHistory.Sample(inputs,sample.origin(),sample.yaw(),sample.scale(),sample.gravity());
        return new GeometryProvider.PublishedFrame(prior.identity(),new GeometryProvider.CausalEndpoint(e.frameSerial()+1,e.authorityTick()+1,e.jointSampleTick()+1,e.root(),normalized,GeometryProvider.Availability.AVAILABLE));
    }
    private static GeometryProvider.PublishedFrame next(GeometryProvider.PublishedFrame prior,Vec3 delta) {
        var e=prior.endpoint();var sample=e.sample();var origin=sample.origin();
                var old=sample.inputs();var input=delta.lengthSqr()==0?old:new io.github.r3neer.scalebrews.collision.api.spi.PoseEngine.Inputs(
            old.walkPhase(),old.walkAmount(),old.age(),old.headYaw(),old.headPitch()+10,true,old.channels());
        var nextSample=new AnatomyPoseHistory.Sample(input,origin,sample.yaw(),sample.scale(),sample.gravity());
        var root=new RootFrame(e.root().sequence()+1,e.root().tick()+1,origin,sample.yaw(),sample.scale(),sample.gravity());
        return new GeometryProvider.PublishedFrame(prior.identity(),new GeometryProvider.CausalEndpoint(e.frameSerial()+1,e.authorityTick()+1,e.jointSampleTick()+(delta.lengthSqr()==0?0:1),root,nextSample,GeometryProvider.Availability.AVAILABLE));
    }
}
