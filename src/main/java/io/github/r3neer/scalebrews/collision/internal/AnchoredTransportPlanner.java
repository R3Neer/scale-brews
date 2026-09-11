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
import org.joml.Matrix4f;

/**
 * S08 adapter from a retained material contact to a certified continuous body path.
 * It is deliberately package-private: BodyPath remains the pure kernel while this class
 * owns live-world obstacle collection and fail-closed policy.
 */
final class AnchoredTransportPlanner {
    private AnchoredTransportPlanner() {}
    private static final int QUERY_BUDGET=256,MAX_STATIC_OBSTACLES=256;
    private static final double MAX_ENVELOPE_SPAN=64,OVERLAP_EPS=1e-8;

    enum Status {NOT_APPLICABLE,COMPLETE,RELEASE}
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
    }

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

        if(!handle.before().sample().inputs().equals(handle.after().sample().inputs()))return Result.release(0);
        var bodyGravity=AnatomyMovement.gravity(body);
        try {
            Vec3 bodyUp=bodyGravity.up(),supportUp=handle.before().root().gravity().up();
            Vec3 normal0=materialBefore.faceNormal(surface.face()),normal1=materialAfter.faceNormal(surface.face());
            double dot0=normal0.dot(bodyUp),dot1=normal1.dot(bodyUp);
            if(!Double.isFinite(dot0+dot1))return Result.release(0);
            double normalRate=Math.abs(Math.toRadians(wrapDegrees(handle.after().root().yaw()-handle.before().root().yaw())));
            double minDot=certifiedMinDot(dot0,dot1,normalRate,bodyUp,supportUp);
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
                    var hit=ConservativeSweep.query(captured,Vec3.ZERO,relative,QUERY_BUDGET);evaluations+=hit.evaluations();
                    if(hit.status()!=ConservativeSweep.Status.CLEAR)return Result.release(evaluations);
                }
            }

            int obstacles=0;
            for(var shape:level.getBlockCollisions(body,envelope))for(var box:shape.toAabbs()) {
                if(++obstacles>MAX_STATIC_OBSTACLES)return Result.release(evaluations);
                ConvexBox obstacle=ConvexBox.of(box,new Matrix4f());
                if(obstacle.overlaps(captured))return Result.release(evaluations);
                if(obstacle.separation(captured).gap()<=ConservativeSweep.SKIN)continue;
                var staticMotion=new ConservativeSweep.Motion(t->obstacle,0,Vec3.ZERO);
                var relative=path.relative(staticMotion).orElse(null);if(relative==null)return Result.release(evaluations);
                var hit=ConservativeSweep.query(captured,Vec3.ZERO,relative,QUERY_BUDGET);evaluations+=hit.evaluations();
                if(hit.status()!=ConservativeSweep.Status.CLEAR)return Result.release(evaluations);
            }

            Entity previous=PlatformPhysics.enter(body);Vec3 allowed;
            try {allowed=clip.apply(captured,displacement);} finally {PlatformPhysics.exit(previous);}
            if(allowed.distanceToSqr(displacement)>OVERLAP_EPS)return Result.release(evaluations);
            return new Result(Status.COMPLETE,displacement,evaluations,
                new Evidence(own.id(),support,surface,handle.after().root(),materialBefore,materialAfter));
        } catch(RuntimeException rejected) {return Result.release(0);}
    }

    private static double certifiedMinDot(double dot0,double dot1,double normalRate,Vec3 bodyUp,Vec3 supportUp) {
        if(normalRate==0 || Math.abs(Math.abs(bodyUp.dot(supportUp))-1)<1e-9)return Math.min(dot0,dot1);
        if(Math.abs(dot1-dot0)>normalRate+1e-6)throw new IllegalArgumentException("Endpoint normals contradict root-yaw rate");
        return Math.min(Math.min(dot0,dot1),(dot0+dot1-normalRate)*.5);
    }

    private static double wrapDegrees(double value) {
        double wrapped=value%360;
        if(wrapped>=180)wrapped-=360;
        if(wrapped< -180)wrapped+=360;
        return wrapped;
    }
}
