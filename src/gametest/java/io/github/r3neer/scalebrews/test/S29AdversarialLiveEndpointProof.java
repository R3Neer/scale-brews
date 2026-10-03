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
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Independent S29 proof: real Q2 root events must publish their materializable endpoints. */
public final class S29AdversarialLiveEndpointProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);var firstAfter=new AtomicLong();var secondAfter=new AtomicLong();
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
            context.waitTicks(3);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                var certificates=inbox().certificates(cow.getUUID(),clientTick());
                int checked=0;
                for(var certificate:certificates)if(certificate.afterFrameSerial()==firstAfter.get() || certificate.afterFrameSerial()==secondAfter.get()) {
                    checked++;
                    var interval=AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),.5);
                    System.out.println("S29 LIVE CERT serial="+certificate.materialSerial()+" frames="+certificate.beforeFrameSerial()+"->"+certificate.afterFrameSerial()+" materializable="+interval.isPresent());
                    if(interval.isEmpty())throw new AssertionError("FR-086/S29: real server Q2 certificate names an endpoint absent from the client pose stream: "+certificate);
                }
                if(checked!=2)throw new AssertionError("S29 fixture expected two live root certificates, found "+checked+": "+certificates);
            });
        } finally {world.getServer().runOnServer(AnatomyRuntime::stop);world.close();}
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
