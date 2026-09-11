package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * S08/FR-057 holdout: a stationary anatomical support not present in A's event batch
 * is still a physical entity obstacle. Suppressing its vanilla AABB may not make its
 * anatomical pieces disappear from a carried body's continuous path.
 */
public final class S08PreparedStaticAnatomicalObstacleProof {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);
    private S08PreparedStaticAnatomicalObstacleProof() {}

    public static void run(GameTestHelper h) {
        var level=h.getLevel();

        // B is a genuine prepared cow binding and remains stationary throughout the test.
        var obstacle=h.spawn(EntityTypes.COW,18,20,2);
        obstacle.setNoAi(true);obstacle.setNoGravity(true);
        obstacle.getAttribute(Attributes.SCALE).setBaseValue(2);obstacle.refreshDimensions();
        try {
            Platforms.tick(level);
            h.assertTrue(AnatomyRuntime.authoritativeFrame(obstacle).isPresent(),
                "FR-057 anatomical-obstacle proof requires B to be a live prepared binding");
            var obstacleFrame=AnatomyMovement.queryFrame(obstacle).orElseThrow();
            var obstaclePiece=obstacleFrame.snapshot().pieces().values().stream()
                .max(java.util.Comparator.comparingDouble(S08PreparedStaticAnatomicalObstacleProof::volume))
                .orElseThrow();

            // The body is deliberately spawned only after B is prepared so it is not itself a
            // runtime support binding. A normal living fixture avoids the prepared-session player
            // handshake, which is unrelated to this physical holdout.
            var body=h.spawn(EntityTypes.SHEEP,14,20,2);
            body.setNoAi(true);body.setNoGravity(true);
            body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();

            // A is a manual support inside the prepared session. Its synthetic rigid floor makes
            // the body travel horizontally through B while A is the only material event in the batch.
            var support=h.spawn(EntityTypes.COW,12,20,2);
            support.setNoAi(true);support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
            try {
                double halfWidth=body.getBoundingBox().getXsize()*.5;
                double bodyHeight=body.getBoundingBox().getYsize();
                var target=obstaclePiece.bounds();
                double margin=.5;
                Vec3 start=new Vec3(target.minX-halfWidth-margin,target.getCenter().y-bodyHeight*.5,target.getCenter().z);
                body.setPos(start);
                AABB captured=body.getBoundingBox();
                double travel=target.getXsize()+2*(halfWidth+margin);
                Vec3 delta=new Vec3(travel,0,0);

                var floorLocal=ConvexBox.of(new AABB(0,0,0,1,.20,1),new Matrix4f());
                var floorBefore=floorLocal.move(new Vec3(captured.getCenter().x-.5,captured.minY-.20,captured.getCenter().z-.5));
                var floorAfter=floorBefore.move(delta);
                long revision=260;
                AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(revision,Map.of("floor",floorBefore))));

                var separation=floorBefore.separation(captured);
                int face=floorBefore.closestFace(separation.normal());
                Vec3 normal=floorBefore.faceNormal(face);
                h.assertTrue(GravityFrame.VANILLA.supports(normal),"Manual A floor must support vanilla gravity");
                Vec3 local=floorBefore.facePoint(face,captured.getCenter());
                var surface=new SurfaceContact(support.getUUID(),revision,"floor",face,local,normal,level.getGameTime());
                h.assertTrue(AnatomyMovement.confirm(body,support,surface),
                    "Fixture must establish a retained contact on A before testing stationary B");

                h.assertTrue(Platforms.eligible(body,obstacle) && AnatomyMovement.replacesPair(body,obstacle),
                    "B must be a real anatomical replacement pair, otherwise vanilla entity collision would hide the bug");
                h.assertTrue(!obstaclePiece.overlaps(captured) && !obstaclePiece.overlaps(captured.move(delta)),
                    "Body endpoints must both be clear of B's selected anatomical piece");
                double mid=(target.getCenter().x-captured.getCenter().x)/delta.x;
                h.assertTrue(mid>0 && mid<1 && obstaclePiece.overlaps(captured.move(delta.scale(mid))),
                    "B's real anatomical piece must intersect only at an intermediate point of A's carry path");

                var motion=new ConservativeSweep.Motion(t->floorBefore.move(delta.scale(t)),0,delta,
                    List.of(new ConservativeSweep.Plane(net.minecraft.core.Direction.UP,floorBefore.bounds().maxY)));
                var event=event(level,support,floorBefore,floorAfter,motion,revision);
                var handle=event.interval().handle();
                var snapshot=new GeometryProvider.MotionSnapshot(revision,level.getGameTime(),level.getGameTime(),
                    support.position(),support.position().add(delta),Map.of("floor",motion));

                // Control: under the production replacement scope, vanilla entity collisions omit B.
                Entity old=PlatformPhysics.enter(body);
                boolean vanillaSeesB;
                try {vanillaSeesB=!level.getEntityCollisions(body,captured.inflate(delta.length()+1)).isEmpty();}
                finally {PlatformPhysics.exit(old);}
                h.assertTrue(!vanillaSeesB,
                    "Control requires B's vanilla collision to be suppressed because anatomy owns this pair");

                var result=AnchoredTransportPlanner.plan(level,body,captured,List.of(event),Map.of(handle,snapshot),
                    (box,requested)->clip(level,body,box,requested));
                h.assertTrue(result.status()==AnchoredTransportPlanner.Status.RELEASE,
                    "FR-057 requires a stationary anatomical support outside A's batch to obstruct the certified carry path: "+result.status());
            } finally {
                AnatomyMovement.clear(body);
                support.discard();body.discard();
            }
        } finally {obstacle.discard();}
    }

    private static MaterialEventDispatcher.Event<Entity> event(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity support,ConvexBox beforeBox,ConvexBox afterBox,
            ConservativeSweep.Motion motion,long revision) {
        long tick=level.getGameTime();
        long registration=AnatomyMovement.registrationGeneration(support);
        var identity=new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
            Identifier.parse("test:s08_static_anatomical_obstacle"),Identifier.parse("test:s08_static_anatomical_obstacle_pose"),1,registration);
        var root0=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var root1=new AnatomyMovement.RootFrame(2,tick,support.position().add(motion.linearTranslation()),0,1,GravityFrame.VANILLA);
        var sample0=new AnatomyPoseHistory.Sample(INPUTS,root0.origin(),0,1,GravityFrame.VANILLA);
        var sample1=new AnatomyPoseHistory.Sample(INPUTS,root1.origin(),0,1,GravityFrame.VANILLA);
        var before=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1,tick,tick,root0,sample0,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision,Map.of("floor",beforeBox)));
        var after=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2,tick,tick,root1,sample1,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision,Map.of("floor",afterBox)));
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        var envelope=union(beforeBox.bounds(),afterBox.bounds()).inflate(motion.maxPointSpeed()+ConservativeSweep.SKIN);
        return new MaterialEventDispatcher.Event<>(new MaterialEventDispatcher.EventId(1,0),support,
            MaterialEventDispatcher.Source.ROOT_MUTATION,new MaterialEventDispatcher.MaterialInterval(handle,envelope),Set.of(support.getUUID()));
    }

    private static Vec3 clip(net.minecraft.server.level.ServerLevel level,Entity body,AABB box,Vec3 requested) {
        Entity old=PlatformPhysics.enter(body);
        try {return Entity.collideBoundingBox(body,requested,box,level,level.getEntityCollisions(body,box.expandTowards(requested)));}
        finally {PlatformPhysics.exit(old);}
    }

    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }

    private static double volume(ConvexBox box) {
        var b=box.bounds();return b.getXsize()*b.getYsize()*b.getZsize();
    }
}
