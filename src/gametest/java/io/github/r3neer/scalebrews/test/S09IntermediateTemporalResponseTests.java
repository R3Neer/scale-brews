package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A9 kernel localization: response, not only CCD, must react to an interior-only material hit. */
public final class S09IntermediateTemporalResponseTests {
    @GameTest
    public void stationaryBodyRespondsToRigidSwingThatIsClearAtBothEndpoints(GameTestHelper h) {
        var local=ConvexBox.of(new AABB(1.9,-.1,-.1,2.1,.1,.1),new Matrix4f());
        var body=new AABB(-.08,-.08,1.92,.08,.08,2.08);
        var motion=new ConservativeSweep.Motion(t->rotateY(local,Math.PI*t),2.2*Math.PI,Vec3.ZERO);

        h.assertTrue(!motion.at().apply(0).overlaps(body) && !motion.at().apply(1).overlaps(body),
            "A9 kernel fixture requires both material endpoints to be clear");
        h.assertTrue(motion.at().apply(.5).overlaps(body),
            "A9 kernel fixture requires a strict interior overlap");
        var ccd=ConservativeSweep.query(body,Vec3.ZERO,motion,256);
        h.assertTrue(ccd.status()==ConservativeSweep.Status.CONTACT && ccd.safeFraction()>0 && ccd.safeFraction()<1,
            "CCD control must detect the interior-only swing contact: "+ccd);

        var response=TemporalResponse.resolve(body,Vec3.ZERO,Map.of("swing",motion),32,256);
        h.assertTrue(response.status()==TemporalResponse.Status.COMPLETE,
            "A resolvable interior-only material hit must complete rather than disappear/exhaust: "+response);
        h.assertTrue(response.displacement().lengthSqr()>1e-12,
            "FR-049 requires the stationary body to receive a physical response from the interior-only hit: "+response);
        h.assertTrue(!motion.at().apply(1).overlaps(body.move(response.displacement())),
            "The response must finish outside the clear endpoint geometry");
        h.assertTrue(response.contacts().stream().anyMatch(c->c.piece().equals("swing") && c.time()>0 && c.time()<1),
            "TemporalResponse must retain evidence of the strict interior contact: "+response.contacts());
        h.succeed();
    }

    /** Reserved S09 holdout: the material contact exists for less than two percent of the interval. */
    @GameTest
    public void veryShortInteriorTouchCannotBeSkippedWhenPieceRetracts(GameTestHelper h) {
        var local=ConvexBox.of(new AABB(1.98,-.02,-.02,2.02,.02,.02),new Matrix4f());
        var body=new AABB(-.015,-.015,1.985,.015,.015,2.015);
        // Max radius is just over 2.02; 2.05*pi safely bounds every point's angular speed.
        var motion=new ConservativeSweep.Motion(t->rotateY(local,Math.PI*t),2.05*Math.PI,Vec3.ZERO);

        h.assertTrue(!motion.at().apply(0).overlaps(body) && !motion.at().apply(1).overlaps(body),
            "Short-touch holdout requires clear endpoints");
        h.assertTrue(!motion.at().apply(.49).overlaps(body) && motion.at().apply(.5).overlaps(body)
                && !motion.at().apply(.51).overlaps(body),
            "Short-touch holdout must enter and leave material contact inside a window narrower than 2% of the interval");

        var ccd=ConservativeSweep.query(body,Vec3.ZERO,motion,256);
        h.assertTrue(ccd.status()==ConservativeSweep.Status.CONTACT
                && ccd.safeFraction()>.49 && ccd.safeFraction()<.51,
            "CCD must not step over the narrow interior contact before the piece retracts: "+ccd);

        var response=TemporalResponse.resolve(body,Vec3.ZERO,Map.of("short_swing",motion),32,256);
        h.assertTrue(response.status()==TemporalResponse.Status.COMPLETE,
            "A narrow but real material contact must resolve completely rather than disappear or exhaust: "+response);
        h.assertTrue(response.displacement().lengthSqr()>1e-12,
            "FR-049 applies even when the material contact exists only briefly: "+response);
        h.assertTrue(response.contacts().stream().anyMatch(c->c.piece().equals("short_swing")
                && c.time()>.49 && c.time()<.51),
            "Response evidence must preserve the brief interior hit itself: "+response.contacts());
        h.assertTrue(!motion.at().apply(1).overlaps(body.move(response.displacement())),
            "Retraction must leave the resolved body clear at the material endpoint");
        h.succeed();
    }

    private static ConvexBox rotateY(ConvexBox box,double radians) {
        double sin=Math.sin(radians),cos=Math.cos(radians);
        return new ConvexBox(box.vertices().stream().map(v->new Vec3(v.x*cos-v.z*sin,v.y,v.x*sin+v.z*cos)).toList());
    }
}
