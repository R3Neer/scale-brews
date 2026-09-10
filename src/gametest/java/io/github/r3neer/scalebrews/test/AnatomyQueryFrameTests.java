package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Q1: current causal convex frames are independent of optional historical motion. */
public class AnatomyQueryFrameTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);
    private static GeometryProvider.GeometryIdentityDescriptor descriptor(UUID epoch,long revision) {
        return new GeometryProvider.GeometryIdentityDescriptor(epoch,revision,Identifier.parse("test:query_frame"),Identifier.parse("test:static"),1);
    }
    /** A server fixture endpoint: one captured root/TRS and its exact convex snapshot. */
    private static GeometryProvider endpointProvider(long revision,Function<net.minecraft.world.entity.LivingEntity,ConvexBox> current,
            Function<net.minecraft.world.entity.LivingEntity,PoseProvider.Inputs> causalInputs,
            Function<net.minecraft.world.entity.LivingEntity,GeometryProvider.Availability> availability,Optional<GeometryProvider.MotionSnapshot> historical) {
        return new GeometryProvider() {
            private AnatomyMovement.RootFrame previous;
            private PoseProvider.Inputs previousInputs;
            private GeometryProvider.Availability previousAvailability;
            private long serial;
            private GeometryProvider.Snapshot snapshot(net.minecraft.world.entity.LivingEntity entity) {
                return new GeometryProvider.Snapshot(revision,Map.of("floor",current.apply(entity)));
            }
            @Override public Optional<GeometryProvider.Snapshot> sample(net.minecraft.world.entity.LivingEntity entity) {return Optional.of(snapshot(entity));}
            @Override public Optional<GeometryProvider.MotionSnapshot> motion(net.minecraft.world.entity.LivingEntity entity) {return historical;}
            @Override public Optional<GeometryProvider.CausalEndpoint> causalEndpoint(net.minecraft.world.entity.LivingEntity entity) {
                var observed=new AnatomyMovement.RootFrame(previous==null?0:previous.sequence()+1,entity.level().getGameTime(),entity.position(),entity.yBodyRot,entity.getScale(),GravityFrame.VANILLA);
                boolean sameRoot=previous!=null && previous.origin().equals(observed.origin()) && previous.yaw()==observed.yaw() && previous.scale()==observed.scale() && previous.gravity().equals(observed.gravity());
                if(!sameRoot)previous=observed;
                var inputs=causalInputs.apply(entity);
                var available=availability.apply(entity);
                if(previousInputs==null || !previousInputs.equals(inputs) || previousAvailability!=available || !sameRoot)serial++;
                previousInputs=inputs;previousAvailability=available;
                var root=previous;var sample=new AnatomyPoseHistory.Sample(inputs,root.origin(),root.yaw(),root.scale(),root.gravity());
                return Optional.of(new GeometryProvider.CausalEndpoint(serial,entity.level().getGameTime(),entity.level().getGameTime(),root,sample,available));
            }
        };
    }
    @GameTest public void currentSnapshotCollidesWithoutHistoricalMotionAndRebinds(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var first=ConvexBox.of(new AABB(-1,-.1,-1,1,.1,1),new Matrix4f()).move(support.position());
        var second=first.move(new Vec3(4,0,0));
        var provider=endpointProvider(7,e->first,e->INPUTS,e->GeometryProvider.Availability.AVAILABLE,Optional.empty());
        var epoch=UUID.randomUUID();
        AnatomyMovement.activate(h.getLevel());
        AnatomyMovement.register(support,provider,descriptor(epoch,7));
        try {
            h.assertTrue(provider.motion(support).isEmpty(),"Fixture explicitly has no certified motion interval");
            var initialFrame=AnatomyMovement.queryFrame(support).orElseThrow();
            var repeated=AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(initialFrame.endpoint()==repeated.endpoint() && initialFrame.authorityTick()==repeated.authorityTick(),
                "Repeated reads of an unchanged material serial retain the complete original endpoint");
            body.setPos(support.getX(),first.bounds().maxY+.5,support.getZ());
            Vec3 landing=AnatomyMovement.collide(body,new Vec3(0,-1,0));body.setPos(body.position().add(landing));AnatomyMovement.afterMove(body);
            h.assertTrue(AnatomyMovement.supported(body),"Current endpoint supplies first descent collision without a motion interval");
            var retained=AnatomyMovement.contact(body);long retainedSequence=retained.sequence();
            Vec3 tangent=AnatomyMovement.collide(body,new Vec3(.1,0,0));body.setPos(body.position().add(tangent));
            h.assertTrue(AnatomyMovement.contact(body)!=null && AnatomyMovement.contact(body).sequence()==retainedSequence
                    && AnatomyMovement.contact(body).support()==support && AnatomyMovement.supported(body),
                "A tangent move with no repeated hit preserves its final-valid existing material support");
            AnatomyMovement.register(support,endpointProvider(7,e->second,e->INPUTS,e->GeometryProvider.Availability.AVAILABLE,Optional.empty()),descriptor(epoch,7));
            var rebound=AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(rebound.identity().bindingGeneration()==initialFrame.identity().bindingGeneration()
                    && rebound.identity().localRegistrationGeneration()>initialFrame.identity().localRegistrationGeneration(),
                "Same authoritative binding retains its wire identity while a provider rebind invalidates local frames");
            h.assertTrue(AnatomyMovement.spaceClear(body,first.bounds().inflate(.01))
                    && !AnatomyMovement.spaceClear(body,second.bounds().inflate(.01)),
                "Same-revision rebind invalidates the prior identity generation and spatial frame");
            var changedGravity=new AnatomyMovement.RootFrame(rebound.root().sequence()+1,rebound.root().tick(),rebound.root().origin(),rebound.root().yaw(),rebound.root().scale(),new GravityFrame(net.minecraft.core.Direction.EAST));
            var changedSample=new AnatomyPoseHistory.Sample(rebound.sample().inputs(),changedGravity.origin(),changedGravity.yaw(),changedGravity.scale(),changedGravity.gravity());
            var changedFrame=new GeometryProvider.QueryFrame(rebound.identity(),new GeometryProvider.CausalEndpoint(rebound.endpoint().frameSerial()+1,rebound.authorityTick(),rebound.endpoint().jointSampleTick(),changedGravity,changedSample,GeometryProvider.Availability.AVAILABLE),rebound.snapshot());
            boolean gravityRejected=false;try {new GeometryProvider.MotionIntervalHandle(rebound.identity(),1,rebound,changedFrame);} catch(IllegalArgumentException expected) {gravityRejected=true;}
            h.assertTrue(gravityRejected,"A gravity lifecycle change cannot be encoded as a continuous material interval");
            AnatomyMovement.invalidateRoot(support);
            h.assertTrue(AnatomyMovement.queryFrame(support).isEmpty(),"Root invalidation quarantines the prior full endpoint until a new serial arrives");
            support.setPos(support.position().add(.01,0,0));
            var afterInvalidation=AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(afterInvalidation.endpoint().frameSerial()>rebound.endpoint().frameSerial()
                    && afterInvalidation.identity().bindingGeneration()==rebound.identity().bindingGeneration(),
                "Root invalidation advances, rather than resets, the endpoint serial inside one binding");
        } finally {AnatomyMovement.deactivate(h.getLevel());support.discard();body.discard();}
        h.succeed();
    }
    @GameTest public void currentTrsFrameOverridesDeliberatelyStaleMotionAndBoundsOverflow(GameTestHelper h) {
        var epoch=UUID.randomUUID();
        h.runAfterDelay(1,()->{
            net.minecraft.world.entity.animal.cow.Cow support=null;
            net.minecraft.world.entity.player.Player body=null;
            try {
                support=h.spawn(EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
                body=h.makeMockPlayer(GameType.SURVIVAL);
                body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
                var old=ConvexBox.of(new AABB(-.5,-.1,-2,.5,.1,2),new Matrix4f()).move(support.position());
                long now=h.getLevel().getGameTime();
                var stale=new GeometryProvider.MotionSnapshot(9,now-1,now,support.position(),support.position(),Map.of("floor",new io.github.r3neer.scalebrews.collision.physics.ConservativeSweep.Motion(t->old,0)));
                var availability=new GeometryProvider.Availability[]{GeometryProvider.Availability.AVAILABLE};
                var provider=endpointProvider(9,e->ConvexBox.of(new AABB(-.5,-.1,-2,.5,.1,2),new Matrix4f()
                    .rotateY((float)Math.toRadians(e.yBodyRot)).scale(e.getScale())).move(e.position()),e->INPUTS,e->availability[0],Optional.of(stale));
                AnatomyMovement.activate(h.getLevel());
                AnatomyMovement.register(support,provider,descriptor(epoch,9));
                support.setPos(support.position().add(4,0,0));support.yBodyRot=90;
                support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(1.5);support.refreshDimensions();
                var fresh=provider.sample(support).orElseThrow().pieces().get("floor");
                h.assertTrue(provider.motion(support).isPresent() && !provider.motion(support).orElseThrow().pieces().get("floor").at().apply(1).bounds().intersects(fresh.bounds()),
                    "Fixture supplies a certified-but-stale historical shape separated from the new TRS endpoint");
                h.assertTrue(!AnatomyMovement.spaceClear(body,fresh.bounds().inflate(.001)),"Yaw, scale, and translation rebuild the instantaneous broadphase from the current frame");
                body.setPos(fresh.bounds().getCenter().x,fresh.bounds().maxY+.5,fresh.bounds().getCenter().z);
                var landing=AnatomyMovement.collide(body,new Vec3(0,-1,0));body.setPos(body.position().add(landing));AnatomyMovement.afterMove(body);
                h.assertTrue(AnatomyMovement.supported(body),"Own-body collision ignores stale motion and lands on the new current convex endpoint");
                var available=AnatomyMovement.queryFrame(support).orElseThrow();
                var repeated=AnatomyMovement.queryFrame(support).orElseThrow();
                h.assertTrue(available.endpoint()==repeated.endpoint(),"A repeated current frame retains its immutable endpoint object");
                availability[0]=GeometryProvider.Availability.UNAVAILABLE;
                var unavailable=AnatomyMovement.publishedFrame(support).orElseThrow();
                h.assertTrue(unavailable.endpoint().availability()==GeometryProvider.Availability.UNAVAILABLE && unavailable.endpoint().frameSerial()>available.endpoint().frameSerial()
                    && AnatomyMovement.queryFrame(support).isEmpty() && AnatomyMovement.spaceClear(body,fresh.bounds()),
                    "Unavailable endpoint advances the same serial and hides a deliberately stale raw sample from every geometry query");
                var beforeCarry=body.position();support.setPos(support.position().add(.1,0,0));AnatomyMovement.carry(body);
                h.assertTrue(AnatomyMovement.contact(body)==null && AnatomyMovement.surface(body)==null
                    && !AnatomyMovement.suppressesPush(body,support) && !AnatomyMovement.supported(body) && body.position().equals(beforeCarry)
                    && !AnatomyMovement.confirm(body,support,new io.github.r3neer.scalebrews.collision.api.SurfaceContact(support.getUUID(),9,"floor",0,Vec3.ZERO,new Vec3(0,1,0),now)),
                    "Accepting one unavailable endpoint clears dependent contact, surface, push suppression, carry, and confirm without a manual invalidation");
                availability[0]=GeometryProvider.Availability.AVAILABLE;
                var restored=AnatomyMovement.queryFrame(support).orElseThrow();
                h.assertTrue(restored.endpoint().frameSerial()>unavailable.endpoint().frameSerial() && restored.snapshot().pieces().containsKey("floor"),
                    "Valid to unavailable to valid publishes discrete frames without tweening the unavailable gap");
                var joint=new float[]{0};
                var jointProvider=endpointProvider(9,e->ConvexBox.of(new AABB(-.5,-.1,-.5,.5,.1,.5),new Matrix4f()).move(e.position().add(joint[0]*3,0,0)),
                    e->new PoseProvider.Inputs(0,0,0,0,0,true,Map.of("joint",joint[0])),e->GeometryProvider.Availability.AVAILABLE,Optional.empty());
                AnatomyMovement.register(support,jointProvider,descriptor(epoch,9));
                var jointsBefore=AnatomyMovement.queryFrame(support).orElseThrow();joint[0]=1;
                var jointsAfter=AnatomyMovement.queryFrame(support).orElseThrow();
                h.assertTrue(jointsAfter.root().equals(jointsBefore.root()) && jointsAfter.endpoint().frameSerial()>jointsBefore.endpoint().frameSerial()
                    && !AnatomyMovement.spaceClear(body,jointsAfter.snapshot().pieces().get("floor").bounds()),
                    "A new joint sample with a fixed root advances full frameSerial and invalidates the spatial frame");
                var extreme=ConvexBox.of(new AABB(-1e20,-1,-1e20,1e20,1,1e20),new Matrix4f());
                AnatomyMovement.register(support,endpointProvider(9,e->extreme,e->INPUTS,e->GeometryProvider.Availability.AVAILABLE,Optional.empty()),descriptor(epoch,9));
                h.assertTrue(!AnatomyMovement.spaceClear(body,new AABB(-1e20,-.5,-1e20,1e20,.5,1e20)),
                    "Finite extreme bounds route through overflow without an overflowing cell walk or AABB collider fallback");
            } finally {AnatomyMovement.deactivate(h.getLevel());if(support!=null)support.discard();if(body!=null)body.discard();}
            h.succeed();
        });
    }
}
