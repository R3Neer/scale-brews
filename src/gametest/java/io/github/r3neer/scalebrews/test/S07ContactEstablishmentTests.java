package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
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

/** S07 holdout: CCD resolution must leave truthful material contact state behind. */
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
            var identity=new GeometryProvider.GeometryIdentity(level.dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),70,
                Identifier.parse("test:s07_contact_model"),Identifier.parse("test:s07_contact_pose"),1,
                AnatomyMovement.registrationGeneration(support));
            long tick=level.getGameTime();
            var before=frame(identity,1,tick,base,beforeBox);
            var after=frame(identity,2,tick,base.add(lift),afterBox);
            var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
            var moving=new ConservativeSweep.Motion(t->beforeBox.move(lift.scale(t)),0,lift);
            var motion=new GeometryProvider.MotionSnapshot(70,tick,tick,base,base.add(lift),Map.of("body",moving));
            var envelope=new AABB(beforeBox.bounds().minX,beforeBox.bounds().minY,beforeBox.bounds().minZ,
                afterBox.bounds().maxX,afterBox.bounds().maxY,afterBox.bounds().maxZ).inflate(ConservativeSweep.SKIN);
            var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
            var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.ROOT,handle);

            var preparedClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
            var preparedCtor=preparedClass.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
                MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);
            preparedCtor.setAccessible(true);
            var prepared=preparedCtor.newInstance(pending,interval,motion);
            var backendClass=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
            var backendCtor=backendClass.getDeclaredConstructor(ServerLevel.class,List.class);
            backendCtor.setAccessible(true);
            var backend=backendCtor.newInstance(level,List.of(prepared));
            var resolve=backendClass.getDeclaredMethod("resolve",MaterialEventDispatcher.Event.class,List.class);
            resolve.setAccessible(true);

            var event=new MaterialEventDispatcher.Event<Entity>(new MaterialEventDispatcher.EventId(1,0),support,
                MaterialEventDispatcher.Source.ROOT_MUTATION,interval,Set.of(support.getUUID()));
            var candidate=new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox());
            double beforeY=body.getY();
            @SuppressWarnings("unchecked")
            var resolution=(MaterialEventDispatcher.Resolution<Entity>)resolve.invoke(backend,event,List.of(candidate));
            h.assertTrue(resolution.outcome().status()==MaterialEventDispatcher.Status.APPLIED_PREFIX,
                "Fixture must exercise a successfully applied material CCD prefix");
            h.assertTrue(body.getY()>beforeY+1e-5,
                "Rising material support must actually displace the stationary body in this fixture");
            var contact=AnatomyMovement.contact(body);
            h.assertTrue(contact!=null && contact.support()==support,
                "A body displaced by material CCD onto a supporting face must leave a material contact/anchor, not only a new position");
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }

    private static GeometryProvider.QueryFrame frame(GeometryProvider.GeometryIdentity identity,long serial,long tick,
            Vec3 origin,ConvexBox box) {
        var root=new AnatomyMovement.RootFrame(serial,tick,origin,0,1,GravityFrame.VANILLA);
        var sample=new AnatomyPoseHistory.Sample(INPUTS,origin,0,1,GravityFrame.VANILLA);
        var endpoint=new GeometryProvider.CausalEndpoint(serial,tick,tick,root,sample,GeometryProvider.Availability.AVAILABLE);
        return new GeometryProvider.QueryFrame(identity,endpoint,new GeometryProvider.Snapshot(identity.revision(),Map.of("body",box)));
    }
}
