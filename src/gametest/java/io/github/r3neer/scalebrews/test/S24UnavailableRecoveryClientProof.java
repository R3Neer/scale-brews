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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.cow.Cow;

/** G3.10 proof that a real unsupported pose publishes unavailable and later recovers without a lifecycle rebind. */
public final class S24UnavailableRecoveryClientProof implements FabricClientGameTest {
    private record Identity(UUID epoch,long revision,long bindingGeneration,long trackingGeneration,int entityId,long frameSerial) {}

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
                if(client.level==null)return false;
                var entity=client.level.getEntity(cowId.get());
                if(!(entity instanceof Cow cow) || !cow.getUUID().equals(cowUuid.get()))return false;
                var history=AnatomyClientNetworking.pose(cowUuid.get());
                return history!=null && history.current()!=null && AnatomyClientNetworking.presentationFrame(cow).isPresent();
            },180);

            var initial=context.computeOnClient(client->{
                var history=AnatomyClientNetworking.pose(cowUuid.get());var packet=history==null?null:history.current();
                if(packet==null || !packet.available())throw new AssertionError("Initial quadruped frame never became available");
                return new Identity(packet.epoch(),packet.revision(),packet.bindingGeneration(),packet.trackingGeneration(),packet.entityId(),packet.frameSerial());
            });

            world.getServer().runOnServer(server->setPose(server,cowId.get(),Pose.SLEEPING));
            context.waitFor(client->{
                if(client.level==null)return false;
                var entity=client.level.getEntity(cowId.get());
                if(!(entity instanceof Cow cow) || !cow.getUUID().equals(cowUuid.get()))return false;
                return AnatomyClientNetworking.presentationFrame(cow).isEmpty()
                    && AnatomyClientNetworking.geometry(cow,client.level.getGameTime()).isEmpty();
            },180);
            context.runOnClient(client->{
                var entity=client.level==null?null:client.level.getEntity(cowId.get());
                if(!(entity instanceof Cow cow) || !cow.getUUID().equals(cowUuid.get()))
                    throw new AssertionError("Unsupported pose lost the client support identity");
                var catalog=AnatomyClientNetworking.catalog();
                if(!catalog.ready() || !initial.epoch().equals(catalog.epoch()) || initial.revision()!=catalog.revision())
                    throw new AssertionError("Unsupported pose changed the accepted catalog/session identity");
                if(!AnatomyClientNetworking.ready(cow))
                    throw new AssertionError("Unsupported pose left READY instead of failing only the material endpoint closed");
            });

            world.getServer().runOnServer(server->setPose(server,cowId.get(),Pose.STANDING));
            context.waitFor(client->{
                if(client.level==null)return false;
                var entity=client.level.getEntity(cowId.get());
                if(!(entity instanceof Cow cow) || !cow.getUUID().equals(cowUuid.get()))return false;
                var history=AnatomyClientNetworking.pose(cowUuid.get());var packet=history==null?null:history.current();
                return packet!=null && packet.available() && packet.frameSerial()>initial.frameSerial()
                    && AnatomyClientNetworking.presentationFrame(cow).isPresent()
                    && AnatomyClientNetworking.geometry(cow,client.level.getGameTime()).isPresent();
            },220);

            context.runOnClient(client->{
                var packet=AnatomyClientNetworking.pose(cowUuid.get()).current();
                if(!initial.epoch().equals(packet.epoch()) || initial.revision()!=packet.revision())
                    throw new AssertionError("Recovery changed the accepted catalog identity");
                if(initial.bindingGeneration()!=packet.bindingGeneration())
                    throw new AssertionError("Recovery fabricated a support rebind instead of resuming the accepted binding");
                if(initial.trackingGeneration()!=packet.trackingGeneration())
                    throw new AssertionError("Recovery fabricated a recipient retrack instead of resuming the active tracking window");
                if(initial.entityId()!=packet.entityId() || !packet.entity().equals(cowUuid.get()))
                    throw new AssertionError("Recovery changed the physical support identity");
                if(packet.frameSerial()<=initial.frameSerial())
                    throw new AssertionError("Recovery reused the pre-unavailable causal frame serial");
            });

            System.out.println("S24_UNAVAILABLE_RECOVERY PASS supported -> unavailable -> supported recovered with stable catalog/binding/tracking identity");
        } catch(Throwable error) {failure=error;throw error;}
        finally {
            try {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
            catch(Throwable cleanup) {if(failure!=null)failure.addSuppressed(cleanup);else if(cleanup instanceof RuntimeException runtime)throw runtime;else throw new AssertionError("S24 unavailable recovery cleanup failed",cleanup);}
        }
    }

    private static void boot(MinecraftServer server,io.github.r3neer.scalebrews.collision.geometry.ModelGeometry geometry,
                             PlatformDefinition profile,AtomicInteger cowId,AtomicReference<UUID> cowUuid) {
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
        if(cow==null)throw new AssertionError("Could not create S24 unavailable-recovery cow");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);
        cowId.set(cow.getId());cowUuid.set(cow.getUUID());
        AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s24_unavailable_cow",profile));
    }

    private static void setPose(MinecraftServer server,int cowId,Pose pose) {
        var entity=server.overworld().getEntity(cowId);
        if(!(entity instanceof Cow cow))throw new AssertionError("S24 unavailable-recovery cow disappeared before pose transition");
        cow.setPose(pose);
        if(cow.getPose()!=pose)throw new AssertionError("Server rejected cow pose transition to "+pose);
    }
}
