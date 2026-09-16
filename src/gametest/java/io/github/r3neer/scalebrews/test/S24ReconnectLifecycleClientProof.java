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

/** Integrated G3.9 proof that disconnect/reconnect creates a fresh client tracking lifetime. */
public final class S24ReconnectLifecycleClientProof implements FabricClientGameTest {
    private record Identity(UUID epoch,long revision,long bindingGeneration,long trackingGeneration) {}

    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);var cowUuid=new AtomicReference<UUID>();var firstPlayer=new AtomicReference<ServerPlayer>();

        try(var server=context.worldBuilder().createServer()) {
            Throwable failure=null;
            try {
                server.runOnServer(minecraft->boot(minecraft,geometry,profile,cowId,cowUuid));
                Identity first;
                try(var connection=server.connect()) {
                    awaitReady(context,cowUuid);
                    first=context.computeOnClient(client->S24ReconnectLifecycleClientProof.identity(cowUuid.get()));
                    if(first.trackingGeneration()<1)
                        throw new AssertionError("Fresh first connection did not acquire an authoritative cow tracking generation");
                    server.runOnServer(minecraft->{
                        var player=minecraft.getPlayerList().getPlayers().getFirst();firstPlayer.set(player);
                        var cow=minecraft.overworld().getEntity(cowId.get());
                        if(cow==null || AnatomyRuntime.trackingGeneration(player,cow)!=first.trackingGeneration())
                            throw new AssertionError("First client pose tracking generation disagrees with its server recipient lifetime");
                    });
                }

                context.waitFor(client->client.level==null,160);
                context.runOnClient(client->{
                    if(AnatomyClientNetworking.catalog().announced())
                        throw new AssertionError("Disconnect retained the previous server catalog ownership");
                    if(AnatomyClientNetworking.pose(cowUuid.get())!=null)
                        throw new AssertionError("Disconnect retained the previous connection's cow pose history");
                });
                context.waitTicks(2);
                server.runOnServer(minecraft->{
                    if(!minecraft.getPlayerList().getPlayers().isEmpty())
                        throw new AssertionError("First dedicated connection still has a live ServerPlayer after close");
                });

                try(var connection=server.connect()) {
                    awaitReady(context,cowUuid);
                    var second=context.computeOnClient(client->S24ReconnectLifecycleClientProof.identity(cowUuid.get()));
                    if(!first.epoch().equals(second.epoch()) || first.revision()!=second.revision())
                        throw new AssertionError("Reconnect to the same running server changed epoch/revision unexpectedly");
                    if(first.bindingGeneration()!=second.bindingGeneration())
                        throw new AssertionError("Reconnect fabricated a rebind for the unchanged server cow");
                    if(second.trackingGeneration()<1)
                        throw new AssertionError("New connection failed to acquire its own recipient tracking authority");
                    server.runOnServer(minecraft->{
                        var current=minecraft.getPlayerList().getPlayers().getFirst();
                        if(current==firstPlayer.get())throw new AssertionError("Reconnect reused the old ServerPlayer object identity");
                        var cow=minecraft.overworld().getEntity(cowId.get());
                        if(cow==null || !cow.getUUID().equals(cowUuid.get()))throw new AssertionError("Server cow identity changed across reconnect");
                        if(AnatomyRuntime.trackingGeneration(current,cow)!=second.trackingGeneration())
                            throw new AssertionError("Client pose tracking generation disagrees with the new server recipient lifetime");
                    });
                }

                System.out.println("S24_RECONNECT_LIFECYCLE PASS disconnect reset client session; same server rebound with independent recipient tracking authority");
            } catch(Throwable error) {failure=error;throw error;}
            finally {
                try {server.runOnServer(AnatomyRuntime::stop);}
                catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("S24 reconnect lifecycle cleanup failed",cleanup);}
            }
        }
    }

    private static Identity identity(UUID cowUuid) {
        var catalog=AnatomyClientNetworking.catalog();var history=AnatomyClientNetworking.pose(cowUuid);var packet=history==null?null:history.current();
        if(!catalog.ready() || packet==null)throw new AssertionError("Connection never reached READY cow anatomy state");
        return new Identity(catalog.epoch(),catalog.revision(),packet.bindingGeneration(),packet.trackingGeneration());
    }

    private static void awaitReady(ClientGameTestContext context,AtomicReference<UUID> cowUuid) {
        context.waitFor(client->{
            if(client.level==null || client.player==null || !AnatomyClientNetworking.catalog().ready())return false;
            var history=AnatomyClientNetworking.pose(cowUuid.get());
            return history!=null && history.current()!=null;
        },200);
    }

    private static void boot(MinecraftServer server,io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
                             PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S24 reconnect lifecycle cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);
        cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_reconnect_cow",profile));
    }
}
