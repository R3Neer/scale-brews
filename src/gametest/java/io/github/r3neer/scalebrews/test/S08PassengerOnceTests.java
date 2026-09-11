package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.GravityFrame;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyPoseHistory;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialEventDispatcher;
import io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import io.github.r3neer.scalebrews.platform.Platforms;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** FR-060: vanilla passengers follow their root once and are not captured again as anatomical carry bodies. */
public final class S08PassengerOnceTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);

    @GameTest
    public void vanillaPassengerIsExcludedBeforeAnatomicalCandidateResolution(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var rider=h.makeMockPlayer(GameType.SURVIVAL);rider.getAttribute(Attributes.SCALE).setBaseValue(.2);rider.refreshDimensions();
        rider.setPos(support.position().add(0,1,0));level.addFreshEntity(rider);
        var bystander=h.makeMockPlayer(GameType.SURVIVAL);bystander.getAttribute(Attributes.SCALE).setBaseValue(.2);bystander.refreshDimensions();
        bystander.setPos(support.position().add(.4,1,0));level.addFreshEntity(bystander);
        try {
            h.assertTrue(Platforms.eligible(rider,support) && Platforms.eligible(bystander,support),
                "Fixture requires both tiny bodies to be independently eligible before vanilla passenger filtering");
            h.assertTrue(rider.startRiding(support,true,true),"Fixture rider must establish an ordinary vanilla passenger relation");
            h.assertTrue(support.getIndirectPassengers().anyMatch(entity->entity==rider),
                "Fixture must expose rider through vanilla indirect-passenger traversal");
            h.assertTrue(Platforms.eligible(rider,support),
                "Passenger must remain otherwise eligible so exclusion is proven to come from the passenger-once rule");

            var interval=interval(support);
            var event=new MaterialEventDispatcher.Event<Entity>(new MaterialEventDispatcher.EventId(1,0),support,
                MaterialEventDispatcher.Source.ROOT_MUTATION,interval,Set.of(support.getUUID()));
            var captured=capture(level,event,16);
            h.assertTrue(captured.bodies().stream().noneMatch(candidate->candidate.body()==rider),
                "A vanilla passenger of the moving root must not receive an independent anatomical carry candidate");
            h.assertTrue(captured.bodies().stream().anyMatch(candidate->candidate.body()==bystander),
                "Control bystander must still be captured, proving the query did not simply reject every nearby body");
        } finally {
            rider.stopRiding();support.discard();rider.discard();bystander.discard();MaterialPhysicsRuntime.clear(level);
        }
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    private static MaterialEventDispatcher.Candidates<Entity> capture(ServerLevel level,MaterialEventDispatcher.Event<Entity> event,int maximum) throws Exception {
        Class<?> type=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        Constructor<?> constructor=type.getDeclaredConstructor(ServerLevel.class,List.class);constructor.setAccessible(true);
        Object backend=constructor.newInstance(level,List.of());
        Method method=type.getDeclaredMethod("capture",MaterialEventDispatcher.Event.class,int.class);method.setAccessible(true);
        return (MaterialEventDispatcher.Candidates<Entity>)method.invoke(backend,event,maximum);
    }

    private static MaterialEventDispatcher.MaterialInterval interval(Entity support) {
        long tick=support.level().getGameTime();var origin=support.position();
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),313,
            Identifier.parse("test:s08_passenger"),Identifier.parse("test:s08_passenger_pose"),1,1);
        var rootA=new AnatomyMovement.RootFrame(1,tick,origin,0,1,GravityFrame.VANILLA);
        var rootB=new AnatomyMovement.RootFrame(2,tick,origin.add(.1,0,0),0,1,GravityFrame.VANILLA);
        var sampleA=new AnatomyPoseHistory.Sample(INPUTS,rootA.origin(),0,1,GravityFrame.VANILLA);
        var sampleB=new AnatomyPoseHistory.Sample(INPUTS,rootB.origin(),0,1,GravityFrame.VANILLA);
        var box=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f()).move(origin);
        var snapshot=new GeometryProvider.Snapshot(313,Map.of("piece",box));
        var before=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(1,tick,tick,rootA,sampleA,GeometryProvider.Availability.AVAILABLE),snapshot);
        var after=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(2,tick,tick,rootB,sampleB,GeometryProvider.Availability.AVAILABLE),snapshot);
        return new MaterialEventDispatcher.MaterialInterval(new GeometryProvider.MotionIntervalHandle(identity,1,before,after),
            support.getBoundingBox().inflate(4));
    }
}
