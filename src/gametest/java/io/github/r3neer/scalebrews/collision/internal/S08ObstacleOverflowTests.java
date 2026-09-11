package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S08/NFR-004/007/008: the live anchored planner has a hard, fail-closed static-obstacle boundary. */
public final class S08ObstacleOverflowTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void twoHundredFiftySixObstaclesAreBoundedAndTheNextFailsClosed(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,10,39,10);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        Vec3 start=h.absoluteVec(new Vec3(10.5,40,10.5));body.setPos(start);
        AABB captured=body.getBoundingBox();
        Vec3 delta=new Vec3(20,0,0);

        var floorLocal=ConvexBox.of(new AABB(-.5,-.10,-.5,.5,0,.5),new Matrix4f());
        var before=floorLocal.move(new Vec3(start.x,captured.minY-.01,start.z));
        var after=before.move(delta);
        int face=3;long revision=413,tick=level.getGameTime();
        var motion=new ConservativeSweep.Motion(t->before.move(delta.scale(t)),0,delta,
            List.of(new ConservativeSweep.Plane(Direction.UP,before.bounds().maxY)));
        var placed=new ArrayList<BlockPos>();

        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(revision,Map.of("floor",before))));
        try {
            Vec3 normal=before.faceNormal(face);
            h.assertTrue(GravityFrame.VANILLA.supports(normal),"Fixture top face must support vanilla gravity");
            var separation=before.separation(captured);
            h.assertTrue(Math.abs(separation.gap()-.01)<1e-9,
                "Fixture must begin with the intended retained-contact gap: "+separation.gap());
            Vec3 local=before.facePoint(face,captured.getCenter());
            h.assertTrue(AnatomyMovement.confirm(body,support,
                new SurfaceContact(support.getUUID(),revision,"floor",face,local,normal,tick)),
                "Fixture must establish retained contact before obstacle-budget planning");

            long registration=AnatomyMovement.registrationGeneration(support);
            var identity=new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
                Identifier.parse("test:s08_obstacle_budget"),Identifier.parse("test:s08_obstacle_budget_pose"),1,registration);
            var root0=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
            var root1=new AnatomyMovement.RootFrame(2,tick,support.position().add(delta),0,1,GravityFrame.VANILLA);
            var sample0=new AnatomyPoseHistory.Sample(INPUTS,root0.origin(),0,1,GravityFrame.VANILLA);
            var sample1=new AnatomyPoseHistory.Sample(INPUTS,root1.origin(),0,1,GravityFrame.VANILLA);
            var q0=new GeometryProvider.QueryFrame(identity,
                new GeometryProvider.CausalEndpoint(1,tick,tick,root0,sample0,GeometryProvider.Availability.AVAILABLE),
                new GeometryProvider.Snapshot(revision,Map.of("floor",before)));
            var q1=new GeometryProvider.QueryFrame(identity,
                new GeometryProvider.CausalEndpoint(2,tick,tick,root1,sample1,GeometryProvider.Availability.AVAILABLE),
                new GeometryProvider.Snapshot(revision,Map.of("floor",after)));
            var handle=new GeometryProvider.MotionIntervalHandle(identity,1,q0,q1);
            var snapshot=new GeometryProvider.MotionSnapshot(revision,tick,tick,root0.origin(),root1.origin(),Map.of("floor",motion));
            var interval=new MaterialEventDispatcher.MaterialInterval(handle,before.bounds().inflate(delta.length()+ConservativeSweep.SKIN));
            var event=new MaterialEventDispatcher.Event<net.minecraft.world.entity.Entity>(
                new MaterialEventDispatcher.EventId(1,0),support,MaterialEventDispatcher.Source.ROOT_MUTATION,interval,Set.of(support.getUUID()));

            // 16 x 16 = exactly 256 full-cube AABBs, high and laterally offset from the body path.
            for(int x=0;x<16;x++)for(int z=14;z<30;z++) {
                var pos=new BlockPos(x,45,z);h.setBlock(pos,Blocks.STONE);placed.add(pos);
            }
            AABB plannerEnvelope=captured.inflate(delta.length()+ConservativeSweep.SKIN);
            h.assertTrue(countBlockAabbs(level,body,plannerEnvelope)==256,
                "Control must expose exactly 256 static obstacle AABBs to the planner");
            Vec3 original=body.position();
            var boundary=AnchoredTransportPlanner.plan(level,body,captured,List.of(event),Map.of(handle,snapshot),(box,requested)->requested);
            h.assertTrue(boundary.status()==AnchoredTransportPlanner.Status.COMPLETE && boundary.evaluations()==256,
                "Exactly 256 irrelevant obstacles must remain bounded and certifiable: status="+boundary.status()+" evals="+boundary.evaluations());
            h.assertTrue(body.position().equals(original),"Planning at the boundary must remain query-only");

            var extra=new BlockPos(16,45,14);h.setBlock(extra,Blocks.STONE);placed.add(extra);
            h.assertTrue(countBlockAabbs(level,body,plannerEnvelope)==257,
                "Overflow control must expose exactly one obstacle beyond the configured boundary");
            var overflow=AnchoredTransportPlanner.plan(level,body,captured,List.of(event),Map.of(handle,snapshot),(box,requested)->requested);
            h.assertTrue(overflow.status()==AnchoredTransportPlanner.Status.EXHAUSTED && overflow.evaluations()==256,
                "The 257th static obstacle must fail closed before any unbounded scan or endpoint-only fallback: status="
                    +overflow.status()+" evals="+overflow.evaluations());
            h.assertTrue(body.position().equals(original),"Obstacle overflow must not apply a speculative partial carry");
        } finally {
            for(var pos:placed)h.setBlock(pos,Blocks.AIR);
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);support.discard();body.discard();
        }
        h.succeed();
    }

    private static int countBlockAabbs(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity body,AABB envelope) {
        int count=0;
        for(var shape:level.getBlockCollisions(body,envelope))count+=shape.toAabbs().size();
        return count;
    }
}
