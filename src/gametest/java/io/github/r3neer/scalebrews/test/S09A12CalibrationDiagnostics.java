package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.AnatomySeparation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Temporary S09 A12 diagnostic. Remove after the exact boundary fixture is understood. */
public final class S09A12CalibrationDiagnostics {
    @GameTest
    public void reportNestedSlabCandidateCounts(GameTestHelper h) {
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);
        body.refreshDimensions();
        body.setPos(h.absoluteVec(new Vec3(8,22,6)));
        AABB captured=body.getBoundingBox();
        Vec3 origin=body.position();

        List<String> rows=new ArrayList<>();
        for(int count=120;count<=135;count++) {
            var result=AnatomySeparation.resolve(captured,slabs(captured,origin,count),4,4096,(box,delta)->delta);
            rows.add(count+":"+result.candidates()+":"+result.separated()+":"+result.displacement().x);
        }
        body.discard();
        h.assertTrue(false,"A12_DIAG "+String.join(" | ",rows));
    }

    private static List<ConvexBox> slabs(AABB body,Vec3 origin,int count) {
        double minX=-body.getXsize()*.5;
        double minY=0,maxY=body.getYsize();
        double minZ=-body.getZsize()*.5,maxZ=body.getZsize()*.5;
        List<ConvexBox> result=new ArrayList<>();
        for(int i=0;i<count;i++) {
            double right=minX+.02+i*.001;
            var local=new AABB(minX-.8,minY-.6,minZ-.6,right,maxY+.6,maxZ+.6);
            result.add(ConvexBox.of(local,new Matrix4f()).move(origin));
        }
        return List.copyOf(result);
    }
}
