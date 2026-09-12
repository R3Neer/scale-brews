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
    private static final int MAX_Q_PROJECTIONS=4,MAX_BISECTIONS=8,PROBE_QUERY_BUDGET=8,SCREEN_DEPTH=8;
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
    /** An active material plane, never a chosen carry vector. */
    private record Constraint(ConservativeSweep.Motion motion,Vec3 normal) {}
    private record RelativeConstraint(Vec3 normal,double minimumAdvance) {}
    private record Proposal(double end,Vec3 delta) {}
    private record Uncovered(double start,double end) {}

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
            // The first contact projects d through the active relative manifold.
            // q exists only when that certified d is immediately recontacted.
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
            // A known first contact bounds the only part of later pieces that can still change this
            // event. Include the full simultaneous-contact tolerance instead of cutting at earliest:
            // the previous exact-horizon attempt dropped contacts at earliest + epsilon.
            double horizon=Double.isFinite(earliest)?Math.min(1,Math.nextUp(earliest+TIME_EPS)):1;
            var interval=pieces.get(id).interval(start,start+(end-start)*horizon);
            Vec3 queryDelta=delta.scale(horizon);
            // Screening is only for unknown candidates. Once a piece is active, its relevance has
            // already been established; re-screening it would repeatedly tax the same real contact.
            var result=constraint==null?firstForPiece(body,queryDelta,interval,budget):budget.query(body,queryDelta,interval);
            if(horizon<1)result=new ConservativeSweep.Result(result.status(),
                Math.clamp(result.safeFraction(),0,1)*horizon,result.normal(),result.evaluations());
            if(result.status()==ConservativeSweep.Status.ITERATION_LIMIT)return new Search(result.status(),result.safeFraction(),List.of());
            if(result.status()==ConservativeSweep.Status.INITIAL_OVERLAP)return new Search(result.status(),0,List.of());
            if(result.status()!=ConservativeSweep.Status.CONTACT)continue;
            if(result.safeFraction()<earliest-TIME_EPS){earliest=result.safeFraction();hits.clear();}
            if(Math.abs(result.safeFraction()-earliest)<=TIME_EPS)hits.add(new Hit(id,result));
        }
        return hits.isEmpty()?Search.clear():new Search(ConservativeSweep.Status.CONTACT,earliest,List.copyOf(hits));
    }

    /**
     * Cheap CCD is cheaper than screening for many ordinary pieces. Probe first with a deliberately
     * small local allowance: a conclusive CLEAR/contact is final, while a local ITERATION_LIMIT only
     * means "not cheap" and falls through to certified temporal screening. A trajectory that cannot
     * be certified fully clear is then narrowed chronologically before exact CCD spends the shared
     * remainder, so a real interior contact does not restart from the whole tick.
     */
    private static ConservativeSweep.Result firstForPiece(AABB body,Vec3 delta,ConservativeSweep.Motion interval,Budget budget) {
        if(interval.deformationSpeed()==0)return budget.query(body,delta,interval);
        var probe=budget.query(body,delta,interval,PROBE_QUERY_BUDGET);
        if(probe.status()!=ConservativeSweep.Status.ITERATION_LIMIT || budget.exhausted())return probe;
        Boolean possible=mayIntersect(body,delta,interval,budget);
        if(possible==null)return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,0,Vec3.ZERO,0);
        if(!possible)return new ConservativeSweep.Result(ConservativeSweep.Status.CLEAR,1,Vec3.ZERO,0);
        Boolean clear=certifiedClearTrajectory(body,delta,interval,budget);
        if(clear==null)return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,0,Vec3.ZERO,0);
        if(clear)return new ConservativeSweep.Result(ConservativeSweep.Status.CLEAR,1,Vec3.ZERO,0);
        return firstWindow(body,delta,interval,0,0,1,budget);
    }

    /**
     * Chronological fallback for an interval that the global clear screen proved ambiguous. Every
     * earlier sibling must be certified clear before the search enters a later sibling. Only the
     * final depth-bounded ambiguous leaf pays exact CCD, and its result is mapped back to the parent
     * interval. There is no local CCD cap: the only normative limit remains the shared response budget.
     */
    private static ConservativeSweep.Result firstWindow(AABB body,Vec3 delta,ConservativeSweep.Motion interval,
            int depth,double origin,double scale,Budget budget) {
        Boolean clear=certifiedClearWindow(body,delta,interval,budget);
        if(clear==null)return new ConservativeSweep.Result(ConservativeSweep.Status.ITERATION_LIMIT,origin,Vec3.ZERO,0);
        if(clear)return new ConservativeSweep.Result(ConservativeSweep.Status.CLEAR,1,Vec3.ZERO,0);
        if(depth>=SCREEN_DEPTH) {
            var result=budget.query(body,delta,interval);
            double fraction=origin+scale*Math.clamp(result.safeFraction(),0,1);
            var status=result.status();
            // Every earlier sibling was certified clear, so an overlap at a later leaf start is the
            // boundary contact of the continuous trajectory rather than a global initial overlap.
            if(status==ConservativeSweep.Status.INITIAL_OVERLAP && origin>TIME_EPS)
                status=ConservativeSweep.Status.CONTACT;
            return new ConservativeSweep.Result(status,fraction,result.normal(),result.evaluations());
        }
        Vec3 halfDelta=delta.scale(.5);
        var left=firstWindow(body,halfDelta,interval.interval(0,.5),depth+1,origin,scale*.5,budget);
        if(left.status()!=ConservativeSweep.Status.CLEAR)return left;
        var right=firstWindow(body.move(halfDelta),halfDelta,interval.interval(.5,1),depth+1,
            origin+scale*.5,scale*.5,budget);
        return right.status()==ConservativeSweep.Status.CLEAR
            ?new ConservativeSweep.Result(ConservativeSweep.Status.CLEAR,1,Vec3.ZERO,0)
            :right;
    }

    /**
     * Midpoint separating-plane certificate for one already time-aligned child window. Translation
     * is compared in the material-relative frame, so the declared linear translation is not counted
     * twice. The residual deformation bound plus projected relative body motion bounds how much the
     * sampled separating plane can worsen from the midpoint to either edge.
     */
    private static Boolean certifiedClearWindow(AABB body,Vec3 delta,ConservativeSweep.Motion interval,Budget budget) {
        if(!budget.sample())return null;
        var material=interval.at().apply(.5);
        if(material==null)return false;
        var separation=material.separation(body.move(delta.scale(.5)));
        Vec3 normal=separation.normal();
        if(!Double.isFinite(separation.gap()) || !Double.isFinite(normal.lengthSqr())
                || Math.abs(normal.lengthSqr()-1)>1e-8)return false;
        Vec3 relative=delta.subtract(interval.linearTranslation());
        double rate=interval.deformationSpeed()+Math.abs(relative.dot(normal));
        if(!Double.isFinite(rate) || rate<0)return false;
        double adverse=.5*rate;
        return separation.gap()-adverse>ConservativeSweep.SKIN;
    }

    /**
     * Cover [0,1] with neighborhoods certified by sampled separating planes. At sample time t, the
     * fixed plane gap can close no faster than residual deformation plus the relative declared
     * translation projected on that normal. Thus (gap-SKIN)/rate is a conservative temporal radius.
     * Largest uncovered windows are sampled first for deterministic, useful progress under a hard budget.
     * {@code null} means the shared budget itself was exhausted; {@code false} means screening cannot
     * prove clearance and the caller must fall back to exact CCD.
     */
    private static Boolean certifiedClearTrajectory(AABB body,Vec3 delta,ConservativeSweep.Motion interval,Budget budget) {
        var pending=new PriorityQueue<Uncovered>(Comparator
            .comparingDouble((Uncovered window)->window.end()-window.start()).reversed()
            .thenComparingDouble(Uncovered::start));
        pending.add(new Uncovered(0,1));
        Vec3 relative=delta.subtract(interval.linearTranslation());
        while(!pending.isEmpty()) {
            var window=pending.remove();
            if(window.end()-window.start()<=TIME_EPS)continue;
            if(!budget.sample())return null;
            double time=(window.start()+window.end())*.5;
            var material=interval.at().apply(time);
            if(material==null)return false;
            var separation=material.separation(body.move(delta.scale(time)));
            Vec3 normal=separation.normal();
            if(!Double.isFinite(separation.gap()) || !Double.isFinite(normal.lengthSqr())
                    || Math.abs(normal.lengthSqr()-1)>1e-8)return false;
            if(separation.gap()<=ConservativeSweep.SKIN)return false;
            double rate=interval.deformationSpeed()+Math.abs(relative.dot(normal));
            if(!Double.isFinite(rate) || rate<0)return false;
            if(rate<=1e-20)continue;
            double radius=(separation.gap()-ConservativeSweep.SKIN)/rate;
            if(!Double.isFinite(radius) || radius<=TIME_EPS)return false;
            double leftEnd=Math.min(window.end(),time-radius);
            if(leftEnd>window.start()+TIME_EPS)pending.add(new Uncovered(window.start(),leftEnd));
            double rightStart=Math.max(window.start(),time+radius);
            if(window.end()>rightStart+TIME_EPS)pending.add(new Uncovered(rightStart,window.end()));
        }
        return true;
    }

    /**
     * Cheap certified broadphase for the whole piece interval. Every material point stays within
     * maxPointSpeed of its start, while the body stays inside its translational swept AABB.
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

    /**
     * Full-interval fixed-plane certificate for an already active contact.
     */
    private static boolean certifies(AABB body,Vec3 finalDelta,Constraint constraint,double start,double end) {
        Vec3 normal=constraint.normal();
        if(!Double.isFinite(normal.lengthSqr()) || Math.abs(normal.lengthSqr()-1)>1e-8)return false;
        var current=constraint.motion().at().apply(start);
        if(minimum(body,normal)-maximum(current,normal)<ConservativeSweep.SKIN)return false;
        var interval=constraint.motion().interval(start,end);
        double maximumAdvance=minimumAdvance(body,interval,normal);
        return finalDelta.dot(normal)>=maximumAdvance;
    }

    /** Bounded feasible half-space projection; this is not claimed to be an exact QP minimizer. */
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

    /** q is a true route: full block clip and every instantaneous convex must clear it. */
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

    /** d projects against every active relative constraint into a bounded feasible set. */
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

    /** Exact cardinal match and exterior AABB only: an outer plane is not a current surface. */
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

    /** Contact hits already paid their CCD evaluation; activate only retains their constraint. */
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
