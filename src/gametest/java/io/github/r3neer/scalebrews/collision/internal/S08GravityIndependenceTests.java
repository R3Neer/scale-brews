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

/** FR-032 holdout: a supported body's gravity is independent from its support root gravity. */
public final class S08GravityIndependenceTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void anchoredTransportMustNotRequireSupportGravityToEqualBodyGravity(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        Vec3 base=support.position().add(0,1,0);
        body.setPos(base.add(.30,0,0));
        AABB captured=body.getBoundingBox();
        var wallBefore=ConvexBox.of(new AABB(captured.maxX,captured.minY-.5,captured.minZ-.5,
            captured.maxX+.5,captured.maxY+.5,captured.maxZ+.5),new Matrix4f());
        Vec3 delta=new Vec3(0,0,.20);
        var wallAfter=wallBefore.move(delta);
        var wallMotion=new ConservativeSweep.Motion(t->wallBefore.move(delta.scale(t)),0,delta);
        var bodyGravity=new GravityFrame(Direction.EAST);
        long revision=208;

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(revision,Map.of("wall",wallBefore))));
        AnatomyMovement.gravity(body,bodyGravity);
        try {
            var separation=wallBefore.separation(captured);
            int face=wallBefore.closestFace(separation.normal());
            Vec3 normal=wallBefore.faceNormal(face);
            h.assertTrue(Math.abs(separation.gap())<=1e-8,"Fixture wall must start in retained-contact range: "+separation.gap());
            h.assertTrue(bodyGravity.supports(normal),"Fixture wall must support the body's lateral gravity");
            Vec3 local=wallBefore.facePoint(face,captured.getCenter());
            var initialSurface=new SurfaceContact(support.getUUID(),revision,"wall",face,local,normal,level.getGameTime());
            h.assertTrue(AnatomyMovement.confirm(body,support,initialSurface),"Fixture must establish a valid lateral retained contact");
            var retainedSurface=AnatomyMovement.surface(body);
            h.assertTrue(retainedSurface!=null && AnatomyMovement.contact(body)!=null,
                "Fixture must retain the explicitly certified lateral face before planner execution");

            long tick=level.getGameTime();
            long registration=AnatomyMovement.registrationGeneration(support);
            var identity=new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
                Identifier.parse("test:s08_gravity_independence"),Identifier.parse("test:s08_gravity_independence_pose"),1,registration);
            var beforeRoot=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
            var afterRoot=new AnatomyMovement.RootFrame(2,tick,support.position().add(delta),0,1,GravityFrame.VANILLA);
            h.assertTrue(!bodyGravity.equals(beforeRoot.gravity()),"Fixture must actually exercise independent body/support gravity frames");
            var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,beforeRoot.origin(),0,1,GravityFrame.VANILLA);
            var afterSample=new AnatomyPoseHistory.Sample(INPUTS,afterRoot.origin(),0,1,GravityFrame.VANILLA);
            var before=new GeometryProvider.QueryFrame(identity,
                new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),
                new GeometryProvider.Snapshot(revision,Map.of("wall",wallBefore)));
            var after=new GeometryProvider.QueryFrame(identity,
                new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),
                new GeometryProvider.Snapshot(revision,Map.of("wall",wallAfter)));
            var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
            var motion=new GeometryProvider.MotionSnapshot(revision,tick,tick,beforeRoot.origin(),afterRoot.origin(),Map.of("wall",wallMotion));
            var envelope=wallBefore.bounds().inflate(wallMotion.maxPointSpeed()+ConservativeSweep.SKIN);
            var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
            var event=new MaterialEventDispatcher.Event<net.minecraft.world.entity.Entity>(
                new MaterialEventDispatcher.EventId(1,0),support,MaterialEventDispatcher.Source.ROOT_MUTATION,interval,Set.of(support.getUUID()));

            var result=AnchoredTransportPlanner.plan(level,body,captured,List.of(event),Map.of(handle,motion),(box,requested)->requested);
            h.assertTrue(result.status()==AnchoredTransportPlanner.Status.COMPLETE,
                "FR-032 requires transport to depend on the body's support normal, not equality with support gravity: "+result.status());
            h.assertTrue(result.displacement().distanceToSqr(delta)<1e-10,
                "Certified lateral-gravity carry must follow the retained material anchor exactly: "+result.displacement());
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.gravity(body,GravityFrame.VANILLA);
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }
}
