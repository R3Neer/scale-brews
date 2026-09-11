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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;
import org.joml.Matrix4f;

/** Additional S07 boundary attacks over the real material backend. */
public final class S07MaterialBoundaryTests {
    private static final PoseProvider.Inputs INPUTS=new PoseProvider.Inputs(0,0,0,0,0,true);
    private record Fixture(LivingEntity support,GeometryProvider.MotionIntervalHandle handle,
        MaterialEventDispatcher.MaterialInterval interval,GeometryProvider.MotionSnapshot motion,Object prepared) {}

    @GameTest public void contactExistingOnlyMidIntervalCannotTunnel(GameTestHelper h) throws Exception {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        body.setPos(support.position().add(0,1,0));
        try {
            var center=body.getBoundingBox().getCenter();
            var local=new AABB(-.35,-.35,-.35,.35,.35,.35);
            var start=ConvexBox.of(local,new Matrix4f()).move(center.add(-2,0,0));
            var end=ConvexBox.of(local,new Matrix4f()).move(center.add(2,0,0));
            var motion=new ConservativeSweep.Motion(t->ConvexBox.of(local,new Matrix4f()).move(center.add(-2+4*t,0,0)),0,new Vec3(4,0,0));
            var fixture=fixture(support,start,end,Map.of("body",motion),union(start.bounds(),end.bounds()).inflate(.5));
            double x=body.getX();
            var outcome=resolve(h.getLevel(),fixture,List.of(new MaterialEventDispatcher.Candidate<Entity>(body,body.getBoundingBox())));
            h.assertTrue(outcome.status()==MaterialEventDispatcher.Status.APPLIED_PREFIX && Math.abs(body.getX()-x)>1e-5,
                "A material collider that exists only mid-interval must continuously displace the body instead of tunneling through it: "+outcome);
        } finally {support.discard();body.discard();}
        h.succeed();
    }

    @GameTest public void candidateBudgetAcceptsExactNAndRejectsNPlusOne(GameTestHelper h) throws Exception {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var first=h.makeMockPlayer(GameType.SURVIVAL);first.getAttribute(Attributes.SCALE).setBaseValue(.2);first.refreshDimensions();
        first.setPos(support.position().add(0,1,0));h.getLevel().addFreshEntity(first);
        try {
            h.assertTrue(Platforms.eligible(first,support),"Fixture body must be eligible for bounded candidate capture");
            var box=ConvexBox.of(new AABB(-1,-1,-1,1,1,1),new Matrix4f()).move(support.position().add(0,1,0));
            var fixture=fixture(support,box,box,Map.of("body",new ConservativeSweep.Motion(t->box,0)),box.bounds().inflate(2));
            var exact=capture(h.getLevel(),fixture,1);
            h.assertTrue(!exact.overflow() && exact.bodies().size()==1,"Exactly N local candidates must be accepted");
            var second=h.makeMockPlayer(GameType.SURVIVAL);second.getAttribute(Attributes.SCALE).setBaseValue(.2);second.refreshDimensions();
            second.setPos(first.position().add(.05,0,0));h.getLevel().addFreshEntity(second);
            try {
                h.assertTrue(Platforms.eligible(second,support),"Second fixture body must be eligible");
                var overflow=capture(h.getLevel(),fixture,1);
                h.assertTrue(overflow.overflow() && overflow.bodies().isEmpty(),"N+1 candidates must fail closed without leaking a partial list");
            } finally {second.discard();}
        } finally {support.discard();first.discard();}
        h.succeed();
    }

    @GameTest public void preexistingBodyOverlapDoesNotQuarantineUnchangedEvent(GameTestHelper h) throws Exception {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var first=h.makeMockServerPlayerInLevel();var second=h.makeMockServerPlayerInLevel();
        first.setPos(support.position().add(8,1,0));second.setPos(first.position());
        try {
            var box=ConvexBox.of(new AABB(-.5,-.5,-.5,.5,.5,.5),new Matrix4f()).move(support.position());
            var fixture=fixture(support,box,box,Map.of(),box.bounds());
            var outcome=resolve(h.getLevel(),fixture,List.of(
                new MaterialEventDispatcher.Candidate<Entity>(first,first.getBoundingBox()),
                new MaterialEventDispatcher.Candidate<Entity>(second,second.getBoundingBox())));
            h.assertTrue(outcome.status()==MaterialEventDispatcher.Status.APPLIED_PREFIX,
                "An event that does not worsen a pre-existing body/body overlap must not quarantine merely because the overlap existed: "+outcome);
        } finally {support.discard();first.discard();second.discard();}
        h.succeed();
    }

