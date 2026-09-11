package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.AnatomySeparation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A12/A13: bounded recovery is invariant under a common large world translation. */
public final class S09SeparationTranslationPrecisionTests {
    @GameTest
    public void recoverySearchSurvivesLargeCommonTranslation(GameTestHelper h) {
        var body=new AABB(-.05,0,-.05,.05,.1,.05);
        var local=slabs(body,Vec3.ZERO,10);
        var localResult=AnatomySeparation.resolve(body,local,4,256,(box,delta)->delta);
        h.assertTrue(localResult.separated() && localResult.displacement().x>0 && localResult.candidates()<32,
            "A13 local recovery control must be a small, ordinary bounded search before translation: "+localResult);

        // GameTest batches routinely sit at multi-million world coordinates. A common translation
        // cannot create extra physical recovery states or alter the minimum relative escape.
        var shift=new Vec3(7_000_000,32,-7_000_000);
        var translatedBody=body.move(shift);
        var translated=local.stream().map(piece->piece.move(shift)).toList();
        var shifted=AnatomySeparation.resolve(translatedBody,translated,4,256,(box,delta)->delta);

        h.assertTrue(shifted.separated(),
            "NFR-001/002 require the same resolvable recovery after a large common translation: local="
                +localResult+" shifted="+shifted);
        h.assertTrue(shifted.candidates()==localResult.candidates(),
            "Common translation must not create numerical recovery candidates: local="+localResult+" shifted="+shifted);
        h.assertTrue(shifted.displacement().distanceToSqr(localResult.displacement())<1e-16,
            "Common translation must preserve the relative shortest recovery vector: local="+localResult+" shifted="+shifted);
        h.succeed();
    }

    private static List<ConvexBox> slabs(AABB body,Vec3 origin,int count) {
        double minX=body.minX-origin.x;
        double minY=body.minY-origin.y,maxY=body.maxY-origin.y;
        double minZ=body.minZ-origin.z,maxZ=body.maxZ-origin.z;
        var result=new ArrayList<ConvexBox>(count);
        for(int i=0;i<count;i++) {
            double right=minX+.02+i*.001;
            var local=new AABB(minX-.8,minY-.6,minZ-.6,right,maxY+.6,maxZ+.6);
            result.add(ConvexBox.of(local,new Matrix4f()).move(origin));
        }
        return List.copyOf(result);
    }
}
