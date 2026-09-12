package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A13: physical results are invariant under registration order and common world translation. */
public final class S09TranslationMetamorphismTests {
    @GameTest
    public void ccdSkinThresholdSurvivesPreparedScaleWorldTranslation(GameTestHelper h) {
        double gap=ConservativeSweep.SKIN-1e-10;
        var localPiece=ConvexBox.of(new AABB(-.1,-.1,-.1,.1,.1,.1),new Matrix4f());
        var localBody=new AABB(.1+gap,-.05,-.05,.2+gap,.05,.05);
        var localMotion=new ConservativeSweep.Motion(t->localPiece,1,Vec3.ZERO);
        var local=ConservativeSweep.query(localBody,Vec3.ZERO,localMotion,256);
        h.assertTrue(local.status()==ConservativeSweep.Status.CONTACT && local.evaluations()==1,
            "Control must begin inside the CCD skin in one evaluation: "+local);

        // GameTest commonly places structures millions of blocks from origin. The common
        // translation must not turn the same sub-skin configuration into an iteration-limit.
        var shift=new Vec3(15_000_000,0,-4_000_000);
        var translatedPiece=localPiece.move(shift);
        var translatedBody=localBody.move(shift);
        var translatedMotion=new ConservativeSweep.Motion(t->translatedPiece,1,Vec3.ZERO);
        var translated=ConservativeSweep.query(translatedBody,Vec3.ZERO,translatedMotion,256);
        h.assertTrue(translated.status()==ConservativeSweep.Status.CONTACT,
            "A common prepared-scale world translation must preserve the CCD skin classification instead of exhausting on coordinate quantization: local="
                +local+" translated="+translated+" localGap="+localPiece.separation(localBody).gap()
                +" translatedGap="+translatedPiece.separation(translatedBody).gap());
        h.succeed();
    }

    @GameTest
    public void simultaneousCornerResponseSurvivesCommonWorldTranslation(GameTestHelper h) {
        var origin=cornerScenario(h,0,0,true);
        var translated=cornerScenario(h,32,32,false);

        h.assertTrue(origin.displacement().distanceToSqr(translated.displacement())<1e-12,
            "A13 common translation plus reversed registration order must preserve relative displacement: origin="
                +origin+" translated="+translated);
        h.assertTrue(origin.floorRetained() && translated.floorRetained(),
            "A13 metamorphism must preserve the gravity-supporting persistent contact, not only the numeric displacement");
        h.assertTrue(Math.abs(origin.displacement().x-.2)<2e-4
                && Math.abs(origin.displacement().y+.2)<2e-4
                && Math.abs(origin.displacement().z-.3)<1e-8,
            "A13 control must remain the intended equal-time floor+wall response: "+origin.displacement());
        h.succeed();
    }

    private record Outcome(Vec3 displacement,boolean floorRetained) {}

    private static Outcome cornerScenario(GameTestHelper h,int dx,int dz,boolean floorFirst) {
        var level=h.getLevel();
        var floorSupport=h.spawn(EntityTypes.COW,8+dx,20,2+dz);
        var wallSupport=h.spawn(EntityTypes.COW,10+dx,20,2+dz);
        for(var support:java.util.List.of(floorSupport,wallSupport)) {
            support.setNoAi(true);support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        }
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        body.setPos(h.absoluteVec(new Vec3(9+dx,22,6+dz)));
        var box=body.getBoundingBox();double gap=.2;
        double floorTop=box.minY-gap,wallX=box.maxX+gap;

        // Build bounded local convexes before the common world translation. This makes the test
        // sensitive to accidental origin-dependent math without manufacturing invalid float-local boxes.
        double floorMinX=box.minX-2,floorMinZ=box.minZ-2;
        double floorWidth=box.getXsize()+4,floorDepth=box.getZsize()+4;
        var floor=ConvexBox.of(new AABB(0,0,0,floorWidth,.2,floorDepth),new Matrix4f())
            .move(new Vec3(floorMinX,floorTop-.2,floorMinZ));
        double wallMinY=box.minY-2,wallMinZ=box.minZ-2;
        double wallHeight=box.getYsize()+4,wallDepth=box.getZsize()+4;
        var wall=ConvexBox.of(new AABB(0,0,0,.2,wallHeight,wallDepth),new Matrix4f())
            .move(new Vec3(wallX,wallMinY,wallMinZ));

        long floorRevision=810+dx,wallRevision=811+dx;
        GeometryProvider floorProvider=ignored->Optional.of(new GeometryProvider.Snapshot(floorRevision,Map.of("floor",floor)));
        GeometryProvider wallProvider=ignored->Optional.of(new GeometryProvider.Snapshot(wallRevision,Map.of("wall",wall)));
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
                "A13 requires both translated supports to remain real live candidates");
            var request=new Vec3(.4,-.4,.3);
            Vec3 moved=AnatomyMovement.collide(body,request);
            body.setPos(body.position().add(moved));
            AnatomyMovement.afterMove(body);
            h.assertTrue(!floor.overlaps(body.getBoundingBox()) && !wall.overlaps(body.getBoundingBox()),
                "A13 translated scenario must finish non-penetrating against both simultaneous constraints");
            var contact=AnatomyMovement.contact(body);
            var surface=AnatomyMovement.surface(body);
            boolean retained=contact!=null && contact.support()==floorSupport && contact.piece().equals("floor")
                && surface!=null && surface.support().equals(floorSupport.getUUID()) && surface.piece().equals("floor");
            return new Outcome(moved,retained);
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            floorSupport.discard();wallSupport.discard();body.discard();
        }
    }
}
