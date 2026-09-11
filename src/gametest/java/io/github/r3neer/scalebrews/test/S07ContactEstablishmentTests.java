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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S07 holdouts for real material CCD/contact behavior. */
public final class S07ContactEstablishmentTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void movingSupportContactMustEstablishMaterialAnchor(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.1);
        body.refreshDimensions();

        Vec3 base=support.position();
        var beforeBox=ConvexBox.of(new AABB(-1,0,-1,1,.5,1),new Matrix4f()).move(base);
        var lift=new Vec3(0,.4,0);
        var afterBox=beforeBox.move(lift);
        GeometryProvider provider=entity->Optional.of(new GeometryProvider.Snapshot(70,Map.of("body",afterBox)));
        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,provider);
        body.setPos(base.x,beforeBox.bounds().maxY+.25,base.z);
        try {
            h.assertTrue(Platforms.eligible(body,support),"Fixture body must be eligible for the support");
            var identity=identity(level,support,70,"contact");
            long tick=level.getGameTime();
            var before=frame(identity,1,tick,base,beforeBox);
            var after=frame(identity,2,tick,base.add(lift),afterBox);
            var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
            var moving=new ConservativeSweep.Motion(t->beforeBox.move(lift.scale(t)),0,lift);
            var motion=new GeometryProvider.MotionSnapshot(70,tick,tick,base,base.add(lift),Map.of("body",moving));
            var envelope=union(beforeBox.bounds(),afterBox.bounds()).inflate(ConservativeSweep.SKIN);
            var resolution=resolve(level,support,body,handle,motion,envelope);
            double moved=body.getY()-(beforeBox.bounds().maxY+.25);
            h.assertTrue(resolution.outcome().status()==MaterialEventDispatcher.Status.APPLIED_PREFIX,
                "Fixture must exercise a successfully applied material CCD prefix");
            h.assertTrue(moved>1e-5,"Rising material support must actually displace the stationary body in this fixture");
            var contact=AnatomyMovement.contact(body);
            h.assertTrue(contact!=null && contact.support()==support,
                "A body displaced by material CCD onto a supporting face must leave a material contact/anchor, not only a new position");
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);support.discard();body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void midIntervalOnlyContactCannotBeReducedToEndpoints(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.1);
        body.refreshDimensions();

        Vec3 center=support.position().add(6,0,0);
        var beforeBox=ConvexBox.of(new AABB(-.4,-.5,-.5,.4,.5,.5),new Matrix4f()).move(center.add(-2,0,0));
        var travel=new Vec3(4,0,0);
        var afterBox=beforeBox.move(travel);
        GeometryProvider provider=entity->Optional.of(new GeometryProvider.Snapshot(71,Map.of("body",afterBox)));
        AnatomyMovement.activate(level);AnatomyMovement.register(support,provider);
        body.setPos(center.x,center.y,center.z);
        try {
            h.assertTrue(Platforms.eligible(body,support),"Fixture body must be eligible for the support");
            h.assertTrue(!beforeBox.overlaps(body.getBoundingBox()) && !afterBox.overlaps(body.getBoundingBox()),
                "Both material endpoints must be clear so only continuous CCD can detect the crossing");
            var identity=identity(level,support,71,"mid_only");long tick=level.getGameTime();
            var before=frame(identity,1,tick,center.add(-2,0,0),beforeBox);
            var after=frame(identity,2,tick,center.add(2,0,0),afterBox);
            var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
            var moving=new ConservativeSweep.Motion(t->beforeBox.move(travel.scale(t)),0,travel);
            var motion=new GeometryProvider.MotionSnapshot(71,tick,tick,center.add(-2,0,0),center.add(2,0,0),Map.of("body",moving));
            var envelope=union(beforeBox.bounds(),afterBox.bounds()).inflate(ConservativeSweep.SKIN);
            Vec3 start=body.position();
            var resolution=resolve(level,support,body,handle,motion,envelope);
            h.assertTrue(resolution.outcome().status()==MaterialEventDispatcher.Status.APPLIED_PREFIX,
                "Mid-interval fixture must complete without falling back to an endpoint-only failure");
            h.assertTrue(body.position().distanceToSqr(start)>1e-10,
                "A material piece that crosses the body between two clear endpoints must cause a continuous response");
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);support.discard();body.discard();
        }
        h.succeed();
    }

    private static MaterialEventDispatcher.Resolution<Entity> resolve(ServerLevel level,net.minecraft.world.entity.LivingEntity support,
            Entity body,GeometryProvider.MotionIntervalHandle handle,GeometryProvider.MotionSnapshot motion,AABB envelope) throws Exception {
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.ROOT,handle);
        var preparedClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        var preparedCtor=preparedClass.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
        preparedCtor.setAccessible(true);
        var prepared=preparedCtor.newInstance(pending,interval,motion);
        var backendClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        var backendCtor=backendClass.getDeclaredConstructor(ServerLevel.class,List.class);backendCtor.setAccessible(true);
        var backend=backendCtor.newInstance(level,List.of(prepared));
        var method=backendClass.getDeclaredMethod("resolve",MaterialEventDispatcher.Event.class,List.class);method.setAccessible(true);
        var event=new MaterialEventDispatcher.Event<Entity>(new MaterialEventDispatcher.EventId(1,0),support,
            MaterialEventDispatcher.Source.ROOT_MUTATION,interval,Set.of(support.getUUID()));
        var candidate=new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox());
        @SuppressWarnings("unchecked")
        var resolution=(MaterialEventDispatcher.Resolution<Entity>)method.invoke(backend,event,List.of(candidate));
        return resolution;
    }

    private static GeometryProvider.GeometryIdentity identity(ServerLevel level,net.minecraft.world.entity.LivingEntity support,long revision,String suffix) {
        return new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),revision,
            Identifier.parse("test:s07_"+suffix+"_model"),Identifier.parse("test:s07_"+suffix+"_pose"),1,
            AnatomyMovement.registrationGeneration(support));
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
