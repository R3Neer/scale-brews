package io.github.r3neer.scalebrews.collision.physics;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;

import java.util.function.DoubleFunction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Temporal convex query. Does not mutate entities or resolve gameplay movement yet. */
public final class ConservativeSweep {
    private ConservativeSweep() {}
    /** Shared numerical clearance used by temporal q corrections and CCD contact. */
    public static final double SKIN=1e-6;
    public enum Status { CLEAR, CONTACT, INITIAL_OVERLAP, ITERATION_LIMIT }
    public record Result(Status status,double safeFraction,Vec3 normal,int evaluations) {}
    /**
     * deformationSpeed bounds EVERY material point's velocity after subtracting the declared
     * constant linearTranslation. The two-argument constructor subtracts nothing. This is not
     * an endpoint-distance estimate; the three-argument constructor requires a verified split.
     * Only verified pose/motion providers may supply this bound. A loose bound costs iterations.
     */
    /** Certified support plane, fixed after subtracting the declared root translation. */
    public record Plane(net.minecraft.core.Direction outward,double offset) {
        public Plane{if(outward==null || !Double.isFinite(offset))throw new IllegalArgumentException("Invalid invariant plane");}
        private double minimum(AABB body) {
            var axis=outward.getAxis();return outward.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE?body.min(axis):-body.max(axis);
        }
        private double dot(Vec3 vector){return vector.x*outward.getStepX()+vector.y*outward.getStepY()+vector.z*outward.getStepZ();}
        boolean separates(AABB body,Vec3 relative) {double start=minimum(body);return start>=offset && start+dot(relative)>=offset;}
        Plane move(Vec3 delta){return new Plane(outward,offset+dot(delta));}
    }
    public record Motion(DoubleFunction<ConvexBox> at,double deformationSpeed,Vec3 linearTranslation,java.util.List<Plane> invariantPlanes) {
        public Motion(DoubleFunction<ConvexBox> at,double deformationSpeed,Vec3 linearTranslation){this(at,deformationSpeed,linearTranslation,java.util.List.of());}
        public Motion(DoubleFunction<ConvexBox> at,double maxPointSpeed){this(at,maxPointSpeed,Vec3.ZERO);}
        public Motion {
            invariantPlanes=java.util.List.copyOf(invariantPlanes);
            if(at==null || !Double.isFinite(deformationSpeed) || deformationSpeed<0 || linearTranslation==null || !Double.isFinite(linearTranslation.lengthSqr()))throw new IllegalArgumentException("Invalid motion bound");
        }
        public double maxPointSpeed(){return deformationSpeed+linearTranslation.length();}
        public Motion interval(double start,double end) {
            if(!Double.isFinite(start+end) || start<0 || end>1 || start>end)throw new IllegalArgumentException("Invalid motion interval");
            double duration=end-start;
            return new Motion(t->at.apply(start+t*duration),deformationSpeed*duration,linearTranslation.scale(duration),invariantPlanes.stream().map(p->p.move(linearTranslation.scale(start))).toList());
        }
    }
    public static Result query(AABB body,Vec3 displacement,Motion motion,int maxIterations) {
        if(maxIterations<1 || maxIterations>4096 || !Double.isFinite(displacement.lengthSqr()))throw new IllegalArgumentException("Invalid sweep budget/movement");
        var relative=displacement.subtract(motion.linearTranslation());
        // A provider-certified invariant projection separates the entire trajectories,
        // including tangential contact. Endpoint samples alone never create certificates.
        if(motion.invariantPlanes().stream().anyMatch(p->p.separates(body,relative)))return new Result(Status.CLEAR,1,Vec3.ZERO,1);
        if(motion.deformationSpeed()==0) {
            var box=motion.at().apply(0);var hit=box.sweep(body,relative);
            if(hit==null)return new Result(Status.CLEAR,1,Vec3.ZERO,1);
            return new Result(hit.penetrating()?Status.INITIAL_OVERLAP:Status.CONTACT,hit.fraction(),hit.penetrating()?box.separation(body).normal():hit.normal(),1);
        }
        double speed=motion.deformationSpeed()+relative.length(),t=0;
        if(!Double.isFinite(speed))throw new IllegalArgumentException("Unbounded motion");
        final double skin=SKIN;
        Vec3 normal=Vec3.ZERO;
        for(int iteration=1;iteration<=maxIterations;iteration++) {
            var separation=motion.at().apply(t).separation(body.move(displacement.scale(t)));
            normal=separation.normal();
            if(separation.gap()<-skin && t==0)return new Result(Status.INITIAL_OVERLAP,0,normal,iteration);
            if(separation.gap()<=skin)return new Result(Status.CONTACT,t,normal,iteration);
            if(speed==0 || t>=1)return new Result(Status.CLEAR,1,Vec3.ZERO,iteration);
            double step=(separation.gap()-skin)/speed;
            if(step>1-t)return new Result(Status.CLEAR,1,Vec3.ZERO,iteration);
            if(step<1e-12)return new Result(Status.CONTACT,t,normal,iteration);
            t+=step;
        }
        // This is NOT success: callers must stop here and record the exhausted budget.
        return new Result(Status.ITERATION_LIMIT,t,normal,maxIterations);
    }
}
