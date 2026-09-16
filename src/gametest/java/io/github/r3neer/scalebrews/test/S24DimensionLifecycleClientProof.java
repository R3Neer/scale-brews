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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Integrated G3.9 proof that temporal anatomy is level-scoped while the catalog is connection-scoped. */
public final class S24DimensionLifecycleClientProof implements FabricClientGameTest {
    private record Identity(UUID epoch,long revision,long bindingGeneration,long trackingGeneration,int entityId) {}

    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla(
            "minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var cowId=new AtomicInteger(-1);var cowUuid=new AtomicReference<UUID>();
        var world=context.worldBuilder().create();Throwable failure=null;
        try {
            world.getServer().runOnServer(server->boot(server,geometry,profile,cowId,cowUuid));
            context.waitFor(client->{
                if(client.level==null || client.level.dimension()!=Level.OVERWORLD)return false;
                var entity=client.level.getEntity(cowId.get());
                if(!(entity instanceof Cow cow) || !cow.getUUID().equals(cowUuid.get()))return false;
                var history=AnatomyClientNetworking.pose(cowUuid.get());
                return history!=null && history.current()!=null && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },160);

            var initial=context.computeOnClient(client->{
                var catalog=AnatomyClientNetworking.catalog();var history=AnatomyClientNetworking.pose(cowUuid.get());
                var packet=history==null?null:history.current();
                if(!catalog.ready() || packet==null)throw new AssertionError("Initial Overworld anatomy session never became READY");
                if(!packet.dimension().equals(Level.OVERWORLD.identifier()))throw new AssertionError("Initial cow pose is not scoped to Overworld");
                return new Identity(catalog.epoch(),catalog.revision(),packet.bindingGeneration(),packet.trackingGeneration(),packet.entityId());
            });

            world.getServer().runOnServer(server->teleport(server,Level.NETHER,new Vec3(0,80,0)));
            context.waitFor(client->client.level!=null && client.player!=null && client.level.dimension()==Level.NETHER
                && AnatomyClientNetworking.ready(client.player),160);
            context.runOnClient(client->{
                var catalog=AnatomyClientNetworking.catalog();
                if(!catalog.ready() || !initial.epoch().equals(catalog.epoch()) || initial.revision()!=catalog.revision())
                    throw new AssertionError("Same-connection dimension switch discarded or replaced the accepted catalog");
                if(AnatomyClientNetworking.pose(cowUuid.get())!=null)
                    throw new AssertionError("Old Overworld pose history survived into the Nether ClientLevel");
            });

            world.getServer().runOnServer(server->teleport(server,Level.OVERWORLD,new Vec3(2,4,2)));
            context.waitFor(client->{
                if(client.level==null || client.level.dimension()!=Level.OVERWORLD)return false;
                var entity=client.level.getEntity(cowId.get());
                if(!(entity instanceof Cow cow) || !cow.getUUID().equals(cowUuid.get()))return false;
                var history=AnatomyClientNetworking.pose(cowUuid.get());
                return history!=null && history.current()!=null && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },200);

            context.runOnClient(client->{
                var catalog=AnatomyClientNetworking.catalog();var history=AnatomyClientNetworking.pose(cowUuid.get());var packet=history.current();
                if(!catalog.ready() || !initial.epoch().equals(catalog.epoch()) || initial.revision()!=catalog.revision())
                    throw new AssertionError("Returning to Overworld changed a connection-scoped catalog identity");
                if(packet.entityId()!=initial.entityId() || !packet.entity().equals(cowUuid.get()))
                    throw new AssertionError("Return tracking reacquired a different support identity");
                if(packet.bindingGeneration()!=initial.bindingGeneration())
                    throw new AssertionError("Dimension re-tracking fabricated a server binding rebind");
                if(packet.trackingGeneration()<=initial.trackingGeneration())
                    throw new AssertionError("Return to the same support reused the pre-dimension tracking generation");
                if(!packet.dimension().equals(Level.OVERWORLD.identifier()))
                    throw new AssertionError("Reacquired pose did not return to the active Overworld identity");
            });

            System.out.println("S24_DIMENSION_LIFECYCLE PASS catalog survived level switch; temporal state restarted with tracking++ and stable binding");
        } catch(Throwable error) {failure=error;throw error;}
        finally {
            try {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
            catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("S24 dimension lifecycle cleanup failed",cleanup);}
        }
    }

    private static void boot(MinecraftServer server,io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
                             PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S24 dimension lifecycle cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);
        cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_dimension_cow",profile));
    }

    private static void teleport(MinecraftServer server,net.minecraft.resources.ResourceKey<Level> targetKey,Vec3 position) {
        var target=server.getLevel(targetKey);if(target==null)throw new AssertionError("Missing target dimension "+targetKey.identifier());
        var player=server.getPlayerList().getPlayers().getFirst();
        var moved=player.teleport(new TeleportTransition(target,position,Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));
        if(moved==null)throw new AssertionError("Server rejected dimension transition to "+targetKey.identifier());
    }
}
