package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.client.collision.network.AnatomyClientNetworking;
import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;
import io.github.r3neer.scalebrews.client.collision.presentation.CausalBodyResidual;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.internal.*;
import io.github.r3neer.scalebrews.collision.physics.SupportTransport;
import io.github.r3neer.scalebrews.collision.runtime.RootFrame;
import io.github.r3neer.scalebrews.collision.runtime.TransportLedger;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Isolated production transition: a duplicated exact carry must preserve its draw sample. */
public final class S31AdversarialResidualWorkProof implements FabricClientGameTest {
    private static volatile double sink;
    @Override public void runTest(ClientGameTestContext context) {
        var geometry=context.computeOnClient(c->GeometryExtractor.vanilla("minecraft:cow","26.2",CowModel.createBodyLayer().bakeRoot(),Set.of()));
        var profile=new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.empty(),List.of(),
            Optional.of(new AnatomyDefinition(Identifier.parse("minecraft:cow"),Identifier.parse("scalebrews:quadruped"),AnatomyFilter.DEFAULT)));
        var id=new AtomicInteger();var saved=new AtomicReference<GeometryProvider.PublishedFrame>();var tracking=new AtomicLong();
        var world=context.worldBuilder().create();
        try {
            world.getServer().runOnServer(s->{var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                if(cow==null)throw new AssertionError("No Cow");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(2,3,2);
                s.overworld().addFreshEntity(cow);id.set(cow.getId());s.getPlayerList().getPlayers().getFirst().teleportTo(2,3,-2);
                AnatomyRuntime.startPrepared(s,Map.of("minecraft:cow",geometry),Map.of("s31_adversary:duplicate",profile));});
            context.waitTicks(5);
            world.getServer().runOnServer(s->{var cow=(Cow)s.overworld().getEntity(id.get());var player=s.getPlayerList().getPlayers().getFirst();
                saved.set(AnatomyMovement.publishedFrame(cow).orElseThrow());tracking.set(AnatomyRuntime.trackingGeneration(player,cow));
                AnatomyRuntime.stop(s);AnatomyNetworking.sendPose(player,saved.get(),tracking.get());});
            context.waitFor(c->c.level.getEntity(id.get()) instanceof Cow cow && AnatomyClientNetworking.presentationFrame(cow).isPresent(),120);
            var before=context.computeOnClient(c->AnatomyClientNetworking.presentationFrame((Cow)c.level.getEntity(id.get())).orElseThrow());
            world.getServer().runOnServer(s->{var p=saved.get();var e=p.endpoint();var sample=e.sample();var origin=sample.origin().add(.2,0,0);
                var nextSample=new AnatomyPoseHistory.Sample(sample.inputs(),origin,sample.yaw(),sample.scale(),sample.gravity());
                var root=new RootFrame(e.root().sequence()+1,e.root().tick()+1,origin,sample.yaw(),sample.scale(),sample.gravity());
                AnatomyNetworking.sendPose(s.getPlayerList().getPlayers().getFirst(),new GeometryProvider.PublishedFrame(p.identity(),
                    new GeometryProvider.CausalEndpoint(e.frameSerial()+1,e.authorityTick()+1,e.jointSampleTick(),root,nextSample,GeometryProvider.Availability.AVAILABLE)),tracking.get());});
            context.waitFor(c->AnatomyClientNetworking.presentationFrame((Cow)c.level.getEntity(id.get()))
                .map(f->f.after().frameSerial()>before.after().frameSerial()).orElse(false),120);
            context.runOnClient(c->{
                var body=c.player;var support=(Cow)c.level.getEntity(id.get());var after=AnatomyClientNetworking.presentationFrame(support).orElseThrow();
                String piece="root/body/cube_0";Vec3 point=Vec3.ZERO;
                var beforePoint=before.evaluated().pieces().get(piece).point(point);var afterPoint=after.evaluated().pieces().get(piece).point(point);
                var delta=afterPoint.subtract(beforePoint);
                if(Math.abs(delta.x-.2)>1e-5 || delta.lengthSqr()<.01)throw new AssertionError("Nonzero ROOT fixture missing");
                CausalBodyResidual.reset();body.setPos(body.position().add(delta));
                try {
                    var keyClass=Class.forName(CausalBodyResidual.class.getName()+"$Key");var keyCtor=keyClass.getDeclaredConstructors()[0];keyCtor.setAccessible(true);
                    var key=keyCtor.newInstance(body.getUUID(),body.getId(),support.getUUID(),support.getId(),before.identity(),before.trackingGeneration(),
                        AnatomyMovement.gravity(body),AnatomyMovement.gravity(support),piece,0);
                    var relationClass=Class.forName(CausalBodyResidual.class.getName()+"$Relation");var ctor=relationClass.getDeclaredConstructors()[0];ctor.setAccessible(true);
                    var relation=ctor.newInstance(body,support,key,before,point);
                    field(relation,"pendingBefore",before);field(relation,"pendingAfter",after);field(relation,"current",after);
                    var owner=CausalBodyResidual.class.getDeclaredField("relation");owner.setAccessible(true);owner.set(null,relation);
                } catch(ReflectiveOperationException failure){throw new AssertionError("Test-only relation setup failed",failure);}
                long sequence=1;TransportLedger.record(body,new SupportTransport(body.level().getGameTime(),sequence,after.after().root().sequence(),delta,delta));
                var cursor=new AnatomyMovement.ClientTransportCursor(support.getUUID(),piece,0,point,before.after().frameSerial(),after.after().frameSerial(),
                    sequence,beforePoint,afterPoint,body.tickCount,body.oldPosition());
                CausalBodyResidual.carried(body,cursor);
                var positive=CausalBodyResidual.rendererOffset(body,.5f);
                if(positive.distanceToSqr(delta.scale(.5))>1e-10)throw new AssertionError("Positive control failed: "+positive);
                var physical=body.position();var bounds=body.getBoundingBox();var velocity=body.getDeltaMovement();
                CausalBodyResidual.carried(body,cursor);
                var duplicate=CausalBodyResidual.rendererOffset(body,.5f);
                System.out.println("S31 ADVERSARY exact duplicate carry before="+positive+" after="+duplicate);
                if(!body.position().equals(physical) || !body.getBoundingBox().equals(bounds) || !body.getDeltaMovement().equals(velocity))throw new AssertionError("Consumer wrote physical state");
                if(duplicate.distanceToSqr(positive)>1e-10)throw new AssertionError("Exact duplicate carry erased the nonzero draw sample");
                try {
                    var owner=CausalBodyResidual.class.getDeclaredField("relation");owner.setAccessible(true);var relation=owner.get(null);
                    var drawField=relation.getClass().getDeclaredField("draw");drawField.setAccessible(true);var drawSample=drawField.get(relation);
                    for(int i=0;i<32768;i++)sink+=CausalBodyResidual.rendererOffset(body,(i&255)/255f).x;
                    var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
                    if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
                    long tid=Thread.currentThread().threadId();long[] ns=new long[32],bytes=new long[32];
                    for(int batch=0;batch<32;batch++) {
                        long allocated=bean.getThreadAllocatedBytes(tid),start=System.nanoTime();double sum=0;
                        for(int i=0;i<4096;i++)sum+=CausalBodyResidual.rendererOffset(body,(i&255)/255f).x;
                        ns[batch]=System.nanoTime()-start;bytes[batch]=bean.getThreadAllocatedBytes(tid)-allocated;sink=sum;
                    }
                    Arrays.sort(ns);Arrays.sort(bytes);
                    if(owner.get(null)!=relation || drawField.get(relation)!=drawSample)throw new AssertionError("Render work mutated relation or immutable draw sample");
                    if(!body.position().equals(physical) || !body.getBoundingBox().equals(bounds) || !body.getDeltaMovement().equals(velocity))throw new AssertionError("Render work wrote physical state");
                    System.out.println("S31 ADVERSARY HOT_PRESENTATION queries=131072 p95_ns_per_query="+(ns[30]/4096.0)+" p99_ns_per_query="+(ns[31]/4096.0)+" p95_bytes_per_query="+(bytes[30]/4096.0)+" sink="+sink);
                } catch(ReflectiveOperationException failure){throw new AssertionError("Cost snapshot setup failed",failure);}
                CausalBodyResidual.reset();TransportLedger.invalidate(body,true);
            });
        } finally {context.runOnClient(c->CausalBodyResidual.reset());world.getServer().runOnServer(AnatomyRuntime::stop);context.waitTicks(2);world.close();}
    }
    private static void field(Object owner,String name,Object value)throws ReflectiveOperationException {
        var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);
    }
}
