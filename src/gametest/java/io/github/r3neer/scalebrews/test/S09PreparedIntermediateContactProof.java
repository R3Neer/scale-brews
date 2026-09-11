package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** S09 A9: a published material interval must hit a stationary body even when both endpoints are clear. */
final class S09PreparedIntermediateContactProof {
    private S09PreparedIntermediateContactProof() {}

    static void run(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,20,20,20);
        support.setNoAi(true);support.setNoGravity(true);
        support.getAttribute(Attributes.SCALE).setBaseValue(1);support.refreshDimensions();
        var body=h.spawn(EntityTypes.SHEEP,24,20,20);
        body.setNoAi(true);body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.08);body.refreshDimensions();
        try {
            Platforms.tick(level);
            var before=AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(AnatomyRuntime.authoritativeFrame(support).isPresent(),"A9 requires a live prepared cow binding");
            h.assertTrue(AnatomyRuntime.authoritativeFrame(body).isEmpty(),"A9 body must not itself be an active prepared support binding");
            h.assertTrue(Platforms.eligible(body,support),"A9 body must be a real material candidate for the prepared cow");
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"A9 baseline must start with no pending material work");

            float yawDelta=120f;
            var certified=hypotheticalRootMotion(support,before,yawDelta);
            h.assertTrue(certified!=null && !certified.pieces().isEmpty(),"A9 fixture requires a certified root-yaw material trajectory");
            assertRuntimeEnvelope(h,certified,"fixture placement interval");

            String chosen=null;AABB chosenMid=null;
            for(var entry:certified.pieces().entrySet().stream().sorted(Comparator.comparing(Map.Entry<String,?>::getKey)).toList()) {
                var mid=entry.getValue().at().apply(.5);var center=mid.bounds().getCenter();
                body.setPos(center.x,center.y-body.getBbHeight()*.5,center.z);
                var candidate=body.getBoundingBox();
                if(!mid.overlaps(candidate))continue;
                if(!endpointClear(certified,candidate,0) || !endpointClear(certified,candidate,1))continue;
                double gap0=minGap(certified,candidate,0),gap1=minGap(certified,candidate,1);
                if(gap0<=.05 || gap1<=.05)continue;
                chosen=entry.getKey();chosenMid=mid.bounds();break;
            }
            h.assertTrue(chosen!=null,"A9 fixture must find a real cow piece whose certified yaw path crosses the tiny stationary body only inside the interval");
            var captured=body.getBoundingBox();
            h.assertTrue(AnatomyMovement.contact(body)==null && AnatomyMovement.surface(body)==null,"A9 body must begin with no retained contact");
            h.assertTrue(endpointClear(certified,captured,0) && endpointClear(certified,captured,1),"A9 body must be clear of every support piece at both certified endpoints");
            h.assertTrue(certified.pieces().get(chosen).at().apply(.5).overlaps(captured),
                "A9 selected piece must really intersect the stationary body at t=.5; piece="+chosen+" mid="+chosenMid);

            var bodyBefore=body.position();var metricsBefore=MaterialPhysicsRuntime.metrics(level);
            var capture=MaterialIntervalRuntime.captureRoot(support);
            h.assertTrue(capture.before()!=null && capture.before().identity().equals(before.identity()),"A9 live root capture must preserve the prepared binding identity");
            support.yBodyRot=capture.root().yaw()+yawDelta;
            MaterialIntervalRuntime.commitRoot(support,capture);

            var liveAfter=AnatomyMovement.queryFrame(support).orElseThrow();
            var liveHandle=new GeometryProvider.MotionIntervalHandle(capture.before().identity(),1,capture.before(),liveAfter);
            var liveMotion=AnatomyRuntime.interval(support,liveHandle).orElseThrow();
            assertRuntimeEnvelope(h,liveMotion,"exact live interval");
            h.assertTrue(endpointClear(liveMotion,captured,0) && endpointClear(liveMotion,captured,1),"A9 live interval itself must remain endpoint-clear after publication");
            h.assertTrue(liveMotion.pieces().containsKey(chosen) && liveMotion.pieces().get(chosen).at().apply(.5).overlaps(captured),
                "A9 live interval must preserve the certified interior hit used by the fixture; piece="+chosen);

            var chosenResponse=TemporalResponse.resolve(captured,Vec3.ZERO,
                Map.of(chosen,liveMotion.pieces().get(chosen)),32,256);
            h.assertTrue(chosenResponse.status()==TemporalResponse.Status.COMPLETE && chosenResponse.displacement().lengthSqr()>1e-10,
                "A9 chosen live piece alone must produce a complete non-zero interior response: "+chosenResponse);

            // Localize the full-manifold exhaustion in canonical piece order. Every prefix should
            // remain bounded; an irrelevant earlier piece must not consume the entire response
            // budget before the causal piece can even be considered.
            var progressive=new TreeMap<String,ConservativeSweep.Motion>();
            String failedAt=null;TemporalResponse.Result failedResponse=null;
            for(var id:new java.util.TreeSet<>(liveMotion.pieces().keySet())) {
                progressive.put(id,liveMotion.pieces().get(id));
                var response=TemporalResponse.resolve(captured,Vec3.ZERO,progressive,32,256);
                if(response.status()==TemporalResponse.Status.ITERATION_LIMIT) {
                    failedAt=id;failedResponse=response;break;
                }
            }
            TemporalResponse.Result failedSolo256=null;ConservativeSweep.Result failedSweep4096=null;double failedMaxPointSpeed=Double.NaN;
            if(failedAt!=null) {
                var failedMotion=liveMotion.pieces().get(failedAt);
                failedSolo256=TemporalResponse.resolve(captured,Vec3.ZERO,Map.of(failedAt,failedMotion),32,256);
                failedSweep4096=ConservativeSweep.query(captured,Vec3.ZERO,failedMotion,4096);
                failedMaxPointSpeed=failedMotion.maxPointSpeed();
            }
            h.assertTrue(failedAt==null,
                "A9 canonical cow prefix must not exhaust before resolving the interior contact: chosen="+chosen
                    +" failedAt="+failedAt+" prefix="+progressive.keySet()+" response="+failedResponse
                    +" failedSolo256="+failedSolo256+" failedSweep4096="+failedSweep4096
                    +" failedMaxPointSpeed="+failedMaxPointSpeed);

            var manifoldResponse=TemporalResponse.resolve(captured,Vec3.ZERO,liveMotion.pieces(),32,256);
            h.assertTrue(manifoldResponse.status()==TemporalResponse.Status.COMPLETE && manifoldResponse.displacement().lengthSqr()>1e-10,
                "A9 full live cow manifold must resolve the same interior-only contact without exhausting: "+manifoldResponse);

            MaterialPhysicsRuntime.drain(level);

            var metricsAfter=MaterialPhysicsRuntime.metrics(level);
            long admitted=metricsAfter.admitted()-metricsBefore.admitted();long candidates=metricsAfter.candidates()-metricsBefore.candidates();
            long evaluations=metricsAfter.evaluations()-metricsBefore.evaluations();long quarantined=metricsAfter.quarantined()-metricsBefore.quarantined();
            long exhausted=metricsAfter.exhausted()-metricsBefore.exhausted();
            h.assertTrue(admitted==1 && quarantined==0 && exhausted==0,
                "A9 requires one clean published event before judging its physical response: admitted="+admitted+" candidates="+candidates
                    +" evaluations="+evaluations+" quarantined="+quarantined+" exhausted="+exhausted);
            h.assertTrue(candidates>=1,"A9 material envelope must capture the stationary body; candidates="+candidates+" evaluations="+evaluations);

            var applied=body.position().subtract(bodyBefore);
            h.assertTrue(applied.lengthSqr()>1e-10,
                "FR-049 requires an intermediate-only material hit to affect the stationary body; displacement="+applied
                    +" candidates="+candidates+" evaluations="+evaluations);
            h.assertTrue(MaterialIntervalRuntime.poll(level).isEmpty(),"A9 synchronous root drain must leave no material debt");
            h.assertTrue(AnatomyMovement.contact(body)==null && AnatomyMovement.surface(body)==null,
                "An intermediate-only hit must not invent a retained endpoint contact after the piece has moved away");
            var after=AnatomyMovement.queryFrame(support).orElseThrow();
            h.assertTrue(after.snapshot().pieces().values().stream().noneMatch(piece->piece.overlaps(body.getBoundingBox())),
                "A9 response must finish non-penetrating against the actual endpoint geometry");
        } finally {
            AnatomyMovement.clear(body);MaterialIntervalRuntime.clear(level);MaterialPhysicsRuntime.clear(level);support.discard();body.discard();
        }
    }

    private static GeometryProvider.MotionSnapshot hypotheticalRootMotion(net.minecraft.world.entity.LivingEntity support,
            GeometryProvider.QueryFrame before,float yawDelta) {
        var root0=before.root();
        var root1=new AnatomyMovement.RootFrame(root0.sequence()+1,root0.tick(),root0.origin(),root0.yaw()+yawDelta,root0.scale(),root0.gravity());
        var sample0=before.sample();
        var sample1=new AnatomyPoseHistory.Sample(sample0.inputs(),sample0.origin(),sample0.yaw()+yawDelta,sample0.scale(),sample0.gravity());
        var endpoint1=new GeometryProvider.CausalEndpoint(before.endpoint().frameSerial()+1,before.authorityTick(),before.endpoint().jointSampleTick(),
            root1,sample1,GeometryProvider.Availability.AVAILABLE);
        var syntheticAfter=new GeometryProvider.QueryFrame(before.identity(),endpoint1,before.snapshot());
        var handle=new GeometryProvider.MotionIntervalHandle(before.identity(),1,before,syntheticAfter);
        return AnatomyRuntime.interval(support,handle).orElse(null);
    }

    private static void assertRuntimeEnvelope(GameTestHelper h,GeometryProvider.MotionSnapshot motion,String label) {
        AABB envelope=null;
        try {
            for(var piece:motion.pieces().values()) {
                var first=piece.at().apply(0);double margin=piece.maxPointSpeed()+ConservativeSweep.SKIN;
                h.assertTrue(first!=null && Double.isFinite(margin),label+" must have finite conservative motion bounds");
                var bounds=first.bounds().inflate(margin);envelope=envelope==null?bounds:union(envelope,bounds);
            }
        } catch(RuntimeException rejected) {h.assertTrue(false,label+" must be sampleable for the runtime envelope: "+rejected);}
        h.assertTrue(envelope!=null && envelope.getXsize()<=64 && envelope.getYsize()<=64 && envelope.getZsize()<=64,
            label+" must fit MaterialPhysicsRuntime's 64-block envelope cap, got "+envelope);
    }

    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }
    private static boolean endpointClear(GeometryProvider.MotionSnapshot motion,AABB body,double t) {
        try {for(var piece:motion.pieces().values())if(piece.at().apply(t).overlaps(body))return false;return true;}
        catch(RuntimeException rejected){return false;}
    }
    private static double minGap(GeometryProvider.MotionSnapshot motion,AABB body,double t) {
        double gap=Double.POSITIVE_INFINITY;for(var piece:motion.pieces().values())gap=Math.min(gap,piece.at().apply(t).separation(body).gap());return gap;
    }
}
