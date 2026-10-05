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

/** Native camera consumer: an unexpected interpolation baseline must reset retained smoothing. */
public final class S31AdversarialNearPlaneClipProof implements FabricClientGameTest {
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
                io.github.r3neer.scalebrews.client.collision.presentation.BodyCameraOffset.reset();
                var camera=c.gameRenderer.mainCamera();
                if(camera.entity()!=body)throw new AssertionError("Camera fixture not observing the local body");
                var cameraBefore=io.github.r3neer.scalebrews.client.collision.presentation.BodyCameraOffset.offset(camera,.5f);
                if(cameraBefore.lengthSqr()<1e-8)throw new AssertionError("Vacuous camera: nonzero draw correction did not reach actual camera");
                // Deterministic collider 1cm ahead of the actual camera; 2cm safety margin forbids this ray.
                var cameraPosition=camera.position();
                var wall=new net.minecraft.core.BlockPos(2,10,2);
                var priorBlock=c.level.getBlockState(wall);
                try {
                    var plane=camera.getNearPlane(c.options.fov().get());
                    var corners=List.of(plane.getTopLeft(),plane.getTopRight(),plane.getBottomLeft(),plane.getBottomRight());
                    double leading=corners.stream().mapToDouble(v->v.x).max().orElseThrow();
                    if(leading<=1e-4)throw new AssertionError("Near-plane orientation fixture missing leading corner");
                    var from=new Vec3(2-cameraBefore.x-leading*.5,10.5,2.5);
                    setCameraPosition(camera,from);
                    c.level.setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                    var center=c.level.clip(new net.minecraft.world.level.ClipContext(from,from.add(cameraBefore),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,body));
                    if(center.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)throw new AssertionError("Center ray fixture was blocked; near-plane test vacuous");
                    boolean cornerBlocked=corners.stream().anyMatch(v->c.level.clip(new net.minecraft.world.level.ClipContext(from.add(v),from.add(v).add(cameraBefore),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,body)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS);
                    if(!cornerBlocked)throw new AssertionError("Near-plane obstacle fixture missing");
                    io.github.r3neer.scalebrews.client.collision.presentation.BodyCameraOffset.reset();
                    var clipped=io.github.r3neer.scalebrews.client.collision.presentation.BodyCameraOffset.offset(camera,.5f);
                    System.out.println("S31 ADVERSARY deterministic camera clip open="+cameraBefore+" wall="+clipped);
                    double allowed=Math.max(0,2-from.x-leading-.02);
                    if(clipped.x>allowed+1e-8 || clipped.x>=cameraBefore.x-1e-8)throw new AssertionError("Camera center missed but near-plane residual crossed collider safety margin");
                    if(!io.github.r3neer.scalebrews.client.collision.presentation.BodyCameraOffset.appliedOffset().equals(clipped))throw new AssertionError("appliedOffset omitted final clipping");
                    if(!body.position().equals(physical) || !body.getBoundingBox().equals(bounds) || !body.getDeltaMovement().equals(velocity))throw new AssertionError("Camera clipping wrote physical state");
                } finally {c.level.setBlock(wall,priorBlock,3);setCameraPosition(camera,cameraPosition);}
                CausalBodyResidual.reset();TransportLedger.invalidate(body,true);
            });
        } finally {context.runOnClient(c->CausalBodyResidual.reset());world.getServer().runOnServer(AnatomyRuntime::stop);context.waitTicks(2);world.close();}
    }
    private static void setCameraPosition(net.minecraft.client.Camera camera,Vec3 position) {
        try {
            for(var method:net.minecraft.client.Camera.class.getDeclaredMethods()) {
                if(!method.getName().equals("setPosition"))continue;
                if(Arrays.equals(method.getParameterTypes(),new Class<?>[]{Vec3.class})) {method.setAccessible(true);method.invoke(camera,position);return;}
                if(Arrays.equals(method.getParameterTypes(),new Class<?>[]{double.class,double.class,double.class})) {method.setAccessible(true);method.invoke(camera,position.x,position.y,position.z);return;}
            }
            throw new AssertionError("Pinned camera position fixture API unavailable");
        } catch(ReflectiveOperationException failure){throw new AssertionError("Camera position fixture failed",failure);}
    }
    private static void field(Object owner,String name,Object value)throws ReflectiveOperationException {
        var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);
    }
}
