package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.collision.internal.MaterialIntervalRuntime;
import io.github.r3neer.scalebrews.collision.internal.ModelGeometryProvider;
import io.github.r3neer.scalebrews.collision.pose.PoseProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S05/NFR-008 holdouts for locality of the live broadphase wrapper, not only its reusable kernel. */
public final class S05LiveBroadphaseLocalityTests {
    @GameTest
    public void localQueryMustNotResampleEveryFarWorldSupport(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        var supports=new ArrayList<LivingEntity>();
        var samples=new AtomicInteger();
        GeometryProvider farProvider=countingProvider(samples,1);

        AnatomyMovement.activate(level);
        try {
            Vec3 origin=body.position();
            addFarSupports(level,origin,supports,farProvider);

            AABB local=body.getBoundingBox();
            h.assertTrue(AnatomyMovement.spaceClear(body,local),
                "Initial local query must materialize the bounded spatial index without seeing far supports");
            samples.set(0);

            h.assertTrue(AnatomyMovement.spaceClear(body,local),
                "Repeated local query must remain clear");
            h.assertTrue(samples.get()==0,
                "NFR-008/S05 locality requires a repeated local query to avoid re-sampling every far-world support; far provider samples="+samples.get());
        } finally {
            AnatomyMovement.deactivate(level);
            supports.forEach(net.minecraft.world.entity.Entity::discard);
            body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void firstLocalQueryAfterLegitimateRebindMustNotResampleFarWorld(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        var supports=new ArrayList<LivingEntity>();
        var farSamples=new AtomicInteger();
        GeometryProvider farProvider=countingProvider(farSamples,1);

        var localSupport=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        if(localSupport==null)throw new IllegalStateException("Local cow fixture could not be created");
        localSupport.setNoAi(true);localSupport.setNoGravity(true);
        localSupport.setPos(body.position().add(3,0,0));
        level.addFreshEntity(localSupport);
        supports.add(localSupport);
        GeometryProvider localA=countingProvider(new AtomicInteger(),2);
        GeometryProvider localB=countingProvider(new AtomicInteger(),3);

        AnatomyMovement.activate(level);
        try {
            addFarSupports(level,body.position(),supports,farProvider);
            AnatomyMovement.register(localSupport,localA);
            AABB localQuery=body.getBoundingBox();
            h.assertTrue(AnatomyMovement.spaceClear(body,localQuery),
                "Initial local query must materialize the world index before the rebind");

            // Rebind is an explicit supported mutation hook. It may do maintenance itself.
            // Reset after the hook: this test constrains the next physical query, not the
            // implementation strategy or cost chosen inside the mutation callback.
            AnatomyMovement.register(localSupport,localB);
            farSamples.set(0);

            h.assertTrue(AnatomyMovement.spaceClear(body,localQuery),
                "First local query after a legitimate local rebind must remain clear");
            h.assertTrue(farSamples.get()==0,
                "NFR-008 forbids deferring a world-wide provider rescan onto the next movement/query after a local rebind; far provider samples="+farSamples.get());
        } finally {
            AnatomyMovement.deactivate(level);
            supports.forEach(net.minecraft.world.entity.Entity::discard);
            body.discard();
        }
        h.succeed();
    }

    @GameTest
    public void firstLocalQueryAfterSupportedRootCommitMustNotResampleFarWorld(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var supports=new ArrayList<LivingEntity>();
        var farSamples=new AtomicInteger();
        GeometryProvider farProvider=countingProvider(farSamples,1);

        var localSupport=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
        if(localSupport==null)throw new IllegalStateException("Local cow fixture could not be created");
        localSupport.setNoAi(true);localSupport.setNoGravity(true);localSupport.yBodyRot=0;
        localSupport.setPos(body.position().add(3,0,0));
        level.addFreshEntity(localSupport);
        supports.add(localSupport);

        var model=new ModelGeometry(2,"test:s05_root_locality","1",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("body","root",List.of(-.5d,0d,-.5d),List.of(.5d,.5d,.5d),null)),
            ModelGeometry.values(new Matrix4f()));
        var localProvider=new ModelGeometryProvider(model,(geometry,inputs)->Optional.of(Map.of()),AnatomyFilter.DEFAULT,1);
        localProvider.pose(localSupport,new PoseProvider.Inputs(0,0,0,0,0,true));
        var descriptor=new GeometryProvider.GeometryIdentityDescriptor(UUID.randomUUID(),1,
            Identifier.parse("test:s05_root_locality_model"),Identifier.parse("test:s05_root_locality_pose"));

        AnatomyMovement.activate(level);
        try {
            addFarSupports(level,body.position(),supports,farProvider);
            AnatomyMovement.register(localSupport,localProvider,descriptor);
            // Canonical provider cadence may do world-wide maintenance outside the measured query.
            AnatomyMovement.tick(level);
            var beforeBox=localProvider.sample(localSupport).orElseThrow().pieces().get("body").bounds();
            h.assertTrue(!AnatomyMovement.spaceClear(body,beforeBox),
                "Starting spatial index must contain the local causal support before root movement");

            var capture=MaterialIntervalRuntime.captureRoot(localSupport);
            localSupport.setPos(localSupport.position().add(.2,0,0));
            MaterialIntervalRuntime.commitRoot(localSupport,capture);

            // Measure only the first body query after the supported root hook. Any eager index
            // maintenance performed inside commitRoot is allowed; a deferred world-wide rescan is not.
            farSamples.set(0);
            var afterBox=beforeBox.move(.2,0,0);
            h.assertTrue(!AnatomyMovement.spaceClear(body,afterBox),
                "First local query after commitRoot must see the moved causal anatomy");
            h.assertTrue(farSamples.get()==0,
                "NFR-008 forbids deferring a world-wide provider rescan onto the first movement/query after commitRoot; far provider samples="+farSamples.get());
        } finally {
            MaterialIntervalRuntime.clear(level);
            AnatomyMovement.deactivate(level);
            supports.forEach(net.minecraft.world.entity.Entity::discard);
            body.discard();
        }
        h.succeed();
    }

    private static GeometryProvider countingProvider(AtomicInteger samples,long revision) {
        return entity->{
            samples.incrementAndGet();
            var box=ConvexBox.of(new AABB(-.25,-.25,-.25,.25,.25,.25),new Matrix4f()).move(entity.position());
            return Optional.of(new GeometryProvider.Snapshot(revision,Map.of("body",box)));
        };
    }

    private static void addFarSupports(net.minecraft.server.level.ServerLevel level,Vec3 origin,
            ArrayList<LivingEntity> supports,GeometryProvider provider) {
        for(int i=0;i<64;i++) {
            var support=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
            if(support==null)throw new IllegalStateException("Cow fixture could not be created");
            support.setNoAi(true);support.setNoGravity(true);
            support.setPos(origin.add(1000+i*8,0,0));
            level.addFreshEntity(support);
            supports.add(support);
            AnatomyMovement.register(support,provider);
        }
    }
}
