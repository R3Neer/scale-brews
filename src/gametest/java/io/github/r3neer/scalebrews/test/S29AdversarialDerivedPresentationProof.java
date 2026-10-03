package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Q2 DERIVED_CARRY is a material presentation cause just as ROOT and JOINT are. */
public final class S29AdversarialDerivedPresentationProof implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(client->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),java.util.Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var baseId=new AtomicInteger(-1);var childId=new AtomicInteger(-1);var afterSerial=new AtomicLong();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(server->{
                var base=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(base==null)throw new AssertionError("S29 base absent");
                base.setNoAi(true);base.setNoGravity(true);base.getAttribute(Attributes.SCALE).setBaseValue(4);base.refreshDimensions();
                base.setPos(2,3,2);server.overworld().addFreshEntity(base);baseId.set(base.getId());
                AnatomyRuntime.startPrepared(server,Map.of("minecraft:cow",geometry),Map.of("scalebrews:s29_derived",profile));
            });
            context.waitFor(client->client.level!=null && client.level.getEntity(baseId.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var base=(Cow)server.overworld().getEntity(baseId.get());
                var top=AnatomyMovement.queryFrame(base).orElseThrow().snapshot().pieces().values().stream()
                    .map(ConvexBox::bounds).max(Comparator.comparingDouble(box->box.maxY)).orElseThrow();
                var child=net.minecraft.world.entity.EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                if(child==null)throw new AssertionError("S29 child absent");
                child.setNoAi(true);child.setNoGravity(true);child.getAttribute(Attributes.SCALE).setBaseValue(.35);child.refreshDimensions();
                child.setPos(top.getCenter().x,top.maxY+.3,top.getCenter().z);server.overworld().addFreshEntity(child);childId.set(child.getId());
                child.move(MoverType.SELF,new Vec3(0,-.6,0));
                if(AnatomyMovement.contact(child)==null || AnatomyMovement.contact(child).support()!=base)
                    throw new AssertionError("S29 derived fixture could not establish child/base material contact");
            });
            context.waitFor(client->client.level.getEntity(childId.get()) instanceof Cow cow
                && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            world.getServer().runOnServer(server->{
                var base=(Cow)server.overworld().getEntity(baseId.get());var child=(Cow)server.overworld().getEntity(childId.get());
                if(AnatomyRuntime.authoritativeFrame(child).isEmpty())throw new AssertionError("Child is not a live anatomical support");
                var before=child.position();long beforeSerial=AnatomyMovement.publishedFrame(child).orElseThrow().endpoint().frameSerial();
                base.move(MoverType.SELF,new Vec3(.2,0,0));
                if(child.position().subtract(before).distanceToSqr(new Vec3(.2,0,0))>1e-10)
                    throw new AssertionError("S29 derived fixture lacks genuine parent carry");
                afterSerial.set(AnatomyMovement.publishedFrame(child).orElseThrow().endpoint().frameSerial());
                if(afterSerial.get()!=beforeSerial+1)throw new AssertionError("Child material endpoint did not advance exactly once");
            });
            context.waitTicks(3);
            context.runOnClient(client->{
                var child=(Cow)client.level.getEntity(childId.get());
                var certificates=inbox().certificates(child.getUUID(),clientTick());
                var found=certificates.stream().filter(p->p.afterFrameSerial()==afterSerial.get()).findFirst();
                System.out.println("S29 DERIVED child afterFrame="+afterSerial+" certified="+found.isPresent());
                if(found.isEmpty())throw new AssertionError("FR-086/S29: server-confirmed DERIVED_CARRY material interval has no client presentation certificate");
                if(AnatomyClientNetworking.presentationInterval(child,found.get().materialSerial(),.5).isEmpty())
                    throw new AssertionError("Derived material certificate is not materializable");
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
