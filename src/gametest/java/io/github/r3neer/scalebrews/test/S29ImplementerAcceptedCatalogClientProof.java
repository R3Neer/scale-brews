package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.api.AnatomyMode;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** IMPLEMENTER proof of the real accepted catalog transaction at the client receiver. */
public final class S29ImplementerAcceptedCatalogClientProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var supportId=new AtomicInteger(-1);
        var priorCertificate=new AtomicReference<AnatomyMaterialIntervalPayload>();
        var freshCertificate=new AtomicReference<AnatomyMaterialIntervalPayload>();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("Accepted catalog fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                server.overworld().addFreshEntity(cow);supportId.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_client_accepted",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(supportId.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),160);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(supportId.get());
                cow.move(MoverType.SELF,new Vec3(.125,0,0));
            });
            context.waitFor(client->{
                var cow=(Cow)client.level.getEntity(supportId.get());
                for(var token:inbox().certificates(cow.getUUID(),clientTick()))
                    if(AnatomyClientNetworking.presentationInterval(cow,token.materialSerial(),.5).isPresent()) {
                        priorCertificate.set(token);return true;
                    }
                return false;
            },160);
            context.waitFor(client->client.level.getEntity(supportId.get()) instanceof Cow cow
                && AnatomyMovement.queryFrame(cow).isPresent(),120);
            var prior=priorCertificate.get();
            world.getServer().runOnServer(server->S29ImplementerCatalogIntervalTests.reloadPrepared(server,
                Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_client_accepted",profile)));
            context.waitFor(client->{
                var cow=(Cow)client.level.getEntity(supportId.get());
                return AnatomyClientNetworking.mode(cow)==AnatomyMode.READY
                    && AnatomyClientNetworking.catalog().revision()==prior.revision()+1
                    && AnatomyClientNetworking.presentationFrame(cow)
                        .map(frame->frame.identity().revision()==prior.revision()+1).orElse(false)
                    && AnatomyMovement.queryFrame(cow)
                        .map(frame->frame.identity().revision()==prior.revision()+1).orElse(false);
            },160);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(supportId.get());
                var liveCertificates=inbox().certificates(cow.getUUID(),clientTick());
                if(liveCertificates.stream().anyMatch(token->token.revision()==prior.revision()))
                    throw new AssertionError("Accepted catalog retained a prior-revision certificate: accepted="
                        +AnatomyClientNetworking.catalog().revision()+" prior="+prior.revision()+" tokens="
                        +liveCertificates.stream().map(token->token.revision()+"/"+token.materialSerial()+"/"+token.trackingGeneration()).toList());
                if(AnatomyClientNetworking.presentationInterval(cow,prior.materialSerial(),.5)
                        .map(frame->frame.identity().revision()==prior.revision()).orElse(false))
                    throw new AssertionError("Accepted catalog revived the prior certified interval");
            });
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(supportId.get());
                cow.move(MoverType.SELF,new Vec3(.125,0,0));
            });
            context.waitFor(client->{
                var cow=(Cow)client.level.getEntity(supportId.get());
                for(var token:inbox().certificates(cow.getUUID(),clientTick()))
                    if(token.revision()==prior.revision()+1 && token.bindingGeneration()>prior.bindingGeneration()
                            && AnatomyClientNetworking.presentationInterval(cow,token.materialSerial(),.5).isPresent()) {
                        freshCertificate.set(token);return true;
                    }
                return false;
            },160);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(supportId.get());var token=freshCertificate.get();
                var interval=AnatomyClientNetworking.presentationInterval(cow,token.materialSerial(),.5).orElseThrow();
                if(interval.identity().revision()!=prior.revision()+1
                        || interval.identity().bindingGeneration()!=token.bindingGeneration()
                        || interval.before().frameSerial()!=token.beforeFrameSerial()
                        || interval.after().frameSerial()!=token.afterFrameSerial())
                    throw new AssertionError("Fresh certified interval did not use the committed binding/endpoints");
                System.out.println("S29 ACCEPTED CATALOG PASS old interval retired, fresh binding and ROOT interval authoritative");
            });
        } finally {
            world.getServer().runOnServer(AnatomyRuntime::stop);
            context.waitTicks(2);
            world.close();
        }
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
