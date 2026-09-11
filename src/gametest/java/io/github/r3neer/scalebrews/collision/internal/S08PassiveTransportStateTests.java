package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.test.mixin.TestFoodAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** FR-061: passive certified transport changes position/accounting, not voluntary movement/fall accounting. */
public final class S08PassiveTransportStateTests {
    @GameTest
    public void certifiedPassiveCarryDoesNotRewriteVoluntaryMovementAccounting(GameTestHelper h) {
        var level=h.getLevel();var support=h.spawn(EntityTypes.COW,2,20,2);support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();body.setNoGravity(true);body.setPos(support.position().add(0,1,0));
        long revision=311,tick=level.getGameTime();
        var material=ConvexBox.of(new AABB(-.5,-.1,-.5,.5,0,.5),new Matrix4f()).move(support.position().add(0,1,0));
        int face=3;Vec3 normal=material.faceNormal(face),local=material.facePoint(face,body.getBoundingBox().getCenter());
        AnatomyMovement.activate(level);AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(revision,java.util.Map.of("floor",material))));
        try {
            var contact=new SurfaceContact(support.getUUID(),revision,"floor",face,local,normal,tick);
            h.assertTrue(AnatomyMovement.confirm(body,support,contact),"Fixture must retain support contact");
            var surface=AnatomyMovement.surface(body);

            Vec3 voluntary=new Vec3(.17,-.31,.09);body.setDeltaMovement(voluntary);body.fallDistance=4.75;
            var food=(TestFoodAccess)body.getFoodData();food.test$exhaustion(2.75f);
            body.awardStat(Stats.CUSTOM.get(Stats.WALK_ONE_CM),17);
            body.awardStat(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM),19);
            body.awardStat(Stats.CUSTOM.get(Stats.SWIM_ONE_CM),23);
            body.awardStat(Stats.CUSTOM.get(Stats.FALL_ONE_CM),29);
            var stats=body.getStats();
            double fall=body.fallDistance;float exhaustion=food.test$exhaustion();
            int walk=stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM));
            int sprint=stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM));
            int swim=stats.getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM));
            int fallStat=stats.getValue(Stats.CUSTOM.get(Stats.FALL_ONE_CM));

            Vec3 applied=new Vec3(.25,0,0);body.setPos(body.position().add(applied));
            var root=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,AnatomyMovement.gravity(support));
            h.assertTrue(AnatomyMovement.recordCertifiedTransport(body,support,surface,root,applied,material,material.move(applied)),
                "Certified passive transport accounting must accept the retained relation");
            h.assertTrue(body.getDeltaMovement().equals(voluntary),
                "Passive carry must not be converted into voluntary body velocity: "+body.getDeltaMovement());
            h.assertTrue(body.fallDistance==fall,
                "Passive carry must not rewrite accumulated fall distance: before="+fall+" after="+body.fallDistance);
            h.assertTrue(Float.compare(food.test$exhaustion(),exhaustion)==0,
                "Passive carry must not alter player exhaustion");
            h.assertTrue(stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM))==walk,
                "Passive carry must not count as walking distance");
            h.assertTrue(stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM))==sprint,
                "Passive carry must not count as sprinting distance");
            h.assertTrue(stats.getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM))==swim,
                "Passive carry must not count as swimming distance");
            h.assertTrue(stats.getValue(Stats.CUSTOM.get(Stats.FALL_ONE_CM))==fallStat,
                "Passive carry must not count as the player's own fall distance");
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);support.discard();body.discard();
        }
        h.succeed();
    }
}
