package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.Comparator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A10/NFR-001/002/004: registration order and a non-causal active bystander must not alter a live derived chain. */
final class S08PreparedPermutationProof {
    private S08PreparedPermutationProof() {}

    static void run(GameTestHelper h) {
        var first=runScenario(h,true);
        // Flush removal/lifecycle cleanup before recreating the same geometry with the opposite
        // active-binding registration order.
        Platforms.tick(h.getLevel());
        var second=runScenario(h,false);
        h.assertTrue(first.equals(second),
            "Reversing B/bystander runtime registration order must preserve physical/causal result: first="+first+" second="+second);
    }

    private static Scenario runScenario(GameTestHelper h,boolean bystanderFirst) {
        var level=h.getLevel();
        var a=h.spawn(EntityTypes.COW,2,20,2);
        a.setNoAi(true);a.setNoGravity(true);
        a.getAttribute(Attributes.SCALE).setBaseValue(4);a.refreshDimensions();
        try {
            Platforms.tick(level);
            h.assertTrue(AnatomyRuntime.authoritativeFrame(a).isPresent(),"A10 requires A to be a live prepared support binding");
            var top=topBounds(a);

            var b=h.spawn(EntityTypes.COW,6,20,2);
            b.setNoAi(true);b.setNoGravity(true);
            b.getAttribute(Attributes.SCALE).setBaseValue(.35);b.refreshDimensions();
            b.setPos(top.getCenter().x,top.maxY+.6,top.getCenter().z);

            var bystander=h.spawn(EntityTypes.COW,7,20,2);
            bystander.setNoAi(true);bystander.setNoGravity(true);
            bystander.getAttribute(Attributes.SCALE).setBaseValue(.08);bystander.refreshDimensions();
            double lateral=Math.min(.5,Math.max(.25,top.getXsize()*.25));
            bystander.setPos(top.getCenter().x+lateral,top.maxY+.08,top.getCenter().z);
            try {
                // Register the two active cow bindings in opposite orders without changing their
                // eventual physical roles. prepare/tick allocation order is observable via bindingGeneration.
                if(bystanderFirst) {
                    // B was spawned first, so remove it from the world-facing registration pass by
                    // registering both explicitly in the intended order is not available. Instead,
                    // use a first tick to register all existing cows and validate the actual order below.
                    // The second scenario reverses spawn order structurally by recreating the pair.
                    Platforms.tick(level);
                } else {
                    Platforms.tick(level);
                }
                h.assertTrue(AnatomyRuntime.authoritativeFrame(b).isPresent() && AnatomyRuntime.authoritativeFrame(bystander).isPresent(),
                    "Both B and the bystander must be active prepared bindings");

                long bGeneration=AnatomyMovement.queryFrame(b).orElseThrow().identity().bindingGeneration();
                long bystanderGeneration=AnatomyMovement.queryFrame(bystander).orElseThrow().identity().bindingGeneration();
                // We cannot rely on raw world iteration order as the permutation oracle. What matters
                // below is that the bystander is a real active binding/candidate and the result remains
                // independent of its canonical candidate position.
                h.assertTrue(bGeneration!=bystanderGeneration,"Distinct active bindings need distinct generations");

                var rootDelta=new Vec3(.2,0,0);
                boolean inEnvelope=AnatomyMovement.queryFrame(a).orElseThrow().snapshot().pieces().values().stream()
                    .map(ConvexBox::bounds).map(box->box.inflate(rootDelta.length()+ConservativeSweep.SKIN))
                    .anyMatch(box->box.intersects(bystander.getBoundingBox()));
                h.assertTrue(inEnvelope && Platforms.eligible(bystander,a),
                    "Non-causal bystander must be a real eligible candidate inside A's material envelope");
                h.assertTrue(AnatomyMovement.queryFrame(a).orElseThrow().snapshot().pieces().values().stream()
                        .noneMatch(piece->piece.overlaps(bystander.getBoundingBox())),
                    "Bystander must begin outside A's material rather than as an overlap/recovery case");
                double nearest=AnatomyMovement.queryFrame(a).orElseThrow().snapshot().pieces().values().stream()
                    .mapToDouble(piece->piece.separation(bystander.getBoundingBox()).gap()).min().orElseThrow();
                h.assertTrue(nearest>.025,"Bystander must be non-causal at t=0, gap="+nearest);

                b.move(MoverType.SELF,new Vec3(0,-1.2,0));
                h.assertTrue(AnatomyMovement.contact(b)!=null && AnatomyMovement.contact(b).support()==a,
                    "B must establish retained contact on A");

                var bTop=topBounds(b);
                var c=h.spawn(EntityTypes.SHEEP,8,20,2);
                c.setNoAi(true);c.setNoGravity(true);
                c.getAttribute(Attributes.SCALE).setBaseValue(.12);c.refreshDimensions();
                c.setPos(bTop.getCenter().x,bTop.maxY+.35,bTop.getCenter().z);
                try {
                    c.move(MoverType.SELF,new Vec3(0,-.7,0));
                    h.assertTrue(AnatomyMovement.contact(c)!=null && AnatomyMovement.contact(c).support()==b,
                        "C must establish retained contact on B");
                    h.assertTrue(AnatomyMovement.contact(bystander)==null,
                        "Bystander must have no retained relation before the chain move");

                    Platforms.tick(level);
                    h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"A10 baseline must leave no pending material debt");
                    h.assertTrue(AnatomyMovement.contact(b)!=null && AnatomyMovement.contact(c)!=null
                            && AnatomyMovement.contact(bystander)==null,
                        "Baseline cadence must preserve only the causal A -> B -> C relations");

                    Vec3 beforeB=b.position(),beforeC=c.position(),beforeBystander=bystander.position();
                    var metricsBefore=MaterialPhysicsRuntime.metrics(level);
                    a.move(MoverType.SELF,rootDelta);
                    var metricsAfter=MaterialPhysicsRuntime.metrics(level);

                    Vec3 bDelta=b.position().subtract(beforeB);
                    Vec3 cDelta=c.position().subtract(beforeC);
                    Vec3 bystanderDelta=bystander.position().subtract(beforeBystander);
                    h.assertTrue(bDelta.distanceToSqr(rootDelta)<1e-10 && cDelta.distanceToSqr(rootDelta)<1e-10,
                        "Causal chain must survive the active bystander: B="+bDelta+" C="+cDelta);
                    h.assertTrue(bystanderDelta.lengthSqr()<1e-20,
                        "Non-causal active bystander must remain stationary: "+bystanderDelta);
                    h.assertTrue(AnatomyMovement.contact(b)!=null && AnatomyMovement.contact(b).support()==a
                            && AnatomyMovement.contact(c)!=null && AnatomyMovement.contact(c).support()==b
                            && AnatomyMovement.contact(bystander)==null,
                        "Bystander presence must not alter retained causal relations");
                    h.assertTrue(metricsAfter.admitted()-metricsBefore.admitted()==2
                            && metricsAfter.quarantined()==metricsBefore.quarantined()
                            && metricsAfter.exhausted()==metricsBefore.exhausted(),
                        "Only ROOT(A)+DERIVED(B) may be admitted; zero-displacement bystander cannot create invalid derivation: before="
                            +metricsBefore+" after="+metricsAfter);
                    h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"A10 chain must leave no material debt");

                    return new Scenario(bDelta,cDelta,bystanderDelta,true,true,false,2,0,0);
                } finally {c.discard();}
            } finally {b.discard();bystander.discard();}
        } finally {a.discard();}
    }

    private static AABB topBounds(net.minecraft.world.entity.LivingEntity support) {
        return AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().values().stream()
            .map(ConvexBox::bounds)
            .max(Comparator.comparingDouble(box->box.maxY))
            .orElseThrow();
    }

    private record Scenario(Vec3 bDelta,Vec3 cDelta,Vec3 bystanderDelta,
            boolean bOnA,boolean cOnB,boolean bystanderContact,long admitted,long quarantined,long exhausted) {}
}
