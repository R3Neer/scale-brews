package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Real accepted reload followed by active and early-replay dimension round trips. */
public final class S29AdversarialCatalogDimensionProof implements FabricClientGameTest {
    private record Material(GeometryProvider.PublishedFrame before,GeometryProvider.PublishedFrame after,
                            AnatomyMaterialIntervalPayload certificate,long generation) {}
    @Override public void runTest(ClientGameTestContext context) {
        var original=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var replacement=new ModelGeometry(original.format(),"minecraft:pig","26.2",
            original.parts(),original.pieces(),original.modelTransform());
        var profile=profile(original.source());var nextProfile=profile(replacement.source());
        var id=new AtomicInteger();var uuid=new AtomicReference<UUID>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("Fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();cow.setPos(2,3,2);
                server.overworld().setChunkForced(0,0,true);server.overworld().addFreshEntity(cow);
                id.set(cow.getId());uuid.set(cow.getUUID());
                AnatomyRuntime.startPrepared(server,Map.of(original.source(),original),Map.of("s29_adversary:profile",profile));
            });
            await(context,id,0);
            var first=move(context,world,id,uuid);
            var oldBundle=new AtomicReference<List<AnatomyCatalogPayload>>();
            world.getServer().runOnServer(server->{oldBundle.set(bundle(server));reload(server,resources(server,replacement,nextProfile));});
            try {await(context,id,first.certificate.revision()+1);}
            catch(AssertionError timeout) {
                context.runOnClient(client->{var cow=client.level.getEntity(id.get());var poses=AnatomyClientNetworking.pose(uuid.get());
                    System.out.println("S29 RELOAD DIAGNOSTIC catalog="+AnatomyClientNetworking.catalog().revision()+" ready="+AnatomyClientNetworking.catalog().ready()
                        +" entity="+cow+" pose="+(poses==null?null:poses.current())+" frame="+(cow instanceof Cow living?AnatomyClientNetworking.presentationFrame(living):null));});
                world.getServer().runOnServer(server->{var cow=(Cow)server.overworld().getEntity(id.get());
                    System.out.println("S29 RELOAD SERVER published="+AnatomyMovement.publishedFrame(cow));});
                throw timeout;
            }
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());var catalog=AnatomyClientNetworking.catalog();
                if(!catalog.epoch().equals(first.certificate.epoch()) || catalog.revision()<=first.certificate.revision())
                    throw new AssertionError("Accepted reload did not advance revision within same server epoch");
                if(!AnatomyClientNetworking.presentationFrame(cow).orElseThrow().identity().model().equals(Identifier.parse(replacement.source())))
                    throw new AssertionError("Accepted reload still renders removed model binding");
                if(AnatomyClientNetworking.presentationInterval(cow,first.certificate.materialSerial(),.5)
                    .filter(frame->frame.identity().revision()==first.certificate.revision()).isPresent())
                    throw new AssertionError("Accepted reload retained old certified material");
                if(AnatomyMovement.queryFrame(cow).isEmpty())throw new AssertionError("Accepted reload failed physical registration");
            });
            world.getServer().runOnServer(server->{
                // Raw delivery models delayed packets already published before reload;
                // the ordinary sendCatalog helper correctly refuses to publish an old revision now.
                for(var packet:oldBundle.get())net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player(server),packet);
                replay(server,first);
            });
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyClientNetworking.catalog().revision()<=first.certificate.revision()
                    || AnatomyClientNetworking.presentationInterval(cow,first.certificate.materialSerial(),.5)
                        .filter(frame->frame.identity().revision()==first.certificate.revision()).isPresent()
                    || inbox().certificates(uuid.get(),clientTick()).stream().anyMatch(token->token.revision()==first.certificate.revision()))
                    throw new AssertionError("Old catalog/interval replay rolled accepted reload backward: oldRevision="
                        +first.certificate.revision()+" catalogRevision="+AnatomyClientNetworking.catalog().revision()
                        +" catalogEpoch="+AnatomyClientNetworking.catalog().epoch()+" oldEpoch="+first.certificate.epoch()
                        +" certificates="+inbox().certificates(uuid.get(),clientTick()));
            });
            var second=move(context,world,id,uuid);
            if(second.certificate.revision()<=first.certificate.revision())throw new AssertionError("Fresh interval uses old catalog revision");
            System.out.println("S29 accepted catalog PASS revision advanced, model replaced, stale bundle/material rejected, new ROOT drawable");

            // Active round trip: lifecycle cleanup plus positive ROOT on a fresh return window.
            world.getServer().runOnServer(server->teleport(server,Level.NETHER,new Vec3(0,80,0)));
            context.waitFor(c->c.level!=null && c.level.dimension()==Level.NETHER,240);
            context.waitTicks(2);
            context.runOnClient(client->{
                if(!AnatomyClientNetworking.catalog().ready() || AnatomyClientNetworking.catalog().revision()!=second.certificate.revision()
                    || AnatomyClientNetworking.pose(uuid.get())!=null || !inbox().certificates(uuid.get(),clientTick()).isEmpty())
                    throw new AssertionError("Dimension barrier retained Overworld material or discarded connection catalog");
            });
            world.getServer().runOnServer(server->teleport(server,Level.OVERWORLD,new Vec3(2,4,2)));
            await(context,id,second.certificate.revision());
            world.getServer().runOnServer(server->replay(server,second));
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyClientNetworking.presentationInterval(cow,second.certificate.materialSerial(),.5).isPresent())
                    throw new AssertionError("A-B-A accepted retired interval after fresh return window");
            });
            var returned=move(context,world,id,uuid);
            if(returned.generation<=second.generation || returned.certificate.revision()!=second.certificate.revision())
                throw new AssertionError("Active dimension return did not use new tracking with stable catalog");
            System.out.println("S29 active dimension PASS A-B-A new tracking, stable catalog, old interval rejected, new ROOT drawable");

            // Hold original authority clock within TTL, suppress any fresh anatomy pose on return.
            world.getServer().runOnServer(server->{server.tickRateManager().setFrozen(true);AnatomyRuntime.stop(server);
                teleport(server,Level.NETHER,new Vec3(0,80,0));});
            context.waitFor(c->c.level!=null && c.level.dimension()==Level.NETHER,240);
            context.waitTicks(2);
            world.getServer().runOnServer(server->teleport(server,Level.OVERWORLD,new Vec3(2,4,2)));
            context.waitFor(c->c.level!=null && c.level.dimension()==Level.OVERWORLD && c.level.getEntity(id.get()) instanceof Cow,240);
            world.getServer().runOnServer(server->{
                if(server.overworld().getGameTime()-returned.after.endpoint().authorityTick()>100)
                    throw new AssertionError("Inconclusive: replay older than pose TTL");
                replay(server,returned);
            });
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyClientNetworking.pose(uuid.get())!=null || AnatomyClientNetworking.presentationFrame(cow).isPresent()
                    || AnatomyClientNetworking.presentationInterval(cow,returned.certificate.materialSerial(),.5).isPresent())
                    throw new AssertionError("NFR-017: early A-B-A replay revived pre-dimension material");
                if(!AnatomyClientNetworking.catalog().ready())throw new AssertionError("Same-connection catalog lost in stopped-runtime dimension trip");
            });
            System.out.println("S29 early dimension PASS exact pre-barrier material rejected before new tracking window");
        } finally {
            world.getServer().runOnServer(server->{server.tickRateManager().setFrozen(false);AnatomyRuntime.stop(server);server.overworld().setChunkForced(0,0,false);});
            context.waitTicks(2);world.close();
        }
    }
    private static PlatformDefinition profile(String model){return new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
        Optional.of(new AnatomyDefinition(Identifier.parse(model),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));}
    private static void await(ClientGameTestContext c,AtomicInteger id,long revision){try {c.waitFor(client->{
        if(client.level==null || client.level.dimension()!=Level.OVERWORLD || !(client.level.getEntity(id.get()) instanceof Cow cow))return false;
        var frame=AnatomyClientNetworking.presentationFrame(cow);return frame.isPresent() && frame.get().identity().revision()>=revision
            && AnatomyMovement.queryFrame(cow).isPresent();
    },260);} catch(AssertionError failure) {
        c.runOnClient(client->{var entity=client.level==null?null:client.level.getEntity(id.get());
            System.out.println("S29 AWAIT DIAGNOSTIC requested="+revision+" level="+client.level
                +" entity="+entity+" catalogEpoch="+AnatomyClientNetworking.catalog().epoch()
                +" catalogRevision="+AnatomyClientNetworking.catalog().revision()+" ready="+AnatomyClientNetworking.catalog().ready()
                +" bindings="+AnatomyClientNetworking.catalog().snapshot().bindings().keySet()
                +" pose="+(entity==null?null:AnatomyClientNetworking.pose(entity.getUUID()))
                +" presentation="+(entity instanceof Cow cow?AnatomyClientNetworking.presentationFrame(cow):null)
                +" physical="+(entity instanceof Cow cow?AnatomyMovement.queryFrame(cow):null));});
        throw failure;
    }}
    private static Material move(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world,AtomicInteger id,AtomicReference<UUID> uuid){
        var before=new AtomicReference<GeometryProvider.PublishedFrame>();var after=new AtomicReference<GeometryProvider.PublishedFrame>();var generation=new AtomicLong();
        world.getServer().runOnServer(server->{var cow=(Cow)server.overworld().getEntity(id.get());
            before.set(AnatomyMovement.publishedFrame(cow).orElseThrow());generation.set(AnatomyRuntime.trackingGeneration(player(server),cow));
            cow.move(MoverType.SELF,new Vec3(.125,0,0));after.set(AnatomyMovement.publishedFrame(cow).orElseThrow());});
        var token=new AtomicReference<AnatomyMaterialIntervalPayload>();
        c.waitFor(client->{var cow=(Cow)client.level.getEntity(id.get());
            for(var candidate:inbox().certificates(uuid.get(),clientTick()))
                if(candidate.beforeFrameSerial()==before.get().endpoint().frameSerial() && candidate.afterFrameSerial()==after.get().endpoint().frameSerial()
                    && candidate.trackingGeneration()==generation.get() && AnatomyClientNetworking.presentationInterval(cow,candidate.materialSerial(),.5).isPresent()) {
                    token.set(candidate);return true;
                }
            return false;
        },260);
        return new Material(before.get(),after.get(),token.get(),generation.get());
    }
    private static net.minecraft.server.level.ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static void replay(MinecraftServer s,Material m){AnatomyNetworking.sendPose(player(s),m.before,m.generation);AnatomyNetworking.sendPose(player(s),m.after,m.generation);AnatomyNetworking.sendMaterialInterval(player(s),m.certificate);}
    private static void teleport(MinecraftServer s,net.minecraft.resources.ResourceKey<Level> key,Vec3 pos){
        var target=s.getLevel(key);if(target==null || player(s).teleport(new TeleportTransition(target,pos,Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING))==null)
            throw new AssertionError("Dimension transition rejected: "+key);
    }
    private static void reload(MinecraftServer s,ResourceManager resources){try{
        var method=AnatomyRuntime.class.getDeclaredMethod("reload",MinecraftServer.class,ResourceManager.class);method.setAccessible(true);method.invoke(null,s,resources);
    }catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static ResourceManager resources(MinecraftServer s,ModelGeometry model,PlatformDefinition profile){
        var installed=s.getResourceManager();var pack=installed.listPacks().findFirst().orElseThrow();
        var geometry=AnatomyCodecs.GEOMETRY.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,model).getOrThrow().toString();
        var definition=PlatformDefinition.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,profile).getOrThrow().toString();
        return (ResourceManager)java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(p,m,args)->{
            if(m.getName().equals("listPacks"))return installed.listPacks();
            if(m.getName().equals("listResources"))return switch((String)args[0]) {
                case "scalebrews/entity_geometry" -> resource(pack,"minecraft:scalebrews/entity_geometry/pig.json",geometry);
                case "scalebrews/entity_platform" -> resource(pack,"s29_adversary:scalebrews/entity_platform/cow.json",definition);
                default -> Map.of();
            };
            throw new UnsupportedOperationException(m.toString());
        });
    }
    private static Map<Identifier,Resource> resource(net.minecraft.server.packs.PackResources pack,String id,String json){
        byte[] bytes=json.getBytes(java.nio.charset.StandardCharsets.UTF_8);return Map.of(Identifier.parse(id),new Resource(pack,()->new java.io.ByteArrayInputStream(bytes)));
    }
    private static List<AnatomyCatalogPayload> bundle(MinecraftServer server){try{
        var f=AnatomyRuntime.class.getDeclaredField("STATES");f.setAccessible(true);var state=((Map<?,?>)f.get(null)).get(server);
        var cf=state.getClass().getDeclaredField("catalog");cf.setAccessible(true);var catalog=(WorldAnatomyCatalog)cf.get(state);
        var m=WorldAnatomyCatalog.class.getDeclaredMethod("preparedPackets",UUID.class);m.setAccessible(true);
        @SuppressWarnings("unchecked") var packets=(List<AnatomyCatalogPayload>)m.invoke(catalog,AnatomyNetworking.epoch(server));return packets;
    }catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static AnatomyCertifiedIntervalInbox inbox(){return (AnatomyCertifiedIntervalInbox)field("certifiedIntervals");}
    private static long clientTick(){return (Long)field("clientTick");}
    private static Object field(String name){try{var f=AnatomyClientNetworking.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
