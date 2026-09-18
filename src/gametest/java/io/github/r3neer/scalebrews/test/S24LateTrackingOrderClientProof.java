package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyNetworking;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * G3.11 / FR-080+081 proof over the real runtime and packet receiver.
 * A late observer must bootstrap from the current authoritative endpoint, then reject an older
 * packet from the same tracking window after a newer frame has already become authoritative.
 */
public final class S24LateTrackingOrderClientProof implements FabricClientGameTest {
    private record Identity(UUID epoch,long revision,long bindingGeneration,long trackingGeneration,int entityId,long frameSerial,long authorityTick) {}

    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);var cowUuid=new AtomicReference<UUID>();var forcedChunk=new AtomicReference<ChunkPos>();
        var preTrackingSerial=new AtomicLong();var preTrackingAuthorityTick=new AtomicLong();
        var staleFrame=new AtomicReference<GeometryProvider.PublishedFrame>();var trackingGeneration=new AtomicLong();
        var world=context.worldBuilder().create();Throwable failure=null;
        try {
            world.getServer().runOnServer(server->bootFar(server,geometry,profile,cowId,cowUuid,forcedChunk));
            context.waitTicks(24);

            world.getServer().runOnServer(server->{
                var cow=cow(server,cowId.get(),cowUuid.get());var player=server.getPlayerList().getPlayers().getFirst();
                if(PlayerLookup.tracking(cow).contains(player))
                    throw new AssertionError("Late-tracking fixture cow entered recipient tracking range before the barrier opened");
                var current=AnatomyMovement.publishedFrame(cow).orElseThrow(
                    ()->new AssertionError("Off-range runtime never produced a current authoritative cow endpoint"));
                preTrackingSerial.set(current.endpoint().frameSerial());preTrackingAuthorityTick.set(current.endpoint().authorityTick());
                if(preTrackingSerial.get()<=1)
                    throw new AssertionError("Late-tracking fixture never advanced beyond its first causal endpoint before observation");
                teleportNear(server,cow);
            });

            context.waitFor(client->{
                if(client.level==null)return false;
                var history=AnatomyClientNetworking.pose(cowUuid.get());var packet=history==null?null:history.current();
                if(packet==null)return false;
                var entity=client.level.getEntity(packet.entityId());
                return entity instanceof Cow cow && cow.getUUID().equals(cowUuid.get())
                    && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },220);

            var first=context.computeOnClient(client->identity(cowUuid.get()));
            if(first.frameSerial()<preTrackingSerial.get() || first.authorityTick()<preTrackingAuthorityTick.get())
                throw new AssertionError("Late observer replayed an older endpoint instead of bootstrapping from current server material state");
            if(first.frameSerial()<=1)
                throw new AssertionError("Late observer was bootstrapped from a fabricated frame-1 history instead of the current endpoint");

            world.getServer().runOnServer(server->{
                var cow=cow(server,cowId.get(),cowUuid.get());var player=server.getPlayerList().getPlayers().getFirst();
                if(!PlayerLookup.tracking(cow).contains(player))
                    throw new AssertionError("Recipient stopped tracking the cow before packet-order proof");
                long generation=AnatomyRuntime.trackingGeneration(player,cow);
                if(generation!=first.trackingGeneration())
                    throw new AssertionError("Client/server disagree on the active late-tracking generation");
                trackingGeneration.set(generation);
                staleFrame.set(AnatomyMovement.publishedFrame(cow).orElseThrow(
                    ()->new AssertionError("Tracked cow lost its authoritative endpoint before stale capture")));
            });

            context.waitFor(client->{
                var history=AnatomyClientNetworking.pose(cowUuid.get());var packet=history==null?null:history.current();
                return packet!=null && packet.frameSerial()>staleFrame.get().endpoint().frameSerial();
            },180);
            var beforeReplay=context.computeOnClient(client->identity(cowUuid.get()));

            // Freeze further anatomy publication before replaying the captured stale frame. Without
            // this barrier a stale packet can transiently roll the receiver backward and then be hidden
            // by the next ordinary fresh publication before the client assertion observes it.
            world.getServer().runOnServer(server->{
                var recipient=server.getPlayerList().getPlayers().getFirst();
                AnatomyRuntime.stop(server);
                AnatomyNetworking.sendPose(recipient,staleFrame.get(),trackingGeneration.get());
            });
            context.waitTicks(5);

            context.runOnClient(client->{
                var now=identity(cowUuid.get());
                if(now.frameSerial()<beforeReplay.frameSerial() || now.authorityTick()<beforeReplay.authorityTick())
                    throw new AssertionError("Delayed same-window pose packet rolled client authority backward");
                if(!now.epoch().equals(beforeReplay.epoch()) || now.revision()!=beforeReplay.revision()
                        || now.bindingGeneration()!=beforeReplay.bindingGeneration() || now.trackingGeneration()!=beforeReplay.trackingGeneration()
                        || now.entityId()!=beforeReplay.entityId())
                    throw new AssertionError("Stale same-window packet changed the accepted causal identity");
                var entity=client.level.getEntity(now.entityId());
                if(!(entity instanceof Cow cow) || AnatomyClientNetworking.presentationFrame(cow).isEmpty())
                    throw new AssertionError("Rejecting stale packet destroyed the current material presentation");
            });

            System.out.println("S24_LATE_TRACKING_ORDER PASS late START_TRACKING bootstrapped current endpoint and stale same-window replay could not roll it back");
        } catch(Throwable error) {failure=error;throw error;}
        finally {
            try {world.getServer().runOnServer(server->{
                var chunk=forcedChunk.get();if(chunk!=null)server.overworld().setChunkForced(chunk.x(),chunk.z(),false);
                AnatomyRuntime.stop(server);
            });world.close();}
            catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("S24 late-tracking order cleanup failed",cleanup);}
        }
    }

    private static Identity identity(UUID cowUuid) {
        var catalog=AnatomyClientNetworking.catalog();var history=AnatomyClientNetworking.pose(cowUuid);var packet=history==null?null:history.current();
        if(!catalog.ready() || packet==null)throw new AssertionError("Client has no accepted late-tracking cow endpoint");
        return new Identity(packet.epoch(),packet.revision(),packet.bindingGeneration(),packet.trackingGeneration(),packet.entityId(),packet.frameSerial(),packet.authorityTick());
    }

    private static void bootFar(MinecraftServer server,io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
                                PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid,AtomicReference<ChunkPos> forcedChunk) {
        var player=server.getPlayerList().getPlayers().getFirst();
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S24 late-tracking cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();cow.setPos(player.getX()+192.0,player.getY(),player.getZ());
        var chunk=cow.chunkPosition();server.overworld().setChunkForced(chunk.x(),chunk.z(),true);forcedChunk.set(chunk);
        server.overworld().addFreshEntity(cow);cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_late_tracking_cow",profile));
    }

    private static Cow cow(MinecraftServer server,int id,UUID uuid) {
        var entity=server.overworld().getEntity(id);
        if(!(entity instanceof Cow cow) || !cow.getUUID().equals(uuid))throw new AssertionError("S24 late-tracking cow disappeared or changed identity");
        return cow;
    }

    private static void teleportNear(MinecraftServer server,Cow cow) {
        var player=server.getPlayerList().getPlayers().getFirst();var target=cow.position().add(0,0,2);
        var moved=player.teleport(new TeleportTransition(server.overworld(),target,Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));
        if(moved==null)throw new AssertionError("Server rejected same-dimension late-tracking teleport");
    }
}
