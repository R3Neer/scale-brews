package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.BodyPath;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * S08 adapter from a retained material contact to a certified continuous body path.
 * It is deliberately package-private: BodyPath remains the pure kernel while this class
 * owns live-world obstacle collection and fail-closed policy.
 */
final class AnchoredTransportPlanner {
    private AnchoredTransportPlanner() {}
    private static final int QUERY_BUDGET=256,MAX_STATIC_OBSTACLES=256;
    private static final double MAX_ENVELOPE_SPAN=64,OVERLAP_EPS=1e-8,NORMAL_EPS=1e-6;

    enum Status {NOT_APPLICABLE,COMPLETE,RELEASE,EXHAUSTED}
    private enum ObstacleStatus {CLEAR,RELEASE,EXHAUSTED}
    record Evidence(MaterialEventDispatcher.EventId parent,LivingEntity support,SurfaceContact surface,
            AnatomyMovement.RootFrame root,ConvexBox materialBefore,ConvexBox materialAfter) {
        Evidence {
            if(parent==null || support==null || surface==null || root==null || materialBefore==null || materialAfter==null)
                throw new IllegalArgumentException("Missing anchored transport evidence");
        }
    }
    record Result(Status status,Vec3 displacement,int evaluations,Evidence evidence) {
        Result {
            if(status==null || displacement==null || !Double.isFinite(displacement.lengthSqr()) || evaluations<0)
                throw new IllegalArgumentException("Invalid anchored transport result");
            if((status==Status.COMPLETE)!=(evidence!=null))throw new IllegalArgumentException("Contradictory anchored transport result");
        }
        static Result none(){return new Result(Status.NOT_APPLICABLE,Vec3.ZERO,0,null);}
        static Result release(int evaluations){return new Result(Status.RELEASE,Vec3.ZERO,evaluations,null);}
        static Result exhausted(int evaluations){return new Result(Status.EXHAUSTED,Vec3.ZERO,evaluations,null);}
    }
    private record ObstacleCheck(ObstacleStatus status,int evaluations) {}

