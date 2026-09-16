package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.collision.pose.QuadrupedPose;
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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Adversarial G3.9 holdout for the contact channel. A contact accepted before a dimension barrier
 * must not become authoritative again after A->B->A merely because the new level has already
 * reacquired fresh support geometry.
 */
public final class S24ContactDimensionReplayClientProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);var cowUuid=new AtomicReference<UUID>();
        var pigId=new AtomicInteger(-1);var pigUuid=new AtomicReference<UUID>();
        var oldSupportFrame=new AtomicReference<GeometryProvider.PublishedFrame>();
        var oldSupportTracking=new AtomicLong();var oldContact=new AtomicReference<AnatomyContactPayload>();
        var forcedChunk=new AtomicReference<ChunkPos>();
        var world=context.worldBuilder().create();Throwable failure=null;
        try {
            world.getServer().runOnServer(server->boot(server,geometry,profile,cowId,cowUuid,forcedChunk));
            context.waitFor(client->{
                if(client.level==null || client.level.dimension()!=Level.OVERWORLD)return false;
                var entity=client.level.getEntity(cowId.get());
                return entity instanceof Cow cow && cow.getUUID().equals(cowUuid.get())
                    && AnatomyClientNetworking.catalog().ready()
                    && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },180);

            // Create one real small body/contact through the production material solver, then freeze
            // anatomy publication. The packet we send below is accepted once before the barrier and
            // replayed byte-for-byte after returning to Overworld.
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(cowId.get());
                if(cow==null)throw new AssertionError("Missing contact replay cow");
                var player=server.getPlayerList().getPlayers().getFirst();
                var pig=net.minecraft.world.entity.EntityTypes.PIG.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(pig==null)throw new AssertionError("Could not create contact replay pig");
                pig.setNoAi(true);pig.setNoGravity(true);
                pig.getAttribute(Attributes.SCALE).setBaseValue(.2);pig.refreshDimensions();
                var evaluator=new ModelGeometryProvider(geometry,new QuadrupedPose(),AnatomyFilter.DEFAULT,AnatomyNetworking.revision(server));
                evaluator.pose(cow,new PoseProvider.Inputs(0,0,0,0,0,true));
                var back=evaluator.sample(cow).orElseThrow().pieces().get("root/body/cube_0").bounds();
                pig.setPos(back.getCenter().x,back.maxY+.1,back.getCenter().z);
                server.overworld().addFreshEntity(pig);pigId.set(pig.getId());pigUuid.set(pig.getUUID());
                pig.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.2,0));
                var surface=AnatomyMovement.surface(pig);
                if(surface==null || !AnatomyMovement.supported(pig))
                    throw new AssertionError("Contact replay fixture did not acquire a real anatomical support");

                var frame=AnatomyMovement.publishedFrame(cow).orElseThrow(
                    ()->new AssertionError("Missing authoritative support frame before contact barrier"));
                long supportGeneration=AnatomyRuntime.trackingGeneration(player,cow);
                long bodyGeneration=AnatomyRuntime.trackingGeneration(player,pig);
                if(supportGeneration<1 || bodyGeneration<1)throw new AssertionError("Missing tracking generation in contact fixture");
                oldSupportFrame.set(frame);oldSupportTracking.set(supportGeneration);
                oldContact.set(new AnatomyContactPayload(
                    AnatomyNetworking.epoch(server),AnatomyNetworking.revision(server),Level.OVERWORLD.identifier(),
                    pig.getId(),pig.getUUID(),bodyGeneration,10_000,server.overworld().getGameTime(),
                    cow.getId(),cow.getUUID(),surface.piece(),surface.face(),surface.localPoint(),surface.normal()));
                AnatomyRuntime.stop(server);
            });

            context.waitFor(client->client.level!=null && client.level.getEntity(pigId.get()) instanceof Pig,100);
            world.getServer().runOnServer(server->ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),oldContact.get()));
            context.waitFor(client->{
                var body=client.level==null?null:client.level.getEntity(pigId.get());
                return body instanceof Pig && AnatomyClientNetworking.presentationContact(body,client.level.getGameTime()).isPresent();
            },100);

            world.getServer().runOnServer(server->teleport(server,Level.NETHER,new Vec3(0,80,0)));
            context.waitFor(client->client.level!=null && client.level.dimension()==Level.NETHER,160);
            context.runOnClient(client->{
                if(!AnatomyClientNetworking.catalog().ready())
                    throw new AssertionError("Same-connection dimension barrier discarded the accepted catalog");
            });

            world.getServer().runOnServer(server->teleport(server,Level.OVERWORLD,new Vec3(2,4,2)));
            context.waitFor(client->{
                if(client.level==null || client.level.dimension()!=Level.OVERWORLD)return false;
                return client.level.getEntity(cowId.get()) instanceof Cow && client.level.getEntity(pigId.get()) instanceof Pig;
            },200);

            // Reacquire the support with a provably post-barrier tracking generation. This makes the
            // subsequent failure, if any, belong to the contact channel rather than the known pose replay seam.
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                AnatomyNetworking.sendPose(player,oldSupportFrame.get(),AnatomyRuntime.nextTrackingGeneration(oldSupportTracking.get()));
            });
            context.waitFor(client->{
                if(client.level==null)return false;
                var cow=client.level.getEntity(cowId.get());
                return cow instanceof Cow living && AnatomyClientNetworking.presentationFrame(living).isPresent();
            },100);
            context.runOnClient(client->{
                var body=client.level.getEntity(pigId.get());
                if(AnatomyClientNetworking.presentationContact(body,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Contact material survived the dimension barrier before replay injection");
            });

            world.getServer().runOnServer(server->{
                long now=server.overworld().getGameTime();
                if(oldContact.get().tick()+100<now)
                    throw new AssertionError("Contact replay fixture aged beyond the receiver retention window before injection: old="+oldContact.get().tick()+" now="+now);
                ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),oldContact.get());
            });
            context.waitTicks(5);

            context.runOnClient(client->{
                var body=client.level.getEntity(pigId.get());
                if(AnatomyClientNetworking.presentationContact(body,client.level.getGameTime()).isPresent())
                    throw new AssertionError("Pre-dimension-barrier contact replay became authoritative after fresh support reacquisition");
            });
            System.out.println("S24_CONTACT_DIMENSION_REPLAY PASS pre-barrier contact rejected after fresh post-barrier support pose");
        } catch(Throwable error) {failure=error;throw error;}
        finally {
            try {
                world.getServer().runOnServer(server->{
                    var chunk=forcedChunk.get();if(chunk!=null)server.overworld().setChunkForced(chunk.x(),chunk.z(),false);
                    AnatomyRuntime.stop(server);
                });
                world.close();
            } catch(Throwable cleanup) {
                if(failure!=null)failure.addSuppressed(cleanup);
                else if(cleanup instanceof RuntimeException runtime)throw runtime;
                else throw new AssertionError("Contact dimension replay cleanup failed",cleanup);
            }
        }
    }

    private static void boot(MinecraftServer server,ModelGeometry geometry,PlatformDefinition profile,
                             AtomicInteger cowId,AtomicReference<UUID> cowUuid,AtomicReference<ChunkPos> forcedChunk) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create contact replay cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();cow.setPos(2,3,2);
        var chunk=cow.chunkPosition();server.overworld().setChunkForced(chunk.x(),chunk.z(),true);forcedChunk.set(chunk);
        server.overworld().addFreshEntity(cow);cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_contact_replay_cow",profile));
    }

    private static void teleport(MinecraftServer server,net.minecraft.resources.ResourceKey<Level> targetKey,Vec3 position) {
        var target=server.getLevel(targetKey);if(target==null)throw new AssertionError("Missing target dimension "+targetKey.identifier());
        var player=server.getPlayerList().getPlayers().getFirst();
        var moved=player.teleport(new TeleportTransition(target,position,Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));
        if(moved==null)throw new AssertionError("Server rejected dimension transition to "+targetKey.identifier());
    }
}
