package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** FR-057 holdouts: endpoint chords cannot replace certified-path collision against blocks or entities. */
public final class S08CurvedPathObstructionTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void initiallyTangentBlockMustStillBlockLaterAnchorArc(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();

        var blockRelative=new BlockPos(6,40,4);
        h.setBlock(blockRelative,Blocks.STONE);
        Vec3 blockMin=h.absoluteVec(new Vec3(blockRelative.getX(),blockRelative.getY(),blockRelative.getZ()));
        var blockBox=new AABB(blockMin.x,blockMin.y,blockMin.z,blockMin.x+1,blockMin.y+1,blockMin.z+1);
        double halfWidth=body.getBoundingBox().getXsize()*.5;
        double initialGap=ConservativeSweep.SKIN*.5;
        Vec3 start=h.absoluteVec(new Vec3(6.5,40,4-halfWidth-initialGap));
        body.setPos(start);
        AABB captured=body.getBoundingBox();
        Vec3 pivot=new Vec3(start.x-2,start.y,start.z);
        double floorHalf=.01;
        // Avoid Matrix4f precision collapse at GameTest's very large absolute coordinates.
        var floorLocal=ConvexBox.of(new AABB(0,0,0,floorHalf*2,.20,floorHalf*2),new Matrix4f());
        var floorBefore=floorLocal.move(new Vec3(start.x-floorHalf,captured.minY-.20,start.z-floorHalf));
        var floorAfter=rotateY(floorBefore,pivot,Math.PI);
        double maxRadius=floorBefore.vertices().stream().mapToDouble(v->Math.hypot(v.x-pivot.x,v.z-pivot.z)).max().orElseThrow();
        var floorMotion=new ConservativeSweep.Motion(t->rotateY(floorBefore,pivot,Math.PI*t),maxRadius*Math.PI,Vec3.ZERO);
        long revision=209;

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(revision,Map.of("floor",floorBefore))));
        try {
            var floorSeparation=floorBefore.separation(captured);
            int face=floorBefore.closestFace(floorSeparation.normal());
            Vec3 normal=floorBefore.faceNormal(face);
            h.assertTrue(GravityFrame.VANILLA.supports(normal),"Fixture floor must support vanilla gravity");
            Vec3 local=floorBefore.facePoint(face,captured.getCenter());
            var initialSurface=new SurfaceContact(support.getUUID(),revision,"floor",face,local,normal,level.getGameTime());
            h.assertTrue(AnatomyMovement.confirm(body,support,initialSurface),"Fixture must establish retained floor contact");

            var obstacle=ConvexBox.of(new AABB(0,0,0,1,1,1),new Matrix4f()).move(blockMin);
            double gap=obstacle.separation(captured).gap();
            h.assertTrue(gap>0 && gap<=ConservativeSweep.SKIN,
                "Fixture block must start separated but inside CCD skin: "+gap);
            h.assertTrue(!blockBox.intersects(captured),"Fixture must not begin overlapping the block");

            Vec3 endpoint=new Vec3(-4,0,0);
            Entity previous=PlatformPhysics.enter(body);Vec3 chordAllowed;
            try {
                chordAllowed=Entity.collideBoundingBox(body,endpoint,captured,level,
                    level.getEntityCollisions(body,captured.expandTowards(endpoint)));
            } finally {PlatformPhysics.exit(previous);}
            h.assertTrue(chordAllowed.distanceToSqr(endpoint)<1e-10,
                "Control requires the straight endpoint chord to remain vanilla-clear: "+chordAllowed);

            double midT=.05,angle=Math.PI*midT;
            Vec3 midDisplacement=new Vec3(2*Math.cos(angle)-2,0,2*Math.sin(angle));
            h.assertTrue(blockBox.intersects(captured.move(midDisplacement)),
                "Control requires the certified anchor arc to enter the block at an intermediate time");
            h.assertTrue(!blockBox.intersects(captured.move(endpoint)),"Control endpoint must be clear of the block");

            var event=event(level,support,captured,pivot,floorBefore,floorAfter,floorMotion,revision,"curved_block");
            var handle=event.interval().handle();
            var motion=new GeometryProvider.MotionSnapshot(revision,level.getGameTime(),level.getGameTime(),pivot,pivot,Map.of("floor",floorMotion));
            var result=AnchoredTransportPlanner.plan(level,body,captured,List.of(event),Map.of(handle,motion),(box,requested)->clip(level,body,box,requested));
            h.assertTrue(result.status()==AnchoredTransportPlanner.Status.RELEASE,
                "FR-057 requires continuous obstruction to reject an arc even when its initial gap is <= SKIN and endpoint chord is clear: "+result.status());
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
            h.setBlock(blockRelative,Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest
    public void solidEntityInMiddleOfArcMustNotBeReducedToEndpointChord(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        var obstacle=h.spawn(EntityTypes.SHULKER,6,40,4);
        obstacle.setNoAi(true);obstacle.setNoGravity(true);
        try {
            Vec3 cell=h.absoluteVec(new Vec3(6,40,4));
            obstacle.setPos(cell.x+.5,cell.y,cell.z+.5);
            AABB obstacleBox=obstacle.getBoundingBox();
            h.assertTrue(obstacleBox.getXsize()>.5 && obstacleBox.getZsize()>.5,
                "Fixture entity must expose a solid-sized collision box: "+obstacleBox);

            double halfWidth=body.getBoundingBox().getXsize()*.5;
            double initialGap=ConservativeSweep.SKIN*.5;
            Vec3 start=new Vec3(obstacleBox.getCenter().x,obstacleBox.minY,obstacleBox.minZ-halfWidth-initialGap);
            body.setPos(start);
            AABB captured=body.getBoundingBox();
            Vec3 pivot=new Vec3(start.x-2,start.y,start.z);
            double floorHalf=.01;
            var floorLocal=ConvexBox.of(new AABB(0,0,0,floorHalf*2,.20,floorHalf*2),new Matrix4f());
            var floorBefore=floorLocal.move(new Vec3(start.x-floorHalf,captured.minY-.20,start.z-floorHalf));
            var floorAfter=rotateY(floorBefore,pivot,Math.PI);
            double maxRadius=floorBefore.vertices().stream().mapToDouble(v->Math.hypot(v.x-pivot.x,v.z-pivot.z)).max().orElseThrow();
            var floorMotion=new ConservativeSweep.Motion(t->rotateY(floorBefore,pivot,Math.PI*t),maxRadius*Math.PI,Vec3.ZERO);
            long revision=210;

            AnatomyMovement.activate(level);
            AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(revision,Map.of("floor",floorBefore))));
            try {
                var floorSeparation=floorBefore.separation(captured);
                int face=floorBefore.closestFace(floorSeparation.normal());
                Vec3 normal=floorBefore.faceNormal(face);
                h.assertTrue(GravityFrame.VANILLA.supports(normal),"Fixture floor must support vanilla gravity");
                Vec3 local=floorBefore.facePoint(face,captured.getCenter());
                h.assertTrue(AnatomyMovement.confirm(body,support,
                    new SurfaceContact(support.getUUID(),revision,"floor",face,local,normal,level.getGameTime())),
                    "Fixture must establish retained floor contact");

                h.assertTrue(!obstacleBox.intersects(captured),"Fixture body must start outside the solid entity");
                double initialEntityGap=obstacleBox.minZ-captured.maxZ;
                h.assertTrue(initialEntityGap>0 && initialEntityGap<=ConservativeSweep.SKIN,
                    "Entity fixture must start separated but inside CCD skin: "+initialEntityGap);

                Vec3 endpoint=new Vec3(-4,0,0);
                Vec3 chordAllowed=clip(level,body,captured,endpoint);
                h.assertTrue(chordAllowed.distanceToSqr(endpoint)<1e-10,
                    "Control requires the straight endpoint chord to remain entity-clear: "+chordAllowed);

                double midT=.05,angle=Math.PI*midT;
                Vec3 midDisplacement=new Vec3(2*Math.cos(angle)-2,0,2*Math.sin(angle));
                h.assertTrue(obstacleBox.intersects(captured.move(midDisplacement)),
                    "Control requires the certified anchor arc to enter the entity at an intermediate time");
                h.assertTrue(!obstacleBox.intersects(captured.move(endpoint)),
                    "Control endpoint must be clear of the entity");
                Vec3 midAllowed=clip(level,body,captured,midDisplacement);
                h.assertTrue(midAllowed.distanceToSqr(midDisplacement)>1e-10,
                    "Control entity must be an applicable vanilla collision obstacle at the intermediate arc point: allowed="+midAllowed);

                var event=event(level,support,captured,pivot,floorBefore,floorAfter,floorMotion,revision,"curved_entity");
                var handle=event.interval().handle();
                var motion=new GeometryProvider.MotionSnapshot(revision,level.getGameTime(),level.getGameTime(),pivot,pivot,Map.of("floor",floorMotion));
                var result=AnchoredTransportPlanner.plan(level,body,captured,List.of(event),Map.of(handle,motion),(box,requested)->clip(level,body,box,requested));
                h.assertTrue(result.status()==AnchoredTransportPlanner.Status.RELEASE,
                    "FR-057 requires continuous entity obstruction to reject an arc even when the endpoint chord is clear: "+result.status());
            } finally {
                AnatomyMovement.clear(body);
                AnatomyMovement.deactivate(level);
            }
        } finally {
            support.discard();body.discard();obstacle.discard();
        }
        h.succeed();
    }

    private static MaterialEventDispatcher.Event<net.minecraft.world.entity.Entity> event(
            net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.LivingEntity support,AABB captured,Vec3 pivot,
            ConvexBox floorBefore,ConvexBox floorAfter,ConservativeSweep.Motion floorMotion,long revision,String suffix) {
        long tick=level.getGameTime();
        long registration=AnatomyMovement.registrationGeneration(support);
        var identity=new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
            Identifier.parse("test:s08_"+suffix),Identifier.parse("test:s08_"+suffix+"_pose"),1,registration);
        var beforeRoot=new AnatomyMovement.RootFrame(1,tick,pivot,0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,tick,pivot,180,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,pivot,0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,pivot,180,1,GravityFrame.VANILLA);
        var before=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision,Map.of("floor",floorBefore)));
        var after=new GeometryProvider.QueryFrame(identity,
            new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(revision,Map.of("floor",floorAfter)));
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        double maxRadius=floorBefore.vertices().stream().mapToDouble(v->Math.hypot(v.x-pivot.x,v.z-pivot.z)).max().orElseThrow();
        var envelope=floorBefore.bounds().inflate(maxRadius*2+1);
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        return new MaterialEventDispatcher.Event<>(new MaterialEventDispatcher.EventId(1,0),support,
            MaterialEventDispatcher.Source.ROOT_MUTATION,interval,Set.of(support.getUUID()));
    }

    private static Vec3 clip(net.minecraft.server.level.ServerLevel level,Entity body,AABB box,Vec3 requested) {
        Entity entered=PlatformPhysics.enter(body);
        try {
            return Entity.collideBoundingBox(body,requested,box,level,level.getEntityCollisions(body,box.expandTowards(requested)));
        } finally {PlatformPhysics.exit(entered);}
    }

    private static ConvexBox rotateY(ConvexBox box,Vec3 pivot,double radians) {
        double sin=Math.sin(radians),cos=Math.cos(radians);
        return new ConvexBox(box.vertices().stream().map(v->{
            double x=v.x-pivot.x,z=v.z-pivot.z;
            return new Vec3(pivot.x+x*cos-z*sin,v.y,pivot.z+x*sin+z*cos);
        }).toList());
    }
}
