package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Independent catalog rollback phase: invalid decoded candidate preserves accepted certified material. */
public final class S29AdversarialCatalogRollbackProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var firstAfter=new AtomicLong();var secondAfter=new AtomicLong();
        var token=new AtomicReference<AnatomyMaterialIntervalPayload>();
        var fragments=new AtomicReference<List<AnatomyCatalogPayload>>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("S29 fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);server.overworld().addFreshEntity(cow);id.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_adversarial",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());
                long before=AnatomyMovement.publishedFrame(cow).orElseThrow().endpoint().frameSerial();
                cow.move(MoverType.SELF,new Vec3(.125,0,0));
                firstAfter.set(AnatomyMovement.publishedFrame(cow).orElseThrow().endpoint().frameSerial());
                cow.move(MoverType.SELF,new Vec3(.125,0,0));
                secondAfter.set(AnatomyMovement.publishedFrame(cow).orElseThrow().endpoint().frameSerial());
                if(firstAfter.get()!=before+1 || secondAfter.get()!=firstAfter.get()+1)
                    throw new AssertionError("S29 fixture did not produce distinct consecutive live root endpoints: "+before+"/"+firstAfter+"/"+secondAfter);
            });
            context.waitFor(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                for(var certificate:inbox().certificates(cow.getUUID(),clientTick()))
                    if(certificate.afterFrameSerial()==secondAfter.get()
                            && AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),.5).isPresent()) {
                        token.set(certificate);return true;
                    }
                return false;
            },120);
            context.runOnClient(client->{
                if(AnatomyMovement.queryFrame((Cow)client.level.getEntity(id.get())).isEmpty())throw new AssertionError("Initial physical authority absent");
            });
            // Structurally valid fragments, honest SHA-256, but no valid catalog JSON.
            // This fails after digest verification, independently of the implementer's digest-failure case.
            byte[] data=new byte[AnatomyCatalogPayload.CHUNK+1];
            String digest=hash(data);var old=token.get();long candidateRevision=old.revision()+1;
            fragments.set(List.of(
                new AnatomyCatalogPayload(old.epoch(),AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),candidateRevision,0,2,data.length,digest,
                    java.util.Arrays.copyOfRange(data,0,AnatomyCatalogPayload.CHUNK)),
                new AnatomyCatalogPayload(old.epoch(),AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),candidateRevision,1,2,data.length,digest,
                    java.util.Arrays.copyOfRange(data,AnatomyCatalogPayload.CHUNK,data.length))));
            world.getServer().runOnServer(server->ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),fragments.get().getFirst()));
            context.waitFor(client->AnatomyClientNetworking.mode(client.level.getEntity(id.get()))==AnatomyMode.BINDING,120);
            context.waitTicks(2);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                if(AnatomyClientNetworking.presentationInterval(cow,old.materialSerial(),.5).isPresent()
                        || AnatomyMovement.queryFrame(cow).isPresent())throw new AssertionError("Pending catalog exposed material or physical authority");
            });
            world.getServer().runOnServer(server->ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),fragments.get().getLast()));
            context.waitFor(client->AnatomyClientNetworking.mode(client.level.getEntity(id.get()))==AnatomyMode.READY,120);
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());var catalog=AnatomyClientNetworking.catalog();
                if(catalog.revision()!=old.revision() || !catalog.epoch().equals(old.epoch()))throw new AssertionError("Rejected JSON replaced accepted catalog identity");
                var interval=AnatomyClientNetworking.presentationInterval(cow,old.materialSerial(),.5).orElseThrow(
                    ()->new AssertionError("Rejected catalog discarded accepted certified material"));
                if(interval.before().frameSerial()!=old.beforeFrameSerial() || interval.after().frameSerial()!=old.afterFrameSerial())
                    throw new AssertionError("Catalog rollback substituted material endpoints");
                if(AnatomyMovement.queryFrame(cow).isEmpty())throw new AssertionError("Rollback retained material but lost physical provider registration");
                System.out.println("S29 adversarial catalog rollback PASS honest digest/invalid JSON: old certified interval and physical authority recovered");
            });
        } finally {
            world.getServer().runOnServer(AnatomyRuntime::stop);
            // Advance the harness phases before its deferred integrated-server disconnect.
            context.waitTicks(2);
            world.close();
        }
    }
    private static String hash(byte[] bytes) {
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    private static AnatomyCertifiedIntervalInbox inbox() {
        try {var field=AnatomyClientNetworking.class.getDeclaredField("certifiedIntervals");field.setAccessible(true);return (AnatomyCertifiedIntervalInbox)field.get(null);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static long clientTick() {
        try {var field=AnatomyClientNetworking.class.getDeclaredField("clientTick");field.setAccessible(true);return field.getLong(null);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
}
