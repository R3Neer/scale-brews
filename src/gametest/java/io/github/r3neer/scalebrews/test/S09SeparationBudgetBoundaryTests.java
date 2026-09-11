package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.physics.AnatomySeparation;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A12 + reserved holdout: exact 128-candidate recovery boundary with a non-causal bystander. */
public final class S09SeparationBudgetBoundaryTests {
    @GameTest
    public void exactBoundaryRecoversButCandidate129FailsClosedAndPairLocal(GameTestHelper h) {
        // A budget is meaningful only if physically identical offsets are one candidate. Verify
        // that prerequisite first: two nested slabs have exactly three physical states before
        // success (ZERO, the inner +X escape and the outer +X escape). Signed-zero/SAT aliases
        // must not turn those three states into several charged queue nodes.
        assertEquivalentOffsetsAreOneCandidate(h);

        // Do not infer the queue cardinality from slab count. ConvexBox.escapeVectors deliberately
        // publishes several SAT exits per overlap, so the kernel itself calibrates the two exact
        // boundaries and the live path must reproduce those already-proven fixtures.
        runCase(h,true,8,6,2);
        runCase(h,false,24,6,18);
        h.succeed();
    }

    private static void assertEquivalentOffsetsAreOneCandidate(GameTestHelper h) {
        var body=new AABB(-.05,0,-.05,.05,.1,.05);
        var inner=ConvexBox.of(new AABB(-1,-1,-1,-.04,1,1),new Matrix4f());
        var outer=ConvexBox.of(new AABB(-1,-1,-1,-.03,1,1),new Matrix4f());
        h.assertTrue(inner.overlaps(body) && outer.overlaps(body),
            "A12 alias control requires two genuine nested initial overlaps");
        var result=AnatomySeparation.resolve(body,java.util.List.of(inner,outer),4,64,(box,delta)->delta);
        h.assertTrue(result.separated() && result.displacement().x>.019 && result.displacement().x<.021
                && Math.abs(result.displacement().y)<1e-12 && Math.abs(result.displacement().z)<1e-12,
            "A12 alias control must escape through the outer +X face: "+result);
        h.assertTrue(result.candidates()==3,
            "A12 recovery budget must count physical offsets, not signed-zero/SAT aliases. Two nested slabs require only ZERO, inner +X and outer +X; got "+result);
    }

