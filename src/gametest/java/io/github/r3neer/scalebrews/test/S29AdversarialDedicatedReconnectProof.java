package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Persistent dedicated authority, new recipient connection, old material replay. */
public final class S29AdversarialDedicatedReconnectProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        boolean early=Boolean.getBoolean("scalebrews.s29EarlyReconnectReplay");
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var uuid=new AtomicReference<UUID>();
        var oldGeneration=new AtomicLong();var nextGeneration=new AtomicLong();
        var oldBefore=new AtomicReference<GeometryProvider.PublishedFrame>();var oldAfter=new AtomicReference<GeometryProvider.PublishedFrame>();
        var oldCertificate=new AtomicReference<AnatomyMaterialIntervalPayload>();var bundle=new AtomicReference<List<AnatomyCatalogPayload>>();
        var chunk=new AtomicReference<net.minecraft.world.level.ChunkPos>();
        try(var server=context.worldBuilder().createServer()) {
            try {
                try(var connection=server.connect()) {
                    context.waitFor(client->client.level!=null && client.player!=null,240);
                    server.runOnServer(mc->{
                        var player=mc.getPlayerList().getPlayers().getFirst();
                        var cow=net.minecraft.world.entity.EntityTypes.COW.create(mc.overworld(),EntitySpawnReason.COMMAND);
                        if(cow==null)throw new AssertionError("Dedicated cow absent");
                        cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();cow.setPos(player.getX()+2,player.getY(),player.getZ());
                        chunk.set(cow.chunkPosition());mc.overworld().setChunkForced(chunk.get().x(),chunk.get().z(),true);
                        mc.overworld().addFreshEntity(cow);id.set(cow.getId());uuid.set(cow.getUUID());
                        AnatomyRuntime.startPrepared(mc,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_dedicated_reconnect",profile));
                    });
                    awaitFrame(context,id);
                    server.runOnServer(mc->{
                        var cow=(Cow)mc.overworld().getEntity(id.get());var player=mc.getPlayerList().getPlayers().getFirst();
                        oldGeneration.set(AnatomyRuntime.trackingGeneration(player,cow));
                        oldBefore.set(AnatomyMovement.publishedFrame(cow).orElseThrow());
                        cow.move(MoverType.SELF,new Vec3(.125,0,0));oldAfter.set(AnatomyMovement.publishedFrame(cow).orElseThrow());
                        bundle.set(catalogBundle(mc));
                    });
                    context.waitFor(client->{
                        var cow=(Cow)client.level.getEntity(id.get());
                        for(var token:inbox().certificates(uuid.get(),clientTick()))
                            if(token.beforeFrameSerial()==oldBefore.get().endpoint().frameSerial() && token.afterFrameSerial()==oldAfter.get().endpoint().frameSerial()
                                && AnatomyClientNetworking.presentationInterval(cow,token.materialSerial(),.5).isPresent()){oldCertificate.set(token);return true;}
                        return false;
                    },260);
                    if(oldGeneration.get()<1)throw new AssertionError("Old window absent");
                    // Keep the separate early-replay holdout inside pose TTL; do not rewrite payload clocks.
                    if(early)server.runOnServer(mc->mc.tickRateManager().setFrozen(true));
                }
                context.waitFor(client->client.level==null,200);
                context.runOnClient(client->{
                    if(AnatomyClientNetworking.catalog().announced() || AnatomyClientNetworking.pose(uuid.get())!=null
                        || !inbox().certificates(uuid.get(),clientTick()).isEmpty())throw new AssertionError("Disconnect retained old material/catalog");
                });
                server.runOnServer(mc->{
                    if(!mc.getPlayerList().getPlayers().isEmpty())throw new AssertionError("Old player survived disconnect");
                    if(mc.overworld().getEntity(id.get())==null)throw new AssertionError("Persistent cow disappeared during gap");
                    if(early)AnatomyRuntime.stop(mc);
                });
                try(var connection=server.connect()) {
                    context.waitFor(client->client.level!=null && client.player!=null && client.level.getEntity(id.get()) instanceof Cow,260);
                    if(early) {
                        // Replay captured authority after a real connection barrier, before any fresh tracking window.
                        server.runOnServer(mc->{var player=mc.getPlayerList().getPlayers().getFirst();
                            if(mc.overworld().getGameTime()-oldAfter.get().endpoint().authorityTick()>100)
                                throw new AssertionError("Fixture inconclusive: replay aged beyond pose TTL before reconnect injection");
                            AnatomyNetworking.sendCatalog(player,bundle.get());
                            AnatomyNetworking.sendPose(player,oldBefore.get(),oldGeneration.get());
                            AnatomyNetworking.sendPose(player,oldAfter.get(),oldGeneration.get());
                            AnatomyNetworking.sendMaterialInterval(player,oldCertificate.get());
                        });
                        context.waitFor(client->AnatomyClientNetworking.catalog().ready(),260);
                        context.waitTicks(3);
                        context.runOnClient(client->{
                            var cow=(Cow)client.level.getEntity(id.get());
                            boolean revived=AnatomyClientNetworking.presentationInterval(cow,oldCertificate.get().materialSerial(),.5).isPresent();
                            System.out.println("S29 dedicated early reconnect old material authoritative="+revived);
                            if(revived)throw new AssertionError("NFR-017: pre-disconnect material revived before new tracking window");
                        });
                    } else {
                        awaitFrame(context,id);
                        server.runOnServer(mc->{var cow=(Cow)mc.overworld().getEntity(id.get());var player=mc.getPlayerList().getPlayers().getFirst();
                            nextGeneration.set(AnatomyRuntime.trackingGeneration(player,cow));
                            if(nextGeneration.get()<=oldGeneration.get())throw new AssertionError("Reconnect did not advance recipient generation");
                            AnatomyNetworking.sendPose(player,oldBefore.get(),oldGeneration.get());
                            AnatomyNetworking.sendPose(player,oldAfter.get(),oldGeneration.get());
                            AnatomyNetworking.sendMaterialInterval(player,oldCertificate.get());
                        });
                        context.waitTicks(3);
                        context.runOnClient(client->{var cow=(Cow)client.level.getEntity(id.get());
                            if(AnatomyClientNetworking.presentationInterval(cow,oldCertificate.get().materialSerial(),.5).isPresent())
                                throw new AssertionError("Dedicated reconnect revived retired certificate");
                            var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow();
                            if(!frame.identity().epoch().equals(oldCertificate.get().epoch()) || frame.identity().revision()!=oldCertificate.get().revision())
                                throw new AssertionError("Persistent server catalog identity changed");
                        });
                        var afterSerial=new AtomicLong();
                        server.runOnServer(mc->{var cow=(Cow)mc.overworld().getEntity(id.get());cow.move(MoverType.SELF,new Vec3(.125,0,0));
                            afterSerial.set(AnatomyMovement.publishedFrame(cow).orElseThrow().endpoint().frameSerial());});
                        context.waitFor(client->{var cow=(Cow)client.level.getEntity(id.get());
                            return inbox().certificates(uuid.get(),clientTick()).stream().anyMatch(token->token.trackingGeneration()==nextGeneration.get()
                                && token.afterFrameSerial()==afterSerial.get() && AnatomyClientNetworking.presentationInterval(cow,token.materialSerial(),.5).isPresent());
                        },260);
                        System.out.println("S29 dedicated reconnect PASS same catalog/support, fresh generation, old replay rejected, new ROOT interval drawable");
                    }
                }
            } finally {server.runOnServer(mc->{mc.tickRateManager().setFrozen(false);AnatomyRuntime.stop(mc);if(chunk.get()!=null)mc.overworld().setChunkForced(chunk.get().x(),chunk.get().z(),false);});}
        }
    }
    private static void awaitFrame(ClientGameTestContext context,AtomicInteger id) {
        context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
            && AnatomyClientNetworking.presentationFrame(cow).isPresent(),260);
    }
    private static List<AnatomyCatalogPayload> catalogBundle(net.minecraft.server.MinecraftServer server) {
        try {var states=AnatomyRuntime.class.getDeclaredField("STATES");states.setAccessible(true);
            var state=((Map<?,?>)states.get(null)).get(server);var field=state.getClass().getDeclaredField("catalog");field.setAccessible(true);
            var catalog=(WorldAnatomyCatalog)field.get(state);
            var method=WorldAnatomyCatalog.class.getDeclaredMethod("preparedPackets",UUID.class);method.setAccessible(true);
            @SuppressWarnings("unchecked") var result=(List<AnatomyCatalogPayload>)method.invoke(catalog,AnatomyNetworking.epoch(server));
            return result;
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static AnatomyCertifiedIntervalInbox inbox() {return (AnatomyCertifiedIntervalInbox)clientField("certifiedIntervals");}
    private static long clientTick(){return (Long)clientField("clientTick");}
    private static Object clientField(String name){try{var f=AnatomyClientNetworking.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
