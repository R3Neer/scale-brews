package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Event-based response, preserving shape time while rechecking every changed body trajectory. */
public final class TemporalResponse {
    private TemporalResponse() {}
    public enum Status { COMPLETE, INITIAL_OVERLAP, ITERATION_LIMIT }
    public record Contact(String piece,double time,Vec3 normal) {}
    public record Result(Status status,Vec3 displacement,double time,List<Contact> contacts,int evaluations) {
        public Result{contacts=List.copyOf(contacts);}
    }
    public static Result resolve(AABB body,Vec3 requested,Map<String,ConservativeSweep.Motion> pieces,int events,int queryBudget) {
        return resolve(body,requested,pieces,events,queryBudget,(box,delta)->delta);
    }
    public static Result resolve(AABB body,Vec3 requested,Map<String,ConservativeSweep.Motion> pieces,int events,int queryBudget,
        java.util.function.BiFunction<AABB,Vec3,Vec3> clip) {
        if(events<1 || events>256 || !Double.isFinite(requested.lengthSqr()))throw new IllegalArgumentException("Invalid response budget");
        var ids=new TreeSet<>(pieces.keySet());
        List<Contact> contacts=new ArrayList<>();Vec3 moved=Vec3.ZERO,left=requested;double time=0;int evaluations=0;
        for(int event=0;event<events;event++) {
            left=clip.apply(body.move(moved),left);
            String best=null;ConservativeSweep.Result first=null;
            for(var id:ids) {
                var hit=ConservativeSweep.query(body.move(moved),left,pieces.get(id).interval(time,1),queryBudget);evaluations+=hit.evaluations();
                if(hit.status()==ConservativeSweep.Status.CLEAR)continue;
                if(first==null || hit.safeFraction()<first.safeFraction()-1e-9){first=hit;best=id;}
            }
            if(first==null)return new Result(Status.COMPLETE,moved.add(left),1,contacts,evaluations);
            if(first.status()==ConservativeSweep.Status.INITIAL_OVERLAP)return new Result(Status.INITIAL_OVERLAP,moved,time,contacts,evaluations);
            var advance=left.scale(first.safeFraction());moved=moved.add(advance);
            time+=first.safeFraction()*(1-time);left=left.scale(1-first.safeFraction());
            if(first.status()==ConservativeSweep.Status.ITERATION_LIMIT)return new Result(Status.ITERATION_LIMIT,moved,time,contacts,evaluations);
            var normal=first.normal();contacts.add(new Contact(best,time,normal));
            var motion=pieces.get(best);var atContact=motion.at().apply(time);
            int face=atContact.closestFace(normal);
            var local=atContact.facePoint(face,body.move(moved).getCenter());
            var carried=motion.at().apply(1).point(local).subtract(atContact.point(local));
            double inward=left.subtract(carried).dot(normal);
            if(inward<0)left=left.subtract(normal.scale(inward));
            // No blind position nudges. An unresolved curved/simultaneous contact exhausts the
            // explicit budget instead of advancing through geometry or inventing a teleport.
            if(time>=1)return new Result(Status.COMPLETE,moved,1,contacts,evaluations);
        }
        return new Result(Status.ITERATION_LIMIT,moved,time,contacts,evaluations);
    }
}
