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
    public void retainedFloorComposesWithNewWallContactWithoutLosingAnchor(GameTestHelper h) {
        var level=h.getLevel();
        var floorSupport=h.spawn(EntityTypes.COW,14,20,2);
        var wallSupport=h.spawn(EntityTypes.COW,16,20,2);
        for(var support:java.util.List.of(floorSupport,wallSupport)) {
            support.setNoAi(true);support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        }
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        body.setPos(h.absoluteVec(new Vec3(15,22,6)));
        var start=body.getBoundingBox();double landingGap=.5,wallGap=.2;
        double floorTop=start.minY-landingGap;

        // Keep the real support entities out of the fixture geometry. The providers own these
        // material pieces, while the cows merely supply independently identifiable supports.
        double floorMinX=start.minX-2,floorMinZ=start.minZ-2;
        double floorWidth=start.getXsize()+4,floorDepth=start.getZsize()+4;
        var floor=ConvexBox.of(new AABB(0,0,0,floorWidth,.2,floorDepth),new Matrix4f())
            .move(new Vec3(floorMinX,floorTop-.2,floorMinZ));
        double wallX=start.maxX+wallGap,wallMinY=start.minY-2,wallMinZ=start.minZ-2;
        double wallHeight=start.getYsize()+4,wallDepth=start.getZsize()+4;
        var wall=ConvexBox.of(new AABB(0,0,0,.2,wallHeight,wallDepth),new Matrix4f())
            .move(new Vec3(wallX,wallMinY,wallMinZ));

        GeometryProvider floorProvider=ignored->Optional.of(new GeometryProvider.Snapshot(112,Map.of("floor",floor)));
        GeometryProvider wallProvider=ignored->Optional.of(new GeometryProvider.Snapshot(113,Map.of("wall",wall)));
        AnatomyMovement.activate(level);
        AnatomyMovement.register(floorSupport,floorProvider);
        AnatomyMovement.register(wallSupport,wallProvider);
        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body,floorSupport)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(body,wallSupport),
                "Retained-plus-new fixture requires both supports to be real live candidates");
            Vec3 landing=AnatomyMovement.collide(body,new Vec3(0,-1,0));
            body.setPos(body.position().add(landing));
            AnatomyMovement.afterMove(body);
            var initial=AnatomyMovement.contact(body);
            h.assertTrue(initial!=null && initial.support()==floorSupport,
                "Fixture must retain the floor before introducing the new wall constraint");
            long sequence=initial.sequence();double y=body.getY();

            var request=new Vec3(.6,0,.25);
            Vec3 moved=AnatomyMovement.collide(body,request);
            body.setPos(body.position().add(moved));
            AnatomyMovement.afterMove(body);
            var retained=AnatomyMovement.contact(body);
            h.assertTrue(Math.abs(moved.x-wallGap)<2e-4 && Math.abs(moved.y)<1e-8
                    && Math.abs(moved.z-request.z)<1e-8,
                "A newly encountered wall must clip only its normal while the retained floor preserves free tangent motion: request="
                    +request+" actual="+moved);
            h.assertTrue(!wall.overlaps(body.getBoundingBox()) && !floor.overlaps(body.getBoundingBox()),
                "Composed retained+new response must finish non-penetrating against both pieces");
            h.assertTrue(retained!=null && retained.support()==floorSupport && retained.sequence()==sequence
                    && Math.abs(body.getY()-y)<1e-8,
                "A new side-wall hit must not replace or churn the still-valid retained floor contact");
            var surface=AnatomyMovement.surface(body);
            h.assertTrue(surface!=null && surface.support().equals(floorSupport.getUUID()) && surface.piece().equals("floor"),
                "The persistent material anchor must remain on the floor after composing the new wall constraint");
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            floorSupport.discard();wallSupport.discard();body.discard();
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

        // ConvexBox.of validates a model-local box. GameTest absolute coordinates can be millions
        // of blocks from origin, so build bounded local cuboids and translate them into world space.
        double floorMinX=box.minX-2,floorMinZ=box.minZ-2;
        double floorWidth=box.getXsize()+4,floorDepth=box.getZsize()+4;
        var floor=ConvexBox.of(new AABB(0,0,0,floorWidth,.2,floorDepth),new Matrix4f())
            .move(new Vec3(floorMinX,floorTop-.2,floorMinZ));
        double wallMinY=box.minY-2,wallMinZ=box.minZ-2;
        double wallHeight=box.getYsize()+4,wallDepth=box.getZsize()+4;
        var wall=ConvexBox.of(new AABB(0,0,0,.2,wallHeight,wallDepth),new Matrix4f())
            .move(new Vec3(wallX,wallMinY,wallMinZ));

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
