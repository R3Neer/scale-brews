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

/** S09 live own-movement holdouts for tangential retention and simultaneous constraints. */
public final class S09LiveOwnMoveTests {
    @GameTest
    public void retainedContactSurvivesTangentWithoutNewHitThenReleasesPastFootprint(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var floor=ConvexBox.of(new AABB(-2,-.2,-2,2,0,2),new Matrix4f()).move(support.position().add(0,1,0));
        GeometryProvider provider=ignored->Optional.of(new GeometryProvider.Snapshot(109,Map.of("floor",floor)));
        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,provider);
        try {
            body.setPos(support.getX()-.6,floor.bounds().maxY+.5,support.getZ());
            Vec3 landing=AnatomyMovement.collide(body,new Vec3(0,-1,0));
            body.setPos(body.position().add(landing));
            AnatomyMovement.afterMove(body);
            var initial=AnatomyMovement.contact(body);
            h.assertTrue(initial!=null && initial.support()==support,
                "Fixture must establish a real retained material contact before the tangent move");
            long sequence=initial.sequence();double y=body.getY();

            var tangentRequest=new Vec3(.4,0,.2);
            Vec3 tangent=AnatomyMovement.collide(body,tangentRequest);
            body.setPos(body.position().add(tangent));
            AnatomyMovement.afterMove(body);
            var retained=AnatomyMovement.contact(body);
            h.assertTrue(tangent.distanceToSqr(tangentRequest)<1e-12,
                "A retained floor must preserve both tangent components: expected="+tangentRequest+" actual="+tangent);
            h.assertTrue(retained!=null && retained.support()==support && retained.sequence()==sequence
                    && Math.abs(body.getY()-y)<1e-12,
                "Tangential motion with no new normal hit must retain the same material relation without vertical drift");

            Vec3 escape=AnatomyMovement.collide(body,new Vec3(3,0,0));
            body.setPos(body.position().add(escape));
            AnatomyMovement.afterMove(body);
            h.assertTrue(escape.x>2.5 && AnatomyMovement.contact(body)==null && AnatomyMovement.surface(body)==null,
                "Leaving the material footprint tangentially must release rather than turn retention into sticky contact");
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void simultaneousFloorAndWallConstraintsIgnoreRegistrationOrder(GameTestHelper h) {
        Vec3 floorFirst=cornerScenario(h,true);
        Vec3 wallFirst=cornerScenario(h,false);
        h.assertTrue(floorFirst.distanceToSqr(wallFirst)<1e-12,
            "Reversing support registration must not change simultaneous-contact response: floor-first="+floorFirst+" wall-first="+wallFirst);
        h.assertTrue(Math.abs(floorFirst.x-.2)<2e-4 && Math.abs(floorFirst.y+.2)<2e-4 && Math.abs(floorFirst.z-.3)<1e-8,
            "Equal-time floor+wall must block both normals while preserving the free tangent: "+floorFirst);
        h.succeed();
    }

    private static Vec3 cornerScenario(GameTestHelper h,boolean floorFirst) {
        var level=h.getLevel();
        var floorSupport=h.spawn(EntityTypes.COW,8,20,2);
        var wallSupport=h.spawn(EntityTypes.COW,10,20,2);
        for(var support:java.util.List.of(floorSupport,wallSupport)) {
            support.setNoAi(true);support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        }
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        body.setPos(h.absoluteVec(new Vec3(9,22,6)));
        var box=body.getBoundingBox();double gap=.2;
        double floorTop=box.minY-gap,wallX=box.maxX+gap;
        var floor=ConvexBox.of(new AABB(box.minX-2,floorTop-.2,box.minZ-2,box.maxX+2,floorTop,box.maxZ+2),new Matrix4f());
        var wall=ConvexBox.of(new AABB(wallX,box.minY-2,box.minZ-2,wallX+.2,box.maxY+2,box.maxZ+2),new Matrix4f());
        GeometryProvider floorProvider=ignored->Optional.of(new GeometryProvider.Snapshot(110,Map.of("floor",floor)));
        GeometryProvider wallProvider=ignored->Optional.of(new GeometryProvider.Snapshot(111,Map.of("wall",wall)));
        AnatomyMovement.activate(level);
        if(floorFirst) {
            AnatomyMovement.register(floorSupport,floorProvider);
            AnatomyMovement.register(wallSupport,wallProvider);
        } else {
            AnatomyMovement.register(wallSupport,wallProvider);
            AnatomyMovement.register(floorSupport,floorProvider);
        }
        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body,floorSupport)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(body,wallSupport),
                "Both supports must be real live candidates in the simultaneous-contact fixture");
            Vec3 moved=AnatomyMovement.collide(body,new Vec3(.4,-.4,.3));
            body.setPos(body.position().add(moved));
            AnatomyMovement.afterMove(body);
            h.assertTrue(!floor.overlaps(body.getBoundingBox()) && !wall.overlaps(body.getBoundingBox()),
                "Simultaneous response must end non-penetrating against both constraints");
            var contact=AnatomyMovement.contact(body);
            h.assertTrue(contact!=null && contact.support()==floorSupport,
                "Persistent support must select the gravity-supporting floor, not the equal-time side wall");
            return moved;
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            floorSupport.discard();wallSupport.discard();body.discard();
        }
    }
}
