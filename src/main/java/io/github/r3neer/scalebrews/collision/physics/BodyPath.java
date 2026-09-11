package io.github.r3neer.scalebrews.collision.physics;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Conservative upright body-centre path for one existing material face anchor.
 * It is a query utility only: it neither mutates an entity nor creates receipts.
 */
public final class BodyPath {
    private static final double SUPPORT=Math.sqrt(.5);
    /** Strict Q2a coordinates: tolerant renderer rounding is deliberately unsupported here. */
    public record LocalAnchor(int face,Vec3 coordinates) {
        public LocalAnchor {
            if(face<0 || face>5 || coordinates==null || !Double.isFinite(coordinates.lengthSqr())
                    || coordinates.x<0 || coordinates.x>1 || coordinates.y<0 || coordinates.y>1 || coordinates.z<0 || coordinates.z>1)
                throw new IllegalArgumentException("Anchor must be exactly inside its material face");
            double onFace=switch(face/2){case 0->coordinates.x;case 1->coordinates.y;default->coordinates.z;};
            if(onFace!=(face%2==0?0:1))throw new IllegalArgumentException("Anchor is not on its declared material face");
        }
    }
    /** Bound certified by the same interval/piece/face evaluator, never endpoint sampling. */
    public record NormalBounds(int face,double minDotAntiGravity,double normalRateBound) {
        public NormalBounds {
            if(face<0 || face>5 || !Double.isFinite(minDotAntiGravity) || minDotAntiGravity<SUPPORT || minDotAntiGravity>1 || !Double.isFinite(normalRateBound) || normalRateBound<0)
                throw new IllegalArgumentException("Invalid normal bounds");
        }
    }
    private final ConservativeSweep.Motion material;
    private final LocalAnchor anchor;
    private final AABB body;
    private final Vec3 center,up,initialOffset;
    private final double initialGap,liftRate,residualSpeed,minDot;

    private BodyPath(ConservativeSweep.Motion material,LocalAnchor anchor,AABB body,Vec3 up,Vec3 point0,Vec3 normal0,NormalBounds bounds) {
        this.material=material;this.anchor=anchor;this.body=body;this.center=body.getCenter();this.up=up;this.initialOffset=center.subtract(point0);
        this.initialGap=normal0.dot(initialOffset)-halfExtent(body,normal0);this.minDot=bounds.minDotAntiGravity();
        double alpha=bounds.minDotAntiGravity(),h=Math.sqrt(square(body.getXsize()*.5)+square(body.getYsize()*.5)+square(body.getZsize()*.5));
        double radius=h+Math.abs(initialGap)+initialOffset.length(),rate=bounds.normalRateBound();
        this.liftRate=rate*(h+initialOffset.length())/alpha+radius*rate/(alpha*alpha);
        this.residualSpeed=material.deformationSpeed()+liftRate;
        if(!Double.isFinite(initialGap) || !Double.isFinite(liftRate) || !Double.isFinite(residualSpeed))throw new IllegalArgumentException("Unbounded body path");
    }
    /** Returns empty unless a continuous normal certificate permits upright carry. */
    public static Optional<BodyPath> fromMaterial(ConservativeSweep.Motion material,LocalAnchor anchor,AABB body,GravityFrame gravity,NormalBounds bounds) {
        if(material==null || anchor==null || body==null || !finite(body) || gravity==null || bounds==null || bounds.face()!=anchor.face())
            return Optional.empty();
        try {
            var first=material.at().apply(0);if(first==null)return Optional.empty();
            Vec3 normal=first.faceNormal(anchor.face()),up=gravity.up();double support=normal.dot(up);
            if(!Double.isFinite(support) || support<SUPPORT || support+1e-6<bounds.minDotAntiGravity())return Optional.empty();
            return Optional.of(new BodyPath(material,anchor,body,up,first.point(anchor.coordinates()),normal,bounds));
        } catch(RuntimeException rejected) {return Optional.empty();}
    }
    /** Exact centre displacement; invalid provider output is rejected instead of chord-warped. */
    public Optional<Vec3> displacement(double time) {
        if(!Double.isFinite(time) || time<0 || time>1)return Optional.empty();
        if(time==0)return Optional.of(Vec3.ZERO);
        try {
            var shape=material.at().apply(time);if(shape==null)return Optional.empty();
            Vec3 normal=shape.faceNormal(anchor.face());double denominator=normal.dot(up);
            if(!Double.isFinite(denominator) || denominator<SUPPORT || denominator+1e-6<minDot)return Optional.empty();
            Vec3 point=shape.point(anchor.coordinates());
            double lift=(halfExtent(body,normal)+initialGap-normal.dot(initialOffset))/denominator;
            if(!Double.isFinite(lift))return Optional.empty();
            Vec3 result=point.add(initialOffset).add(up.scale(lift)).subtract(center);
            return Double.isFinite(result.lengthSqr())?Optional.of(result):Optional.empty();
        } catch(RuntimeException unavailable) {return Optional.empty();}
    }
    /** Declared linear cancellation term for a rigidly translated material face. */
    public Vec3 linearTranslation(){return material.linearTranslation();}
    /** Residual speed after that exact linear term, including the continuous upright lift bound. */
    public double residualSpeed(){return residualSpeed;}
    /** Relative convex path against the stationary initial body; invariant planes are intentionally dropped. */
    public Optional<ConservativeSweep.Motion> relative(ConservativeSweep.Motion other) {
        if(other==null || displacement(0).isEmpty() || displacement(1).isEmpty())return Optional.empty();
        return Optional.of(new ConservativeSweep.Motion(t->other.at().apply(t).move(displacement(t).orElseThrow().scale(-1)),
            other.deformationSpeed()+residualSpeed,other.linearTranslation().subtract(linearTranslation()),List.of()));
    }
    private static double halfExtent(AABB body,Vec3 normal) {
        return body.getXsize()*.5*Math.abs(normal.x)+body.getYsize()*.5*Math.abs(normal.y)+body.getZsize()*.5*Math.abs(normal.z);
    }
    private static boolean finite(AABB body) {
        return Double.isFinite(body.minX) && Double.isFinite(body.minY) && Double.isFinite(body.minZ) && Double.isFinite(body.maxX) && Double.isFinite(body.maxY) && Double.isFinite(body.maxZ)
            && body.getXsize()>0 && body.getYsize()>0 && body.getZsize()>0;
    }
    private static double square(double value){return value*value;}
}
