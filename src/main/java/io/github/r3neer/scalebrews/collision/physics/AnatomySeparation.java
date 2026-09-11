package io.github.r3neer.scalebrews.collision.physics;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;

import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded shortest-candidate search. Never alters blocks or moves through unrelated geometry. */
public final class AnatomySeparation {
    /*
     * escapeVectors() carries a 1e-6 separation skin. Candidate paths may reach the same
     * physical offset through different SAT-axis/overlap orders, and raw double addition then
     * differs by a few ulps. Exact Vec3 identity lets those numerical aliases fill the bounded
     * queue and consume the observable recovery budget. Keep state identity 100x finer than the
     * skin (and 10x above the SAT epsilon) so aliases collapse before enqueue while physically
     * distinct recovery exits remain distinct.
     */
    private static final double CANDIDATE_QUANTUM=1e-8;
    private AnatomySeparation() {}
    public record Result(boolean separated,Vec3 displacement,int candidates) {}
    public static Result resolve(AABB body,Collection<ConvexBox> pieces,double maxDistance,int budget,
        java.util.function.BiFunction<AABB,Vec3,Vec3> clip) {
        if(!Double.isFinite(maxDistance) || maxDistance<0 || budget<1 || budget>4096)throw new IllegalArgumentException("Invalid separation budget");
        ConvexBox.requireBounds(body);
        if(pieces==null || clip==null)throw new IllegalArgumentException("Missing separation input");
        var initiallyClear=pieces.stream().filter(p->!p.overlaps(body)).toList();
        var queue=new PriorityQueue<Vec3>(Comparator.comparingDouble(Vec3::lengthSqr).thenComparingDouble(v->v.x).thenComparingDouble(v->v.y).thenComparingDouble(v->v.z));
        Set<Vec3> discovered=new HashSet<>();
        discovered.add(Vec3.ZERO);queue.add(Vec3.ZERO);int inspected=0;
        double maxDistanceSqr=maxDistance*maxDistance;
        while(!queue.isEmpty() && inspected<budget) {
            var offset=queue.remove();inspected++;
            if(offset.lengthSqr()>maxDistanceSqr)continue;
            var allowed=clip.apply(body,offset);
            if(allowed==null || !Double.isFinite(allowed.lengthSqr()))return new Result(false,Vec3.ZERO,inspected);
            if(allowed.distanceToSqr(offset)>1e-12)continue;
            boolean crosses=false;
            for(var piece:initiallyClear) {var hit=piece.sweep(body,offset);if(hit!=null && hit.fraction()<1-1e-8){crosses=true;break;}}
            if(crosses)continue;
            var moved=body.move(offset);var overlapping=pieces.stream().filter(p->p.overlaps(moved)).toList();
            if(overlapping.isEmpty())return new Result(true,offset,inspected);
            for(var piece:overlapping)for(var escape:piece.escapeVectors(moved)) {
                var raw=offset.add(escape);
                if(raw.lengthSqr()>maxDistanceSqr)continue;
                var next=canonicalCandidate(raw);
                if(next.lengthSqr()<=maxDistanceSqr && !discovered.contains(next) && queue.size()<budget*32) {
                    discovered.add(next);queue.add(next);
                }
            }
        }
        return new Result(false,Vec3.ZERO,inspected);
    }
    private static Vec3 canonicalCandidate(Vec3 value) {
        return new Vec3(canonicalComponent(value.x),canonicalComponent(value.y),canonicalComponent(value.z));
    }
    private static double canonicalComponent(double value) {
        double snapped=Math.rint(value/CANDIDATE_QUANTUM)*CANDIDATE_QUANTUM;
        return snapped==0?0:snapped;
    }
}