    static Result plan(ServerLevel level,Entity body,AABB captured,
            List<MaterialEventDispatcher.Event<Entity>> events,
            Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> motions,
            BiFunction<AABB,Vec3,Vec3> clip) {
        var retained=AnatomyMovement.contact(body);var surface=AnatomyMovement.surface(body);
        if(retained==null || surface==null)return Result.none();
        MaterialEventDispatcher.Event<Entity> own=null;
        for(var event:events)if(event.support()==retained.support()) {
            if(own!=null)return Result.release(0);
            own=event;
        }
        if(own==null)return Result.none();
        if(!(own.support() instanceof LivingEntity support) || !Platforms.eligible(body,support)
                || !surface.support().equals(support.getUUID()) || surface.revision()!=retained.revision()
                || !surface.piece().equals(retained.piece()) || surface.face()<0 || surface.face()>5)
            return Result.release(0);
        var handle=own.interval().handle();var motion=motions.get(handle);
        if(motion==null || handle.identity().revision()!=retained.revision())return Result.release(0);
        var pieceMotion=motion.pieces().get(retained.piece());
        var materialBefore=handle.before().snapshot().pieces().get(retained.piece());
        var materialAfter=handle.after().snapshot().pieces().get(retained.piece());
        if(pieceMotion==null || materialBefore==null || materialAfter==null)return Result.release(0);

        var bodyGravity=AnatomyMovement.gravity(body);
        try {
            Vec3 bodyUp=bodyGravity.up(),supportUp=handle.before().root().gravity().up();
            Vec3 normal0=materialBefore.faceNormal(surface.face()),normal1=materialAfter.faceNormal(surface.face());
            double dot0=normal0.dot(bodyUp),dot1=normal1.dot(bodyUp);
            if(!Double.isFinite(dot0+dot1))return Result.release(0);

            boolean rootOnly=handle.before().sample().inputs().equals(handle.after().sample().inputs());
            double normalRate;
            if(hasInvariantFacePlane(pieceMotion,normal0,normal1))normalRate=0;
            else if(rootOnly)normalRate=Math.abs(Math.toRadians(wrapDegrees(handle.after().root().yaw()-handle.before().root().yaw())));
            else normalRate=certifiedFaceNormalRate(pieceMotion,materialBefore,materialAfter,surface.face());
            double minDot=rootOnly
                ?certifiedRootMinDot(dot0,dot1,normalRate,bodyUp,supportUp)
                :certifiedGenericMinDot(dot0,dot1,normalRate);

            var bounds=new BodyPath.NormalBounds(surface.face(),minDot,normalRate);
            var anchor=new BodyPath.LocalAnchor(surface.face(),surface.localPoint());
            var path=BodyPath.fromMaterial(pieceMotion,anchor,captured,bodyGravity,bounds).orElse(null);
            if(path==null)return Result.release(0);
            var displacement=path.displacement(1).orElse(null);if(displacement==null)return Result.release(0);
            double speed=path.linearTranslation().length()+path.residualSpeed()+ConservativeSweep.SKIN;
            if(!Double.isFinite(speed) || speed>MAX_ENVELOPE_SPAN)return Result.release(0);
            var envelope=captured.inflate(speed);
            if(envelope.getXsize()>MAX_ENVELOPE_SPAN || envelope.getYsize()>MAX_ENVELOPE_SPAN || envelope.getZsize()>MAX_ENVELOPE_SPAN)
                return Result.release(0);

            int evaluations=0;
            for(var event:events) {
                if(!(event.support() instanceof LivingEntity eventSupport) || !Platforms.eligible(body,eventSupport))continue;
                var eventMotion=motions.get(event.interval().handle());if(eventMotion==null)return Result.release(evaluations);
                for(var entry:eventMotion.pieces().entrySet()) {
                    if(eventSupport==support && entry.getKey().equals(retained.piece()))continue;
                    var relative=path.relative(entry.getValue()).orElse(null);if(relative==null)return Result.release(evaluations);
                    int remaining=QUERY_BUDGET-evaluations;if(remaining<=0)return Result.exhausted(evaluations);
                    var hit=ConservativeSweep.query(captured,Vec3.ZERO,relative,remaining);evaluations+=hit.evaluations();
                    if(hit.status()==ConservativeSweep.Status.ITERATION_LIMIT)return Result.exhausted(evaluations);
                    if(hit.status()!=ConservativeSweep.Status.CLEAR)return Result.release(evaluations);
                }
            }

            int obstacles=0;
            for(var shape:level.getBlockCollisions(body,envelope))for(var box:shape.toAabbs()) {
                if(++obstacles>MAX_STATIC_OBSTACLES)return Result.exhausted(evaluations);
                var checked=checkStaticObstacle(path,captured,box,evaluations);evaluations=checked.evaluations();
                if(checked.status()==ObstacleStatus.EXHAUSTED)return Result.exhausted(evaluations);
                if(checked.status()==ObstacleStatus.RELEASE)return Result.release(evaluations);
            }

            // Entity collisions must be checked along the same certified body path as blocks.
            // Endpoint-only collideBoundingBox is insufficient for a curved anchor trajectory:
            // an entity can occupy only the middle of the arc while the straight chord is clear.
            List<VoxelShape> entityShapes;
            Entity previous=PlatformPhysics.enter(body);
            try {entityShapes=List.copyOf(level.getEntityCollisions(body,envelope));}
            finally {PlatformPhysics.exit(previous);}
            for(var shape:entityShapes)for(var box:shape.toAabbs()) {
                if(++obstacles>MAX_STATIC_OBSTACLES)return Result.exhausted(evaluations);
                var checked=checkStaticObstacle(path,captured,box,evaluations);evaluations=checked.evaluations();
                if(checked.status()==ObstacleStatus.EXHAUSTED)return Result.exhausted(evaluations);
                if(checked.status()==ObstacleStatus.RELEASE)return Result.release(evaluations);
            }

            previous=PlatformPhysics.enter(body);Vec3 allowed;
            try {allowed=clip.apply(captured,displacement);} finally {PlatformPhysics.exit(previous);}
            if(allowed.distanceToSqr(displacement)>OVERLAP_EPS)return Result.release(evaluations);
            return new Result(Status.COMPLETE,displacement,evaluations,
                new Evidence(own.id(),support,surface,handle.after().root(),materialBefore,materialAfter));
        } catch(RuntimeException rejected) {return Result.release(0);}
    }

    /** One static world AABB checked continuously in the body's certified path frame. */
    private static ObstacleCheck checkStaticObstacle(BodyPath path,AABB captured,AABB box,int evaluations) {
        var obstacle=axisAligned(box);
        if(obstacle.overlaps(captured))return new ObstacleCheck(ObstacleStatus.RELEASE,evaluations);
        var staticMotion=new ConservativeSweep.Motion(t->obstacle,0,Vec3.ZERO);
        var relative=path.relative(staticMotion).orElse(null);
        if(relative==null)return new ObstacleCheck(ObstacleStatus.RELEASE,evaluations);
        int remaining=QUERY_BUDGET-evaluations;
        if(remaining<=0)return new ObstacleCheck(ObstacleStatus.EXHAUSTED,evaluations);
        var hit=ConservativeSweep.query(captured,Vec3.ZERO,relative,remaining);
        int total=evaluations+hit.evaluations();
        if(hit.status()==ConservativeSweep.Status.ITERATION_LIMIT)return new ObstacleCheck(ObstacleStatus.EXHAUSTED,total);
        return new ObstacleCheck(hit.status()==ConservativeSweep.Status.CLEAR?ObstacleStatus.CLEAR:ObstacleStatus.RELEASE,total);
    }

