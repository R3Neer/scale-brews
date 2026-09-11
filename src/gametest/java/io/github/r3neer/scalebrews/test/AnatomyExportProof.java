package io.github.r3neer.scalebrews.test;

import com.google.gson.GsonBuilder;
import io.github.r3neer.scalebrews.client.platform.anatomy.GeometryExtractor;
import io.github.r3neer.scalebrews.platform.anatomy.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.file.*;

/** Original-model export fixture. Does not register exported geometry as gameplay yet. */
public class AnatomyExportProof implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client->{
            var cow=CowModel.createBodyLayer().bakeRoot();
            verifyVanilla(cow,Set.of(),"minecraft:cow");
            verifyCowPoses();
            verifyVanillaFamilies();
            var skinLayers=Set.of("hat","jacket","left_sleeve","right_sleeve","left_pants","right_pants");
            for(boolean slim:new boolean[]{false,true}) {
                var root=LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot();
                verifyVanilla(root,skinLayers,"minecraft:player_"+(slim?"slim":"wide"));
                verifyPlayerPoses(root,slim,skinLayers);
            }
            if(FabricLoader.getInstance().isModLoaded("alexsmobs"))try {
                Object bear=Class.forName("com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear").getConstructor().newInstance();
                // The model constructor defaults to young; this fixture explicitly tests adults.
                bear.getClass().getField("young").setBoolean(bear,false);
                verifyAlexVertices(bear);
                var result=GeometryExtractor.alex("alexsmobs:grizzly_bear","2.1.9",bear,Set.of("hat","microphone"));
                if(result.pieces().isEmpty())throw new AssertionError("Alex extraction returned no cubes");
                save(result);
                System.out.println("ANATOMY_EXPORT alexsmobs:grizzly_bear pieces="+result.pieces().size());
            }catch(ReflectiveOperationException e){throw new AssertionError("Alex extraction failed",e);}
        });
        AnatomyLivePoseAcceptanceTests.verifyCow(context);
        if(FabricLoader.getInstance().isModLoaded("alexsmobs"))try(var world=context.worldBuilder().create()) {
            context.runOnClient(client->{
                try {
                    Object model=Class.forName("com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear").getConstructor().newInstance();
                    model.getClass().getField("young").setBoolean(model,false);
                    var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("alexsmobs:grizzly_bear"));
                    var entity=type.create(client.level,net.minecraft.world.entity.EntitySpawnReason.LOAD);
                    var geometry=GeometryExtractor.alex("alexsmobs:grizzly_bear","2.1.9",model,Set.of("hat","microphone"));
                    var renderer=client.getEntityRenderDispatcher().getRenderer(entity);
                    geometry=geometry.withModelTransform(rendererRoot(renderer,entity,true));
                    save(geometry);
                    for(int tick=0;tick<80;tick++) {
                        var inputs=new PoseProvider.Inputs(tick*.37f,(tick%20)/20f,tick,tick%80-40,tick%50-25,true);
                        model.getClass().getMethod("setupAnim",entity.getClass(),float.class,float.class,float.class,float.class,float.class)
                            .invoke(model,entity,inputs.walkPhase(),inputs.walkAmount(),inputs.age(),inputs.headYaw(),inputs.headPitch());
                        var expected=GeometryExtractor.alex("alexsmobs:grizzly_bear","2.1.9",model,Set.of("hat","microphone"));
                        compare(geometry.evaluate(new Matrix4f(),new GrizzlyPose().evaluate(geometry,inputs).orElseThrow(),new AnatomyFilter(0,0,0)),
                            expected.evaluate(new Matrix4f(),Map.of(),new AnatomyFilter(0,0,0)),"grizzly tick "+tick);
                    }
                    System.out.println("ANATOMY_POSE alexsmobs:grizzly_bear 80 animated pose comparisons passed");
                }catch(ReflectiveOperationException e){throw new AssertionError("Grizzly pose proof failed",e);}
            });
            var models=context.computeOnClient(client->{
                Map<String,ModelGeometry> result=new TreeMap<>();
                try(var files=Files.list(FabricLoader.getInstance().getGameDir().resolve("anatomy-export"))) {
                    for(var file:files.filter(p->p.toString().endsWith(".json")).toList()) {
                        var geometry=new com.google.gson.Gson().fromJson(Files.readString(file),ModelGeometry.class);result.put(geometry.source(),geometry);
                    }
                }catch(java.io.IOException e){throw new RuntimeException(e);}
                return result;
            });
            world.getServer().runOnServer(server->AnatomyNetworking.sendCatalog(server.getPlayerList().getPlayers().getFirst(),1,models));
            context.waitTicks(10);
            context.runOnClient(client->{
                var received=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.catalog();
                if(received.revision()!=1 || !received.snapshot().models().equals(models))throw new AssertionError("Real network catalog differs from server authority");
                System.out.println("ANATOMY_NETWORK server catalog round trip passed: "+models.size()+" models");
            });
            var sentPose=new PoseProvider.Inputs(.75f,.5f,12,179,10,true);
            var sentFrame=new AtomicReference<AnatomyPoseHistory.Sample>();
            var firstPublished=new AtomicReference<GeometryProvider.PublishedFrame>();
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                AnatomyMovement.gravity(player,new GravityFrame(net.minecraft.core.Direction.EAST));
                long authorityTick=server.overworld().getGameTime();
                var sample=new AnatomyPoseHistory.Sample(sentPose,player.position(),player.yBodyRot,player.getScale(),AnatomyMovement.gravity(player));
                var root=new AnatomyMovement.RootFrame(1,authorityTick,sample.origin(),sample.yaw(),sample.scale(),sample.gravity());
                var identity=new GeometryProvider.GeometryIdentity(player.level().dimension(),player.getUUID(),player.getId(),
                    AnatomyNetworking.epoch(server),1,net.minecraft.resources.Identifier.parse("minecraft:player_wide"),
                    net.minecraft.resources.Identifier.parse("scalebrews:player_walking"),1,1);
                var endpoint=new GeometryProvider.CausalEndpoint(1,authorityTick,authorityTick,root,sample,GeometryProvider.Availability.AVAILABLE);
                var published=new GeometryProvider.PublishedFrame(identity,endpoint);
                sentFrame.set(sample);
                firstPublished.set(published);
                // Publication must retain this frame even if root state changes
                // after provider capture and before networking serializes it.
                player.setPos(sample.origin().add(3,1,-2));
                player.yBodyRot+=47;
                AnatomyMovement.gravity(player,new GravityFrame(net.minecraft.core.Direction.SOUTH));
                AnatomyNetworking.sendPose(player,published);
            });
            context.waitTicks(10);
            context.runOnClient(client->{
                var history=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.pose(client.player.getUUID());
                if(history==null || !history.current().inputs().equals(sentPose))throw new AssertionError("Authoritative pose channels did not survive real network transfer");
                var packet=history.current();
                var sample=sentFrame.get();
                if(sample==null || !packet.origin().equals(sample.origin()) || packet.yaw()!=sample.yaw() || packet.scale()!=sample.scale())
                    throw new AssertionError("Pose publication read a live root instead of the captured authority frame");
                if(packet.gravity()!=net.minecraft.core.Direction.EAST)throw new AssertionError("Gravity frame did not survive actual pose network transfer");
                var geometry=models.get("minecraft:player_wide");
                var evaluator=new ModelGeometryProvider(geometry,new PlayerWalkingPose(),AnatomyFilter.DEFAULT,packet.revision());
                var frame=history.sample(packet.jointSampleTick());
                var result=evaluator.sampleAt(client.player,frame).orElseThrow();
                var root=new GravityFrame(packet.gravity()).matrix().rotateY((float)Math.toRadians(180-packet.yaw())).scale(packet.scale()).mul(ModelGeometry.matrix(geometry.modelTransform()));
                Map<String,ConvexBox> expected=new LinkedHashMap<>();
                geometry.evaluate(root,new PlayerWalkingPose().evaluate(geometry,sentPose).orElseThrow(),AnatomyFilter.DEFAULT)
                    .forEach((id,box)->expected.put(id,box.move(packet.origin())));
                compare(result.pieces(),expected,"network pose world geometry");
                var shared=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(client.player,packet.jointSampleTick()).orElseThrow();
                compare(shared.pieces(),expected,"shared client geometry query");
                for(int query=0;query<100;query++)evaluator.sampleAt(client.player,frame);
                if(evaluator.evaluations()!=1)throw new AssertionError("Observer queries recomputed identical geometry");
                var nextInputs=new PoseProvider.Inputs(2,.8f,13,-170,-20,true);
                var next=new AnatomyPosePayload(packet.epoch(),packet.revision(),packet.dimension(),packet.entityId(),packet.entity(),packet.model(),packet.provider(),
                    packet.jointSampleTick()+1,nextInputs,packet.origin().add(.1,.05,0),packet.yaw()+10,packet.scale()*1.05f,packet.gravity());
                var interpolation=new AnatomyPoseHistory();interpolation.accept(packet);interpolation.accept(next);
                var segment=interpolation.segment(packet.jointSampleTick()+.5);
                var trajectory=evaluator.motionBetween(segment.before(),segment.after()).orElseThrow();
                Map<String,ConvexBox> midpoint=new LinkedHashMap<>();trajectory.pieces().forEach((id,motion)->midpoint.put(id,motion.at().apply(.5)));
                var sampled=evaluator.sampleInterpolated(client.player,interpolation,packet.jointSampleTick()+.5).orElseThrow();
                compare(sampled.pieces(),midpoint,"presentation follows physical joint trajectory");
                long count=evaluator.evaluations();
                for(int query=0;query<100;query++)evaluator.sampleInterpolated(client.player,interpolation,packet.jointSampleTick()+.5);
                if(evaluator.evaluations()!=count)throw new AssertionError("Interpolated geometry is not shared across queries");
                System.out.println("ANATOMY_NETWORK authoritative pose round trip passed");
            });
            // Exercise the actual client receiver's material lifecycle.  The
            // unavailable endpoint advances the frame fence, so neither an old
            // frame nor the discarded interpolator may revive convex geometry.
            world.getServer().runOnServer(server->{
                var first=firstPublished.get();var endpoint=first.endpoint();var root=endpoint.root();
                var unavailable=new GeometryProvider.CausalEndpoint(2,endpoint.authorityTick()+1,endpoint.jointSampleTick()+1,
                    new AnatomyMovement.RootFrame(2,root.tick()+1,root.origin(),root.yaw(),root.scale(),root.gravity()),endpoint.sample(),GeometryProvider.Availability.UNAVAILABLE);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),new GeometryProvider.PublishedFrame(first.identity(),unavailable));
            });
            context.waitTicks(5);
            context.runOnClient(client->{
                if(io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(client.player,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Unavailable causal endpoint retained client geometry");
            });
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),firstPublished.get()));
            context.waitTicks(5);
            context.runOnClient(client->{
                if(io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(client.player,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Delayed pre-unavailable frame revived client geometry");
            });
            world.getServer().runOnServer(server->{
                var first=firstPublished.get();var endpoint=first.endpoint();var root=endpoint.root();
                var restored=new GeometryProvider.CausalEndpoint(3,endpoint.authorityTick()+2,endpoint.jointSampleTick()+2,
                    new AnatomyMovement.RootFrame(3,root.tick()+2,root.origin(),root.yaw(),root.scale(),root.gravity()),endpoint.sample(),GeometryProvider.Availability.AVAILABLE);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),new GeometryProvider.PublishedFrame(first.identity(),restored));
            });
            context.waitTicks(5);
            context.runOnClient(client->{
                var history=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.pose(client.player.getUUID());
                if(history==null || history.current().frameSerial()!=3 || history.segment(client.level.getGameTime()).fraction()!=1
                        || io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(client.player,client.level.getGameTime()).isEmpty())
                    throw new AssertionError("Available endpoint did not rebind fresh geometry after the unavailable gap");
            });
            world.getServer().runOnServer(server->{
                var first=firstPublished.get();var endpoint=first.endpoint();var root=endpoint.root();
                var reboundIdentity=new GeometryProvider.GeometryIdentity(first.identity().dimension(),first.identity().support(),first.identity().entityId(),
                    first.identity().epoch(),first.identity().revision(),first.identity().model(),first.identity().poseProvider(),2,2);
                var rebound=new GeometryProvider.CausalEndpoint(4,endpoint.authorityTick()+3,endpoint.jointSampleTick()+3,
                    new AnatomyMovement.RootFrame(4,root.tick()+3,root.origin(),root.yaw(),root.scale(),root.gravity()),endpoint.sample(),GeometryProvider.Availability.AVAILABLE);
                AnatomyNetworking.sendPose(server.getPlayerList().getPlayers().getFirst(),new GeometryProvider.PublishedFrame(reboundIdentity,rebound));
            });
            context.waitTicks(5);
            context.runOnClient(client->{
                var history=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.pose(client.player.getUUID());
                if(history==null || history.current().bindingGeneration()!=2 || history.current().frameSerial()!=4
                        || io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(client.player,client.level.getGameTime()).isEmpty())
                    throw new AssertionError("New binding generation did not replace receiver material atomically");
            });
            var runtimeCow=new java.util.concurrent.atomic.AtomicReference<net.minecraft.world.entity.animal.cow.Cow>();
            var runtimeOccupied=new java.util.concurrent.atomic.AtomicReference<net.minecraft.world.phys.AABB>();
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(player.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                cow.setPos(player.position().add(3,1,0));cow.setNoAi(true);cow.setNoGravity(true);player.level().addFreshEntity(cow);runtimeCow.set(cow);
                var binding=new AnatomyDefinition(net.minecraft.resources.Identifier.parse("minecraft:cow"),net.minecraft.resources.Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT);
                var profile=new io.github.r3neer.scalebrews.platform.PlatformDefinition(net.minecraft.resources.Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),Optional.of(binding));
                AnatomyRuntime.startPrepared(server,models,Map.of("scalebrews:proof_cow",profile));
            });
            context.waitTicks(30);
            context.runOnClient(client->{
                var history=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.pose(runtimeCow.get().getUUID());
                if(history==null || !history.current().inputs().ordinary() || !history.current().model().toString().equals("minecraft:cow"))
                    throw new AssertionError("Runtime did not automatically bind/tick/publish tracked cow");
                var entity=(net.minecraft.world.entity.LivingEntity)client.level.getEntity(history.current().entityId());
                var catalog=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.catalog();
                if(entity==null || catalog.revision()<=1 || catalog.snapshot().profiles().size()!=1 || io.github.r3neer.scalebrews.platform.Platforms.definition(entity)==null || io.github.r3neer.scalebrews.platform.Platforms.definition(entity).anatomy().isEmpty())
                    throw new AssertionError("Runtime catalog did not atomically replace earlier geometry-only session with species bindings");
                if(entity==null || io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(entity,history.current().jointSampleTick()).isEmpty())
                    throw new AssertionError("Automatically received pose did not reconstruct geometry");
                var shape=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(entity,history.current().jointSampleTick()).orElseThrow();
                var center=shape.pieces().values().iterator().next().bounds().getCenter();
                var occupied=new net.minecraft.world.phys.AABB(center.subtract(.005,.005,.005),center.add(.005,.005,.005));runtimeOccupied.set(occupied);
                if(!AnatomyMovement.active(client.player) || AnatomyMovement.spaceClear(client.player,occupied))
                    throw new AssertionError("Received anatomical pose was not bound to the client's shared physical query");
            });
            var transportedPig=new java.util.concurrent.atomic.AtomicReference<net.minecraft.world.entity.animal.pig.Pig>();
            var transportStart=new java.util.concurrent.atomic.AtomicLong();
            var transportElapsed=new java.util.concurrent.atomic.AtomicLong();
            var bodyStart=new java.util.concurrent.atomic.AtomicReference<net.minecraft.world.phys.Vec3>();
            var driveSupport=new java.util.concurrent.atomic.AtomicBoolean(true);
            world.getServer().runOnServer(server->{
                var cow=runtimeCow.get();var level=(net.minecraft.server.level.ServerLevel)cow.level();
                var pig=net.minecraft.world.entity.EntityTypes.PIG.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                pig.setNoAi(true);pig.setNoGravity(true);
                pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);pig.refreshDimensions();
                var evaluator=new ModelGeometryProvider(models.get("minecraft:cow"),new QuadrupedPose(),AnatomyFilter.DEFAULT,1);
                evaluator.pose(cow,new PoseProvider.Inputs(0,0,0,0,0,true));
                var back=evaluator.sample(cow).orElseThrow().pieces().get("root/body/cube_0").bounds();
                pig.setPos(back.getCenter().x,back.maxY+.1,back.getCenter().z);level.addFreshEntity(pig);
                pig.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(0,-.2,0));
                if(!AnatomyMovement.supported(pig))throw new AssertionError("Tick fixture could not acquire original cow back");
                transportedPig.set(pig);transportStart.set(level.getGameTime());
                bodyStart.set(pig.position());
                net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_LEVEL_TICK.register(ticking->{
                    if(driveSupport.get() && ticking==level && cow.isAlive())
                        cow.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(.04,.01,0));
                });
            });
            var confirmedOnClient=new java.util.concurrent.atomic.AtomicBoolean();
            for(int attempt=0;attempt<40 && !confirmedOnClient.get();attempt++) {
                context.waitTicks(5);
                context.runOnClient(client->{
                    var pig=client.level.getEntity(transportedPig.get().getUUID());
                    if(pig!=null) {
                        var contact=AnatomyMovement.contact(pig);
                        confirmedOnClient.set(contact!=null && contact.support().getUUID().equals(runtimeCow.get().getUUID()));
                        if(AnatomyMovement.transport(pig)!=null)
                            throw new AssertionError("Observer client simulated transport for a server-owned mob");
                    }
                });
            }
            if(!confirmedOnClient.get())throw new AssertionError("Server contact was not confirmed on the observer client");
            context.runOnClient(client->{
                var pig=client.level.getEntity(transportedPig.get().getUUID());
                if(pig==null || io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isEmpty())
                    throw new AssertionError("Real server contact did not reach the observer presentation receiver");
            });
            try {for(int attempt=0;attempt<120 && transportElapsed.get()<60;attempt++) {
                context.waitTicks(5);
                world.getServer().runOnServer(server->{
                    var pig=transportedPig.get();var cow=runtimeCow.get();
                    transportElapsed.set(cow.level().getGameTime()-transportStart.get());
                    if(!AnatomyMovement.supported(pig))throw new AssertionError("Automatic runtime lost support after "+transportElapsed.get()+" server ticks");
                });
            }}finally{driveSupport.set(false);}
            if(transportElapsed.get()<60)throw new AssertionError("Integrated server did not complete transport tick window");
            world.getServer().runOnServer(server->{
                var displacement=transportedPig.get().position().subtract(bodyStart.get());
                if(displacement.x<.25 || displacement.y<.1)throw new AssertionError("Transport fixture did not actually translate and ascend: "+displacement);
                System.out.println("ANATOMY_RUNTIME original cow carries a mob through 60 actual server ticks");
                runtimeCow.get().setBaby(true);
            });
            var unsupportedReceived=new java.util.concurrent.atomic.AtomicBoolean();
            for(int attempt=0;attempt<40 && !unsupportedReceived.get();attempt++) {
                context.waitTicks(5);
                context.runOnClient(client->{
                    var cow=(net.minecraft.world.entity.LivingEntity)client.level.getEntity(runtimeCow.get().getUUID());
                    var pig=client.level.getEntity(transportedPig.get().getUUID());
                    unsupportedReceived.set(cow!=null && io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(cow,client.level.getGameTime()).isEmpty()
                        && pig!=null && AnatomyMovement.contact(pig)==null
                        && io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isEmpty());
                });
            }
            context.runOnClient(client->{
                var history=io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.pose(runtimeCow.get().getUUID());
                if(history!=null)throw new AssertionError("Unavailable runtime pose retained a geometry pose history");
                var entity=(net.minecraft.world.entity.LivingEntity)client.level.getEntity(runtimeCow.get().getId());
                if(entity==null || io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(entity,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Unsupported runtime pose retained frozen geometry");
                if(!AnatomyMovement.spaceClear(client.player,runtimeOccupied.get()))throw new AssertionError("Unsupported pose left a stale client collider");
                if(!unsupportedReceived.get())throw new AssertionError("Unavailable receiver did not clear geometry, contact, and presentation material");
            });
            world.getServer().runOnServer(server->runtimeCow.get().setBaby(false));
            context.waitFor(client->{
                var cow=client.level==null?null:client.level.getEntity(runtimeCow.get().getUUID());
                var pig=client.level==null?null:client.level.getEntity(transportedPig.get().getUUID());
                return cow instanceof LivingEntity living && io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.geometry(living,client.level.getGameTime()).isPresent()
                    && pig!=null && AnatomyMovement.contact(pig)==null
                    && io.github.r3neer.scalebrews.client.platform.anatomy.AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isEmpty();
            },200);
            world.getServer().runOnServer(server->{
                runtimeCow.get().setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);transportedPig.get().discard();runtimeCow.get().setBaby(true);
            });
            System.out.println("ANATOMY_RUNTIME actual receiver unavailable/recovery clears material without reviving contact passed");
            world.getServer().runOnServer(server->{AnatomyRuntime.stop(server);runtimeCow.get().discard();});
        }
    }
    private static void compare(Map<String,ConvexBox> predicted,Map<String,ConvexBox> actual,String label) {
        if(!predicted.keySet().equals(actual.keySet()))throw new AssertionError("Animated piece mismatch: "+label);
        for(String key:actual.keySet())for(int i=0;i<8;i++)if(predicted.get(key).vertices().get(i).distanceToSqr(actual.get(key).vertices().get(i))>1e-10)
            throw new AssertionError("Server pose differs from original: "+label+" piece "+key);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void verifyVanillaFamilies() {
        for(int tick=0;tick<80;tick++) {
            float walk=tick*.37f,speed=(tick%20)/20f,age=tick,yaw=tick%80-40,pitch=tick%50-25;
            var chickenRoot=net.minecraft.client.model.animal.chicken.AdultChickenModel.createBodyLayer().bakeRoot();
            var chicken=new net.minecraft.client.model.animal.chicken.AdultChickenModel(chickenRoot);var chickenState=new net.minecraft.client.renderer.entity.state.ChickenRenderState();
            common(chickenState,walk,speed,age,yaw,pitch);chickenState.flap=tick*.41f;chickenState.flapSpeed=(tick%10)/10f;
            checkPose("chicken "+tick,chickenRoot,chicken,chickenState,new VanillaFamilyPose(VanillaFamilyPose.Family.CHICKEN),
                new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true,Map.of("flap",chickenState.flap,"flap_speed",chickenState.flapSpeed)),Set.of());

            var llamaRoot=net.minecraft.client.model.animal.llama.LlamaModel.createBodyLayer(CubeDeformation.NONE).bakeRoot();
            var llama=new net.minecraft.client.model.animal.llama.LlamaModel(llamaRoot);var llamaState=new net.minecraft.client.renderer.entity.state.LlamaRenderState();common(llamaState,walk,speed,age,yaw,pitch);
            checkPose("llama "+tick,llamaRoot,llama,llamaState,new QuadrupedPose(),new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true),Set.of("right_chest","left_chest"));

            var villagerRoot=LayerDefinition.create(net.minecraft.client.model.npc.VillagerModel.createBodyModel(),64,64).bakeRoot();
            var villager=new net.minecraft.client.model.npc.VillagerModel(villagerRoot);var villagerState=new net.minecraft.client.renderer.entity.state.VillagerRenderState();common(villagerState,walk,speed,age,yaw,pitch);villagerState.isUnhappy=tick%3==0;
            checkPose("villager "+tick,villagerRoot,villager,villagerState,new VanillaFamilyPose(VanillaFamilyPose.Family.VILLAGER),
                new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true,Map.of("unhappy",villagerState.isUnhappy?1f:0f)),Set.of("hat","hat_rim"));

            var golemRoot=net.minecraft.client.model.animal.golem.IronGolemModel.createBodyLayer().bakeRoot();
            var golem=new net.minecraft.client.model.animal.golem.IronGolemModel(golemRoot);var golemState=new net.minecraft.client.renderer.entity.state.IronGolemRenderState();common(golemState,walk,speed,age,yaw,pitch);
            golemState.attackTicksRemaining=tick%4==0?tick%10:0;golemState.offerFlowerTick=tick%4==1?tick%70:0;
            checkPose("iron_golem "+tick,golemRoot,golem,golemState,new VanillaFamilyPose(VanillaFamilyPose.Family.IRON_GOLEM),
                new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true,Map.of("attack",golemState.attackTicksRemaining,"flower",(float)golemState.offerFlowerTick)),Set.of());

            var ghastRoot=net.minecraft.client.model.monster.ghast.GhastModel.createBodyLayer().bakeRoot();
            var ghast=new net.minecraft.client.model.monster.ghast.GhastModel(ghastRoot);var ghastState=new net.minecraft.client.renderer.entity.state.GhastRenderState();common(ghastState,walk,speed,age,yaw,pitch);
            checkPose("ghast "+tick,ghastRoot,ghast,ghastState,new VanillaFamilyPose(VanillaFamilyPose.Family.GHAST),new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true),Set.of());

            var felineRoot=LayerDefinition.create(net.minecraft.client.model.animal.feline.AdultFelineModel.createBodyMesh(CubeDeformation.NONE),64,32).bakeRoot();
            var feline=new net.minecraft.client.model.animal.feline.AdultFelineModel(felineRoot);var felineState=new net.minecraft.client.renderer.entity.state.FelineRenderState();common(felineState,walk,speed,age,yaw,pitch);
            felineState.ageScale=1;felineState.isCrouching=tick%3==0;felineState.isSprinting=tick%3==1;
            checkPose("feline "+tick,felineRoot,feline,felineState,new VanillaFamilyPose(VanillaFamilyPose.Family.FELINE),
                new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true,Map.of("crouching",felineState.isCrouching?1f:0f,"sprinting",felineState.isSprinting?1f:0f)),Set.of());

            var horseRoot=LayerDefinition.create(net.minecraft.client.model.animal.equine.AbstractEquineModel.createBodyMesh(CubeDeformation.NONE),64,64).bakeRoot();
            var horse=new net.minecraft.client.model.animal.equine.HorseModel(horseRoot);var horseState=new net.minecraft.client.renderer.entity.state.EquineRenderState();common(horseState,walk,speed,age,yaw,pitch);
            horseState.ageScale=1;horseState.animateTail=tick%2==0;
            checkPose("equine "+tick,horseRoot,horse,horseState,new VanillaFamilyPose(VanillaFamilyPose.Family.EQUINE),
                new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true,Map.of("tail",horseState.animateTail?1f:0f)),Set.of("left_bit","right_bit","left_rein","right_rein","head_saddle","mouth_saddle_wrap"));

            var beeRoot=net.minecraft.client.model.animal.bee.AdultBeeModel.createBodyLayer().bakeRoot();
            var bee=new net.minecraft.client.model.animal.bee.AdultBeeModel(beeRoot);var beeState=new net.minecraft.client.renderer.entity.state.BeeRenderState();common(beeState,walk,speed,age,yaw,pitch);
            beeState.isOnGround=tick%4==0;beeState.isAngry=tick%4==1;beeState.rollAmount=(tick%5)/5f;beeState.hasStinger=false;
            checkPose("bee "+tick,beeRoot,bee,beeState,new VanillaFamilyPose(VanillaFamilyPose.Family.BEE),
                new PoseProvider.Inputs(walk,speed,age,yaw,pitch,true,Map.of("on_ground",beeState.isOnGround?1f:0f,"angry",beeState.isAngry?1f:0f,"roll",beeState.rollAmount)),Set.of("stinger"));
        }
        System.out.println("ANATOMY_POSE 640 additional vanilla family comparisons passed");
    }
    private static void common(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,float walk,float speed,float age,float yaw,float pitch) {
        state.walkAnimationPos=walk;state.walkAnimationSpeed=speed;state.ageInTicks=age;state.yRot=yaw;state.xRot=pitch;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void checkPose(String label,ModelPart root,net.minecraft.client.model.EntityModel model,Object state,PoseProvider provider,PoseProvider.Inputs inputs,Set<String> excluded) {
        var geometry=GeometryExtractor.vanilla("proof:"+label.substring(0,label.indexOf(' ')),"26.2",root,excluded);
        model.setupAnim(state);
        compare(geometry.evaluate(new Matrix4f(),provider.evaluate(geometry,inputs).orElseThrow(),AnatomyFilter.DEFAULT),
            GeometryExtractor.vanilla(geometry.source(),"26.2",root,excluded).evaluate(new Matrix4f(),Map.of(),AnatomyFilter.DEFAULT),label);
    }
    private static void verifyPlayerPoses(ModelPart root,boolean slim,Set<String> skinLayers) {
        var model=new PlayerModel(root,slim);
        String source="minecraft:player_"+(slim?"slim":"wide");
        var geometry=GeometryExtractor.vanilla(source,"26.2",root,skinLayers);
        for(int tick=0;tick<80;tick++) {
            var state=new net.minecraft.client.renderer.entity.state.AvatarRenderState();
            state.walkAnimationPos=tick*.37f;state.walkAnimationSpeed=(tick%20)/20f;state.ageInTicks=tick;
            state.xRot=tick%50-25;state.yRot=tick%80-40;state.speedValue=1;
            model.setupAnim(state);
            var inputs=new PoseProvider.Inputs(state.walkAnimationPos,state.walkAnimationSpeed,tick,state.yRot,state.xRot,true);
            compare(geometry.evaluate(new Matrix4f(),new PlayerWalkingPose().evaluate(geometry,inputs).orElseThrow(),new AnatomyFilter(0,0,0)),
                GeometryExtractor.vanilla(source,"26.2",root,skinLayers).evaluate(new Matrix4f(),Map.of(),new AnatomyFilter(0,0,0)),source+" tick "+tick);
        }
        System.out.println("ANATOMY_POSE "+source+" 80 animated pose comparisons passed");
    }
    private static void verifyAlexVertices(Object model) throws ReflectiveOperationException {
        var geometry=GeometryExtractor.alex("alexsmobs:grizzly_bear","2.1.9",model,Set.of());
        var boxes=geometry.evaluate(new Matrix4f(),Map.of(),new AnatomyFilter(0,0,0));
        int[] count={0};
        var consumer=(com.mojang.blaze3d.vertex.VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(
            AnatomyExportProof.class.getClassLoader(),new Class[]{com.mojang.blaze3d.vertex.VertexConsumer.class},(proxy,method,args)->{
                if(method.getName().equals("addVertex") && args.length>=3 && args[0] instanceof Number) {
                    var vertex=new net.minecraft.world.phys.Vec3(((Number)args[0]).doubleValue(),((Number)args[1]).doubleValue(),((Number)args[2]).doubleValue());
                    if(boxes.values().stream().flatMap(b->b.vertices().stream()).noneMatch(v->v.distanceToSqr(vertex)<1e-10))
                        throw new AssertionError("Alex emitted a vertex absent from exported anatomy: "+vertex);
                    count[0]++;
                }
                return method.getReturnType()==com.mojang.blaze3d.vertex.VertexConsumer.class?proxy:null;
            });
        model.getClass().getMethod("renderToBuffer",PoseStack.class,com.mojang.blaze3d.vertex.VertexConsumer.class,int.class,int.class,float.class,float.class,float.class,float.class)
            .invoke(model,new PoseStack(),consumer,0,0,1f,1f,1f,1f);
        if(count[0]==0)throw new AssertionError("No Alex render vertices captured");
        System.out.println("ANATOMY_REFERENCE alexsmobs:grizzly_bear vertices="+count[0]);
    }
    private static void verifyCowPoses() {
        var model=new CowModel(CowModel.createBodyLayer().bakeRoot());
        var geometry=GeometryExtractor.vanilla("minecraft:cow","26.2",model.root(),Set.of());
        for(int tick=0;tick<80;tick++) {
            var state=new net.minecraft.client.renderer.entity.state.LivingEntityRenderState();
            state.walkAnimationPos=tick*.37f;state.walkAnimationSpeed=(tick%20)/20f;state.xRot=tick%50-25;state.yRot=tick%80-40;
            model.setupAnim(state);
            var inputs=new PoseProvider.Inputs(state.walkAnimationPos,state.walkAnimationSpeed,tick,state.yRot,state.xRot,true);
            var pose=new QuadrupedPose().evaluate(geometry,inputs).orElseThrow();
            var predicted=geometry.evaluate(new Matrix4f(),pose,new AnatomyFilter(0,0,0));
            var actual=GeometryExtractor.vanilla("minecraft:cow","26.2",model.root(),Set.of()).evaluate(new Matrix4f(),Map.of(),new AnatomyFilter(0,0,0));
            if(!predicted.keySet().equals(actual.keySet()))throw new AssertionError("Animated piece mismatch");
            for(String key:actual.keySet())for(int i=0;i<8;i++)if(predicted.get(key).vertices().get(i).distanceToSqr(actual.get(key).vertices().get(i))>1e-10)
                throw new AssertionError("Server-safe cow pose differs from vanilla at tick "+tick+" piece "+key);
        }
        System.out.println("ANATOMY_POSE minecraft:cow 80 animated pose comparisons passed");
    }
    private static void verifyVanilla(ModelPart root,Set<String> excluded,String source) {
        var result=GeometryExtractor.vanilla(source,"26.2",root,excluded);
        try {
            var dispatcher=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher();
            var field=dispatcher.getClass().getDeclaredField(source.startsWith("minecraft:player_")?"playerRenderers":"renderers");field.setAccessible(true);
            var renderers=(Map<?,?>)field.get(dispatcher);
            Object renderer=source.startsWith("minecraft:player_")?renderers.values().iterator().next():renderers.get(net.minecraft.world.entity.EntityTypes.COW);
            Object state=source.startsWith("minecraft:player_")?new net.minecraft.client.renderer.entity.state.AvatarRenderState():new net.minecraft.client.renderer.entity.state.LivingEntityRenderState();
            result=result.withModelTransform(rendererRoot(renderer,state,false));
        }catch(ReflectiveOperationException e){throw new AssertionError("Original renderer transform extraction failed",e);}
        var boxes=result.evaluate(new Matrix4f(),Map.of(),new AnatomyFilter(0,0,0));
        int[] checked={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var box=boxes.get("root"+path+"/cube_"+index);
            if(box==null)return;
            for(var polygon:cube.polygons)for(var vertex:polygon.vertices()) {
                Vector3f p=pose.pose().transformPosition(vertex.worldX(),vertex.worldY(),vertex.worldZ(),new Vector3f());
                if(box.vertices().stream().noneMatch(v->v.distanceToSqr(new net.minecraft.world.phys.Vec3(p.x,p.y,p.z))<1e-10))
                    throw new AssertionError("Export disagrees with real transformed vertex: "+source+path);
                checked[0]++;
            }
        });
        if(checked[0]==0)throw new AssertionError("No reference vertices compared");
        save(result);
        System.out.println("ANATOMY_EXPORT "+source+" vertices="+checked[0]+" pieces="+boxes.size());
    }
    private static void save(ModelGeometry model) {
        try {
            var gson=new GsonBuilder().setPrettyPrinting().create();
            String json=gson.toJson(model);
            var reloaded=gson.fromJson(json,ModelGeometry.class);
            if(!model.equals(reloaded))throw new AssertionError("Catalog round trip changed model");
            Path output=FabricLoader.getInstance().getGameDir().resolve("anatomy-export");
            Files.createDirectories(output);
            Files.writeString(output.resolve(model.source().replace(':','_')+".json"),json);
            Path reports=output.resolveSibling("anatomy-report");Files.createDirectories(reports);
            Files.writeString(reports.resolve(model.source().replace(':','_')+".json"),gson.toJson(model.filterReport(AnatomyFilter.DEFAULT)));
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static Matrix4f rendererRoot(Object renderer,Object stateOrEntity,boolean alex) throws ReflectiveOperationException {
        PoseStack stack=new PoseStack();stack.scale(-1,-1,1);
        java.lang.reflect.Method scale=null;
        for(Class<?> type=renderer.getClass();type!=null && scale==null;type=type.getSuperclass()) {
            for(var method:type.getDeclaredMethods()) {
                var params=method.getParameterTypes();
                if(method.getName().equals("scale") && params.length==(alex?3:2) && params[0].isInstance(stateOrEntity) && params[1]==PoseStack.class) {scale=method;break;}
            }
        }
        if(scale==null)throw new NoSuchMethodException("No verified renderer scale hook: "+renderer.getClass());
        scale.setAccessible(true);
        if(alex)scale.invoke(renderer,stateOrEntity,stack,0f);else scale.invoke(renderer,stateOrEntity,stack);
        stack.translate(0,-1.501f,0);
        System.out.println("ANATOMY_RENDER_ROOT "+renderer.getClass().getName()+" "+ModelGeometry.values(new Matrix4f(stack.last().pose())));
        return new Matrix4f(stack.last().pose());
    }
}
