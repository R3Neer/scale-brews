package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A4: three simultaneous live constraints must all enter the response manifold. */
public final class S09TripleContactTests {
    @GameTest
    public void threeWayCornerBlocksAllNormalsWithoutBudgetExhaustion(GameTestHelper h) {
        var level=h.getLevel();
        var floorSupport=h.spawn(EntityTypes.COW,2,20,2);
        var xSupport=h.spawn(EntityTypes.COW,4,20,2);
        var zSupport=h.spawn(EntityTypes.COW,6,20,2);
        for(var support:java.util.List.of(floorSupport,xSupport,zSupport)) {
            support.setNoAi(true);support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        }
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        body.setPos(h.absoluteVec(new Vec3(9,22,6)));
        var box=body.getBoundingBox();double gap=.2;

        double floorWidth=box.getXsize()+4,floorDepth=box.getZsize()+4;
        var floor=ConvexBox.of(new AABB(0,0,0,floorWidth,.2,floorDepth),new Matrix4f())
            .move(new Vec3(box.minX-2,box.minY-gap-.2,box.minZ-2));
        double wallHeight=box.getYsize()+4;
        var xWall=ConvexBox.of(new AABB(0,0,0,.2,wallHeight,box.getZsize()+4),new Matrix4f())
            .move(new Vec3(box.maxX+gap,box.minY-2,box.minZ-2));
        var zWall=ConvexBox.of(new AABB(0,0,0,box.getXsize()+4,wallHeight,.2),new Matrix4f())
            .move(new Vec3(box.minX-2,box.minY-2,box.maxZ+gap));

        GeometryProvider floorProvider=ignored->Optional.of(new GeometryProvider.Snapshot(901,Map.of("floor",floor)));
        GeometryProvider xProvider=ignored->Optional.of(new GeometryProvider.Snapshot(902,Map.of("x_wall",xWall)));
        GeometryProvider zProvider=ignored->Optional.of(new GeometryProvider.Snapshot(903,Map.of("z_wall",zWall)));
        AnatomyMovement.activate(level);
        // Deliberately non-geometric registration order. A4 is about the manifold, not the order
        // humans happened to write three register calls in this file.
        AnatomyMovement.register(zSupport,zProvider);
        AnatomyMovement.register(floorSupport,floorProvider);
        AnatomyMovement.register(xSupport,xProvider);
        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body,floorSupport)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(body,xSupport)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(body,zSupport),
                "A4 requires all three support relations to enter the live candidate set");
            var beforeMetrics=AnatomyMovement.sweepMetrics(level);
            var request=new Vec3(.4,-.4,.4);
            Vec3 moved=AnatomyMovement.collide(body,request);
            var afterMetrics=AnatomyMovement.sweepMetrics(level);
            body.setPos(body.position().add(moved));
            AnatomyMovement.afterMove(body);

            h.assertTrue(Math.abs(moved.x-gap)<2e-4 && Math.abs(moved.y+gap)<2e-4 && Math.abs(moved.z-gap)<2e-4,
                "A4 three-way corner must block all three incoming normal components at the common first-contact time: request="
                    +request+" moved="+moved);
            h.assertTrue(!floor.overlaps(body.getBoundingBox()) && !xWall.overlaps(body.getBoundingBox())
                    && !zWall.overlaps(body.getBoundingBox()),
                "A4 completed live response must finish non-penetrating against all three simultaneous pieces");
            h.assertTrue(afterMetrics.exhausted()==beforeMetrics.exhausted(),
                "Three simultaneous constraints are ordinary multicontact, not a license to exhaust the response budget: before="
                    +beforeMetrics+" after="+afterMetrics);
            var contact=AnatomyMovement.contact(body);var surface=AnatomyMovement.surface(body);
            h.assertTrue(contact!=null && contact.support()==floorSupport && contact.piece().equals("floor")
                    && surface!=null && surface.support().equals(floorSupport.getUUID()) && surface.piece().equals("floor"),
                "With vanilla gravity the canonical persistent relation after a three-way corner must be the supporting floor, contact="
                    +contact+" surface="+surface);
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);
            floorSupport.discard();xSupport.discard();zSupport.discard();body.discard();
        }
        h.succeed();
    }
}
