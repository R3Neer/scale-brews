package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.Comparator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Prepared S08 holdout: prove the live material runtime, not isolated seams,
 * propagates one anchored cause through A -> B -> C exactly once.
 */
public final class S08PreparedDerivedChainProof {
    private S08PreparedDerivedChainProof() {}

    static void run(GameTestHelper h) {
        activeBindingWithoutMaterialIntervalKeepsLegacyFallback(h);
        derivedChainRunsExactlyOnce(h);
    }

    /**
     * I6 is a temporal ownership boundary, not merely a binding boundary. A live prepared support
     * can mutate through the supported setPos/spatialMutation hook without creating a ROOT interval:
     * until S06 actually publishes a certified contribution, the legacy endpoint path remains the
     * only owner capable of preserving the retained contact.
     */
    private static void activeBindingWithoutMaterialIntervalKeepsLegacyFallback(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,10,20,10);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        try {
            Platforms.tick(level);
            h.assertTrue(AnatomyRuntime.authoritativeFrame(support).isPresent(),
                "I6 fixture requires a genuinely active prepared runtime binding");
            var top=topBounds(support);
            var body=h.spawn(EntityTypes.SHEEP,11,20,10);
            body.setNoAi(true);body.setNoGravity(true);
            body.getAttribute(Attributes.SCALE).setBaseValue(.12);body.refreshDimensions();
            body.setPos(top.getCenter().x,top.maxY+.45,top.getCenter().z);
            try {
                body.move(MoverType.SELF,new Vec3(0,-.9,0));
                h.assertTrue(AnatomyMovement.contact(body)!=null && AnatomyMovement.contact(body).support()==support,
                    "I6 fixture body must retain the active prepared support before external root mutation");

                // Establish the S06 baseline while the support is still stationary.
                Platforms.tick(level);
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                    "Stationary baseline must leave no material contribution pending");
                h.assertTrue(AnatomyMovement.contact(body)!=null && AnatomyMovement.contact(body).support()==support,
                    "Baseline cadence must preserve the retained relation");

                var before=body.position();
                var delta=new Vec3(.2,0,0);
                // Deliberately bypass Entity.move. The production setPos mixin updates the support's
                // live root/spatial frame, but it does not capture/commit a ROOT interval.
                support.setPos(support.position().add(delta));
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                    "Direct active-binding setPos must not magically fabricate a dispatcher-owned ROOT/JOINT interval");

