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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A10/NFR-001/002/004: registration order and a non-causal active bystander must not alter a live derived chain. */
final class S08PreparedPermutationProof {
    private S08PreparedPermutationProof() {}

    static void run(GameTestHelper h) {
        var bystanderFirst=runScenario(h,true);
        Platforms.tick(h.getLevel()); // flush removed bindings before reusing the same local geometry
        var chainFirst=runScenario(h,false);
        assertSame(h,bystanderFirst.bDelta(),chainFirst.bDelta(),"B final displacement must ignore registration permutation");
        assertSame(h,bystanderFirst.cDelta(),chainFirst.cDelta(),"C final displacement must ignore registration permutation");
        assertSame(h,bystanderFirst.bystanderDelta(),chainFirst.bystanderDelta(),"Bystander result must ignore registration permutation");
        h.assertTrue(bystanderFirst.admitted()==chainFirst.admitted()
                && bystanderFirst.quarantined()==chainFirst.quarantined()
                && bystanderFirst.exhausted()==chainFirst.exhausted(),
            "Registration permutation must preserve causal metrics: bystander-first="+bystanderFirst+" chain-first="+chainFirst);
    }

    private static Scenario runScenario(GameTestHelper h,boolean bystanderFirst) {
        var level=h.getLevel();
        var a=h.spawn(EntityTypes.COW,2,20,2);
        a.setNoAi(true);a.setNoGravity(true);
        a.getAttribute(Attributes.SCALE).setBaseValue(4);a.refreshDimensions();
        LivingEntity b=null,bystander=null;
        try {
            Platforms.tick(level);
            h.assertTrue(AnatomyRuntime.authoritativeFrame(a).isPresent(),"A10 requires A to be a live prepared support binding");
            var top=topBounds(a);

            long bGeneration,bystanderGeneration;
            if(bystanderFirst) {
                bystander=spawnBystander(h,a);
                Platforms.tick(level);
                h.assertTrue(AnatomyRuntime.authoritativeFrame(bystander).isPresent(),"Bystander-first permutation must register the bystander");
                bystanderGeneration=AnatomyMovement.queryFrame(bystander).orElseThrow().identity().bindingGeneration();

                b=spawnDependent(h,top);
                Platforms.tick(level);
                h.assertTrue(AnatomyRuntime.authoritativeFrame(b).isPresent(),"Bystander-first permutation must then register B");
                bGeneration=AnatomyMovement.queryFrame(b).orElseThrow().identity().bindingGeneration();
                h.assertTrue(bystanderGeneration<bGeneration,
                    "Fixture must really register bystander before B: bystander="+bystanderGeneration+" B="+bGeneration);
            } else {
                b=spawnDependent(h,top);
                Platforms.tick(level);
                h.assertTrue(AnatomyRuntime.authoritativeFrame(b).isPresent(),"Chain-first permutation must register B first");
                bGeneration=AnatomyMovement.queryFrame(b).orElseThrow().identity().bindingGeneration();

                bystander=spawnBystander(h,a);
                Platforms.tick(level);
                h.assertTrue(AnatomyRuntime.authoritativeFrame(bystander).isPresent(),"Chain-first permutation must then register the bystander");
                bystanderGeneration=AnatomyMovement.queryFrame(bystander).orElseThrow().identity().bindingGeneration();
                h.assertTrue(bGeneration<bystanderGeneration,
                    "Fixture must really register B before bystander: B="+bGeneration+" bystander="+bystanderGeneration);
            }

            final LivingEntity activeB=b;
            final LivingEntity activeBystander=bystander;
            var rootDelta=new Vec3(.2,0,0);
            boolean inEnvelope=AnatomyMovement.queryFrame(a).orElseThrow().snapshot().pieces().values().stream()
                .map(ConvexBox::bounds).map(box->box.inflate(rootDelta.length()+ConservativeSweep.SKIN))
                .anyMatch(box->box.intersects(activeBystander.getBoundingBox()));
            h.assertTrue(inEnvelope && Platforms.eligible(activeBystander,a),
                "Non-causal bystander must be a real eligible candidate inside A's material envelope");
            h.assertTrue(AnatomyMovement.queryFrame(a).orElseThrow().snapshot().pieces().values().stream()
                    .noneMatch(piece->piece.overlaps(activeBystander.getBoundingBox())),
                "Bystander must begin outside A's material rather than as overlap/recovery");
            double nearest=AnatomyMovement.queryFrame(a).orElseThrow().snapshot().pieces().values().stream()
                .mapToDouble(piece->piece.separation(activeBystander.getBoundingBox()).gap()).min().orElseThrow();
            h.assertTrue(nearest>.025,"Bystander must be non-causal at t=0, gap="+nearest);

            activeB.move(MoverType.SELF,new Vec3(0,-1.2,0));
            h.assertTrue(AnatomyMovement.contact(activeB)!=null && AnatomyMovement.contact(activeB).support()==a,
                "B must establish retained contact on A");

            var bTop=topBounds(activeB);
            var c=h.spawn(EntityTypes.SHEEP,8,20,2);
            c.setNoAi(true);c.setNoGravity(true);
            c.getAttribute(Attributes.SCALE).setBaseValue(.12);c.refreshDimensions();
            c.setPos(bTop.getCenter().x,bTop.maxY+.35,bTop.getCenter().z);
            try {
                c.move(MoverType.SELF,new Vec3(0,-.7,0));
                h.assertTrue(AnatomyMovement.contact(c)!=null && AnatomyMovement.contact(c).support()==activeB,
                    "C must establish retained contact on B");
                h.assertTrue(AnatomyMovement.contact(activeBystander)==null,
                    "Bystander must have no retained relation before the chain move");

                Platforms.tick(level);
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"A10 baseline must leave no pending material debt");
                h.assertTrue(AnatomyMovement.contact(activeB)!=null && AnatomyMovement.contact(c)!=null
                        && AnatomyMovement.contact(activeBystander)==null,
                    "Baseline cadence must preserve only the causal A -> B -> C relations");

                Vec3 beforeB=activeB.position(),beforeC=c.position(),beforeBystander=activeBystander.position();
                var metricsBefore=MaterialPhysicsRuntime.metrics(level);
                a.move(MoverType.SELF,rootDelta);
                var metricsAfter=MaterialPhysicsRuntime.metrics(level);

                Vec3 bDelta=activeB.position().subtract(beforeB);
                Vec3 cDelta=c.position().subtract(beforeC);
                Vec3 bystanderDelta=activeBystander.position().subtract(beforeBystander);
                assertSame(h,bDelta,rootDelta,"Causal B carry must survive the active bystander");
                assertSame(h,cDelta,rootDelta,"Derived C carry must survive the active bystander");
                h.assertTrue(bystanderDelta.lengthSqr()<1e-20,
                    "Non-causal active bystander must remain stationary: "+bystanderDelta);
                h.assertTrue(AnatomyMovement.contact(activeB)!=null && AnatomyMovement.contact(activeB).support()==a
                        && AnatomyMovement.contact(c)!=null && AnatomyMovement.contact(c).support()==activeB
                        && AnatomyMovement.contact(activeBystander)==null,
                    "Bystander presence must not alter retained causal relations");
                long admitted=metricsAfter.admitted()-metricsBefore.admitted();
                long quarantined=metricsAfter.quarantined()-metricsBefore.quarantined();
                long exhausted=metricsAfter.exhausted()-metricsBefore.exhausted();
                h.assertTrue(admitted==2 && quarantined==0 && exhausted==0,
                    "Only ROOT(A)+DERIVED(B) may be admitted; a zero-displacement active bystander cannot create INVALID_DERIVATION: before="
                        +metricsBefore+" after="+metricsAfter);
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"A10 chain must leave no material debt");
                return new Scenario(bDelta,cDelta,bystanderDelta,admitted,quarantined,exhausted);
            } finally {c.discard();}
        } finally {
            if(b!=null)b.discard();
            if(bystander!=null)bystander.discard();
            a.discard();
        }
    }

    private static LivingEntity spawnDependent(GameTestHelper h,AABB top) {
        var b=h.spawn(EntityTypes.COW,6,20,2);
        b.setNoAi(true);b.setNoGravity(true);
        b.getAttribute(Attributes.SCALE).setBaseValue(.35);b.refreshDimensions();
        b.setPos(top.getCenter().x,top.maxY+.6,top.getCenter().z);
        return b;
    }

    /**
     * Put the bystander just outside A's global -X material boundary. The +X ROOT motion therefore
     * moves every A piece away from it, while the 0.05 gap still places its tiny body inside the
     * conservative 0.2-block interval envelope. It is a real broadphase candidate but cannot be a
     * causal contact, and it stays well away from the top-centred B -> C chain.
     */
    private static LivingEntity spawnBystander(GameTestHelper h,LivingEntity a) {
        var bystander=h.spawn(EntityTypes.COW,7,20,2);
        bystander.setNoAi(true);bystander.setNoGravity(true);
        bystander.getAttribute(Attributes.SCALE).setBaseValue(.08);bystander.refreshDimensions();
        var side=AnatomyMovement.queryFrame(a).orElseThrow().snapshot().pieces().values().stream()
            .map(ConvexBox::bounds)
            .min(Comparator.comparingDouble((AABB box)->box.minX))
            .orElseThrow();
        double gap=.05;
        double halfWidth=bystander.getBoundingBox().getXsize()*.5;
        double height=bystander.getBoundingBox().getYsize();
        bystander.setPos(side.minX-gap-halfWidth,side.getCenter().y-height*.5,side.getCenter().z);
        return bystander;
    }

    private static AABB topBounds(LivingEntity support) {
        return AnatomyMovement.queryFrame(support).orElseThrow().snapshot().pieces().values().stream()
            .map(ConvexBox::bounds)
            .max(Comparator.comparingDouble(box->box.maxY))
            .orElseThrow();
    }

    private static void assertSame(GameTestHelper h,Vec3 actual,Vec3 expected,String message) {
        h.assertTrue(actual.distanceToSqr(expected)<1e-10,message+": expected="+expected+" actual="+actual);
    }

    private record Scenario(Vec3 bDelta,Vec3 cDelta,Vec3 bystanderDelta,long admitted,long quarantined,long exhausted) {}
}