    @GameTest public void staticBlockObstructionPreventsMaterialPushThroughWall(GameTestHelper h) throws Exception {
        var support=h.spawn(EntityTypes.GHAST,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        body.setPos(support.position().add(0,1,0));
        try {
            var startBox=body.getBoundingBox();
            var wall=BlockPos.containing(body.getX()+1,body.getY(),body.getZ());
            h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
            var center=body.getBoundingBox().getCenter();var local=new AABB(-.35,-.35,-.35,.35,.35,.35);
            var from=ConvexBox.of(local,new Matrix4f()).move(center.add(-1,0,0));
            var to=ConvexBox.of(local,new Matrix4f()).move(center.add(2,0,0));
            var motion=new ConservativeSweep.Motion(t->ConvexBox.of(local,new Matrix4f()).move(center.add(-1+3*t,0,0)),0,new Vec3(3,0,0));
            var fixture=fixture(support,from,to,Map.of("body",motion),union(from.bounds(),to.bounds()).inflate(.5));
            resolve(h.getLevel(),fixture,List.of(new MaterialEventDispatcher.Candidate<Entity>(body,startBox)));
            h.assertTrue(body.getBoundingBox().maxX<=wall.getX()+1e-6,
                "Material CCD must route through vanilla block clipping; the body cannot be carried through a static wall");
        } finally {support.discard();body.discard();h.getLevel().setBlockAndUpdate(BlockPos.containing(body.getX()+1,body.getY(),body.getZ()),Blocks.AIR.defaultBlockState());}
        h.succeed();
    }

    @GameTest public void pendingIntervalStagingSaturatesExplicitly(GameTestHelper h) {
        var support=h.spawn(EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        AnatomyMovement.activate(h.getLevel());
        AnatomyMovement.register(support,endpointProvider(91),new GeometryProvider.GeometryIdentityDescriptor(
            UUID.randomUUID(),91,Identifier.parse("test:s07_model"),Identifier.parse("test:s07_pose"),1));
        try {
            MaterialIntervalRuntime.observe(support);MaterialIntervalRuntime.poll(h.getLevel());
            for(int i=0;i<MaterialIntervalRuntime.MAX_PENDING_INTERVALS+1;i++) {
                var capture=MaterialIntervalRuntime.captureRoot(support);
                support.setPos(support.position().add(.001,0,0));
                MaterialIntervalRuntime.commitRoot(support,capture);
            }
            h.assertTrue(MaterialIntervalRuntime.consumeSaturation(h.getLevel()),"Pending interval overflow must expose an explicit saturation signal");
            h.assertTrue(MaterialIntervalRuntime.poll(h.getLevel()).size()<=MaterialIntervalRuntime.MAX_PENDING_INTERVALS,
                "Staging queue must remain bounded at its declared cap");
        } finally {MaterialIntervalRuntime.clear(h.getLevel());AnatomyMovement.deactivate(h.getLevel());support.discard();}
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    private static MaterialEventDispatcher.Outcome resolve(ServerLevel level,Fixture fixture,List<MaterialEventDispatcher.Candidate<Entity>> candidates) throws Exception {
        Object backend=backend(level,List.of(fixture.prepared()));
        Method method=backend.getClass().getDeclaredMethod("resolve",MaterialEventDispatcher.Event.class,List.class);method.setAccessible(true);
        var event=new MaterialEventDispatcher.Event<Entity>(new MaterialEventDispatcher.EventId(1,0),fixture.support(),
            MaterialEventDispatcher.Source.ROOT_MUTATION,fixture.interval(),Set.of(fixture.support().getUUID()));
        var result=(MaterialEventDispatcher.Resolution<Entity>)method.invoke(backend,event,candidates);
        return result.outcome();
    }

    @SuppressWarnings("unchecked")
    private static MaterialEventDispatcher.Candidates<Entity> capture(ServerLevel level,Fixture fixture,int maximum) throws Exception {
        Object backend=backend(level,List.of(fixture.prepared()));
        Method method=backend.getClass().getDeclaredMethod("capture",MaterialEventDispatcher.Event.class,int.class);method.setAccessible(true);
        var event=new MaterialEventDispatcher.Event<Entity>(new MaterialEventDispatcher.EventId(1,0),fixture.support(),
            MaterialEventDispatcher.Source.ROOT_MUTATION,fixture.interval(),Set.of(fixture.support().getUUID()));
        return (MaterialEventDispatcher.Candidates<Entity>)method.invoke(backend,event,maximum);
    }

    private static Object backend(ServerLevel level,List<Object> prepared) throws Exception {
        Class<?> type=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Backend");
        Constructor<?> constructor=type.getDeclaredConstructor(ServerLevel.class,List.class);constructor.setAccessible(true);
        return constructor.newInstance(level,prepared);
    }

    private static Fixture fixture(LivingEntity support,ConvexBox beforeBox,ConvexBox afterBox,
            Map<String,ConservativeSweep.Motion> motions,AABB envelope) throws Exception {
        long tick=support.level().getGameTime();
        var identity=new GeometryProvider.GeometryIdentity(support.level().dimension(),support.getUUID(),support.getId(),UUID.randomUUID(),90,
            Identifier.parse("test:s07_model"),Identifier.parse("test:s07_pose"),1,1);
        var beforeRoot=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,GravityFrame.VANILLA);
        var afterRoot=new AnatomyMovement.RootFrame(2,tick,support.position(),0,1,GravityFrame.VANILLA);
        var beforeSample=new AnatomyPoseHistory.Sample(INPUTS,beforeRoot.origin(),0,1,GravityFrame.VANILLA);
        var afterSample=new AnatomyPoseHistory.Sample(INPUTS,afterRoot.origin(),0,1,GravityFrame.VANILLA);
        var before=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(1,tick,tick,beforeRoot,beforeSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(90,Map.of("body",beforeBox)));
        var after=new GeometryProvider.QueryFrame(identity,new GeometryProvider.CausalEndpoint(2,tick,tick,afterRoot,afterSample,GeometryProvider.Availability.AVAILABLE),
            new GeometryProvider.Snapshot(90,Map.of("body",afterBox)));
        var handle=new GeometryProvider.MotionIntervalHandle(identity,1,before,after);
        var interval=new MaterialEventDispatcher.MaterialInterval(handle,envelope);
        var motion=new GeometryProvider.MotionSnapshot(90,tick,tick,support.position(),support.position(),motions);
        var pending=new MaterialIntervalRuntime.Pending(support,MaterialIntervalRuntime.Source.ROOT,handle);
        Class<?> prepared=Class.forName("io.github.r3neer.scalebrews.collision.internal.MaterialPhysicsRuntime$Prepared");
        Constructor<?> constructor=prepared.getDeclaredConstructor(MaterialIntervalRuntime.Pending.class,
            MaterialEventDispatcher.MaterialInterval.class,GeometryProvider.MotionSnapshot.class);constructor.setAccessible(true);
        return new Fixture(support,handle,interval,motion,constructor.newInstance(pending,interval,motion));
    }

    private static GeometryProvider endpointProvider(long revision) {
        return new GeometryProvider() {
            private AnatomyMovement.RootFrame previous;private long serial;
            @Override public Optional<Snapshot> sample(LivingEntity entity) {
                return Optional.of(new Snapshot(revision,Map.of("body",ConvexBox.of(new AABB(-.5,-.5,-.5,.5,.5,.5),new Matrix4f()).move(entity.position()))));
            }
            @Override public Optional<CausalEndpoint> causalEndpoint(LivingEntity entity) {
                var observed=new AnatomyMovement.RootFrame(previous==null?0:previous.sequence()+1,entity.level().getGameTime(),entity.position(),entity.yBodyRot,entity.getScale(),GravityFrame.VANILLA);
                boolean changed=previous==null || !previous.origin().equals(observed.origin()) || previous.yaw()!=observed.yaw() || previous.scale()!=observed.scale();
                if(changed){previous=observed;serial++;}
                var sample=new AnatomyPoseHistory.Sample(INPUTS,previous.origin(),previous.yaw(),previous.scale(),previous.gravity());
                return Optional.of(new CausalEndpoint(serial,entity.level().getGameTime(),entity.level().getGameTime(),previous,sample,Availability.AVAILABLE));
            }
        };
    }

    private static AABB union(AABB a,AABB b) {
        return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),
            Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));
    }
}
