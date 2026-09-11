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

/** NFR-008 holdout: unrelated dirty supports may not be drained by a local physical query. */
public final class S05DirtyMutationLocalityTests {
    @GameTest
    public void manyFarRebindsMustNotBeProcessedByUnrelatedLocalQuery(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        var supports=new ArrayList<LivingEntity>();
        var farSamples=new AtomicInteger();
        GeometryProvider initial=countingProvider(new AtomicInteger(),1);
        GeometryProvider rebound=countingProvider(farSamples,2);

        AnatomyMovement.activate(level);
        try {
            Vec3 origin=body.position();
            for(int i=0;i<64;i++) {
                var support=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);
                if(support==null)throw new IllegalStateException("Cow fixture could not be created");
                support.setNoAi(true);support.setNoGravity(true);
                support.setPos(origin.add(1000+i*8,0,0));
                level.addFreshEntity(support);
                supports.add(support);
                AnatomyMovement.register(support,initial);
            }

            AABB local=body.getBoundingBox();
            h.assertTrue(AnatomyMovement.spaceClear(body,local),
                "Initial local query must build the starting spatial index");

            // Every rebind is an explicit supported mutation hook. An implementation may pay
            // maintenance here; reset afterwards so this test constrains only the next local query.
            for(var support:supports)AnatomyMovement.register(support,rebound);
            farSamples.set(0);

            h.assertTrue(AnatomyMovement.spaceClear(body,local),
                "An unrelated local query must remain clear after far-world rebinds");
            h.assertTrue(farSamples.get()==0,
                "NFR-008 forbids draining/resampling all far dirty supports from an unrelated local movement/query; far provider samples="+farSamples.get());
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
}
