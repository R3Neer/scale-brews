package io.github.r3neer.scalebrews.collision.physics;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;

import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded shortest-candidate search. Never alters blocks or moves through unrelated geometry. */
public final class AnatomySeparation {
    private AnatomySeparation() {}
    public record Result(boolean separated,Vec3 displacement,int candidates) {}
    public static Result resolve(AABB body,Collection<ConvexBox> pieces,double maxDistance,int budget,
        java.util.function.BiFunction<AABB,Vec3,Vec3> clip) {
        if(!Double.isFinite(maxDistance) || maxDistance<0 || budget<1 || budget>4096)throw new IllegalArgumentException("Invalid separation budget");
        ConvexBox.requireBounds(body);
        if(pieces==null || clip==null)throw new IllegalArgumentException("Missing separation input");
        var initiallyClear=pieces.stream().filter(p->!p.overlaps(body)).toList();
        var queue=new PriorityQueue<Vec3>(Comparator.comparingDouble(Vec3::lengthSqr).thenComparingDouble(v->v.x).thenComparingDouble(v->v.y).thenComparingDouble(v->v.z));
        Set<Vec3> visited=new HashSet<>();queue.add(Vec3.ZERO);int inspected=0;
        while(!queue.isEmpty() && inspected<budget) {
            var offset=queue.remove();if(!visited.add(offset))continue;inspected++;
            if(offset.lengthSqr()>maxDistance*maxDistance)continue;
            var allowed=clip.apply(body,offset);
            if(allowed==null || !Double.isFinite(allowed.lengthSqr()))return new Result(false,Vec3.ZERO,inspected);
            if(allowed.distanceToSqr(offset)>1e-12)continue;
            boolean crosses=false;
            for(var piece:initiallyClear) {var hit=piece.sweep(body,offset);if(hit!=null && hit.fraction()<1-1e-8){crosses=true;break;}}
            if(crosses)continue;
            var moved=body.move(offset);var overlapping=pieces.stream().filter(p->p.overlaps(moved)).toList();
            if(overlapping.isEmpty())return new Result(true,offset,inspected);
            for(var piece:overlapping)for(var escape:piece.escapeVectors(moved)) {
                var next=offset.add(escape);
                if(next.lengthSqr()<=maxDistance*maxDistance && !visited.contains(next) && queue.size()<budget*32)queue.add(next);
            }
        }
        return new Result(false,Vec3.ZERO,inspected);
    }
}