    private static void runCase(GameTestHelper h,boolean mustRecover,
            double bodyX,double bodyZ,int supportX) {
        var level=h.getLevel();
        var culprit=h.spawn(EntityTypes.COW,supportX,20,2);
        var bystander=h.spawn(EntityTypes.COW,supportX,20,10);
        for(var support:java.util.List.of(culprit,bystander)) {
            support.setNoAi(true);support.setNoGravity(true);
            // The support entities are identity/policy anchors, not extra walls in the recovery
            // puzzle. A tiny body is already well below the default .85 width-ratio limit, so
            // scale 1 keeps them eligible without letting their vanilla AABBs obstruct live clip.
            support.getAttribute(Attributes.SCALE).setBaseValue(1);support.refreshDimensions();
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
        var orderedSlabs=orderedSlabs(slabs);
        var bystanderPiece=bystander(captured,origin);

        if(mustRecover) {
            var oneShort=AnatomySeparation.resolve(captured,orderedSlabs,4,127,(box,delta)->delta);
            var exact=AnatomySeparation.resolve(captured,orderedSlabs,4,128,(box,delta)->delta);
            h.assertTrue(!oneShort.separated() && oneShort.candidates()==127,
                "A12 calibrated fixture must still be unresolved one candidate below the live separation budget: "+oneShort);
            h.assertTrue(exact.separated() && exact.candidates()==128 && exact.displacement().x>0,
                "A12 calibrated fixture must find its first valid escape on candidate 128: "+exact+" slabs="+slabCount);
        } else {
            var capped=AnatomySeparation.resolve(captured,orderedSlabs,4,128,(box,delta)->delta);
            var plusOne=AnatomySeparation.resolve(captured,orderedSlabs,4,129,(box,delta)->delta);
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

            // Prove the culprit is actually reachable through the live spatial index before using
            // collide(). This ray crosses only the nested slabs; the bystander lives outside z=0.
            double midY=origin.y+body.getBbHeight()*.5;
            var indexed=AnatomyMovement.raycast(body,new Vec3(origin.x-1,midY,origin.z),new Vec3(origin.x+1,midY,origin.z));
            h.assertTrue(indexed!=null && indexed.support()==culprit,
                "A12 culprit must be present in the same live spatial index used by own-move; selection="+indexed);

            // Production inserts candidate geometry into a TreeMap keyed by scoped piece id. The
            // culprit was spawned first, so its zero-padded support-id prefix sorts before the
            // bystander, and its slab ids sort lexically. Mirror that order exactly: Map.copyOf
            // intentionally promises no encounter order, which matters at an exact node budget.
            var livePieces=new ArrayList<ConvexBox>(orderedSlabs);livePieces.add(bystanderPiece);
            java.util.function.BiFunction<AABB,Vec3,Vec3> liveClip=(bounds,delta)->
                Entity.collideBoundingBox(body,delta,bounds,level,level.getEntityCollisions(body,bounds.expandTowards(delta)));
            if(mustRecover) {
                var below=AnatomySeparation.resolve(captured,livePieces,4,127,liveClip);
                var exactLive=AnatomySeparation.resolve(captured,livePieces,4,128,liveClip);
                h.assertTrue(!below.separated() && below.candidates()==127
                        && exactLive.separated() && exactLive.candidates()==128 && exactLive.displacement().x>0,
                    "A12 candidate-128 control must remain exact under canonical piece order and the same live clip: below="
                        +below+" exact="+exactLive+" slabs="+slabCount);
            } else {
                var cappedLive=AnatomySeparation.resolve(captured,livePieces,4,128,liveClip);
                var plusOneLive=AnatomySeparation.resolve(captured,livePieces,4,129,liveClip);
                h.assertTrue(!cappedLive.separated() && cappedLive.candidates()==128
                        && plusOneLive.separated() && plusOneLive.candidates()==129 && plusOneLive.displacement().x>0,
                    "A12 candidate-129 control must remain exact under canonical piece order and the same live clip: capped="
                        +cappedLive+" plusOne="+plusOneLive+" slabs="+slabCount);
            }

            var beforeMetrics=AnatomyMovement.sweepMetrics(level);var before=body.position();
            // Candidate 128 clears by exactly the separation skin. A small outward own-move keeps
            // this case focused on the 128-candidate recovery boundary instead of immediately
            // re-entering TemporalResponse's independent t=0 contact/budget machinery. The +1
            // case stays stationary so an unsearched correction can never hide as requested motion.
            Vec3 requested=mustRecover?new Vec3(.05,0,0):Vec3.ZERO;
            Vec3 allowed=AnatomyMovement.collide(body,requested);
            var afterMetrics=AnatomyMovement.sweepMetrics(level);
            boolean culpritSuspended=AnatomyMovement.suspended(body,culprit);
            boolean bystanderSuspended=AnatomyMovement.suspended(body,bystander);
            h.assertTrue(body.position().equals(before),
                "AnatomyMovement.collide must remain query-only at the separation boundary");
            h.assertTrue(afterMetrics.queries()-beforeMetrics.queries()==1,
                "A12 must exercise one live own-move query, not only the isolated separation kernel");

            if(mustRecover) {
                var exact=AnatomySeparation.resolve(captured,livePieces,4,128,liveClip);
                Vec3 expected=exact.displacement().add(requested);
                h.assertTrue(allowed.distanceToSqr(expected)<1e-12,
                    "Candidate 128 must be exported as the complete bounded correction before the outward own-move: kernel="
                        +exact+" requested="+requested+" live="+allowed+" metrics="+afterMetrics
                        +" culpritSuspended="+culpritSuspended+" bystanderSuspended="+bystanderSuspended
                        +" indexed="+indexed);
                h.assertTrue(slabs.values().stream().noneMatch(piece->piece.overlaps(captured.move(allowed))),
                    "Exact-budget recovery plus outward motion must finish clear of every culprit slab");
                h.assertTrue(!culpritSuspended && !bystanderSuspended,
                    "A successful candidate-128 recovery must suspend neither the culprit nor the non-causal bystander");
                h.assertTrue(afterMetrics.pieces()-beforeMetrics.pieces()==slabCount+1,
                    "Exact live response must retain all culprit pieces plus the non-causal bystander after recovery");
            } else {
                h.assertTrue(allowed.lengthSqr()<1e-20,
                    "Candidate 129 is outside the live budget: own-move must fail closed rather than teleport to an unsearched escape, got "+allowed);
                h.assertTrue(culpritSuspended,
                    "The still-overlapping culprit pair must be suspended when candidate 129 is required");
                h.assertTrue(!bystanderSuspended,
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
     * one-slab/one-node relationship. Search the compact fixture family in the same canonical
     * piece-id order used by production, then require an exact transition.
     */
    private static int calibratedSlabCount(AABB body,Vec3 origin,boolean candidate128) {
        for(int count=1;count<=192;count++) {
            var pieces=orderedSlabs(slabs(body,origin,count));
            int low=candidate128?127:128,high=low+1;
            var below=AnatomySeparation.resolve(body,pieces,4,low,(box,delta)->delta);
            var exact=AnatomySeparation.resolve(body,pieces,4,high,(box,delta)->delta);
            if(!below.separated() && below.candidates()==low
                    && exact.separated() && exact.candidates()==high && exact.displacement().x>0)
                return count;
        }
        return -1;
    }

    private static java.util.List<ConvexBox> orderedSlabs(Map<String,ConvexBox> slabs) {
        return java.util.List.copyOf(new TreeMap<>(slabs).values());
    }

    private static Map<String,ConvexBox> slabs(AABB body,Vec3 origin,int count) {
        // Build in entity-local coordinates without subtracting enormous GameTest world
        // coordinates. The mock player's position is X/Z center and Y feet, so dimensions are
        // sufficient and make the exact queue boundary invariant under common translation.
        double minX=-body.getXsize()*.5,maxX=body.getXsize()*.5;
        double minY=0,maxY=body.getYsize();
        double minZ=-body.getZsize()*.5,maxZ=body.getZsize()*.5;
        var result=new TreeMap<String,ConvexBox>();
        for(int i=0;i<count;i++) {
            double right=minX+.02+i*.001;
            var local=new AABB(minX-.8,minY-.6,minZ-.6,right,maxY+.6,maxZ+.6);
            result.put(String.format(java.util.Locale.ROOT,"slab_%03d",i),ConvexBox.of(local,new Matrix4f()).move(origin));
        }
        return Map.copyOf(result);
    }

    private static ConvexBox bystander(AABB body,Vec3 origin) {
        double minX=-body.getXsize()*.5,maxX=body.getXsize()*.5;
        double minY=0,maxY=body.getYsize();
        double maxZ=body.getZsize()*.5;
        return ConvexBox.of(new AABB(minX-.2,minY-.2,maxZ+.20,maxX+.4,maxY+.2,maxZ+.30),new Matrix4f()).move(origin);
    }
}