                AnatomyMovement.carry(body);
                assertDelta(h,before,body.position(),delta,
                    "Without a certified material interval, I6 requires legacy carry fallback even for an active runtime binding");
                h.assertTrue(AnatomyMovement.contact(body)!=null && AnatomyMovement.contact(body).support()==support,
                    "Fallback carry must preserve the valid retained relation");
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                    "Legacy fallback must not invent duplicate material debt");
            } finally {body.discard();}
        } finally {support.discard();}
    }

    private static void derivedChainRunsExactlyOnce(GameTestHelper h) {
        var level=h.getLevel();
        var a=h.spawn(EntityTypes.COW,2,20,2);
        a.setNoAi(true);a.setNoGravity(true);
        a.getAttribute(Attributes.SCALE).setBaseValue(4);a.refreshDimensions();
        try {
            // A must be a genuine prepared binding before the rest of the fixture exists.
            Platforms.tick(level);
            h.assertTrue(AnatomyRuntime.authoritativeFrame(a).isPresent(),
                "S08 chain fixture requires A to be a live prepared support binding");
            var aTop=topBounds(a);

            // Spawn and position B before prepare() sees it so no synthetic setup teleport
            // becomes a material interval. B is both the body carried by A and a live support.
            var b=h.spawn(EntityTypes.COW,6,20,2);
            b.setNoAi(true);b.setNoGravity(true);
            b.getAttribute(Attributes.SCALE).setBaseValue(.35);b.refreshDimensions();
            b.setPos(aTop.getCenter().x,aTop.maxY+.6,aTop.getCenter().z);
            try {
                Platforms.tick(level);
                h.assertTrue(AnatomyRuntime.authoritativeFrame(b).isPresent(),
                    "S08 chain fixture requires B to be a live prepared support binding");
                b.move(MoverType.SELF,new Vec3(0,-1.2,0));
                h.assertTrue(AnatomyMovement.contact(b)!=null && AnatomyMovement.contact(b).support()==a,
                    "B must establish a retained anatomical contact on A before the derived chain starts");

                var bTop=topBounds(b);
                // C deliberately has no prepared anatomy binding: it terminates the chain and
                // prevents a third derived event from making the exactly-once oracle ambiguous.
                var c=h.spawn(EntityTypes.SHEEP,7,20,2);
                c.setNoAi(true);c.setNoGravity(true);
                c.getAttribute(Attributes.SCALE).setBaseValue(.12);c.refreshDimensions();
                c.setPos(bTop.getCenter().x,bTop.maxY+.35,bTop.getCenter().z);
                try {
                    c.move(MoverType.SELF,new Vec3(0,-.7,0));
                    h.assertTrue(AnatomyMovement.contact(c)!=null && AnatomyMovement.contact(c).support()==b,
                        "C must establish a retained anatomical contact on B before A moves");
                    h.assertTrue(AnatomyRuntime.authoritativeFrame(c).isEmpty(),
                        "C must terminate this proof rather than emit a third derived interval");

                    // Settle provider/interval baselines after fixture placement. No support moves here.
                    Platforms.tick(level);
                    h.assertTrue(AnatomyMovement.contact(b)!=null && AnatomyMovement.contact(b).support()==a
                            && AnatomyMovement.contact(c)!=null && AnatomyMovement.contact(c).support()==b,
                        "A -> B -> C contacts must survive the baseline cadence");
                    h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                        "Baseline cadence must leave no material work pending before the chain move");

                    var beforeB=b.position();
                    var beforeC=c.position();
                    var metricsBefore=MaterialPhysicsRuntime.metrics(level);
                    var rootDelta=new Vec3(.2,0,0);

                    // Production route: Entity.move captures/commits ROOT and drains synchronously.
                    a.move(MoverType.SELF,rootDelta);

                    var afterB=b.position();
                    var afterC=c.position();
                    assertDelta(h,beforeB,afterB,rootDelta,"A must carry B exactly once");
                    assertDelta(h,beforeC,afterC,rootDelta,"B's derived interval must carry C exactly once");
                    h.assertTrue(AnatomyMovement.contact(b)!=null && AnatomyMovement.contact(b).support()==a,
                        "B must retain its A contact after certified carry");
                    h.assertTrue(AnatomyMovement.contact(c)!=null && AnatomyMovement.contact(c).support()==b,
                        "C must retain its B contact after the derived carry");

                    var metricsAfter=MaterialPhysicsRuntime.metrics(level);
                    h.assertTrue(metricsAfter.admitted()-metricsBefore.admitted()==2,
                        "One root A event plus exactly one derived B event must be admitted; before="
                            +metricsBefore+" after="+metricsAfter);
                    h.assertTrue(metricsAfter.quarantined()==metricsBefore.quarantined()
                            && metricsAfter.exhausted()==metricsBefore.exhausted(),
                        "A valid A -> B -> C chain must not rely on quarantine/exhaustion");
                    h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                        "The synchronous derived chain must leave no ROOT/JOINT debt behind");

                    // I6 ownership fence: the legacy endpoint carry path must see the certified
                    // after anchors and therefore have nothing left to replay.
                    AnatomyMovement.carry(b);
                    AnatomyMovement.carry(c);
                    h.assertTrue(b.position().equals(afterB) && c.position().equals(afterC),
                        "Legacy carry must not replay a dispatcher-owned A -> B -> C displacement");
                } finally {c.discard();}
            } finally {b.discard();}
        } finally {a.discard();}
    }

    private static AABB topBounds(net.minecraft.world.entity.LivingEntity support) {
        return AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().values().stream()
            .map(ConvexBox::bounds)
            .max(Comparator.comparingDouble(box->box.maxY))
            .orElseThrow();
    }

    private static void assertDelta(GameTestHelper h,Vec3 before,Vec3 after,Vec3 expected,String message) {
        var actual=after.subtract(before);
        h.assertTrue(actual.distanceToSqr(expected)<1e-10,message+": expected="+expected+" actual="+actual);
    }
}
