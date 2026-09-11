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

/** S05/NFR-008 holdout for locality of the live broadphase wrapper, not only its reusable kernel. */
public final class S05LiveBroadphaseLocalityTests {
    @GameTest
    public void localQueryMustNotResampleEveryFarWorldSupport(GameTestHelper h) {
        var level=h.getLevel();
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        var supports=new ArrayList<LivingEntity>();
        var samples=new AtomicInteger();
        GeometryProvider farProvider=entity->{
            samples.incrementAndGet();
            var box=ConvexBox.of(new AABB(-.25,-.25,-.25,.25,.25,.25),new Matrix4f()).move(entity.position());
            return Optional.of(new GeometryProvider.Snapshot(1,Map.of("body",box)));
        };

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
                AnatomyMovement.register(support,farProvider);
            }

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
}
