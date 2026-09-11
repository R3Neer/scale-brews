package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S07 adversarial proof that contact truth retains the support/piece that actually produced the CCD hit. */
public final class S07ContactProvenanceTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    private record Input(LivingEntity support,GeometryProvider.MotionIntervalHandle handle,
            GeometryProvider.MotionSnapshot motion,AABB envelope) {}

    @GameTest
    public void zeroDisplacementJointBatchMustAnchorActualHitNotNearbySupport(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var hitSupport=h.spawn(EntityTypes.COW,2,20,2);
        var nearSupport=h.spawn(EntityTypes.COW,3,20,2);
        hitSupport.setNoAi(true);hitSupport.setNoGravity(true);
        nearSupport.setNoAi(true);nearSupport.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.1);body.refreshDimensions();

        Vec3 base=hitSupport.position();
        body.setPos(base.x,base.y+1.2,base.z);
        double bodyBottom=body.getBoundingBox().minY;
        var approximate=ConvexBox.of(new AABB(base.x-1,bodyBottom-.5,base.z-1,base.x+1,bodyBottom,base.z+1),new Matrix4f());
        double correction=approximate.separation(body.getBoundingBox()).gap();
        var hitAfter=approximate.move(new Vec3(0,correction,0));
        var nearAfter=hitAfter.move(new Vec3(0,-.001,0));
        var lift=new Vec3(0,.4,0);
        var hitBefore=hitAfter.move(lift.scale(-1));
        var nearBefore=nearAfter.move(lift.scale(-1));

        AnatomyMovement.activate(level);
        AnatomyMovement.register(hitSupport,e->Optional.of(new GeometryProvider.Snapshot(73,Map.of("hit",hitAfter))));
        AnatomyMovement.register(nearSupport,e->Optional.of(new GeometryProvider.Snapshot(74,Map.of("near",nearAfter))));
        try {
            h.assertTrue(Platforms.eligible(body,hitSupport) && Platforms.eligible(body,nearSupport),
                "Both supports must be eligible so event order, not policy, is the only ambiguity");
            h.assertTrue(Math.abs(hitAfter.separation(body.getBoundingBox()).gap())<1e-9,
                "Hit support must reach the exact body endpoint");
            double nearGap=nearAfter.separation(body.getBoundingBox()).gap();
            h.assertTrue(nearGap>ConservativeSweep.SKIN && nearGap<.025,
                "Distractor support must finish inside retention tolerance without ever reaching the body: "+nearGap);

            long tick=level.getGameTime();
            var near=input(level,nearSupport,74,"test:s07_provenance_near",nearBefore,nearAfter,lift,tick);
            var hit=input(level,hitSupport,73,"test:s07_provenance_hit",hitBefore,hitAfter,lift,tick);
            Vec3 before=body.position();
            var result=resolveBatch(level,List.of(near,hit),body); // Distractor intentionally first.
            h.assertTrue(result.outcomes().stream().allMatch(o->o.status()==MaterialEventDispatcher.Status.APPLIED_PREFIX),
                "Both material events must be processed successfully");
            h.assertTrue(body.position().distanceToSqr(before)<1e-18,
                "Exact endpoint contact should not manufacture body displacement in the provenance fixture");
            var contact=AnatomyMovement.contact(body);
            h.assertTrue(contact!=null && contact.support()==hitSupport,
                "Contact provenance must identify the support that actually produced the CCD hit; a merely-near earlier event cannot steal the anchor");
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            hitSupport.discard();nearSupport.discard();body.discard();
        }
        h.succeed();
    }

    private static Input input(ServerLevel level,LivingEntity support,long revision,String model,
            ConvexBox beforeBox,ConvexBox afterBox,Vec3 lift,long tick) {
        var identity=new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
            Identifier.parse(model),Identifier.parse("test:s07_provenance_pose"),1,AnatomyMovement.registrationGeneration(support));
        var before=frame(identity,1,tick,support.position(),beforeBox);
        var after=frame(identity,2,tick,support.position().add(lift),afterBox);
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        var moving=new ConservativeSweep.Motion(t->beforeBox.move(lift.scale(t)),0,lift);
        var motion=new GeometryProvider.MotionSnapshot(revision,tick,tick,support.position(),support.position().add(lift),Map.of("body",moving));
        return new Input(support,handle,motion,union(beforeBox.bounds(),afterBox.bounds()).inflate(ConservativeSweep.SKIN));
    }

    @SuppressWarnings("unchecked")
    private static MaterialEventDispatcher.BatchResolution<Entity> resolveBatch(ServerLevel level,List<Input> inputs,Entity body) throws Exception {
        var preparedClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Constructor<?> preparedCtor=preparedClass.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
        preparedCtor.setAccessible(true);
        var prepared=new ArrayList<Object>();
        var events=new ArrayList<MaterialEventDispatcher.Event<Entity>>();
        int ordinal=0;
        for(var input:inputs) {
            var interval=new MaterialEventDispatcher.MaterialInterval(input.handle(),input.envelope());
            var pending=new MaterialIntervalRuntime.Pending(input.support(),MaterialIntervalRuntime.Source.JOINT,input.handle());
            prepared.add(preparedCtor.newInstance(pending,interval,input.motion()));
            events.add(new MaterialEventDispatcher.Event<>(new MaterialEventDispatcher.EventId(1,ordinal++),input.support(),
                MaterialEventDispatcher.Source.JOINT_BATCH,interval,Set.of(input.support().getUUID())));
        }
        var backendClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        Constructor<?> backendCtor=backendClass.getDeclaredConstructor(ServerLevel.class,List.class);
        backendCtor.setAccessible(true);
        var backend=backendCtor.newInstance(level,prepared);
        Method resolve=backendClass.getDeclaredMethod("resolveJointBatch",List.class,List.class);
        resolve.setAccessible(true);
        var candidate=new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox());
        return (MaterialEventDispatcher.BatchResolution<Entity>)resolve.invoke(backend,events,List.of(candidate));
    }

    private static GeometryProvider.QueryFrame frame(GeometryProvider.GeometryIdentity identity,long serial,long tick,
            Vec3 origin,ConvexBox box) {
        var root=new AnatomyMovement.RootFrame(serial,tick,origin,0,1,GravityFrame.VANILLA);
        var sample=new AnatomyPoseHistory.Sample(INPUTS,origin,0,1,GravityFrame.VANILLA);
        var endpoint=new GeometryProvider.CausalEndpoint(serial,tick,tick,root,sample,GeometryProvider.Availability.AVAILABLE);
        return new GeometryProvider.QueryFrame(identity,endpoint,new GeometryProvider.Snapshot(identity.revision(),Map.of("body",box)));
    }

    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }
}
