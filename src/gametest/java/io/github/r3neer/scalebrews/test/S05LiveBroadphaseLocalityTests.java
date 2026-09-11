package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
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
