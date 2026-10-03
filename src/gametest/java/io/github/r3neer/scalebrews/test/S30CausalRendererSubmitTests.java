package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalModelRender;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalPresentationState;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyNetworking;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Real renderer submission is compared with the accepted geometry frame, not a test-side animation. */
public final class S30CausalRendererSubmitTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of())
            .withModelTransform(new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger();
        var tracking=new AtomicLong();
        var changed=new AtomicReference<GeometryProvider.PublishedFrame>();
        var queuedState=new AtomicReference<LivingEntityRenderState>();
        var queuedModel=new AtomicReference<CowModel>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("No Cow");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                server.overworld().addFreshEntity(cow);cowId.set(cow.getId());
                for(int x=1;x<=3;x++)for(int z=-3;z<=-1;z++)
                    server.overworld().setBlock(new BlockPos(x,2,z),Blocks.STONE.defaultBlockState(),3);
                server.getPlayerList().getPlayers().getFirst().teleportTo(2,3,-2);
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s30_cow",profile));
            });
            context.waitTicks(5);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(cowId.get());
                var player=server.getPlayerList().getPlayers().getFirst();
                var published=AnatomyMovement.publishedFrame(cow).orElseThrow();
                long generation=AnatomyRuntime.trackingGeneration(player,cow);
                if(generation<1)throw new AssertionError("No tracked recipient");
                tracking.set(generation);
                if(published.endpoint().availability()!=GeometryProvider.Availability.AVAILABLE)
                    throw new AssertionError("Renderer fixture must start with available material");
                AnatomyNetworking.sendPose(player,published,generation);
            });
            context.waitFor(client->{
                var entity=client.level==null?null:client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },120);
            context.takeScreenshot("s30-original-cow-before");
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(cowId.get());
                var prior=AnatomyMovement.publishedFrame(cow).orElseThrow();
                var recipient=server.getPlayerList().getPlayers().getFirst();
                long generation=AnatomyRuntime.trackingGeneration(recipient,cow);
                if(generation<1)throw new AssertionError("No tracking authority");
                AnatomyRuntime.stop(server);
                var next=rootOnly(prior);changed.set(next);
                AnatomyNetworking.sendPose(recipient,next,generation);
            });
            context.waitFor(client->{
                var entity=client.level==null?null:client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow)
                    .map(frame->frame.evaluated().origin().equals(new Vec3(2.75,3.25,1.5))).orElse(false);
            },120);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                var physicalPosition=cow.position();var physicalBounds=cow.getBoundingBox();
                var physicalVelocity=cow.getDeltaMovement();
                var renderer=client.getEntityRenderDispatcher().getRenderer(cow);
                if(!(renderer instanceof LivingEntityRenderer<?,?,?> living))throw new AssertionError("No living renderer");
                @SuppressWarnings({"rawtypes","unchecked"}) var raw=(LivingEntityRenderer)living;
                LivingEntityRenderState state=(LivingEntityRenderState)raw.createRenderState();
                raw.extractRenderState(cow,state,1);
                if(((CausalPresentationState)state).scalebrews$causalToken()==null)throw new AssertionError("Renderer failed to capture accepted endpoint");
                var pose=new PoseStack();pose.translate(state.x,state.y,state.z);
                var storage=new SubmitNodeStorage();
                raw.submit(state,pose,storage,new CameraRenderState());
                SubmitNodeCollection collected=storage.order(0);
                if(!((CausalPresentationState)state).scalebrews$causalSubmitted())throw new AssertionError("Renderer did not submit causal root");
                var submits=new ArrayList<SubmitNode>();
                collected.solid.sortInto((node,strict)->submits.add(node));
                var original=submits.stream().filter(node->node instanceof ModelFeatureRenderer.Submit<?> submit && submit.state()==state)
                    .map(node->(ModelFeatureRenderer.Submit<?>)node).findFirst().orElseThrow();
                var expected=new Matrix4f().translate((float)frame.evaluated().origin().x,
                    (float)frame.evaluated().origin().y,(float)frame.evaluated().origin().z)
                    .mul(frame.evaluated().rootTrs().matrix()).mul(frame.evaluated().modelTransform().matrix());
                if(!original.pose().pose().equals(expected,2e-4f))
                    throw new AssertionError("Queued original Cow model has noncausal root: got "+original.pose().pose()+" expected "+expected);
                if(!pose.last().pose().equals(new Matrix4f().translate((float)state.x,(float)state.y,(float)state.z),1e-6f))
                    throw new AssertionError("Renderer leaked its model transform into the shared PoseStack");
                if(!(original.model() instanceof CowModel model))throw new AssertionError("Queued base model is not original CowModel");
                queuedState.set(state);queuedModel.set(model);
                state.walkAnimationPos=2;state.walkAnimationSpeed=1;
                model.setupAnim(state);
                var leg=model.root().getChild("right_front_leg");
                var vanilla=leg.storePose();
                CausalModelRender.draw(state,model,()->{
                    var actualPose=new PoseStack();leg.translateAndRotate(actualPose);
                    if(!actualPose.last().pose().equals(frame.evaluated().localPartTransforms().get("root/right_front_leg").matrix(),2e-4f))
                        throw new AssertionError("Delayed original-model draw did not consume the accepted joint");
                });
                if(!leg.storePose().equals(vanilla))throw new AssertionError("Delayed draw leaked into the shared Cow model");
                if(!cow.position().equals(physicalPosition) || !cow.getBoundingBox().equals(physicalBounds)
                        || !cow.getDeltaMovement().equals(physicalVelocity))
                    throw new AssertionError("Presentation changed physical entity state");
            });
            context.takeScreenshot("s30-causal-original-cow");
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(
                server.getPlayerList().getPlayers().getFirst(),unavailable(changed.get()),tracking.get()));
            context.waitFor(client->{
                var entity=client.level==null?null:client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isEmpty();
            },120);
            context.runOnClient(client->CausalModelRender.draw(queuedState.get(),queuedModel.get(),
                ()->{throw new AssertionError("Retired queued model drew after unavailable barrier");}));
        } finally {
            world.getServer().runOnServer(AnatomyRuntime::stop);
            world.close();
        }
    }

    private static GeometryProvider.PublishedFrame rootOnly(GeometryProvider.PublishedFrame prior){
        var old=prior.endpoint();var sample=old.sample();var origin=new Vec3(2.75,3.25,1.5);
        var nextSample=new io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory.Sample(
            sample.inputs(),origin,sample.yaw(),sample.scale(),sample.gravity());
        var root=new RootFrame(old.root().sequence()+1,old.root().tick(),origin,sample.yaw(),sample.scale(),sample.gravity());
        var endpoint=new GeometryProvider.CausalEndpoint(old.frameSerial()+1,old.authorityTick()+1,old.jointSampleTick(),
            root,nextSample,GeometryProvider.Availability.AVAILABLE);
        return new GeometryProvider.PublishedFrame(prior.identity(),endpoint);
    }
    private static GeometryProvider.PublishedFrame unavailable(GeometryProvider.PublishedFrame prior){
        var old=prior.endpoint();
        var endpoint=new GeometryProvider.CausalEndpoint(old.frameSerial()+1,old.authorityTick()+1,old.jointSampleTick(),
            old.root(),old.sample(),GeometryProvider.Availability.UNAVAILABLE);
        return new GeometryProvider.PublishedFrame(prior.identity(),endpoint);
    }
}
