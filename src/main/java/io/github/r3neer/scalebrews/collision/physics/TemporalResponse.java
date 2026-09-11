package io.github.r3neer.scalebrews.collision.physics;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;

import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Event response which preserves material time. {@code q} is only a bounded
 * numerical manifold separation; {@code d} is reswept continuously before use.
 */
public final class TemporalResponse {
    private TemporalResponse() {}
    public enum Status { COMPLETE, INITIAL_OVERLAP, ITERATION_LIMIT }
    public record Contact(String piece,double time,Vec3 normal) {}
    public record Result(Status status,Vec3 displacement,double time,List<Contact> contacts,int evaluations) {
        public Result {contacts=List.copyOf(contacts);}
    }
    private static final double TIME_EPS=1e-10;
    private static final double Q_MIN=4*ConservativeSweep.SKIN,Q_MAX=8*ConservativeSweep.SKIN;
    private static final double TARGET_WINDOW_DEFORMATION=.5;
    private static final int MAX_Q_PROJECTIONS=4,MAX_BISECTIONS=8,TARGET_WINDOW_DEPTH=5,MAX_WINDOW_DEPTH=8,LOCAL_QUERY_BUDGET=24;
    private static final class Budget {
        private int remaining,used;
        Budget(int limit) {remaining=limit;}
        ConservativeSweep.Result query(AABB body,Vec3 delta,ConservativeSweep.Motion motion) {
            return query(body,delta,motion,remaining);
        }
        ConservativeSweep.Result query(AABB body,Vec3 delta,ConservativeSweep.Motion motion,int localLimit) {
            if(remaining<1)return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,0,Vec3.ZERO,0);
            int allowance=Math.min(remaining,Math.max(1,localLimit));
            var result=ConservativeSweep.query(body,delta,motion,allowance);
            remaining-=result.evaluations();used+=result.evaluations();return result;
        }
        /** Geometry sampled outside ConservativeSweep still consumes this response budget. */
        boolean sample(){if(remaining<1)return false;remaining--;used++;return true;}
        boolean exhausted(){return remaining<1;}
    }
    private record Hit(String piece,ConservativeSweep.Result result) {}
    private record Search(ConservativeSweep.Status status,double fraction,List<Hit> hits) {
        static Search clear(){return new Search(ConservativeSweep.Status.CLEAR,1,List.of());}
    }
    private record Constraint(ConservativeSweep.Motion motion,Vec3 normal) {}
    private record RelativeConstraint(Vec3 normal,double minimumAdvance) {}
    private record Proposal(double end,Vec3 delta) {}

    /** Pure/kernel view retains the certified prefix on exhaustion for diagnosis and composition tests. */
    public static Result resolve(AABB body,Vec3 requested,Map<String,ConservativeSweep.Motion> pieces,int events,int queryBudget) {
        return resolveRaw(body,requested,pieces,events,queryBudget,(box,delta)->delta);
    }

    /**
     * Live integration boundary. An incomplete temporal solve may report a certified prefix internally,
     * but callers that can mutate world state must not export that prefix or contacts derived from it.
     */
    public static Result resolve(AABB body,Vec3 requested,Map<String,ConservativeSweep.Motion> pieces,int events,int queryBudget,
            java.util.function.BiFunction<AABB,Vec3,Vec3> clip) {
        var result=resolveRaw(body,requested,pieces,events,queryBudget,clip);
        return result.status()==Status.ITERATION_LIMIT
            ?new Result(Status.ITERATION_LIMIT,Vec3.ZERO,result.time(),List.of(),result.evaluations())
            :result;
    }

    private static Result resolveRaw(AABB body,Vec3 requested,Map<String,ConservativeSweep.Motion> pieces,int events,int queryBudget,
            java.util.function.BiFunction<AABB,Vec3,Vec3> clip) {
        ConvexBox.requireBounds(body);
        if(requested==null || pieces==null)throw new IllegalArgumentException("Missing response input");
        if(events<1 || events>256 || queryBudget<1 || queryBudget>4096 || !Double.isFinite(requested.lengthSqr()) || clip==null)
            throw new IllegalArgumentException("Invalid response budget");
        var ids=new TreeSet<>(pieces.keySet());
        if(ids.stream().anyMatch(id->pieces.get(id)==null))throw new IllegalArgumentException("Null motion");
        var budget=new Budget(queryBudget);List<Contact> contacts=new ArrayList<>();
        Vec3 moved=Vec3.ZERO;double time=0,correctedAt=Double.NaN,lastContactAt=Double.NaN;
        var active=new TreeMap<String,Constraint>();
        for(int event=0;event<events;event++) {
            if(!prune(body.move(moved),active,time,budget))return limit(moved,time,contacts,budget);
            var full=proposal(body.move(moved),requested,active.values(),time,1,clip);
            if(full==null)return limit(moved,time,contacts,budget);
            var search=first(body.move(moved),full.delta(),pieces,ids,active,time,full.end(),budget);
            if(search.status()==ConservativeSweep.Status.ITERATION_LIMIT)return limit(moved,time,contacts,budget);
            if(search.status()==ConservativeSweep.Status.INITIAL_OVERLAP)
                return new Result(time<=TIME_EPS?Status.INITIAL_OVERLAP:Status.ITERATION_LIMIT,moved,time,contacts,budget.used);
            if(search.status()==ConservativeSweep.Status.CLEAR)
                return new Result(Status.COMPLETE,moved.add(full.delta()),1,contacts,budget.used);
            double fraction=Math.clamp(search.fraction(),0,1);
            moved=moved.add(full.delta().scale(fraction));time=time+(full.end()-time)*fraction;
            for(var hit:search.hits())contacts.add(new Contact(hit.piece(),time,hit.result().normal()));
            activate(search.hits(),pieces,active);
            if(fraction<=TIME_EPS && Math.abs(lastContactAt-time)<=TIME_EPS) {
                if(Math.abs(correctedAt-time)<=TIME_EPS) {
                    var prefix=certifiedPrefix(body.move(moved),requested,active,time,pieces,ids,clip,budget);
                    if(prefix==null)return limit(moved,time,contacts,budget);
                    moved=moved.add(prefix.delta());time=prefix.end();correctedAt=Double.NaN;
                    if(time>=1-TIME_EPS)return new Result(Status.COMPLETE,moved,1,contacts,budget.used);
                    continue;
                }
                Vec3 q=separate(active.values());
                AABB contactBox=body.move(moved);
                if(q==null || !validCorrection(contactBox,q,pieces,ids,time,clip,budget))return limit(moved,time,contacts,budget);
                moved=moved.add(q);correctedAt=time;
                continue;
            }
            lastContactAt=time;correctedAt=Double.NaN;
            if(time>=1-TIME_EPS)return new Result(Status.COMPLETE,moved,1,contacts,budget.used);
        }
        return limit(moved,time,contacts,budget);
    }

    private static Search first(AABB body,Vec3 delta,Map<String,ConservativeSweep.Motion> pieces,SortedSet<String> ids,
            Map<String,Constraint> active,double start,double end,Budget budget) {
        double earliest=Double.POSITIVE_INFINITY;List<Hit> hits=new ArrayList<>();
        for(var id:ids) {
            var constraint=active.get(id);
            if(constraint!=null) {
                if(!budget.sample())return new Search(ConservativeSweep.Status.ITERATION_LIMIT,0,List.of());
                if(certifies(body,delta,constraint,start,end))continue;
            }
            var result=firstForPiece(body,delta,pieces.get(id).interval(start,end),budget);
            if(result.status()==ConservativeSweep.Status.ITERATION_LIMIT)return new Search(result.status(),result.safeFraction(),List.of());
            if(result.status()==ConservativeSweep.Status.INITIAL_OVERLAP)return new Search(result.status(),0,List.of());
            if(result.status()!=ConservativeSweep.Status.CONTACT)continue;
            if(result.safeFraction()<earliest-TIME_EPS){earliest=result.safeFraction();hits.clear();}
            if(Math.abs(result.safeFraction()-earliest)<=TIME_EPS)hits.add(new Hit(id,result));
        }
        return hits.isEmpty()?Search.clear():new Search(ConservativeSweep.Status.CONTACT,earliest,List.copyOf(hits));
    }

    /**
     * A loose deformation bound can make a physically clear piece consume almost the entire shared
     * response budget. First reject the whole interval by a certified envelope. If it still can
     * reach the body, recursively split only the possible temporal windows until the deformation
     * bound is small enough for a capped CCD query. A local cap is not a global failure: an
     * unresolved window is refined further while the shared budget still has evidence left to buy.
     */
    private static ConservativeSweep.Result firstForPiece(AABB body,Vec3 delta,ConservativeSweep.Motion interval,Budget budget) {
        Boolean possible=mayIntersect(body,delta,interval,budget);
        if(possible==null)return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,0,Vec3.ZERO,0);
        if(!possible)return new ConservativeSweep.Result(ConservativeSweep.Status.CLEAR,1,Vec3.ZERO,0);
        return firstPossibleWindow(body,delta,interval,0,0,1,budget);
    }

    /** Result safeFraction is always expressed in the original piece interval, not this child window. */
    private static ConservativeSweep.Result firstPossibleWindow(AABB body,Vec3 delta,ConservativeSweep.Motion interval,
            int depth,double origin,double scale,Budget budget) {
        boolean shouldSplit=depth<TARGET_WINDOW_DEPTH && interval.deformationSpeed()>TARGET_WINDOW_DEFORMATION;
        if(!shouldSplit) {
            var result=budget.query(body,delta,interval,LOCAL_QUERY_BUDGET);
            if(result.status()!=ConservativeSweep.Status.ITERATION_LIMIT || budget.exhausted() || depth>=MAX_WINDOW_DEPTH)
                return mapWindowResult(result,origin,scale);
            // Exhausting a local allowance proves only that this window is still too coarse.
            // Preserve the global budget and refine this one branch instead of quarantining the event.
        }
        if(depth>=MAX_WINDOW_DEPTH)
            return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,origin,Vec3.ZERO,0);

        var halfDelta=delta.scale(.5);
        var left=interval.interval(0,.5);
        Boolean possible=mayIntersect(body,halfDelta,left,budget);
        if(possible==null)return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,origin,Vec3.ZERO,0);
        if(possible) {
            var hit=firstPossibleWindow(body,halfDelta,left,depth+1,origin,scale*.5,budget);
            if(hit.status()!=ConservativeSweep.Status.CLEAR)return hit;
        }

        var right=interval.interval(.5,1);
        AABB rightBody=body.move(halfDelta);
        possible=mayIntersect(rightBody,halfDelta,right,budget);
        if(possible==null)return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,origin+scale*.5,Vec3.ZERO,0);
        if(possible) {
            var hit=firstPossibleWindow(rightBody,halfDelta,right,depth+1,origin+scale*.5,scale*.5,budget);
            if(hit.status()!=ConservativeSweep.Status.CLEAR)return hit;
        }
        return new ConservativeSweep.Result(ConservativeSweep.Status.CLEAR,1,Vec3.ZERO,0);
    }

    private static ConservativeSweep.Result mapWindowResult(ConservativeSweep.Result result,double origin,double scale) {
        double fraction=origin+scale*Math.clamp(result.safeFraction(),0,1);
        var status=result.status();
        // Chronological left windows were already certified clear, so an overlap at a later child
        // start is the boundary contact of this continuous trajectory, not a global initial overlap.
        if(status==ConservativeSweep.Status.INITIAL_OVERLAP && origin>TIME_EPS)status=ConservativeSweep.Status.CONTACT;
        return new ConservativeSweep.Result(status,fraction,result.normal(),result.evaluations());
    }

    /**
     * Certified envelope for one time-aligned material/body window. Every material point stays
     * within maxPointSpeed of the window-start convex, and the body remains inside its matching
     * translational swept AABB. Geometry sampling is charged to the same shared response budget.
     */
    private static Boolean mayIntersect(AABB body,Vec3 delta,ConservativeSweep.Motion interval,Budget budget) {
        if(!budget.sample())return null;
        var first=interval.at().apply(0);
        double margin=interval.maxPointSpeed()+ConservativeSweep.SKIN;
        if(first==null || !Double.isFinite(margin))throw new IllegalArgumentException("Invalid material broadphase envelope");
        var material=first.bounds().inflate(margin);
        var sweptBody=body.expandTowards(delta).inflate(ConservativeSweep.SKIN);
        return material.intersects(sweptBody);
    }

    private static boolean certifies(AABB body,Vec3 finalDelta,Constraint constraint,double start,double end) {
        Vec3 normal=constraint.normal();
        if(!Double.isFinite(normal.lengthSqr()) || Math.abs(normal.lengthSqr()-1)>1e-8)return false;
        var current=constraint.motion().at().apply(start);
        if(minimum(body,normal)-maximum(current,normal)<ConservativeSweep.SKIN)return false;
        var interval=constraint.motion().interval(start,end);
        double maximumAdvance=minimumAdvance(body,interval,normal);
        return finalDelta.dot(normal)>=maximumAdvance;
    }

    private static Vec3 separate(Collection<Constraint> constraints) {
        Vec3 q=Vec3.ZERO;
        for(int pass=0;pass<MAX_Q_PROJECTIONS;pass++)for(var constraint:constraints) {
            Vec3 normal=constraint.normal();
            if(normal.lengthSqr()<1e-20 || !Double.isFinite(normal.lengthSqr()))return null;
            double deficit=Q_MIN-q.dot(normal);
            if(deficit>0)q=q.add(normal.scale(deficit));
            if(q.lengthSqr()>Q_MAX*Q_MAX)return null;
        }
        for(var constraint:constraints)if(q.dot(constraint.normal())<Q_MIN-TIME_EPS)return null;
        return q;
    }

    private static boolean validCorrection(AABB body,Vec3 q,Map<String,ConservativeSweep.Motion> pieces,SortedSet<String> ids,
            double time,java.util.function.BiFunction<AABB,Vec3,Vec3> clip,Budget budget) {
        Vec3 clipped=clip.apply(body,q);
        if(clipped==null || !Double.isFinite(clipped.lengthSqr()) || clipped.distanceToSqr(q)>1e-18)return false;
        AABB after=body.move(q);
        for(var id:ids) {
            var motion=pieces.get(id).interval(time,time);
            if(!budget.sample())return false;
            if(motion.at().apply(0).separation(after).gap()<ConservativeSweep.SKIN)return false;
            if(budget.query(body,q,motion).status()!=ConservativeSweep.Status.CLEAR)return false;
        }
        return true;
    }

    private static Proposal proposal(AABB body,Vec3 requested,Collection<Constraint> active,double start,double end,
            java.util.function.BiFunction<AABB,Vec3,Vec3> clip) {
        Vec3 delta=requested.scale(end-start);
        var relative=new ArrayList<RelativeConstraint>();
        for(var constraint:active) {
            var interval=constraint.motion().interval(start,end);
            double bound=minimumAdvance(body,interval,constraint.normal());
            relative.add(new RelativeConstraint(constraint.normal(),bound));
        }
        for(int pass=0;pass<MAX_Q_PROJECTIONS;pass++)for(var constraint:relative) {
            double deficit=constraint.minimumAdvance()-delta.dot(constraint.normal());
            if(deficit>0)delta=delta.add(constraint.normal().scale(deficit));
        }
        Vec3 clipped=clip.apply(body,delta);
        if(clipped==null || !Double.isFinite(clipped.lengthSqr()))return null;
        for(var constraint:relative)if(clipped.dot(constraint.normal())<constraint.minimumAdvance()-TIME_EPS)return null;
        return new Proposal(end,clipped);
    }

    private static double minimumAdvance(AABB body,ConservativeSweep.Motion interval,Vec3 normal) {
        for(var plane:interval.invariantPlanes()) {
            var direction=plane.outward();
            if(normal.x==direction.getStepX() && normal.y==direction.getStepY() && normal.z==direction.getStepZ()
                && plane.separates(body,Vec3.ZERO))
                return interval.linearTranslation().dot(normal);
        }
        return interval.deformationSpeed()+interval.linearTranslation().dot(normal);
    }

    private static Proposal certifiedPrefix(AABB body,Vec3 requested,Map<String,Constraint> active,double start,
            Map<String,ConservativeSweep.Motion> pieces,SortedSet<String> ids,
            java.util.function.BiFunction<AABB,Vec3,Vec3> clip,Budget budget) {
        for(int split=1;split<=MAX_BISECTIONS;split++) {
            double end=start+(1-start)/Math.pow(2,split);var proposal=proposal(body,requested,active.values(),start,end,clip);
            if(proposal==null)return null;
            var search=first(body,proposal.delta(),pieces,ids,active,start,end,budget);
            if(search.status()==ConservativeSweep.Status.CLEAR)return proposal;
            if(search.status()==ConservativeSweep.Status.ITERATION_LIMIT || budget.exhausted())return null;
        }
        return null;
    }

    private static void activate(List<Hit> hits,Map<String,ConservativeSweep.Motion> pieces,Map<String,Constraint> active) {
        for(var hit:hits)active.put(hit.piece(),new Constraint(pieces.get(hit.piece()),hit.result().normal()));
    }

    private static boolean prune(AABB body,Map<String,Constraint> active,double time,Budget budget) {
        var iterator=active.entrySet().iterator();
        while(iterator.hasNext()) {
            var constraint=iterator.next().getValue();
            if(!budget.sample())return false;
            if(constraint.motion().at().apply(time).separation(body).gap()>Q_MAX)iterator.remove();
        }
        return true;
    }

    private static double minimum(AABB body,Vec3 normal) {
        double center=body.getCenter().dot(normal);
        double radius=(body.getXsize()*Math.abs(normal.x)+body.getYsize()*Math.abs(normal.y)+body.getZsize()*Math.abs(normal.z))*.5;
        return center-radius;
    }

    private static double maximum(ConvexBox body,Vec3 normal) {
        double maximum=Double.NEGATIVE_INFINITY;
        for(var vertex:body.vertices())maximum=Math.max(maximum,vertex.dot(normal));
        return maximum;
    }

    private static Result limit(Vec3 moved,double time,List<Contact> contacts,Budget budget) {
        return new Result(Status.ITERATION_LIMIT,moved,time,contacts,budget.used);
    }
}
