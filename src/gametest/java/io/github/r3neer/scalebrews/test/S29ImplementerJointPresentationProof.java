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
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;

/** IMPLEMENTER integration regression: live JOINT publication, without a root move. */
public final class S29ImplementerJointPresentationProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger(-1);
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("Joint fixture cow absent");
                cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);cow.yHeadRot=0;cow.yBodyRot=0;
                server.overworld().addFreshEntity(cow);id.set(cow.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_joint_development",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(id.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var cow=(Cow)server.overworld().getEntity(id.get());
                cow.setXRot(35);
            });
            context.waitFor(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                for(var certificate:inbox().certificates(cow.getUUID(),clientTick())) {
                    var frame=AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),.5).orElse(null);
                    if(frame!=null && Math.abs(frame.after().sample().inputs().headPitch()-frame.before().sample().inputs().headPitch())>20) return true;
                }
                return false;
            },120);
            context.runOnClient(client->{
                var cow=(Cow)client.level.getEntity(id.get());
                for(var certificate:inbox().certificates(cow.getUUID(),clientTick())) {
                    var mid=AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),.5).orElse(null);
                    if(mid==null || Math.abs(mid.after().sample().inputs().headPitch()-mid.before().sample().inputs().headPitch())<=20)continue;
                    if(!mid.before().rootTransform().equals(mid.after().rootTransform()))throw new AssertionError("Joint fixture also changed root authority");
                    var a=AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),0).orElseThrow();
                    var b=AnatomyClientNetworking.presentationInterval(cow,certificate.materialSerial(),1).orElseThrow();
                    boolean changed=a.evaluated().pieces().entrySet().stream().anyMatch(entry->entry.getKey().contains("/head/")
                        && !entry.getValue().equals(b.evaluated().pieces().get(entry.getKey())));
                    if(!changed)throw new AssertionError("Certified joint endpoints did not move original head geometry");
                    if(a.before().jointSampleTick()>=b.after().jointSampleTick())throw new AssertionError("Joint authority clock did not advance");
                    System.out.println("S29 JOINT PASS serial="+certificate.materialSerial()+" root unchanged, authority head geometry changed");
                    return;
                }
                throw new AssertionError("Joint interval disappeared before assertion");
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
