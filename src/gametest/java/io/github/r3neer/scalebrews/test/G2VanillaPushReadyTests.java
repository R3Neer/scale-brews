package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.geometry.AnatomyFilter;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import io.github.r3neer.scalebrews.collision.internal.AnatomyDefinition;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import io.github.r3neer.scalebrews.platform.PlatformDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Adversarial FR-053 holdout: READY anatomy support must not consume vanilla Entity.push. */
public final class G2VanillaPushReadyTests {
    private static final Identifier MODEL=Identifier.parse("test:g2_push_model");
    private static final Identifier STATIC_POSE=Identifier.parse("scalebrews:static");

    private static ModelGeometry catalogModel() {
        return new ModelGeometry(1,"test:g2_push_model","1",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("floor","root",List.of(-1d,-.1d,-1d),List.of(1d,.1d,1d),null)),
            ModelGeometry.values(new Matrix4f()));
    }

    private static PlatformDefinition cowProfile() {
        return new PlatformDefinition(Identifier.parse("minecraft:cow"),true,.6,Optional.of(.85),List.of(),
            Optional.of(new AnatomyDefinition(MODEL,STATIC_POSE,AnatomyFilter.DEFAULT)));
    }

    @GameTest
    public void readyMaterialSupportPreservesVanillaEntityPush(GameTestHelper h) {
        var server=h.getLevel().getServer();
        AnatomyRuntime.startPrepared(server,Map.of(MODEL.toString(),catalogModel()),Map.of("cow",cowProfile()));
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
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body,support),
                "Holdout must use a canonically eligible body/support pair, not a fixture-only provider rejected by READY policy");

            body.setPos(support.getX()+.2,floor.bounds().maxY+.5,support.getZ());

            body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
            support.push(body);
            Vec3 vanillaSupportCallerBody=body.getDeltaMovement(),vanillaSupportCallerSupport=support.getDeltaMovement();

            body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
            body.push(support);
            Vec3 vanillaBodyCallerBody=body.getDeltaMovement(),vanillaBodyCallerSupport=support.getDeltaMovement();

            h.assertTrue(vanillaSupportCallerBody.lengthSqr()>1e-12 || vanillaSupportCallerSupport.lengthSqr()>1e-12,
                "Holdout precondition: support.push(body) must produce a measurable vanilla response before material contact");
            h.assertTrue(vanillaBodyCallerBody.lengthSqr()>1e-12 || vanillaBodyCallerSupport.lengthSqr()>1e-12,
                "Holdout precondition: body.push(support) must produce a measurable vanilla response before material contact");

            body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
            var landing=AnatomyMovement.collide(body,new Vec3(0,-1,0));body.setPos(body.position().add(landing));
            AnatomyMovement.afterMove(body);
            h.assertTrue(AnatomyMovement.supported(body) && AnatomyMovement.contact(body)!=null
                    && AnatomyMovement.contact(body).support()==support && AnatomyMovement.suppressesPush(body,support),
                "Holdout must acquire the exact material pair currently selected by production push suppression");

            body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
            support.push(body);
            Vec3 managedSupportCallerBody=body.getDeltaMovement(),managedSupportCallerSupport=support.getDeltaMovement();

            body.setDeltaMovement(Vec3.ZERO);support.setDeltaMovement(Vec3.ZERO);
            body.push(support);
            Vec3 managedBodyCallerBody=body.getDeltaMovement(),managedBodyCallerSupport=support.getDeltaMovement();

            h.assertTrue(managedSupportCallerBody.distanceToSqr(vanillaSupportCallerBody)<=1e-12
                    && managedSupportCallerSupport.distanceToSqr(vanillaSupportCallerSupport)<=1e-12
                    && managedBodyCallerBody.distanceToSqr(vanillaBodyCallerBody)<=1e-12
                    && managedBodyCallerSupport.distanceToSqr(vanillaBodyCallerSupport)<=1e-12,
                "A moving-platform contact must preserve vanilla Entity.push in both caller directions exactly once: "
                    +"support->body vanilla body="+vanillaSupportCallerBody+" support="+vanillaSupportCallerSupport
                    +" managed body="+managedSupportCallerBody+" support="+managedSupportCallerSupport
                    +"; body->support vanilla body="+vanillaBodyCallerBody+" support="+vanillaBodyCallerSupport
                    +" managed body="+managedBodyCallerBody+" support="+managedBodyCallerSupport);
        } finally {
            body.discard();support.discard();AnatomyRuntime.stop(server);
        }
        h.succeed();
    }
}
