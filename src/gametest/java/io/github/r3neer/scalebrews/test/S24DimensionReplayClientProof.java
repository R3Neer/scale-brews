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
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Adversarial G3.9 holdout for NFR-017: a frame from the first visit to a dimension must not
 * become authoritative again merely because the client later returns to the same dimension.
 */
public final class S24DimensionReplayClientProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);var cowUuid=new AtomicReference<UUID>();
        var oldFrame=new AtomicReference<GeometryProvider.PublishedFrame>();var oldGeneration=new AtomicLong();
        var world=context.worldBuilder().create();Throwable failure=null;
        try {
            world.getServer().runOnServer(server->boot(server,geometry,profile,cowId,cowUuid));
            context.waitFor(client->{
                if(client.level==null || client.level.dimension()!=Level.OVERWORLD)return false;
                var entity=client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && cow.getUUID().equals(cowUuid.get())
                    && AnatomyClientNetworking.pose(cowUuid.get())!=null
                    && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },160);

            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(cowId.get());
                if(cow==null)throw new AssertionError("Missing replay fixture cow before dimension barrier");
                var player=server.getPlayerList().getPlayers().getFirst();
                oldFrame.set(AnatomyMovement.publishedFrame(cow).orElseThrow(
                    ()->new AssertionError("Missing authoritative frame before dimension barrier")));
                long generation=AnatomyRuntime.trackingGeneration(player,cow);
                if(generation<1)throw new AssertionError("Missing active tracking generation before dimension barrier");
                oldGeneration.set(generation);
                // Prevent START_TRACKING on the return trip from publishing a newer frame first. The client
                // connection/catalog remains alive; only server anatomical authority is stopped.
                AnatomyRuntime.stop(server);
                teleport(server,Level.NETHER,new Vec3(0,80,0));
            });

            context.waitFor(client->client.level!=null && client.level.dimension()==Level.NETHER,160);
            context.runOnClient(client->{
                if(!AnatomyClientNetworking.catalog().ready())
                    throw new AssertionError("Same-connection dimension barrier unexpectedly discarded the accepted catalog");
                if(AnatomyClientNetworking.pose(cowUuid.get())!=null)
                    throw new AssertionError("Old Overworld pose survived into the Nether before replay injection");
            });

            world.getServer().runOnServer(server->teleport(server,Level.OVERWORLD,new Vec3(2,4,2)));
            context.waitFor(client->{
                if(client.level==null || client.level.dimension()!=Level.OVERWORLD)return false;
                var entity=client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && cow.getUUID().equals(cowUuid.get());
            },200);
            context.runOnClient(client->{
                if(AnatomyClientNetworking.pose(cowUuid.get())!=null)
                    throw new AssertionError("Runtime-stopped return trip somehow reacquired a fresh pose before adversarial replay");
            });

            // Replay exactly the immutable frame/generation accepted during the first Overworld visit.
            world.getServer().runOnServer(server->AnatomyNetworking.sendPose(
                server.getPlayerList().getPlayers().getFirst(),oldFrame.get(),oldGeneration.get()));
            context.waitTicks(5);

            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(cowId.get());
                if(AnatomyClientNetworking.pose(cowUuid.get())!=null || AnatomyClientNetworking.presentationFrame(cow).isPresent())
                    throw new AssertionError("Pre-dimension-barrier pose replay became authoritative after A->B->A round trip");
            });

            System.out.println("S24_DIMENSION_REPLAY PASS pre-barrier Overworld frame rejected after A-B-A round trip");
        } catch(Throwable error) {failure=error;throw error;}
        finally {
            try {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
            catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("Dimension replay proof cleanup failed",cleanup);}
        }
    }

    private static void boot(MinecraftServer server,io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
                             PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S24 dimension replay cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);
        cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_dimension_replay_cow",profile));
    }

    private static void teleport(MinecraftServer server,net.minecraft.resources.ResourceKey<Level> targetKey,Vec3 position) {
        var target=server.getLevel(targetKey);if(target==null)throw new AssertionError("Missing target dimension "+targetKey.identifier());
        var player=server.getPlayerList().getPlayers().getFirst();
        var moved=player.teleport(new TeleportTransition(target,position,Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));
        if(moved==null)throw new AssertionError("Server rejected dimension transition to "+targetKey.identifier());
    }
}
