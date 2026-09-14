package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Adversarial FR-053 holdout: READY anatomy support must not consume vanilla Entity.push. */
public final class G2VanillaPushReadyTests {
    @GameTest
    public void readyMaterialSupportPreservesVanillaEntityPush(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyRuntime.startPrepared(server,Map.of(),Map.of());
        var support=h.spawn(EntityTypes.COW,2,20,2);
        var body=h.makeMockPlayer(GameType.SURVIVAL);
        try {
            support.setNoAi(true);support.setNoGravity(true);
            body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
            var floor=ConvexBox.of(new AABB(-1,-.1,-1,1,.1,1),new Matrix4f()).move(support.position());
            GeometryProvider provider=entity->Optional.of(new GeometryProvider.Snapshot(0,Map.of("floor",floor)));
            AnatomyMovement.register(support,provider);

            h.assertTrue(AnatomyApi.ready(body) && AnatomyApi.ready(support),
                "Holdout must traverse the production READY guard used by PlatformEntityMixin.push");

            body.setPos(support.getX()+.2,floor.bounds().maxY+.5,support.getZ());
            body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
            support.push(body);
            Vec3 vanillaBody=body.getDeltaMovement(),vanillaSupport=support.getDeltaMovement();
            h.assertTrue(vanillaBody.lengthSqr()>1e-12 || vanillaSupport.lengthSqr()>1e-12,
                "Holdout precondition: READY but unmanaged pair must retain a measurable vanilla Entity.push response");

            body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
            var landing=AnatomyMovement.collide(body,new Vec3(0,-1,0));body.setPos(body.position().add(landing));
            AnatomyMovement.afterMove(body);
            h.assertTrue(AnatomyMovement.supported(body) && AnatomyMovement.contact(body)!=null
                    && AnatomyMovement.contact(body).support()==support && AnatomyMovement.suppressesPush(body,support),
                "Holdout must acquire the exact material pair currently selected by production push suppression");

            support.push(body);
            Vec3 managedBody=body.getDeltaMovement(),managedSupport=support.getDeltaMovement();
            h.assertTrue(managedBody.distanceToSqr(vanillaBody)<=1e-12 && managedSupport.distanceToSqr(vanillaSupport)<=1e-12,
                "A moving-platform contact must preserve vanilla Entity.push exactly once: vanilla body="+vanillaBody
                    +" support="+vanillaSupport+" managed body="+managedBody+" support="+managedSupport);
        } finally {
            body.discard();support.discard();AnatomyRuntime.stop(server);
        }
        h.succeed();
    }
}
