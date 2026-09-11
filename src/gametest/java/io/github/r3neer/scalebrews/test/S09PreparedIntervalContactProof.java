package io.github.r3neer.scalebrews.test;

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
import net.minecraft.world.phys.Vec3;

/** S09 prepared material-interval adversaries: stationary reacquisition and endpoint non-magnetization. */
final class S09PreparedIntervalContactProof {
    private S09PreparedIntervalContactProof() {}

    /** A8: a real prepared material interval must establish contact without any own-move from the body. */
    static void run(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,12,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        try {
            Platforms.tick(level);
            h.assertTrue(AnatomyRuntime.authoritativeFrame(support).isPresent(),
                "S09 A8 requires a genuine prepared cow binding, not a manual provider fixture");
            var frame=AnatomyMovement.queryFrame(support).orElseThrow();
            var topEntry=frame.snapshot().pieces().entrySet().stream()
                .max(Comparator.comparingDouble(entry->entry.getValue().bounds().maxY))
                .orElseThrow();
            var top=topEntry.getValue();

            var body=h.spawn(EntityTypes.OAK_BOAT,12,24,2);
            body.setNoGravity(true);body.setDeltaMovement(Vec3.ZERO);
            try {
                var center=top.bounds().getCenter();
                double initialGap=.10;
                body.setPos(center.x,top.bounds().maxY+initialGap,center.z);
                var captured=body.getBoundingBox();
                h.assertTrue(Platforms.eligible(body,support),
                    "Prepared stationary-contact fixture requires the boat to be an eligible body on the cow");
                h.assertTrue(AnatomyMovement.contact(body)==null,
                    "A8 body must start without retained material contact");
                h.assertTrue(frame.snapshot().pieces().values().stream().noneMatch(piece->piece.overlaps(captured)),
                    "A8 body must begin geometrically separated from every support piece");
                double nearest=frame.snapshot().pieces().values().stream()
                    .mapToDouble(piece->piece.separation(captured).gap()).min().orElseThrow();
                h.assertTrue(nearest>.025,
                    "A8 must begin outside contact-retention tolerance, nearest gap="+nearest);
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                    "A8 baseline must contain no pre-existing material debt");

                var before=body.position();
                var metricsBefore=MaterialPhysicsRuntime.metrics(level);
                var lift=new Vec3(0,.20,0);

                // Production route: the support's own Entity.move captures/commits ROOT and drains
                // synchronously. The boat deliberately never calls move/collide in this scenario.
                support.move(MoverType.SELF,lift);

                var contact=AnatomyMovement.contact(body);
                h.assertTrue(contact!=null && contact.support()==support,
                    "A published ROOT interval crossing a stationary body must establish material contact without own-move");
                h.assertTrue(AnatomyMovement.supported(body),
                    "The newly established A8 contact must be valid against the certified after geometry");
                var applied=body.position().subtract(before);
                h.assertTrue(applied.y>.05 && applied.y<.16 && Math.abs(applied.x)<1e-8 && Math.abs(applied.z)<1e-8,
                    "The material interval must separate the stationary boat only along the support normal: "+applied);

                var metricsAfter=MaterialPhysicsRuntime.metrics(level);
                h.assertTrue(metricsAfter.admitted()-metricsBefore.admitted()==1
                        && metricsAfter.quarantined()==metricsBefore.quarantined()
                        && metricsAfter.exhausted()==metricsBefore.exhausted(),
                    "A valid stationary reacquisition must be one admitted material event without quarantine/exhaustion: before="
                        +metricsBefore+" after="+metricsAfter);
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                    "Synchronous stationary reacquisition must leave no material interval debt");
            } finally {
                AnatomyMovement.clear(body);
                body.discard();
            }
        } finally {support.discard();}
    }

    /** A10: an endpoint inside retention tolerance but never temporally touching may not create contact. */
    static void nearEndpointWithoutTemporalContact(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,20,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(4);support.refreshDimensions();
        try {
            Platforms.tick(level);
            h.assertTrue(AnatomyRuntime.authoritativeFrame(support).isPresent(),
                "S09 A10 requires a genuine prepared binding so endpoint proximity is tested through the production material route");
            var beforeFrame=AnatomyMovement.queryFrame(support).orElseThrow();
            var top=beforeFrame.snapshot().pieces().values().stream()
                .max(Comparator.comparingDouble(piece->piece.bounds().maxY)).orElseThrow();
            var center=top.bounds().getCenter();

            var body=h.spawn(EntityTypes.OAK_BOAT,20,24,2);
            body.setNoGravity(true);body.setDeltaMovement(Vec3.ZERO);
            try {
                double initialGap=.060;
                body.setPos(center.x,top.bounds().maxY+initialGap,center.z);
                var captured=body.getBoundingBox();
                double beforeGap=beforeFrame.snapshot().pieces().values().stream()
                    .mapToDouble(piece->piece.separation(captured).gap()).min().orElseThrow();
                h.assertTrue(beforeGap>.025 && AnatomyMovement.contact(body)==null && AnatomyMovement.surface(body)==null,
                    "A10 must start genuinely outside retention tolerance with no prior relation, gap="+beforeGap);
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                    "A10 baseline must contain no material debt from earlier prepared scenarios");

                var bodyBefore=body.position();var metricsBefore=MaterialPhysicsRuntime.metrics(level);
                support.move(MoverType.SELF,new Vec3(0,.040,0));

                var afterFrame=AnatomyMovement.queryFrame(support).orElseThrow();
                double afterGap=afterFrame.snapshot().pieces().values().stream()
                    .mapToDouble(piece->piece.separation(body.getBoundingBox()).gap()).min().orElseThrow();
                h.assertTrue(afterGap>io.github.r3neer.scalebrews.collision.physics.ConservativeSweep.SKIN
                        && afterGap<.025,
                    "A10 endpoint must finish visually/retention-close while remaining strictly outside CCD contact skin, gap="+afterGap);
                h.assertTrue(afterFrame.snapshot().pieces().values().stream().noneMatch(piece->piece.overlaps(body.getBoundingBox())),
                    "A10 material geometry may approach but never geometrically overlap the stationary body");
                h.assertTrue(body.position().equals(bodyBefore),
                    "Endpoint proximity without temporal contact must not move the stationary body");
                h.assertTrue(AnatomyMovement.contact(body)==null && AnatomyMovement.surface(body)==null,
                    "A10 forbids magnetizing a new material relation from endpoint proximity alone");

                var metricsAfter=MaterialPhysicsRuntime.metrics(level);
                h.assertTrue(metricsAfter.admitted()-metricsBefore.admitted()==1
                        && metricsAfter.quarantined()==metricsBefore.quarantined()
                        && metricsAfter.exhausted()==metricsBefore.exhausted(),
                    "A10 near-endpoint ROOT interval must publish cleanly without turning absence of contact into failure: before="
                        +metricsBefore+" after="+metricsAfter);
                h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),
                    "A10 synchronous prepared interval must leave no material debt");
            } finally {
                AnatomyMovement.clear(body);body.discard();
            }
        } finally {support.discard();}
    }
}
