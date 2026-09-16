package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.ChunkPos;

/**
 * Adversarial G3.9 reconnect proof. The fixture deliberately creates the support only after
 * the first client is connected and force-loads its chunk across the disconnect, so the test
 * measures connection lifecycle rather than unrelated no-player chunk/entity lifecycle.
 */
public final class S24AdversarialReconnectLifecycleProof implements FabricClientGameTest {
    private record Identity(UUID epoch,long revision,long bindingGeneration,long trackingGeneration,Object historyIdentity) {}

    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);
        var cowUuid=new AtomicReference<UUID>();
        var forcedChunk=new AtomicReference<ChunkPos>();
        var firstPlayer=new AtomicReference<ServerPlayer>();

        try(var server=context.worldBuilder().createServer()) {
            Throwable failure=null;
            try {
                Identity first;
                try(var connection=server.connect()) {
                    context.waitFor(client->client.level!=null && client.player!=null,240);
                    server.runOnServer(minecraft->bootAfterJoin(minecraft,geometry,profile,cowId,cowUuid,forcedChunk,firstPlayer));
                    awaitReady(context,cowUuid);
                    first=context.computeOnClient(client->identity(cowUuid.get()));
                    if(first.trackingGeneration()<1)
                        throw new AssertionError("First connection never acquired recipient tracking authority");
                }

                context.waitFor(client->client.level==null,200);
                context.runOnClient(client->{
                    if(AnatomyClientNetworking.catalog().announced())
                        throw new AssertionError("Disconnect retained the old connection catalog");
                    if(AnatomyClientNetworking.pose(cowUuid.get())!=null)
                        throw new AssertionError("Disconnect retained the old cow frame history");
                });

                server.runOnServer(minecraft->{
                    if(!minecraft.getPlayerList().getPlayers().isEmpty())
                        throw new AssertionError("Old dedicated ServerPlayer survived connection close");
                    var cow=minecraft.overworld().getEntity(cowId.get());
                    if(!(cow instanceof Cow) || !cow.getUUID().equals(cowUuid.get()))
                        throw new AssertionError("Forced support fixture did not survive the reconnect gap");
                });

                try(var connection=server.connect()) {
                    awaitReady(context,cowUuid);
                    var second=context.computeOnClient(client->identity(cowUuid.get()));
                    if(!first.epoch().equals(second.epoch()) || first.revision()!=second.revision())
                        throw new AssertionError("Reconnect to one running server changed epoch/revision");
                    if(first.bindingGeneration()!=second.bindingGeneration())
                        throw new AssertionError("Reconnect fabricated a support rebind");
                    if(second.trackingGeneration()<1)
                        throw new AssertionError("Second connection never acquired recipient tracking authority");
                    if(first.historyIdentity()==second.historyIdentity())
                        throw new AssertionError("Reconnect reused the previous connection's client frame-history object");

                    server.runOnServer(minecraft->{
                        var secondPlayer=minecraft.getPlayerList().getPlayers().getFirst();
                        if(secondPlayer==firstPlayer.get())
                            throw new AssertionError("Reconnect reused the old ServerPlayer object identity");
                        var cow=minecraft.overworld().getEntity(cowId.get());
                        if(!(cow instanceof Cow) || !cow.getUUID().equals(cowUuid.get()))
                            throw new AssertionError("Server support identity changed across reconnect");
                    });
                }
                System.out.println("S24_ADVERSARIAL_RECONNECT PASS connection state reset while persistent server support/catalog authority survived");
            } catch(Throwable error) {failure=error;throw error;}
            finally {
                try {
                    server.runOnServer(minecraft->{
                        var chunk=forcedChunk.get();
                        if(chunk!=null)minecraft.overworld().setChunkForced(chunk.x(),chunk.z(),false);
                        AnatomyRuntime.stop(minecraft);
                    });
                } catch(Throwable cleanup) {
                    if(failure!=null)failure.addSuppressed(cleanup);
                    else if(cleanup instanceof RuntimeException runtime)throw runtime;
                    else throw new AssertionError("S24 adversarial reconnect cleanup failed",cleanup);
                }
            }
        }
    }

    private static Identity identity(UUID cowUuid) {
        var catalog=AnatomyClientNetworking.catalog();
        var history=AnatomyClientNetworking.pose(cowUuid);
        var packet=history==null?null:history.current();
        if(!catalog.ready() || packet==null)
            throw new AssertionError("Connection never reached READY cow anatomy state");
        return new Identity(catalog.epoch(),catalog.revision(),packet.bindingGeneration(),packet.trackingGeneration(),history);
    }

    private static void awaitReady(ClientGameTestContext context,AtomicReference<UUID> cowUuid) {
        context.waitFor(client->{
            if(client.level==null || client.player==null || !AnatomyClientNetworking.catalog().ready())return false;
            var history=AnatomyClientNetworking.pose(cowUuid.get());
            if(history==null || history.current()==null)return false;
            var entity=client.level.getEntity(history.current().entityId());
            return entity instanceof Cow cow && cow.getUUID().equals(cowUuid.get())
                && AnatomyClientNetworking.presentationFrame(cow).isPresent();
        },260);
    }

    private static void bootAfterJoin(MinecraftServer server,
                                      io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
                                      PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid,
                                      AtomicReference<ChunkPos> forcedChunk,AtomicReference<ServerPlayer> firstPlayer) {
        var player=server.getPlayerList().getPlayers().getFirst();
        firstPlayer.set(player);
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S24 adversarial reconnect cow");
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setPersistenceRequired();
        cow.setPos(player.getX()+2.0,player.getY(),player.getZ());
        var chunk=cow.chunkPosition();
        server.overworld().setChunkForced(chunk.x(),chunk.z(),true);
        forcedChunk.set(chunk);
        server.overworld().addFreshEntity(cow);
        cowId.set(cow.getId());
        cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_adversarial_reconnect_cow",profile));
    }
}
