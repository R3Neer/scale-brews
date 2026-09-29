package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * G4.3 / FR-079+081: a late remote observer reconstructs current confirmed
 * presentation/contact without replaying historical server carry as local transport.
 */
public final class S27LateObserverPresentationClientProof implements FabricClientGameTest {
    private record ClientState(Vec3 position,long frameSerial,long contactTick,
            boolean supported,boolean simulates,boolean predicts,boolean hasTransport) {}

    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));

        var cowId=new AtomicInteger(-1);var pigId=new AtomicInteger(-1);
        var cowUuid=new AtomicReference<UUID>();var pigUuid=new AtomicReference<UUID>();
        var forcedChunk=new AtomicReference<ChunkPos>();
        var preTrackingTransport=new AtomicLong();var firstGeneration=new AtomicLong();var secondGeneration=new AtomicLong();
        var firstServerPosition=new AtomicReference<Vec3>();var retrackServerPosition=new AtomicReference<Vec3>();

        var world=context.worldBuilder().create();Throwable failure=null;
        try {
            world.getServer().runOnServer(server->bootFar(server,geometry,profile,cowId,cowUuid,forcedChunk));
            context.waitTicks(24);
            world.getServer().runOnServer(server->createSupportedPig(server,cowId.get(),cowUuid.get(),pigId,pigUuid));

            // Build real transport history while the player is not tracking either support or body.
            for(int step=0;step<12;step++) {
                final int index=step;
                world.getServer().runOnServer(server->moveSupport(server,cowId.get(),cowUuid.get(),index));
                context.waitTicks(1);
            }
            world.getServer().runOnServer(server->{
                var cow=cow(server,cowId.get(),cowUuid.get());var pig=pig(server,pigId.get(),pigUuid.get());
                var player=server.getPlayerList().getPlayers().getFirst();
                if(PlayerLookup.tracking(cow).contains(player) || PlayerLookup.tracking(pig).contains(player))
                    throw new AssertionError("S27 fixture entered tracking range before late-observer barrier");
                var transport=AnatomyMovement.transport(pig);
                if(transport==null || transport.sequence()<8)
                    throw new AssertionError("S27 fixture did not accumulate real pre-tracking server transport: "+transport);
                preTrackingTransport.set(transport.sequence());firstServerPosition.set(pig.position());
                teleportNear(server,cow);
            });

            waitForObserverPresentation(context,cowId.get(),cowUuid.get(),pigId.get(),pigUuid.get());
            waitForClientPosition(context,pigId.get(),pigUuid.get(),firstServerPosition.get());
            var first=context.computeOnClient(client->clientState(client,cowId.get(),cowUuid.get(),pigId.get(),pigUuid.get()));
            assertPassive(first,"first late-track");
            if(first.frameSerial()<2)
                throw new AssertionError("Late observer bootstrapped from an implausible frame-1 presentation: "+first);
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();var pig=pig(server,pigId.get(),pigUuid.get());
                long generation=AnatomyRuntime.trackingGeneration(player,pig);
                if(generation<1)throw new AssertionError("Late observer never acquired body tracking generation");
                firstGeneration.set(generation);
            });

            // One more confirmed server carry must advance presentation/vanilla position, never client transport.
            world.getServer().runOnServer(server->moveSupport(server,cowId.get(),cowUuid.get(),100));
            context.waitTicks(3);
            var afterServer=new AtomicReference<Vec3>();
            world.getServer().runOnServer(server->afterServer.set(pig(server,pigId.get(),pigUuid.get()).position()));
            waitForClientPosition(context,pigId.get(),pigUuid.get(),afterServer.get());
            context.waitFor(client->{
                if(client.level==null)return false;
                var cow=client.level.getEntity(cowId.get());
                if(!(cow instanceof Cow living) || !living.getUUID().equals(cowUuid.get()))return false;
                return AnatomyClientNetworking.presentationFrame(living)
                    .map(frame->frame.after().frameSerial()>first.frameSerial()).orElse(false);
            },120);
            var after=context.computeOnClient(client->clientState(client,cowId.get(),cowUuid.get(),pigId.get(),pigUuid.get()));
            assertPassive(after,"tracked support advance");
            if(after.position().distanceToSqr(first.position())<1e-4 || after.frameSerial()<=first.frameSerial())
                throw new AssertionError("Observer presentation did not advance with confirmed server state: before="+first+" after="+after);

            // Lose tracking, advance more server transport, then re-enter. Historical carry must not replay locally.
            teleportFarAndWait(context,world,cowId.get(),cowUuid.get(),pigId.get(),pigUuid.get());
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();var pig=pig(server,pigId.get(),pigUuid.get());
                if(AnatomyRuntime.trackingGeneration(player,pig)!=0)
                    throw new AssertionError("STOP_TRACKING did not retire observer body tracking generation");
            });
            for(int step=0;step<8;step++) {
                final int index=200+step;
                world.getServer().runOnServer(server->moveSupport(server,cowId.get(),cowUuid.get(),index));
                context.waitTicks(1);
            }
            world.getServer().runOnServer(server->{
                var cow=cow(server,cowId.get(),cowUuid.get());var pig=pig(server,pigId.get(),pigUuid.get());
                var transport=AnatomyMovement.transport(pig);
                if(transport==null || transport.sequence()<=preTrackingTransport.get())
                    throw new AssertionError("Server observer transport did not advance while recipient was untracked: "+transport);
                retrackServerPosition.set(pig.position());teleportNear(server,cow);
            });

            waitForObserverPresentation(context,cowId.get(),cowUuid.get(),pigId.get(),pigUuid.get());
            waitForClientPosition(context,pigId.get(),pigUuid.get(),retrackServerPosition.get());
            var retracked=context.computeOnClient(client->clientState(client,cowId.get(),cowUuid.get(),pigId.get(),pigUuid.get()));
            assertPassive(retracked,"retrack");
            if(retracked.frameSerial()<=after.frameSerial())
                throw new AssertionError("Retrack did not bootstrap current support presentation: before="+after+" retracked="+retracked);
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();var pig=pig(server,pigId.get(),pigUuid.get());
                long generation=AnatomyRuntime.trackingGeneration(player,pig);
                if(generation<=firstGeneration.get())
                    throw new AssertionError("Retrack did not create a fresh body tracking generation: first="+firstGeneration.get()+" second="+generation);
                secondGeneration.set(generation);
            });

            System.out.println("S27_LATE_OBSERVER PASS current presentation/contact with zero local carry across late-track + retrack generations "
                +firstGeneration.get()+"->"+secondGeneration.get());
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
                else throw new AssertionError("S27 late observer cleanup failed",cleanup);
            }
        }
    }

    private static void assertPassive(ClientState state,String phase) {
        if(!state.supported())
            throw new AssertionError("Remote observer lost confirmed read-only contact during "+phase+": "+state);
        if(state.simulates() || state.predicts() || state.hasTransport())
            throw new AssertionError("Remote observer gained local physics during "+phase+": "+state);
    }

    private static ClientState clientState(net.minecraft.client.Minecraft client,int cowId,UUID cowUuid,int pigId,UUID pigUuid) {
        if(client.level==null)throw new AssertionError("Missing client level");
        var support=client.level.getEntity(cowId);var body=client.level.getEntity(pigId);
        if(!(support instanceof Cow cow) || !cow.getUUID().equals(cowUuid)
                || !(body instanceof Pig pig) || !pig.getUUID().equals(pigUuid))
            throw new AssertionError("S27 observer entities missing or replaced");
        var presentation=AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).orElseThrow(
            ()->new AssertionError("Remote observer has no confirmed presentation contact"));
        if(!presentation.support().getUUID().equals(cowUuid))
            throw new AssertionError("Remote observer presentation points at the wrong support");
        var frame=AnatomyClientNetworking.presentationFrame(cow).orElseThrow(
            ()->new AssertionError("Remote observer support has no presentation frame"));
        return new ClientState(pig.position(),frame.after().frameSerial(),presentation.serverTick(),
            AnatomyMovement.supported(pig),AnatomyMovement.simulates(pig),AnatomyMovement.predictsBody(pig),
            AnatomyMovement.transport(pig)!=null);
    }

    private static void waitForObserverPresentation(ClientGameTestContext context,int cowId,UUID cowUuid,int pigId,UUID pigUuid) {
        context.waitFor(client->{
            if(client.level==null)return false;
            var support=client.level.getEntity(cowId);var body=client.level.getEntity(pigId);
            if(!(support instanceof Cow cow) || !cow.getUUID().equals(cowUuid)
                    || !(body instanceof Pig pig) || !pig.getUUID().equals(pigUuid))return false;
            return AnatomyClientNetworking.presentationFrame(cow).isPresent()
                && AnatomyClientNetworking.presentationContact(pig,client.level.getGameTime()).isPresent();
        },220);
    }

    private static void waitForClientPosition(ClientGameTestContext context,int pigId,UUID pigUuid,Vec3 expected) {
        context.waitFor(client->{
            if(client.level==null)return false;var entity=client.level.getEntity(pigId);
            return entity instanceof Pig pig && pig.getUUID().equals(pigUuid)
                && pig.position().distanceToSqr(expected)<.04;
        },180);
    }

    private static void teleportFarAndWait(ClientGameTestContext context,
            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world,
            int cowId,UUID cowUuid,int pigId,UUID pigUuid) {
        world.getServer().runOnServer(server->{
            var cow=cow(server,cowId,cowUuid);var player=server.getPlayerList().getPlayers().getFirst();
            var target=cow.position().add(-192,0,0);
            var moved=player.teleport(new TeleportTransition(server.overworld(),target,Vec3.ZERO,
                player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));
            if(moved==null)throw new AssertionError("S27 server rejected far observer teleport");
        });
        var stopped=new java.util.concurrent.atomic.AtomicBoolean();
        for(int attempt=0;attempt<180;attempt++) {
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                stopped.set(!PlayerLookup.tracking(cow(server,cowId,cowUuid)).contains(player)
                    && !PlayerLookup.tracking(pig(server,pigId,pigUuid)).contains(player));
            });
            if(stopped.get())return;
            context.waitTicks(1);
        }
        throw new AssertionError("S27 observer never left tracking range");
    }

    private static void bootFar(MinecraftServer server,io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
            PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid,AtomicReference<ChunkPos> forcedChunk) {
        var player=server.getPlayerList().getPlayers().getFirst();
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S27 late-observer cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPersistenceRequired();
        cow.setPos(player.getX()+192.0,player.getY(),player.getZ());
        var chunk=cow.chunkPosition();server.overworld().setChunkForced(chunk.x(),chunk.z(),true);forcedChunk.set(chunk);
        server.overworld().addFreshEntity(cow);cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s27_observer_cow",profile));
    }

    private static void createSupportedPig(MinecraftServer server,int cowId,UUID cowUuid,
            AtomicInteger pigId,AtomicReference<UUID> pigUuid) {
        var cow=cow(server,cowId,cowUuid);var level=server.overworld();
        var frame=AnatomyMovement.queryFrame(cow).orElseThrow(
            ()->new AssertionError("S27 cow has no prepared query frame"));
        var piece=frame.snapshot().pieces().get("root/body/cube_0");
        if(piece==null)throw new AssertionError("S27 cow fixture lacks root/body/cube_0");
        var pig=net.minecraft.world.entity.EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);
        if(pig==null)throw new AssertionError("Could not create S27 observer pig");
        pig.setNoAi(true);pig.setNoGravity(true);pig.setPersistenceRequired();
        var scale=pig.getAttribute(Attributes.SCALE);if(scale==null)throw new AssertionError("Pig lacks SCALE attribute");
        scale.setBaseValue(.2);pig.refreshDimensions();
        var box=piece.bounds();pig.setPos(box.getCenter().x,box.maxY+.1,box.getCenter().z);level.addFreshEntity(pig);
        pig.move(MoverType.SELF,new Vec3(0,-.2,0));
        if(!AnatomyMovement.supported(pig))
            throw new AssertionError("S27 observer pig could not acquire real cow material support");
        pigId.set(pig.getId());pigUuid.set(pig.getUUID());
    }

    private static void moveSupport(MinecraftServer server,int cowId,UUID cowUuid,int step) {
        var cow=cow(server,cowId,cowUuid);
        cow.move(MoverType.SELF,new Vec3(.04,.01,(step&1)==0?.006:-.006));
    }

    private static Cow cow(MinecraftServer server,int id,UUID uuid) {
        var entity=server.overworld().getEntity(id);
        if(!(entity instanceof Cow cow) || !cow.getUUID().equals(uuid))
            throw new AssertionError("S27 cow disappeared or changed identity");
        return cow;
    }

    private static Pig pig(MinecraftServer server,int id,UUID uuid) {
        var entity=server.overworld().getEntity(id);
        if(!(entity instanceof Pig pig) || !pig.getUUID().equals(uuid))
            throw new AssertionError("S27 pig disappeared or changed identity");
        return pig;
    }

    private static void teleportNear(MinecraftServer server,Cow cow) {
        var player=server.getPlayerList().getPlayers().getFirst();var target=cow.position().add(0,0,3);
        var moved=player.teleport(new TeleportTransition(server.overworld(),target,Vec3.ZERO,
            player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));
        if(moved==null)throw new AssertionError("S27 server rejected near observer teleport");
    }
}
