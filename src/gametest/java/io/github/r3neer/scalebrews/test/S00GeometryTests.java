package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.*;
import io.github.r3neer.scalebrews.collision.internal.HierarchyMotion;
import io.github.r3neer.scalebrews.collision.physics.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import static io.github.r3neer.scalebrews.test.S00Fixtures.*;

/** A01..A06: arithmetic oracles and observable geometric properties, on real dependencies. */
public final class S00GeometryTests {
    private static ConvexBox cube(double side) {
        var vertices=new ArrayList<Vec3>();
        for(int i=0;i<8;i++)vertices.add(new Vec3((i&1)*side,((i>>1)&1)*side,((i>>2)&1)*side));
        return new ConvexBox(vertices);
    }
    @GameTest public void acceptedFiniteGeometryIsClosedUnderNormalsAndInverse(GameTestHelper h) {
        for(double side:new double[]{1,1e10,1e50,1e100,1e110}) {
            ConvexBox shape;
            try{shape=cube(side);}catch(IllegalArgumentException rejected){continue;}
            for(int face=0;face<6;face++)check(Double.isFinite(shape.faceNormal(face).lengthSqr()) && Math.abs(shape.faceNormal(face).lengthSqr()-1)<1e-12,"Accepted box has invalid normal at size "+side);
            near(shape.coordinates(new Vec3(.5*side,.5*side,.5*side)),new Vec3(.5,.5,.5),1e-12,"Accepted box has invalid inverse");
        }h.succeed();
    }
    @GameTest public void invalidBoundsAndCoordinatesCannotEnterGeometryQueries(GameTestHelper h) {
        var shape=box(UNIT);var nan=new Vec3(Double.NaN,0,0);
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            var body=new AABB(invalid,0,0,1,1,1);
            rejects(()->shape.overlaps(body));rejects(()->shape.separation(body));rejects(()->shape.sweep(body,Vec3.ZERO));
            rejects(()->ConservativeSweep.query(body,Vec3.ZERO,new ConservativeSweep.Motion(t->shape,0),16));
            rejects(()->TemporalResponse.resolve(body,Vec3.ZERO,Map.of(),16,16));
            rejects(()->ConservativeSweep.query(body,Vec3.ZERO,new ConservativeSweep.Motion(t->shape,0,Vec3.ZERO,List.of(new ConservativeSweep.Plane(Direction.UP,0))),16));
        }
        rejects(()->shape.point(nan));rejects(()->shape.coordinates(nan));rejects(()->shape.sweep(UNIT,nan));
        rejects(()->shape.facePoint(6,Vec3.ZERO));rejects(()->shape.closestFace(Vec3.ZERO));h.succeed();
    }
    @GameTest public void invalidClipCannotClaimSuccessfulRecovery(GameTestHelper h) {
        var shape=box(UNIT);
        for(var body:List.of(new AABB(.2,.2,.2,.8,.8,.8),new AABB(2,2,2,3,3,3))) {
            for(var invalid:Arrays.asList(null,new Vec3(Double.NaN,0,0),new Vec3(Double.POSITIVE_INFINITY,0,0))) {
                var result=AnatomySeparation.resolve(body,List.of(shape),2,64,(bounds,delta)->invalid);
                check(!result.separated() && result.displacement().equals(Vec3.ZERO),"Invalid clip became successful recovery");
            }
        }h.succeed();
    }
    @GameTest public void gravityFramesPreserveGeometryAndRejectNonfiniteSupport(GameTestHelper h) {
        var random=new Random(0x5004);
        for(var direction:Direction.values()) {
            var frame=new GravityFrame(direction);var up=frame.up();
            var infinite=new Vec3(up.x==0?0:up.x*Double.POSITIVE_INFINITY,up.y==0?0:up.y*Double.POSITIVE_INFINITY,up.z==0?0:up.z*Double.POSITIVE_INFINITY);
            check(!frame.supports(infinite) && !frame.supports(Vec3.ZERO) && !frame.supports(null),"Invalid support normal accepted under "+direction);
            for(int i=0;i<128;i++){var v=new Vec3(random.nextDouble()*20-10,random.nextDouble()*20-10,random.nextDouble()*20-10);near(frame.toLocal(frame.toWorld(v)),v,1e-12,"Gravity round trip");}
            for(double degrees:new double[]{44.9,45,45.1})for(double magnitude:new double[]{.001,1,1000}) {
                double angle=Math.toRadians(degrees);var n=frame.toWorld(new Vec3(Math.sin(angle),Math.cos(angle),0)).scale(magnitude);
                check(frame.supports(n)==(degrees<=45),"45 degree boundary under "+direction+" at magnitude "+magnitude);
            }
            check(GravityFrame.dominant(frame.toWorld(new Vec3(Math.sqrt(.5),Math.sqrt(.5),0))).isEmpty(),"Cardinal tie became a direction");
        }h.succeed();
    }
    private static ConservativeSweep.Motion tinyRotation() {
        var model=new ModelGeometry(2,"test:lever","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("lever","root",List.of(999d,0d,-.01),List.of(1000d,1d,.01),null)),ModelGeometry.values(new Matrix4f()));
        return new HierarchyMotion(model,Map.of(),Map.of(),new Matrix4f(),new Matrix4f().rotateY(1e-4f),Vec3.ZERO,Vec3.ZERO,new AnatomyFilter(0,0,0)).pieces().get("lever");
    }
    @GameTest public void tinyRotationCannotHaveAZeroMotionCertificate(GameTestHelper h) {
        var motion=tinyRotation();double distance=motion.at().apply(1).vertices().getFirst().distanceTo(motion.at().apply(0).vertices().getFirst());
        check(distance>.09,"Lever fixture did not move");
        check(motion.maxPointSpeed()>=distance,"Certified speed "+motion.maxPointSpeed()+" is below displacement "+distance);h.succeed();
    }
    @GameTest public void tinyRotationCannotTunnelBetweenClearEndpoints(GameTestHelper h) {
        var motion=tinyRotation();var body=new AABB(999.4,.2,-.065,999.6,.4,-.055);
        check(!motion.at().apply(0).overlaps(body) && !motion.at().apply(1).overlaps(body),"Endpoints must be clear");
        check(motion.at().apply(.5).overlaps(body),"Independent interior witness must overlap");
        var result=ConservativeSweep.query(body,Vec3.ZERO,motion,256);
        check(result.status()==ConservativeSweep.Status.CONTACT && result.safeFraction()>0 && result.safeFraction()<1,"Interior rotating contact lost: "+result);h.succeed();
    }
    @GameTest public void translatingSweepMatchesAnalyticTimeOfImpact(GameTestHelper h) {
        var random=new Random(0x5002);
        for(double size:new double[]{.001,.1,1,100,1024})for(int i=0;i<256;i++) {
            double gap=size*(.1+random.nextDouble()*4),dx=gap*(1.1+random.nextDouble()*5);
            var shape=box(new AABB(0,0,0,size,size,size));var body=new AABB(-gap-size*.2,size*.3,size*.3,-gap,size*.5,size*.5);
            var movement=new Vec3(dx,size*(random.nextDouble()-.5)*.1,size*(random.nextDouble()-.5)*.1);
            var hit=shape.sweep(body,movement);
            check(hit!=null && !hit.penetrating() && Math.abs(hit.fraction()-gap/dx)<1e-8,"Sweep disagrees with independent plane TOI at size "+size);
            near(hit.normal(),new Vec3(-1,0,0),1e-12,"Sweep normal must oppose entry");
        }h.succeed();
    }
    @GameTest public void affineRoundTripsFacesAndRaysSurviveReflectionAndTranslation(GameTestHelper h) {
        var random=new Random(0x5003);
        for(int i=0;i<256;i++) {
            double sx=(i%2==0?1:-1)*(.5+random.nextDouble()*2),sy=.5+random.nextDouble()*2,sz=.5+random.nextDouble()*2;
            var x=new Vec3(sx,0,0);var y=new Vec3(random.nextDouble()-.5,sy,0);var z=new Vec3(random.nextDouble()-.5,random.nextDouble()-.5,sz);
            var origin=i%3==0?new Vec3(29000000,-100,1000000):new Vec3(-3,2,1);var vertices=new ArrayList<Vec3>();
            for(int v=0;v<8;v++)vertices.add(origin.add(x.scale(v&1)).add(y.scale((v>>1)&1)).add(z.scale((v>>2)&1)));
            var shape=new ConvexBox(vertices);var local=new Vec3(random.nextDouble(),random.nextDouble(),random.nextDouble());
            near(shape.coordinates(shape.point(local)),local,1e-6,"Affine local/world round trip");
            var edges=List.of(x,y,z);
            for(int face=0;face<6;face++) {
                var normal=shape.faceNormal(face);int axis=face/2;
                check(Math.abs(normal.lengthSqr()-1)<1e-10,"Non-unit affine face normal");
                check(normal.dot(edges.get(axis))*(face%2==0?-1:1)>0,"Reflected normal points inward");
                for(int tangent=0;tangent<3;tangent++)if(tangent!=axis)check(Math.abs(normal.dot(edges.get(tangent)))<1e-7,"Face normal is not orthogonal");
                double[] start={.5,.5,.5};start[axis]=face%2==0?-1:2;
                var hit=shape.raycast(shape.point(new Vec3(start[0],start[1],start[2])),shape.point(new Vec3(.5,.5,.5)));
                check(hit!=null && hit.face()==face && Math.abs(hit.fraction()-2d/3)<1e-6,"Ray lost its local material face");
            }
        }h.succeed();
    }
    @GameTest public void continuousQueryFindsInteriorMotionAndReportsExhaustion(GameTestHelper h) {
        var shape=box(new AABB(-2,0,0,-1,1,1));var body=new AABB(-.1,.2,.2,.1,.4,.4);
        var motion=new ConservativeSweep.Motion(t->shape.move(new Vec3(2*Math.sin(Math.PI*t),0,0)),2*Math.PI);
        check(!motion.at().apply(0).overlaps(body) && !motion.at().apply(1).overlaps(body),"Endpoints must not disclose the interior collision");
        var hit=ConservativeSweep.query(body,Vec3.ZERO,motion,256);double expected=Math.asin(.45)/Math.PI;
        check(hit.status()==ConservativeSweep.Status.CONTACT && Math.abs(hit.safeFraction()-expected)<1e-5,"Continuous contact disagrees with analytic sine trajectory: "+hit);
        var exhausted=ConservativeSweep.query(body,Vec3.ZERO,motion,1);
        check(exhausted.status()==ConservativeSweep.Status.ITERATION_LIMIT && exhausted.safeFraction()<expected,"Budget exhaustion fabricated a clear interval");
        var response=TemporalResponse.resolve(body,Vec3.ZERO,Map.of("moving",motion),1,1);
        check(response.status()==TemporalResponse.Status.ITERATION_LIMIT && response.time()<1,"Response promoted exhaustion to completion");h.succeed();
    }
    @GameTest public void recoveryIsBoundedAndIndependentOfPieceOrder(GameTestHelper h) {
        var body=new AABB(.25,.25,.25,.75,.75,.75);
        var pieces=new ArrayList<>(List.of(box(UNIT),box(new AABB(-.1,0,0,1,1,1)),box(new AABB(0,0,0,1.1,1,1))));
        var first=AnatomySeparation.resolve(body,pieces,2,256,(b,d)->d);
        check(first.separated() && first.displacement().length()<=2 && first.candidates()<=256,"Recovery exceeded its declared budget");
        for(var piece:pieces)check(!piece.overlaps(body.move(first.displacement())),"Recovery remained penetrating");
        Collections.reverse(pieces);var reversed=AnatomySeparation.resolve(body,pieces,2,256,(b,d)->d);
        near(first.displacement(),reversed.displacement(),1e-12,"Recovery depends on collection order");
        var blocked=AnatomySeparation.resolve(body,pieces,0,1,(b,d)->Vec3.ZERO);
        check(!blocked.separated() && blocked.displacement().equals(Vec3.ZERO),"Failed recovery created a teleport");h.succeed();
    }
}
