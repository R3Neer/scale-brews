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

/** S09 A12/A13: an exact recovery-budget boundary is invariant under a common large translation. */
public final class S09SeparationTranslationPrecisionTests {
    @GameTest
    public void exactRecoveryBoundarySurvivesLargeCommonTranslation(GameTestHelper h) {
        var body=new AABB(-.05,0,-.05,.05,.1,.05);
        int count=findExact128(body,Vec3.ZERO);
        h.assertTrue(count>0,
            "A12 local control must contain a nested-slab fixture whose first valid escape is candidate 128");

        var local=slabs(body,Vec3.ZERO,count);
        var local127=AnatomySeparation.resolve(body,local,4,127,(box,delta)->delta);
        var local128=AnatomySeparation.resolve(body,local,4,128,(box,delta)->delta);
        h.assertTrue(!local127.separated() && local127.candidates()==127
                && local128.separated() && local128.candidates()==128,
            "A12 local boundary must be exact before metamorphic translation: 127="+local127+" 128="+local128+" count="+count);

        // GameTest batches routinely live at multi-million world coordinates. A common translation
        // cannot create/delete recovery candidates or change which budget value first succeeds.
        var shift=new Vec3(7_000_000,32,-7_000_000);
        var translatedBody=body.move(shift);
        var translated=local.stream().map(piece->piece.move(shift)).toList();
        var shifted127=AnatomySeparation.resolve(translatedBody,translated,4,127,(box,delta)->delta);
        var shifted128=AnatomySeparation.resolve(translatedBody,translated,4,128,(box,delta)->delta);

        h.assertTrue(!shifted127.separated() && shifted127.candidates()==127
                && shifted128.separated() && shifted128.candidates()==128,
            "NFR-001/002 require common translation to preserve the exact recovery-budget boundary: local127="
                +local127+" local128="+local128+" shifted127="+shifted127+" shifted128="+shifted128+" count="+count);
        h.assertTrue(shifted128.displacement().distanceToSqr(local128.displacement())<1e-18,
            "Common translation must preserve the relative recovery vector: local="+local128+" shifted="+shifted128);
        h.succeed();
    }

    private static int findExact128(AABB body,Vec3 origin) {
        for(int count=1;count<=192;count++) {
            var pieces=slabs(body,origin,count);
            var below=AnatomySeparation.resolve(body,pieces,4,127,(box,delta)->delta);
            var exact=AnatomySeparation.resolve(body,pieces,4,128,(box,delta)->delta);
            if(!below.separated() && below.candidates()==127
                    && exact.separated() && exact.candidates()==128 && exact.displacement().x>0)
                return count;
        }
        return -1;
    }

    private static List<ConvexBox> slabs(AABB body,Vec3 origin,int count) {
        double minX=body.minX-origin.x,maxX=body.maxX-origin.x;
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
