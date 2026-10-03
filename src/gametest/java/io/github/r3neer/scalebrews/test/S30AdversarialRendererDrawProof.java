package io.github.r3neer.scalebrews.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalPresentationState;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyNetworking;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Actual ModelFeatureRenderer delayed draw vertices versus accepted causal material. */
public final class S30AdversarialRendererDrawProof implements FabricClientGameTest {
    static final class CaptureBuffer extends StagedVertexBuffer {
        final List<Vec3> vertices=new ArrayList<>();
        CaptureBuffer(){super(()->"S30 adversarial draw capture",65536);}
        @Override public VertexConsumer getVertexBuilder(Draw draw) {
            return (VertexConsumer)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{
                if(method.getName().equals("addVertex") && args.length>=3 && args[0] instanceof Number)
                    vertices.add(new Vec3(((Number)args[0]).doubleValue(),((Number)args[1]).doubleValue(),((Number)args[2]).doubleValue()));
                return method.getReturnType()==VertexConsumer.class?proxy:null;
            });
        }
    }
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",
            CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()).withModelTransform(new Matrix4f().scaling(-1,-1,1).translate(0,-1.501f,0)));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger();
        var queued=new AtomicReference<ModelFeatureRenderer.Submit<?>>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("No Cow fixture");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                cow.setYRot(90);cow.yBodyRot=90;cow.yHeadRot=90;
                server.overworld().addFreshEntity(cow);id.set(cow.getId());
                for(int x=1;x<=3;x++)for(int z=-3;z<=-1;z++)server.overworld().setBlock(new BlockPos(x,2,z),Blocks.STONE.defaultBlockState(),3);
                server.getPlayerList().getPlayers().getFirst().teleportTo(2,3,-2);
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s30_adversarial_draw",profile));
            });
            context.waitTicks(5);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                var frame=AnatomyMovement.publishedFrame(cow).orElseThrow();long generation=AnatomyRuntime.trackingGeneration(player,cow);
                if(frame.endpoint().availability()!=GeometryProvider.Availability.AVAILABLE || generation<1)
                    throw new AssertionError("Initial registration/tracking fixture is not prepared");
                AnatomyNetworking.sendPose(player,frame,generation);
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            context.takeScreenshot("s30-adversarial-original-joint-before");
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());cow.setXRot(37);
            });
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow)
                .map(frame->Math.abs(frame.after().sample().inputs().headPitch()-37)<.01).orElse(false),120);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                var pos=cow.position();var bounds=cow.getBoundingBox();var velocity=cow.getDeltaMovement();
                if(!(client.getEntityRenderDispatcher().getRenderer(cow) instanceof LivingEntityRenderer<?,?,?> renderer))throw new AssertionError("Not original living renderer");
                @SuppressWarnings({"unchecked","rawtypes"}) var raw=(LivingEntityRenderer)renderer;
                var state=(LivingEntityRenderState)raw.createRenderState();raw.extractRenderState(cow,state,.25f);
                state.xRot=-61;state.yRot=54;state.walkAnimationPos=2.7f;state.walkAnimationSpeed=1;
                var pose=new PoseStack();pose.translate(state.x,state.y,state.z);
                var storage=new SubmitNodeStorage();raw.submit(state,pose,storage,new CameraRenderState());
                if(!((CausalPresentationState)state).scalebrews$causalSubmitted())throw new AssertionError("No causal original model submission");
                var nodes=new ArrayList<SubmitNode>();storage.order(0).solid.sortInto((node,strict)->nodes.add(node));
                var submit=nodes.stream().filter(n->n instanceof ModelFeatureRenderer.Submit<?> s && s.state()==state)
                    .map(n->(ModelFeatureRenderer.Submit<?>)n).findFirst().orElseThrow();queued.set(submit);
                try(var capture=new CaptureBuffer()) {
                    var feature=new ModelFeatureRenderer();
                    feature.prepareGroup(new FeatureFrameContext(null,null,null,null,null,null,null,capture),List.of(submit),false);
                    if(capture.vertices.isEmpty())throw new AssertionError("Vacuous draw: no actual vertices");
                    int expected=0;
                    for(var box:frame.evaluated().pieces().values())for(var vertex:box.vertices()) {
                        expected++;
                        if(capture.vertices.stream().noneMatch(v->v.distanceToSqr(vertex)<1e-8))
                            throw new AssertionError("Actual delayed draw lacks causal material vertex "+vertex);
                    }
                    System.out.println("S30 ADVERSARY delayed actual renderer PASS vertices="+capture.vertices.size()+" retainedCorners="+expected);
                }
                if(!cow.position().equals(pos) || !cow.getBoundingBox().equals(bounds) || !cow.getDeltaMovement().equals(velocity))
                    throw new AssertionError("Draw changed physical state");
            });
            context.takeScreenshot("s30-adversarial-original-joint-after");
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                var published=AnatomyMovement.publishedFrame(cow).orElseThrow();long generation=AnatomyRuntime.trackingGeneration(player,cow);
                var e=published.endpoint();var old=e.sample();var origin=old.origin().add(.31,.17,-.23);
                AnatomyRuntime.stop(server);
                var sample=new io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory.Sample(old.inputs(),origin,old.yaw(),old.scale(),old.gravity());
                var root=new io.github.r3neer.scalebrews.collision.runtime.RootFrame(e.root().sequence()+1,e.root().tick(),origin,old.yaw(),old.scale(),old.gravity());
                AnatomyNetworking.sendPose(player,new GeometryProvider.PublishedFrame(published.identity(),new GeometryProvider.CausalEndpoint(
                    e.frameSerial()+1,e.authorityTick()+1,e.jointSampleTick(),root,sample,GeometryProvider.Availability.AVAILABLE)),generation);
            });
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow)
                .map(frame->Math.abs(frame.evaluated().origin().x-2.31)<1e-5).orElse(false),120);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());var position=cow.position();
                var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                queued.set(submit(client,cow));
                var expected=new Matrix4f().translate((float)frame.evaluated().origin().x,(float)frame.evaluated().origin().y,(float)frame.evaluated().origin().z)
                    .mul(frame.evaluated().rootTrs().matrix()).mul(frame.evaluated().modelTransform().matrix());
                if(!queued.get().pose().pose().equals(expected,2e-4f))throw new AssertionError("Original model root was not applied exactly once");
                assertPositive(queued.get(),"nonzero causal root correction");
                if(!cow.position().equals(position))throw new AssertionError("Causal root drawing changed physical position");
                System.out.println("S30 ADVERSARY nonzero accepted ROOT correction PASS exact queued matrix, physical position unchanged");
            });
            context.takeScreenshot("s30-adversarial-original-root-after");
            world.getServer().runOnServer(server->AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s30_adversarial_draw",profile)));
            context.waitTicks(5);
            world.getServer().runOnServer(server->{var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                AnatomyNetworking.sendPose(player,AnatomyMovement.publishedFrame(cow).orElseThrow(),AnatomyRuntime.trackingGeneration(player,cow));});
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow)
                .map(frame->Math.abs(frame.evaluated().origin().x-2)<1e-5).orElse(false),240);
            context.runOnClient(client->{assertNoVertices(queued.get(),"runtime rebind/tracking window");queued.set(submit(client,(Cow)client.level.getEntity(id.get())));});
            world.getServer().runOnServer(server->{
                var target=server.getLevel(Level.NETHER);
                if(target==null || server.getPlayerList().getPlayers().getFirst().teleport(
                    new TeleportTransition(target,new Vec3(0,80,0),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING))==null)
                    throw new AssertionError("Dimension fixture could not enter Nether");
            });
            context.waitFor(client->client.level!=null && client.level.dimension()==Level.NETHER,240);
            context.runOnClient(client->{
                try(var capture=new CaptureBuffer()) {
                    new ModelFeatureRenderer().prepareGroup(new FeatureFrameContext(null,null,null,null,null,null,null,capture),List.of(queued.get()),false);
                    if(!capture.vertices.isEmpty())throw new AssertionError("Queued original model drew across dimension barrier");
                }
            });
            world.getServer().runOnServer(server->{
                if(server.getPlayerList().getPlayers().getFirst().teleport(new TeleportTransition(server.overworld(),
                    new Vec3(2,3,-2),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING))==null)
                    throw new AssertionError("Dimension fixture could not return to Overworld");
            });
            context.waitFor(client->client.level!=null && client.level.dimension()==Level.OVERWORLD
                && client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent(),240);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                if(!(client.getEntityRenderDispatcher().getRenderer(cow) instanceof LivingEntityRenderer<?,?,?> renderer))throw new AssertionError("No returned original renderer");
                @SuppressWarnings({"unchecked","rawtypes"}) var raw=(LivingEntityRenderer)renderer;
                var state=(LivingEntityRenderState)raw.createRenderState();raw.extractRenderState(cow,state,.75f);
                var pose=new PoseStack();pose.translate(state.x,state.y,state.z);
                var storage=new SubmitNodeStorage();raw.submit(state,pose,storage,new CameraRenderState());
                var nodes=new ArrayList<SubmitNode>();storage.order(0).solid.sortInto((node,strict)->nodes.add(node));
                var fresh=nodes.stream().filter(n->n instanceof ModelFeatureRenderer.Submit<?> s && s.state()==state)
                    .map(n->(ModelFeatureRenderer.Submit<?>)n).findFirst().orElseThrow();
                if(!((CausalPresentationState)state).scalebrews$causalSubmitted())throw new AssertionError("Return window lacks positive causal model");
                try(var capture=new CaptureBuffer()) {
                    new ModelFeatureRenderer().prepareGroup(new FeatureFrameContext(null,null,null,null,null,null,null,capture),List.of(fresh),false);
                    if(capture.vertices.isEmpty())throw new AssertionError("New dimension window did not draw original model");
                }
                queued.set(fresh);
                System.out.println("S30 ADVERSARY delayed dimension A-B-A PASS retired draw zero, fresh original model positive");
            });
            long priorRevision=context.computeOnClient(client->AnatomyClientNetworking.catalog().revision());
            world.getServer().runOnServer(server->{
                var installed=server.getResourceManager();var pack=installed.listPacks().findFirst().orElseThrow();
                var modelJson=io.github.r3neer.scalebrews.collision.internal.AnatomyCodecs.GEOMETRY.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,geometry).getOrThrow().toString();
                var profileJson=PlatformDefinition.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,profile).getOrThrow().toString();
                var resources=(net.minecraft.server.packs.resources.ResourceManager)Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{net.minecraft.server.packs.resources.ResourceManager.class},(proxy,method,args)->{
                        if(method.getName().equals("listPacks"))return installed.listPacks();
                        if(method.getName().equals("listResources"))return switch((String)args[0]) {
                            case "scalebrews/entity_geometry" -> Map.of(Identifier.parse("minecraft:scalebrews/entity_geometry/cow.json"),
                                new net.minecraft.server.packs.resources.Resource(pack,()->new java.io.ByteArrayInputStream(modelJson.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                            case "scalebrews/entity_platform" -> Map.of(Identifier.parse("s30_adversary:scalebrews/entity_platform/cow.json"),
                                new net.minecraft.server.packs.resources.Resource(pack,()->new java.io.ByteArrayInputStream(profileJson.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                            default -> Map.of();
                        };
                        throw new UnsupportedOperationException(method.toString());
                    });
                try {
                    var reload=AnatomyRuntime.class.getDeclaredMethod("reload",net.minecraft.server.MinecraftServer.class,net.minecraft.server.packs.resources.ResourceManager.class);
                    reload.setAccessible(true);reload.invoke(null,server,resources);
                } catch(ReflectiveOperationException failure){throw new AssertionError("Accepted catalog fixture",failure);}
            });
            context.waitFor(client->AnatomyClientNetworking.catalog().ready() && AnatomyClientNetworking.catalog().revision()>priorRevision,240);
            context.runOnClient(client->assertNoVertices(queued.get(),"accepted catalog/binding barrier"));
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow)
                .map(frame->frame.identity().revision()>priorRevision).orElse(false),240);
            context.runOnClient(client->{queued.set(submit(client,(Cow)client.level.getEntity(id.get())));assertPositive(queued.get(),"accepted catalog/binding recovery");});
            var priorSerial=new AtomicLong(context.computeOnClient(client->AnatomyClientNetworking.presentationFrame((Cow)client.level.getEntity(id.get())).orElseThrow().after().frameSerial()));
            world.getServer().runOnServer(server->{var cow=(Cow)server.overworld().getEntity(id.get());cow.teleportTo(cow.getX()+.75,cow.getY(),cow.getZ());});
            context.waitFor(client->client.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow)
                .map(frame->frame.after().frameSerial()>priorSerial.get() && frame.evaluated().origin().x>2.5).orElse(false),240);
            context.runOnClient(client->{
                assertNoVertices(queued.get(),"support teleport endpoint replacement");
                queued.set(submit(client,(Cow)client.level.getEntity(id.get())));assertPositive(queued.get(),"support teleport fresh root");
                System.out.println("S30 ADVERSARY accepted catalog/binding and short teleport PASS retired draw zero, fresh original model positive");
            });
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());var player=server.getPlayerList().getPlayers().getFirst();
                var old=AnatomyMovement.publishedFrame(cow).orElseThrow();long generation=AnatomyRuntime.trackingGeneration(player,cow);
                AnatomyRuntime.stop(server);
                var e=old.endpoint();
                AnatomyNetworking.sendPose(player,new GeometryProvider.PublishedFrame(old.identity(),new GeometryProvider.CausalEndpoint(
                    e.frameSerial()+1,e.authorityTick()+1,e.jointSampleTick(),e.root(),e.sample(),GeometryProvider.Availability.UNAVAILABLE)),generation);
            });
            context.waitFor(client->AnatomyClientNetworking.presentationFrame((Cow)client.level.getEntity(id.get())).isEmpty(),120);
            context.runOnClient(client->{
                try(var capture=new CaptureBuffer()) {
                    new ModelFeatureRenderer().prepareGroup(new FeatureFrameContext(null,null,null,null,null,null,null,capture),List.of(queued.get()),false);
                    if(!capture.vertices.isEmpty())throw new AssertionError("Delayed actual renderer drew across unavailable barrier");
                    System.out.println("S30 ADVERSARY delayed lifecycle barrier PASS emittedVertices=0");
                }
            });
        } finally {world.getServer().runOnServer(AnatomyRuntime::stop);context.waitTicks(2);world.close();}
    }
    static ModelFeatureRenderer.Submit<?> submit(net.minecraft.client.Minecraft client,Cow cow) {
        if(!(client.getEntityRenderDispatcher().getRenderer(cow) instanceof LivingEntityRenderer<?,?,?> renderer))throw new AssertionError("Missing original renderer");
        @SuppressWarnings({"unchecked","rawtypes"}) var raw=(LivingEntityRenderer)renderer;
        var state=(LivingEntityRenderState)raw.createRenderState();raw.extractRenderState(cow,state,.5f);
        var pose=new PoseStack();pose.translate(state.x,state.y,state.z);
        var storage=new SubmitNodeStorage();raw.submit(state,pose,storage,new CameraRenderState());
        if(!((CausalPresentationState)state).scalebrews$causalSubmitted())throw new AssertionError("Fresh root submission failed");
        var nodes=new ArrayList<SubmitNode>();storage.order(0).solid.sortInto((node,strict)->nodes.add(node));
        return nodes.stream().filter(n->n instanceof ModelFeatureRenderer.Submit<?> s && s.state()==state)
            .map(n->(ModelFeatureRenderer.Submit<?>)n).findFirst().orElseThrow();
    }
    static void assertNoVertices(ModelFeatureRenderer.Submit<?> submit,String scope) {
        try(var capture=new CaptureBuffer()) {
            new ModelFeatureRenderer().prepareGroup(new FeatureFrameContext(null,null,null,null,null,null,null,capture),List.of(submit),false);
            if(!capture.vertices.isEmpty())throw new AssertionError("Queued original model drew across "+scope);
        }
    }
    static void assertPositive(ModelFeatureRenderer.Submit<?> submit,String scope) {
        try(var capture=new CaptureBuffer()) {
            new ModelFeatureRenderer().prepareGroup(new FeatureFrameContext(null,null,null,null,null,null,null,capture),List.of(submit),false);
            if(capture.vertices.isEmpty())throw new AssertionError("Vacuous original model recovery at "+scope);
        }
    }
}
