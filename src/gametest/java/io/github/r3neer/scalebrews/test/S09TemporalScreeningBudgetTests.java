package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.HierarchyMotion;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A9/A11: screening may reduce CCD work, never turn a within-budget clear sweep into exhaustion. */
public final class S09TemporalScreeningBudgetTests {
    @GameTest
    public void screeningCannotCostMoreThanTheClearSweepItReplaces(GameTestHelper h) {
        var body=new AABB(-.05,-.05,-.05,.05,.05,.05);
        // Keep the actual geometry static and clear by ~0.064 blocks, but use a valid loose
        // deformation bound near the prepared cow's historical 13-16 block/tick bounds. This
        // reproduces the A9 screening ratio without relying on a theatrical speed=200 certificate.
        var distant=ConvexBox.of(new AABB(.114,-.05,-.05,.214,.05,.05),new Matrix4f());
        var motion=new ConservativeSweep.Motion(t->distant,16.0,Vec3.ZERO);

        var direct=ConservativeSweep.query(body,Vec3.ZERO,motion,256);
        h.assertTrue(direct.status()==ConservativeSweep.Status.CLEAR && direct.evaluations()>128 && direct.evaluations()<256,
            "Fixture requires a valid clear CCD that is expensive but still inside the contractual 256 budget: "+direct);

        var response=TemporalResponse.resolve(body,Vec3.ZERO,Map.of("clear-but-loose",motion),32,256);
        h.assertTrue(response.status()==TemporalResponse.Status.COMPLETE,
            "A screening optimization may not make a single-piece CLEAR query exhaust when the unscreened CCD already fits the same 256 budget: direct="
                +direct+" response="+response);
        h.assertTrue(response.displacement().lengthSqr()<1e-20 && response.contacts().isEmpty(),
            "The clear loose-bound piece must remain physically inert: "+response);
        h.succeed();
    }

    @GameTest
    public void horizonClippingKeepsContactsInsideSimultaneousTolerance(GameTestHelper h) {
        var body=new AABB(-1,0,-1,-.8,.2,-.8);
        var request=new Vec3(1.5,0,0);
        var firstBox=ConvexBox.of(new AABB(0,-2,-2,1,2,2),new Matrix4f());
        // 7.5e-11 blocks / 1.5 blocks of body travel = 5e-11 material-time separation,
        // deliberately inside TemporalResponse.TIME_EPS=1e-10 while remaining non-zero.
        var nearBox=firstBox.move(new Vec3(7.5e-11,0,0));
        var first=new ConservativeSweep.Motion(t->firstBox,0);
        var near=new ConservativeSweep.Motion(t->nearBox,0);
        var firstHit=ConservativeSweep.query(body,request,first,16);
        var nearHit=ConservativeSweep.query(body,request,near,16);
        double separation=nearHit.safeFraction()-firstHit.safeFraction();
        h.assertTrue(firstHit.status()==ConservativeSweep.Status.CONTACT && nearHit.status()==ConservativeSweep.Status.CONTACT
                && separation>1e-12 && separation<1e-10,
            "Fixture must preserve two distinct contacts inside the simultaneous-time tolerance: first="+firstHit+" near="+nearHit);

        var response=TemporalResponse.resolve(body,request,Map.of("a_first",first,"b_near",near),32,256);
        var ids=java.util.Set.copyOf(response.contacts().stream().map(TemporalResponse.Contact::piece).toList());
        h.assertTrue(response.status()==TemporalResponse.Status.COMPLETE && ids.equals(java.util.Set.of("a_first","b_near")),
            "A bounded earliest-contact horizon must still collect every contact inside TIME_EPS: "+response);
        h.succeed();
    }

    @GameTest
    public void rigidHierarchyDoesNotMultiplyRootYawSpeedByDimension(GameTestHelper h) {
        var parts=List.of(
            new ModelGeometry.Part("p0",null,ModelGeometry.values(new Matrix4f().translation(.25f,0,0))),
            new ModelGeometry.Part("p1","p0",ModelGeometry.values(new Matrix4f().translation(.25f,0,0))),
            new ModelGeometry.Part("p2","p1",ModelGeometry.values(new Matrix4f().translation(.25f,0,0))),
            new ModelGeometry.Part("p3","p2",ModelGeometry.values(new Matrix4f().translation(.25f,0,0)))
        );
        var model=new ModelGeometry(1,"s09-rigid-chain","test",parts,
            List.of(new ModelGeometry.Piece("tip","p3",List.of(-.1d,-.1d,-.1d),List.of(.1d,.1d,.1d),null)),
            ModelGeometry.values(new Matrix4f()));
        var motion=new HierarchyMotion(model,Map.of(),Map.of(),new Matrix4f(),
            new Matrix4f().rotateY((float)Math.toRadians(120)),Vec3.ZERO,Vec3.ZERO,AnatomyFilter.DEFAULT)
            .pieces().get("tip");

        h.assertTrue(motion.maxPointSpeed()<3,
            "Rigid identity-linear joints must not compound a sqrt(3) Frobenius factor into root yaw speed: "+motion.maxPointSpeed());
        var previous=motion.at().apply(0);
        for(int step=1;step<=1000;step++) {
            var next=motion.at().apply(step/1000d);
            for(int vertex=0;vertex<8;vertex++)
                h.assertTrue(next.vertices().get(vertex).distanceTo(previous.vertices().get(vertex))*1000<=motion.maxPointSpeed()+.001,
                    "Tightened hierarchy speed must remain a conservative bound at step "+step+" vertex "+vertex);
            previous=next;
        }
        h.succeed();
    }
}