    /** Build an axis-aligned convex directly in double world coordinates; identity Matrix4f would round large worlds to float. */
    private static ConvexBox axisAligned(AABB box) {
        ConvexBox.requireBounds(box);
        return new ConvexBox(List.of(
            new Vec3(box.minX,box.minY,box.minZ),new Vec3(box.maxX,box.minY,box.minZ),
            new Vec3(box.minX,box.maxY,box.minZ),new Vec3(box.maxX,box.maxY,box.minZ),
            new Vec3(box.minX,box.minY,box.maxZ),new Vec3(box.maxX,box.minY,box.maxZ),
            new Vec3(box.minX,box.maxY,box.maxZ),new Vec3(box.maxX,box.maxY,box.maxZ)));
    }

    /** Exact hierarchy certificates win over derivative bounds when the retained face plane is fixed. */
    private static boolean hasInvariantFacePlane(ConservativeSweep.Motion motion,Vec3 before,Vec3 after) {
        for(var plane:motion.invariantPlanes()) {
            Vec3 outward=new Vec3(plane.outward().getStepX(),plane.outward().getStepY(),plane.outward().getStepZ());
            if(before.distanceToSqr(outward)<=NORMAL_EPS*NORMAL_EPS && after.distanceToSqr(outward)<=NORMAL_EPS*NORMAL_EPS)return true;
        }
        return false;
    }

    /**
     * Every material point has residual velocity <= deformationSpeed after exact linear translation.
     * Therefore each face edge changes at <=2V. The cross product of the two tangential edges has
     * a certified Lipschitz rate; a positive lower bound on its magnitude then bounds the normalized
     * face-normal rate. No intermediate sampling is used as authority.
     */
    private static double certifiedFaceNormalRate(ConservativeSweep.Motion motion,ConvexBox before,ConvexBox after,int face) {
        double v=motion.deformationSpeed();
        if(v==0)return 0;
        Vec3[] a=edges(before),b=edges(after);int axis=face/2,i=(axis+1)%3,j=(axis+2)%3;
        double e1=Math.min(a[i].length(),b[i].length())+2*v;
        double e2=Math.min(a[j].length(),b[j].length())+2*v;
        double crossRate=2*v*(e1+e2);
        double c0=a[i].cross(a[j]).length(),c1=b[i].cross(b[j]).length();
        if(!Double.isFinite(crossRate+c0+c1) || crossRate<0 || c0<=0 || c1<=0
                || Math.abs(c0-c1)>crossRate+NORMAL_EPS)throw new IllegalArgumentException("Face-area endpoints contradict motion bound");
        if(crossRate==0)return 0;
        double crossMin=(c0+c1-crossRate)*.5;
        if(!(crossMin>1e-12) || !Double.isFinite(crossMin))throw new IllegalArgumentException("Face normal cannot be bounded through interval");
        double rate=2*crossRate/crossMin;
        if(!Double.isFinite(rate))throw new IllegalArgumentException("Unbounded face normal rate");
        return rate;
    }

    private static Vec3[] edges(ConvexBox box) {
        var vertices=box.vertices();Vec3 origin=vertices.getFirst();
        return new Vec3[]{vertices.get(1).subtract(origin),vertices.get(2).subtract(origin),vertices.get(4).subtract(origin)};
    }

    /** Tight root-only certificate: root yaw is rigid rotation about supportUp. */
    private static double certifiedRootMinDot(double dot0,double dot1,double normalRate,Vec3 bodyUp,Vec3 supportUp) {
        if(normalRate==0 || Math.abs(Math.abs(bodyUp.dot(supportUp))-1)<1e-9)return Math.min(dot0,dot1);
        return certifiedGenericMinDot(dot0,dot1,normalRate);
    }

    /** Endpoint cones from a certified unit-normal Lipschitz rate bound the full interval. */
    private static double certifiedGenericMinDot(double dot0,double dot1,double normalRate) {
        if(Math.abs(dot1-dot0)>normalRate+NORMAL_EPS)throw new IllegalArgumentException("Endpoint normals contradict certified normal rate");
        return Math.min(Math.min(dot0,dot1),(dot0+dot1-normalRate)*.5);
    }

    private static double wrapDegrees(double value) {
        double wrapped=value%360;
        if(wrapped>=180)wrapped-=360;
        if(wrapped< -180)wrapped+=360;
        return wrapped;
    }
}
