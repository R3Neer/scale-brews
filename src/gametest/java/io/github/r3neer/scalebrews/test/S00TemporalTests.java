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

/** Reserved temporal/multicontact holdouts plus a second hostile inspection of numeric certificates. */
public final class S00TemporalTests {
    @GameTest public void holdoutH01RootReturnAndCounterJointCannotHideInteriorCollision(GameTestHelper h) {
        var geometry=new ModelGeometry(2,"test:holdout","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f())),
            new ModelGeometry.Part("tip","root",ModelGeometry.values(new Matrix4f().translation(2,0,0)))),
            List.of(new ModelGeometry.Piece("tip","tip",List.of(-.08,-.08,-.08),List.of(.08,.08,.08),null)),ModelGeometry.values(new Matrix4f()));
        float angle=.6f;var start=Map.of("tip",new Matrix4f().translation(2,0,0));
        var middle=Map.of("tip",new Matrix4f().translation(2,0,0).rotateZ(-angle));
        var outward=new HierarchyMotion(geometry,start,middle,new Matrix4f(),new Matrix4f().rotateZ(angle),Vec3.ZERO,Vec3.ZERO,new AnatomyFilter(0,0,0)).pieces().get("tip");
        var inward=new HierarchyMotion(geometry,middle,start,new Matrix4f().rotateZ(angle),new Matrix4f(),Vec3.ZERO,Vec3.ZERO,new AnatomyFilter(0,0,0)).pieces().get("tip");
        // Two actual intervals are preserved; equal distant endpoints do NOT certify a static path.
        var roundTrip=new ConservativeSweep.Motion(t->t<=.5?outward.at().apply(2*t):inward.at().apply(2*t-1),2*Math.max(outward.maxPointSpeed(),inward.maxPointSpeed()));
        double x=2*Math.cos(angle),y=2*Math.sin(angle);var obstacle=new AABB(x-.04,y-.04,-.04,x+.04,y+.04,.04);
        check(!roundTrip.at().apply(0).overlaps(obstacle) && !roundTrip.at().apply(1).overlaps(obstacle) && roundTrip.at().apply(.5).overlaps(obstacle),"H01 needs a strictly interior contact");
        var hit=ConservativeSweep.query(obstacle,Vec3.ZERO,roundTrip,4096);
        double analytical=Math.asin((y-.12)/2)/(2*angle);
        check(hit.status()==ConservativeSweep.Status.CONTACT && Math.abs(hit.safeFraction()-analytical)<1e-5,"H01 lost real material history or counter-joint composition: "+hit);
        var firstHalf=ConservativeSweep.query(obstacle,Vec3.ZERO,roundTrip.interval(0,.5),4096);
        check(firstHalf.status()==ConservativeSweep.Status.CONTACT && Math.abs(firstHalf.safeFraction()*.5-hit.safeFraction())<1e-5,"Equivalent temporal split changed contact");
        var shift=new Vec3(29000000,100,-1000000);var moved=new ConservativeSweep.Motion(t->roundTrip.at().apply(t).move(shift),roundTrip.maxPointSpeed());
        var translated=ConservativeSweep.query(obstacle.move(shift),Vec3.ZERO,moved,4096);
        check(translated.status()==hit.status() && Math.abs(translated.safeFraction()-hit.safeFraction())<1e-5,"Common translation changed temporal physics");h.succeed();
    }
    @GameTest public void holdoutH05EqualContactsIgnoreMapOrderAndSignedZero(GameTestHelper h) {
        var body=new AABB(-1,0,-1,-.8,.2,-.8);
        var x=box(new AABB(0,-2,-2,1,2,2));var z=box(new AABB(-2,-2,0,2,2,1));
        var entries=List.of(Map.entry("x",new ConservativeSweep.Motion(t->x,0)),Map.entry("z",new ConservativeSweep.Motion(t->z,0)));
        Vec3 expected=null;
        for(int permutation=0;permutation<32;permutation++) {
            var shuffled=new ArrayList<>(entries);Collections.shuffle(shuffled,new Random(permutation));
            var pieces=new LinkedHashMap<String,ConservativeSweep.Motion>();for(var e:shuffled)pieces.put(e.getKey(),e.getValue());
            var result=TemporalResponse.resolve(body,new Vec3(1.5,permutation%2==0?0d:-0d,1.5),pieces,32,256);
            check(result.status()==TemporalResponse.Status.COMPLETE && result.time()==1,"Tied contact did not terminate safely");
            check(!x.overlaps(body.move(result.displacement())) && !z.overlaps(body.move(result.displacement())),"Tied contact left penetration");
            near(result.displacement(),new Vec3(.8,0,.8),1e-5,"Corner contact differs from independent plane constraints");
            if(expected==null)expected=result.displacement();else near(result.displacement(),expected,1e-12,"Permutation or signed zero changed physics");
            check(result.contacts().stream().map(TemporalResponse.Contact::piece).collect(java.util.stream.Collectors.toSet()).equals(Set.of("x","z")),"One equal-time contact was discarded");
        }h.succeed();
    }
    private static AABB rotate(AABB bounds,GravityFrame gravity){return ConvexBox.of(bounds,gravity.matrix()).bounds();}
    @GameTest public void sixGravityMaterialPathsMatchRigidTransportAndRelativeCcd(GameTestHelper h) {
        for(var direction:Direction.values()) {
            var gravity=new GravityFrame(direction);var shape=ConvexBox.of(new AABB(-1,-.2,-1,1,0,1),gravity.matrix());
            var delta=gravity.toWorld(new Vec3(.7,-.2,.3));var body=rotate(new AABB(-.1,.05,-.1,.1,.25,.1),gravity);
            var material=new ConservativeSweep.Motion(t->shape.move(delta.scale(t)),0,delta);
            var path=BodyPath.fromMaterial(material,new BodyPath.LocalAnchor(3,new Vec3(.5,1,.5)),body,gravity,new BodyPath.NormalBounds(3,1,0)).orElseThrow();
            for(double t:new double[]{0,.125,.5,.875,1})near(path.displacement(t).orElseThrow(),delta.scale(t),1e-12,"Material carry changed with gravity "+direction);
            var obstacle=box(body.move(delta.scale(.5)));var fixed=new ConservativeSweep.Motion(t->obstacle,0);
            var expected=obstacle.sweep(body,delta);check(expected!=null,"Relative CCD fixture missed its obstacle");
            var actual=ConservativeSweep.query(body,Vec3.ZERO,path.relative(fixed).orElseThrow(),256);
            check(actual.status()==ConservativeSweep.Status.CONTACT && Math.abs(actual.safeFraction()-expected.fraction())<1e-10,"Relative path CCD changed under "+direction);
            var cancellation=path.relative(material).orElseThrow();check(cancellation.deformationSpeed()==0 && cancellation.linearTranslation().lengthSqr()==0,"Rigid material term was counted twice");
        }h.succeed();
    }
    @GameTest public void thinAffineFacePlaneCannotDisappearFromSat(GameTestHelper h) {
        var x=new Vec3(6.312514969513066e-7,5.316958010320105e-7,-5.646424733950354e-7);
        var y=new Vec3(-4.2518864952864844e-7,8.46118266385731e-7,3.214008270064177e-7);
        var z=new Vec3(648.6417808842865,37.194817560161816,760.1844418546907);var vertices=new ArrayList<Vec3>();
        for(int i=0;i<8;i++)vertices.add(x.scale(i&1).add(y.scale((i>>1)&1)).add(z.scale((i>>2)&1)));
        var shape=new ConvexBox(vertices);var face=shape.point(new Vec3(.5,.5,0));var normal=shape.faceNormal(4);var center=face.add(normal.scale(2.5e-8));
        var body=new AABB(center.x-1e-8,center.y-1e-8,center.z-1e-8,center.x+1e-8,center.y+1e-8,center.z+1e-8);
        double planeGap=normal.dot(center.subtract(face))-1e-8*(Math.abs(normal.x)+Math.abs(normal.y)+Math.abs(normal.z));
        check(planeGap>1e-8,"Reference face plane does not strictly separate fixture");
        check(!shape.overlaps(body) && shape.separation(body).gap()>1e-8,"Absolute axis cutoff invented overlap for an accepted affine box");h.succeed();
    }
    @GameTest public void bodyPathRejectsAnImpossibleNormalCertificate(GameTestHelper h) {
        var shape=ConvexBox.of(new AABB(-1,-.1,-1,1,0,1),new Matrix4f().rotateZ(.2f));
        var path=BodyPath.fromMaterial(new ConservativeSweep.Motion(t->shape,0),new BodyPath.LocalAnchor(3,new Vec3(.5,1,.5)),
            new AABB(-.1,.1,-.1,.1,.3,.1),GravityFrame.VANILLA,new BodyPath.NormalBounds(3,1,0));
        check(path.isEmpty(),"A normal below the claimed certified minimum was accepted");h.succeed();
    }
    @GameTest public void bodyPathRejectsUnavailableProviderSamples(GameTestHelper h) {
        var anchor=new BodyPath.LocalAnchor(3,new Vec3(.5,1,.5));var body=new AABB(-.1,.1,-.1,.1,.3,.1);var bounds=new BodyPath.NormalBounds(3,1,0);
        check(BodyPath.fromMaterial(new ConservativeSweep.Motion(t->null,0),anchor,body,GravityFrame.VANILLA,bounds).isEmpty(),"Null provider sample became a body path");
        check(BodyPath.fromMaterial(new ConservativeSweep.Motion(t->{throw new IllegalArgumentException("retired model");},0),anchor,body,GravityFrame.VANILLA,bounds).isEmpty(),"Withdrawn provider sample became a body path");h.succeed();
    }
    @GameTest public void bodyPathInteriorFailureDoesNotInventDisplacement(GameTestHelper h) {
        var shape=box(new AABB(-1,-.1,-1,1,0,1));var material=new ConservativeSweep.Motion(t->{if(t==.5)throw new IllegalArgumentException("retired interval");return shape;},0);
        var path=BodyPath.fromMaterial(material,new BodyPath.LocalAnchor(3,new Vec3(.5,1,.5)),new AABB(-.1,.1,-.1,.1,.3,.1),GravityFrame.VANILLA,new BodyPath.NormalBounds(3,1,0)).orElseThrow();
        check(path.displacement(.5).isEmpty(),"Interior resource loss became a displacement");h.succeed();
    }
    @GameTest public void invariantPlaneCertificatesAreBoundedAndUnique(GameTestHelper h) {
        var shape=box(UNIT);var planes=new ArrayList<ConservativeSweep.Plane>();
        for(var direction:Direction.values())planes.add(new ConservativeSweep.Plane(direction,2));
        new ConservativeSweep.Motion(t->shape,0,Vec3.ZERO,planes);
        planes.add(new ConservativeSweep.Plane(Direction.UP,2));
        rejects(()->new ConservativeSweep.Motion(t->shape,0,Vec3.ZERO,planes));
        rejects(()->new ConservativeSweep.Motion(t->shape,0,Vec3.ZERO,List.of(new ConservativeSweep.Plane(Direction.UP,2),new ConservativeSweep.Plane(Direction.UP,3))));h.succeed();
    }

    @GameTest public void acceptedSubnormalEdgeComponentsNeverProduceNonfiniteEscape(GameTestHelper h) {
        var x=new Vec3(1,Double.MIN_VALUE,0);var y=new Vec3(0,1,0);var z=new Vec3(0,0,1);
        var vertices=new ArrayList<Vec3>();
        for(int i=0;i<8;i++)vertices.add(x.scale(i&1).add(y.scale((i>>1)&1)).add(z.scale((i>>2)&1)));
        var shape=new ConvexBox(vertices);var body=new AABB(.2,.2,.2,.8,.8,.8);
        var escapes=shape.escapeVectors(body);
        check(!escapes.isEmpty(),"Penetrating affine fixture needs escape candidates");
        for(var escape:escapes)check(Double.isFinite(escape.lengthSqr()),"Accepted subnormal edge generated a nonfinite escape vector");
        h.succeed();
    }
    @GameTest public void normalCertificateRemainsBindingInsideTheInterval(GameTestHelper h) {
        var motion=new ConservativeSweep.Motion(t->ConvexBox.of(new AABB(-1,-.1,-1,1,0,1),new Matrix4f().rotateZ((float)(.2*t))),1);
        var path=BodyPath.fromMaterial(motion,new BodyPath.LocalAnchor(3,new Vec3(.5,1,.5)),
            new AABB(-.1,.1,-.1,.1,.3,.1),GravityFrame.VANILLA,new BodyPath.NormalBounds(3,1,0)).orElseThrow();
        check(path.displacement(.5).isEmpty(),"An interior normal contradicted its certified minimum but still produced displacement");
        h.succeed();
    }
    @GameTest public void planeCardinalityIsRejectedBeforeCopyingProviderEntries(GameTestHelper h) {
        List<ConservativeSweep.Plane> oversized=new AbstractList<>() {
            public int size(){return 7;}
            public ConservativeSweep.Plane get(int index){throw new AssertionError("Oversized plane collection was traversed before its cap");}
        };
        rejects(()->new ConservativeSweep.Motion(t->box(UNIT),0,Vec3.ZERO,oversized));
        h.succeed();
    }
}
