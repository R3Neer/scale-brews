package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** FR-056/057: a changing joint endpoint can carry an anchored body when its face has a continuous normal certificate. */
public final class S08JointAnchorTransportTests {
    @GameTest
    public void jointChangingIntervalUsesMaterialAnchorInsteadOfEndpointChordGate(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        Vec3 origin=support.position().add(0,1,0);
        var localBox=new AABB(-.5,-.10,-.5,.5,0,.5);
        double angle=Math.toRadians(20);
        ConvexBox before=ConvexBox.of(localBox,new Matrix4f()).move(origin);
        java.util.function.DoubleFunction<ConvexBox> trajectory=t->
            ConvexBox.of(localBox,new Matrix4f().rotateY((float)(angle*t))).move(origin);
        ConvexBox after=trajectory.apply(1);
        int face=3;
        Vec3 target=before.point(new Vec3(.9,1,.5));
        body.setPos(target.x,origin.y,target.z);
        AABB captured=body.getBoundingBox();
        double radius=Math.sqrt(.5*.5+.5*.5);
        var motion=new ConservativeSweep.Motion(trajectory,angle*radius*1.01,Vec3.ZERO,
            List.of(new ConservativeSweep.Plane(Direction.UP,before.bounds().maxY)));
        long revision=309,tick=level.getGameTime();

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(revision,Map.of("floor",before))));
        try {
            var separation=before.separation(captured);
            Vec3 normal=before.faceNormal(face);
            h.assertTrue(Math.abs(separation.gap())<=.025,"Fixture body must begin in retained floor range: "+separation.gap());
            h.assertTrue(GravityFrame.VANILLA.supports(normal),"Fixture top face must support vanilla gravity");
            Vec3 local=before.facePoint(face,captured.getCenter());
            var initial=new SurfaceContact(support.getUUID(),revision,"floor",face,local,normal,tick);
            h.assertTrue(AnatomyMovement.confirm(body,support,initial),"Fixture must establish retained contact");
            var surface=AnatomyMovement.surface(body);

            long registration=AnatomyMovement.registrationGeneration(support);
            var identity=new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
                Identifier.parse("test:s08_joint_anchor"),Identifier.parse("test:s08_joint_anchor_pose"),1,registration);
            var root=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
            var input0=new PoseProvider.Inputs(0,0,0,0,0,true,Map.of("joint",0f));
            var input1=new PoseProvider.Inputs(0,0,0,0,0,true,Map.of("joint",1f));
            var sample0=new AnatomyPoseHistory.Sample(input0,root.origin(),root.yaw(),root.scale(),root.gravity());
            var sample1=new AnatomyPoseHistory.Sample(input1,root.origin(),root.yaw(),root.scale(),root.gravity());
            var q0=new GeometryProvider.QueryFrame(identity,
                new GeometryProvider.CausalEndpoint(1,tick,tick,root,sample0,GeometryProvider.Availability.AVAILABLE),
                new GeometryProvider.Snapshot(revision,Map.of("floor",before)));
            var q1=new GeometryProvider.QueryFrame(identity,
                new GeometryProvider.CausalEndpoint(2,tick,tick,root,sample1,GeometryProvider.Availability.AVAILABLE),
                new GeometryProvider.Snapshot(revision,Map.of("floor",after)));
            var handle=new GeometryProvider.MotionIntervalHandle(identity,1,q0,q1);
            var snapshot=new GeometryProvider.MotionSnapshot(revision,tick,tick,root.origin(),root.origin(),Map.of("floor",motion));
            var interval=new MaterialEventDispatcher.MaterialInterval(handle,before.bounds().inflate(motion.maxPointSpeed()+ConservativeSweep.SKIN));
            var event=new MaterialEventDispatcher.Event<net.minecraft.world.entity.Entity>(
                new MaterialEventDispatcher.EventId(1,0),support,MaterialEventDispatcher.Source.JOINT_BATCH,interval,Set.of(support.getUUID()));

            var result=AnchoredTransportPlanner.plan(level,body,captured,List.of(event),Map.of(handle,snapshot),(box,requested)->requested);
            Vec3 expected=after.point(surface.localPoint()).subtract(before.point(surface.localPoint()));
            h.assertTrue(result.status()==AnchoredTransportPlanner.Status.COMPLETE,
                "A changing joint with a certified invariant support plane must permit anchored carry: "+result.status());
            h.assertTrue(result.displacement().distanceToSqr(expected)<1e-9,
                "Joint carry must follow the material-local anchor path endpoint: expected="+expected+" actual="+result.displacement());
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);support.discard();body.discard();
        }
        h.succeed();
    }
}
