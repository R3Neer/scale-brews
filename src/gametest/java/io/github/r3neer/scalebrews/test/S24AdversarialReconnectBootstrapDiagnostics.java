package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyCatalogPayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyContactPayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPosePayload;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;

/** Diagnostic-only adversarial probe for the first dedicated connection used by S24 reconnect. */
public final class S24AdversarialReconnectBootstrapDiagnostics implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);var cowUuid=new AtomicReference<UUID>();

        try(var server=context.worldBuilder().createServer()) {
            Throwable failure=null;
            try {
                server.runOnServer(minecraft->boot(minecraft,geometry,profile,cowId,cowUuid));
                try(var connection=server.connect()) {
                    await(context,client->client.level!=null && client.player!=null,240,"client level/player connection");

                    server.runOnServer(minecraft->{
                        var players=minecraft.getPlayerList().getPlayers();
                        if(players.isEmpty())throw new AssertionError("BOOTSTRAP_STAGE server-player: no ServerPlayer after client connection");
                        var player=players.getFirst();var entity=minecraft.overworld().getEntity(cowId.get());
                        if(!(entity instanceof Cow cow) || !cow.getUUID().equals(cowUuid.get()))
                            throw new AssertionError("BOOTSTRAP_STAGE server-cow: canonical cow missing before publication");
                        if(AnatomyRuntime.authoritativeFrame(cow).isEmpty())
                            throw new AssertionError("BOOTSTRAP_STAGE server-runtime: cow has no authoritative anatomy frame");
                        if(!ServerPlayNetworking.canSend(player,AnatomyCatalogPayload.TYPE))
                            throw new AssertionError("BOOTSTRAP_STAGE protocol: client did not negotiate anatomy catalog channel");
                        if(!ServerPlayNetworking.canSend(player,AnatomyPosePayload.TYPE))
                            throw new AssertionError("BOOTSTRAP_STAGE protocol: client did not negotiate anatomy pose channel");
                        if(!ServerPlayNetworking.canSend(player,AnatomyContactPayload.TYPE))
                            throw new AssertionError("BOOTSTRAP_STAGE protocol: client did not negotiate anatomy contact channel");
                        if(!PlayerLookup.tracking(cow).contains(player))
                            throw new AssertionError("BOOTSTRAP_STAGE vanilla-tracking: connected player is not tracking the prepared cow");
                    });

                    await(context,client->AnatomyClientNetworking.catalog().announced(),240,"catalog announcement");
                    await(context,client->AnatomyClientNetworking.catalog().ready(),240,"catalog READY assembly");
                    await(context,client->{
                        var entity=client.level==null?null:client.level.getEntity(cowId.get());
                        return entity instanceof Cow cow && cow.getUUID().equals(cowUuid.get());
                    },240,"client cow visibility");
                    await(context,client->{
                        var history=AnatomyClientNetworking.pose(cowUuid.get());
                        return history!=null && history.current()!=null;
                    },240,"client pose history");
                    await(context,client->{
                        var entity=client.level==null?null:client.level.getEntity(cowId.get());
                        return entity instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent();
                    },240,"materializable presentation frame");
                }
                System.out.println("S24_RECONNECT_BOOTSTRAP_DIAGNOSTIC PASS dedicated first connection reached every anatomy publication stage");
            } catch(Throwable error) {failure=error;throw error;}
            finally {
                try {server.runOnServer(AnatomyRuntime::stop);}
                catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("S24 reconnect diagnostic cleanup failed",cleanup);}
            }
        }
    }

    private static void await(ClientGameTestContext context,Predicate<Minecraft> predicate,int ticks,String stage) {
        try {context.waitFor(predicate,ticks);}
        catch(AssertionError timeout) {throw new AssertionError("BOOTSTRAP_STAGE "+stage+": timed out",timeout);}
    }

    private static void boot(MinecraftServer server,io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
                             PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S24 reconnect diagnostic cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);
        cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_reconnect_diagnostic_cow",profile));
    }
}
