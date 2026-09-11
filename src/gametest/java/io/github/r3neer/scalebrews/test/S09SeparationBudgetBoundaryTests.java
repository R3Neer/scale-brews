package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.physics.AnatomySeparation;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A12 + reserved holdout: exact 128-candidate recovery boundary with a non-causal bystander. */
public final class S09SeparationBudgetBoundaryTests {
    @GameTest
    public void exactBoundaryRecoversButCandidate129FailsClosedAndPairLocal(GameTestHelper h) {
        // Do not infer the queue cardinality from slab count. ConvexBox.escapeVectors deliberately
        // publishes both exits on every SAT axis, so the kernel itself calibrates the two exact
        // boundaries and the live path must reproduce those already-proven fixtures.
        runCase(h,true,8,6,2);
        runCase(h,false,24,6,18);
        h.succeed();
    }

    private static void runCase(GameTestHelper h,boolean mustRecover,
            double bodyX,double bodyZ,int supportX) {
        var level=h.getLevel();
        var culprit=h.spawn(EntityTypes.COW,supportX,20,2);
        var bystander=h.spawn(EntityTypes.COW,supportX,20,10);
        for(var support:java.util.List.of(culprit,bystander)) {
            support.setNoAi(true);support.setNoGravity(true);
            support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        }
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();
        body.setPos(h.absoluteVec(new Vec3(bodyX,22,bodyZ)));
        var captured=body.getBoundingBox();var origin=body.position();
        int slabCount=calibratedSlabCount(captured,origin,mustRecover);
        h.assertTrue(slabCount>0,
            mustRecover
                ?"A12 calibration must find a nested-slab fixture whose first valid escape is candidate 128"
                :"A12 calibration must find a nested-slab fixture whose first valid escape is candidate 129");
        var slabs=slabs(captured,origin,slabCount);
        var bystanderPiece=bystander(captured,origin);

        if(mustRecover) {
            var oneShort=AnatomySeparation.resolve(captured,slabs.values(),4,127,(box,delta)->delta);
            var exact=AnatomySeparation.resolve(captured,slabs.values(),4,128,(box,delta)->delta);
            h.assertTrue(!oneShort.separated() && oneShort.candidates()==127,
                "A12 calibrated fixture must still be unresolved one candidate below the live separation budget: "+oneShort);
            h.assertTrue(exact.separated() && exact.candidates()==128 && exact.displacement().x>0,
                "A12 calibrated fixture must find its first valid escape on candidate 128: "+exact+" slabs="+slabCount);
        } else {
            var capped=AnatomySeparation.resolve(captured,slabs.values(),4,128,(box,delta)->delta);
            var plusOne=AnatomySeparation.resolve(captured,slabs.values(),4,129,(box,delta)->delta);
            h.assertTrue(!capped.separated() && capped.candidates()==128,
                "A12 calibrated +1 fixture must exhaust all 128 live candidates without exporting an escape: "+capped);
            h.assertTrue(plusOne.separated() && plusOne.candidates()==129 && plusOne.displacement().x>0,
                "A12 calibrated +1 fixture must prove candidate 129 is the first valid escape: "+plusOne+" slabs="+slabCount);
        }
        h.assertTrue(slabs.values().stream().allMatch(piece->piece.overlaps(captured)),
            "Every culprit slab must genuinely participate in initial-overlap recovery");
        h.assertTrue(!bystanderPiece.overlaps(captured)
                && bystanderPiece.sweep(captured,new Vec3(.25,0,0))==null,
            "Reserved bystander must be query-local but non-causal for every +X recovery candidate");

        AnatomyMovement.activate(level);
        AnatomyMovement.register(culprit,ignored->java.util.Optional.of(new GeometryProvider.Snapshot(700+slabCount,slabs)));
        AnatomyMovement.register(bystander,ignored->java.util.Optional.of(new GeometryProvider.Snapshot(900+slabCount,Map.of("bystander",bystanderPiece))));
        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body,culprit)
                    && io.github.r3neer.scalebrews.platform.Platforms.eligible(body,bystander),
                "A12 live fixture requires both culprit and non-causal bystander to be eligible indexed supports");
            var beforeMetrics=AnatomyMovement.sweepMetrics(level);var before=body.position();
            Vec3 allowed=AnatomyMovement.collide(body,Vec3.ZERO);
            var afterMetrics=AnatomyMovement.sweepMetrics(level);
            h.assertTrue(body.position().equals(before),
                "AnatomyMovement.collide must remain query-only at the separation boundary");
            h.assertTrue(afterMetrics.queries()-beforeMetrics.queries()==1,
                "A12 must exercise one live own-move query, not only the isolated separation kernel");

            if(mustRecover) {
                var exact=AnatomySeparation.resolve(captured,slabs.values(),4,128,(box,delta)->delta);
                h.assertTrue(allowed.distanceToSqr(exact.displacement())<1e-12,
                    "Candidate 128 must be exported as the complete bounded initial correction: kernel="+exact+" live="+allowed);
                h.assertTrue(slabs.values().stream().noneMatch(piece->piece.overlaps(captured.move(allowed))),
                    "Exact-budget recovery must finish clear of every culprit slab");
                h.assertTrue(!AnatomyMovement.suspended(body,culprit) && !AnatomyMovement.suspended(body,bystander),
                    "A successful candidate-128 recovery must suspend neither the culprit nor the non-causal bystander");
                h.assertTrue(afterMetrics.pieces()-beforeMetrics.pieces()==slabCount+1,
                    "Exact live response must retain all culprit pieces plus the non-causal bystander after recovery");
            } else {
                h.assertTrue(allowed.lengthSqr()<1e-20,
                    "Candidate 129 is outside the live budget: own-move must fail closed rather than teleport to an unsearched escape, got "+allowed);
                h.assertTrue(AnatomyMovement.suspended(body,culprit),
                    "The still-overlapping culprit pair must be suspended when candidate 129 is required");
                h.assertTrue(!AnatomyMovement.suspended(body,bystander),
                    "Budget exhaustion must not suspend the non-overlapping bystander support");
                h.assertTrue(afterMetrics.pieces()-beforeMetrics.pieces()==1,
                    "After localized recovery exhaustion only the non-causal bystander motion should remain in the live response");
            }
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);
            culprit.discard();bystander.discard();body.discard();
        }
    }

    /**
     * The test cares about the kernel's observable 128-candidate boundary, not an assumed
     * one-slab/one-node relationship. Search only the compact neighborhood this fixture family
     * occupies and require an exact transition so a future queue-policy change fails loudly.
     */
    private static int calibratedSlabCount(AABB body,Vec3 origin,boolean candidate128) {
        for(int count=96;count<=160;count++) {
            var pieces=slabs(body,origin,count).values();
            int low=candidate128?127:128,high=low+1;
            var below=AnatomySeparation.resolve(body,pieces,4,low,(box,delta)->delta);
            var exact=AnatomySeparation.resolve(body,pieces,4,high,(box,delta)->delta);
            if(!below.separated() && below.candidates()==low
                    && exact.separated() && exact.candidates()==high && exact.displacement().x>0)
                return count;
        }
        return -1;
    }

    private static Map<String,ConvexBox> slabs(AABB body,Vec3 origin,int count) {
        double minX=body.minX-origin.x,maxX=body.maxX-origin.x;
        double minY=body.minY-origin.y,maxY=body.maxY-origin.y;
        double minZ=body.minZ-origin.z,maxZ=body.maxZ-origin.z;
        var result=new TreeMap<String,ConvexBox>();
        for(int i=0;i<count;i++) {
            double right=minX+.02+i*.001;
            var local=new AABB(minX-.8,minY-.6,minZ-.6,right,maxY+.6,maxZ+.6);
            result.put(String.format(java.util.Locale.ROOT,"slab_%03d",i),ConvexBox.of(local,new Matrix4f()).move(origin));
        }
        return Map.copyOf(result);
    }

    private static ConvexBox bystander(AABB body,Vec3 origin) {
        double minX=body.minX-origin.x,maxX=body.maxX-origin.x;
        double minY=body.minY-origin.y,maxY=body.maxY-origin.y;
        double maxZ=body.maxZ-origin.z;
        return ConvexBox.of(new AABB(minX-.2,minY-.2,maxZ+.20,maxX+.4,maxY+.2,maxZ+.30),new Matrix4f()).move(origin);
    }
}
