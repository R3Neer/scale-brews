package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;

/** IMPLEMENTER regression for physical recovery after a rejected catalog candidate. */
public final class S29ImplementerRejectedCatalogClientProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var fragments=new AtomicReference<List<AnatomyCatalogPayload>>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("Catalog rollback fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_catalog_rollback",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyMovement.queryFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var epoch=AnatomyNetworking.epoch(server);long revision=AnatomyNetworking.revision(server)+1;
                int total=AnatomyCatalogPayload.CHUNK+1;
                var first=new AnatomyCatalogPayload(epoch,AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),revision,0,2,total,"0".repeat(64),new byte[AnatomyCatalogPayload.CHUNK]);
                var last=new AnatomyCatalogPayload(epoch,AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),revision,1,2,total,"0".repeat(64),new byte[1]);
                fragments.set(List.of(first,last));
                ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),first);
            });
            context.waitFor(client->AnatomyClientNetworking.mode(client.level.getEntity(id.get()))==AnatomyMode.BINDING,120);
            context.waitTicks(2);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyMovement.queryFrame(cow).isPresent())throw new AssertionError("Pending replacement must pause physical authority");
            });
            world.getServer().runOnServer(server->ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),fragments.get().getLast()));
            context.waitFor(client->AnatomyClientNetworking.mode(client.level.getEntity(id.get()))==AnatomyMode.READY,120);
            context.waitTicks(4);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyClientNetworking.catalog().revision()!=fragments.get().getFirst().revision()-1)
                    throw new AssertionError("Rejected candidate replaced the accepted revision");
                if(AnatomyClientNetworking.presentationFrame(cow).isEmpty())throw new AssertionError("Rejected catalog lost accepted presentation authority");
                if(AnatomyMovement.queryFrame(cow).isEmpty()) {
                    boolean cached;
                    try {
                        var field=AnatomyClientNetworking.class.getDeclaredField("providers");field.setAccessible(true);
                        cached=((Map<?,?>)field.get(null)).containsKey(cow.getUUID());
                    } catch(ReflectiveOperationException diagnosticFailure) {throw new AssertionError("S29 provider diagnostic unavailable",diagnosticFailure);}
                    throw new AssertionError("NFR-016: rejected catalog recovered READY but left accepted support physically unregistered"
                        +" active="+AnatomyMovement.active(cow)+" registration="+AnatomyMovement.registrationGeneration(cow)
                        +" clientProviderCached="+cached+" published="+AnatomyMovement.publishedFrame(cow).isPresent());
                }
                System.out.println("S29 REJECTED CATALOG PASS accepted presentation and physical authority recovered");
            });
        } finally {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
    }
}
