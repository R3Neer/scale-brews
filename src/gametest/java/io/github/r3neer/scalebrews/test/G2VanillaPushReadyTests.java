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

/** Adversarial FR-053/FR-093 holdout: READY anatomy support must preserve vanilla Entity.push exactly once. */
public final class G2VanillaPushReadyTests {
    private static final Identifier MODEL=Identifier.parse("test:g2_push_model");
    private static final Identifier STATIC_POSE=Identifier.parse("scalebrews:static");

    private static ModelGeometry catalogModel() {
        return new ModelGeometry(1,"test:g2_push_model","1",
            List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),
            List.of(new ModelGeometry.Piece("floor","root",List.of(-1d,-.1d,-1d),List.of(1d,.1d,1d),null)),
            ModelGeometry.values(new Matrix4f()));
    }

    private static PlatformDefinition profile(String entity) {
        return new PlatformDefinition(Identifier.parse(entity),true,.6,Optional.of(.85),List.of(),
            Optional.of(new AnatomyDefinition(MODEL,STATIC_POSE,AnatomyFilter.DEFAULT)));
    }

    private static PlatformDefinition cowProfile() {
        return profile("minecraft:cow");
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
            h.assertTrue(!AnatomyMovement.supported(body) && AnatomyMovement.contact(body)==null
                    && !AnatomyMovement.suppressesPush(body,support),
                "Pre-contact control must remain outside the managed material relation before measuring vanilla push");

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

        // Transition holdout: a legitimate legacy support relation must not keep suppressing vanilla push
        // during the same-tick handoff to READY anatomy ownership. Waiting for Platforms.tick would hide
        // a stale-state ownership bug rather than certify the transition boundary.
        var transitionSupport=h.spawn(EntityTypes.GHAST,4,20,4);
        var transitionBody=h.makeMockPlayer(GameType.SURVIVAL);
        try {
            transitionSupport.setNoAi(true);transitionSupport.setNoGravity(true);
            transitionBody.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.2);
            transitionBody.refreshDimensions();

            transitionBody.setPos(transitionSupport.getX()+.2,transitionSupport.getY()+.6,transitionSupport.getZ());
            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            transitionSupport.push(transitionBody);
            Vec3 transitionVanillaSupportCallerBody=transitionBody.getDeltaMovement();
            Vec3 transitionVanillaSupportCallerSupport=transitionSupport.getDeltaMovement();

            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            transitionBody.push(transitionSupport);
            Vec3 transitionVanillaBodyCallerBody=transitionBody.getDeltaMovement();
            Vec3 transitionVanillaBodyCallerSupport=transitionSupport.getDeltaMovement();

            h.assertTrue(transitionVanillaSupportCallerBody.lengthSqr()>1e-12
                    || transitionVanillaSupportCallerSupport.lengthSqr()>1e-12,
                "Transition holdout precondition: support.push(body) must have a measurable vanilla response");
            h.assertTrue(transitionVanillaBodyCallerBody.lengthSqr()>1e-12
                    || transitionVanillaBodyCallerSupport.lengthSqr()>1e-12,
                "Transition holdout precondition: body.push(support) must have a measurable vanilla response");

            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            transitionBody.setPos(transitionSupport.position().add(.2,5,0));
            transitionBody.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-2,0));
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.state(transitionBody).support==transitionSupport,
                "Transition holdout must acquire stale state through the production legacy movement path");

            // Boundary control: before shared ownership begins, the exact same production-acquired
            // PlatformState relation is still owned by the legacy route and must suppress pair push.
            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            transitionSupport.push(transitionBody);
            Vec3 legacySupportCallerBody=transitionBody.getDeltaMovement();
            Vec3 legacySupportCallerSupport=transitionSupport.getDeltaMovement();

            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            transitionBody.push(transitionSupport);
            Vec3 legacyBodyCallerBody=transitionBody.getDeltaMovement();
            Vec3 legacyBodyCallerSupport=transitionSupport.getDeltaMovement();

            h.assertTrue(legacySupportCallerBody.equals(Vec3.ZERO)
                    && legacySupportCallerSupport.equals(Vec3.ZERO)
                    && legacyBodyCallerBody.equals(Vec3.ZERO)
                    && legacyBodyCallerSupport.equals(Vec3.ZERO),
                "Legacy support must retain its own push suppression until shared anatomy ownership actually begins");

            AnatomyRuntime.startPrepared(server,Map.of(MODEL.toString(),catalogModel()),Map.of("ghast",profile("minecraft:ghast")));
            var transitionFloor=ConvexBox.of(new AABB(-1,-.1,-1,1,.1,1),new Matrix4f()).move(transitionSupport.position());
            GeometryProvider transitionProvider=entity->Optional.of(new GeometryProvider.Snapshot(0,Map.of("floor",transitionFloor)));
            AnatomyMovement.register(transitionSupport,transitionProvider);

            h.assertTrue(AnatomyApi.ready(transitionBody) && AnatomyApi.ready(transitionSupport),
                "Transition holdout must enter READY anatomy ownership before the legacy participant cleanup tick");
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.state(transitionBody).support==transitionSupport,
                "Transition holdout requires the naturally acquired legacy support relation to survive until handoff is exercised");

            transitionBody.setPos(transitionSupport.getX()+.2,transitionFloor.bounds().maxY+.5,transitionSupport.getZ());
            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            var transitionLanding=AnatomyMovement.collide(transitionBody,new Vec3(0,-1,0));
            transitionBody.setPos(transitionBody.position().add(transitionLanding));
            AnatomyMovement.afterMove(transitionBody);
            h.assertTrue(AnatomyMovement.supported(transitionBody)
                    && AnatomyMovement.contact(transitionBody)!=null
                    && AnatomyMovement.contact(transitionBody).support()==transitionSupport
                    && AnatomyMovement.suppressesPush(transitionBody,transitionSupport),
                "Transition holdout must establish the canonical material pair before probing Entity.push");

            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            transitionSupport.push(transitionBody);
            Vec3 transitionManagedSupportCallerBody=transitionBody.getDeltaMovement();
            Vec3 transitionManagedSupportCallerSupport=transitionSupport.getDeltaMovement();

            transitionBody.setDeltaMovement(Vec3.ZERO);transitionSupport.setDeltaMovement(Vec3.ZERO);
            transitionBody.push(transitionSupport);
            Vec3 transitionManagedBodyCallerBody=transitionBody.getDeltaMovement();
            Vec3 transitionManagedBodyCallerSupport=transitionSupport.getDeltaMovement();

            h.assertTrue(transitionManagedSupportCallerBody.distanceToSqr(transitionVanillaSupportCallerBody)<=1e-12
                    && transitionManagedSupportCallerSupport.distanceToSqr(transitionVanillaSupportCallerSupport)<=1e-12
                    && transitionManagedBodyCallerBody.distanceToSqr(transitionVanillaBodyCallerBody)<=1e-12
                    && transitionManagedBodyCallerSupport.distanceToSqr(transitionVanillaBodyCallerSupport)<=1e-12,
                "READY handoff must invalidate stale legacy push suppression immediately, without waiting for Platforms.tick: "
                    +"support->body vanilla body="+transitionVanillaSupportCallerBody+" support="+transitionVanillaSupportCallerSupport
                    +" managed body="+transitionManagedSupportCallerBody+" support="+transitionManagedSupportCallerSupport
                    +"; body->support vanilla body="+transitionVanillaBodyCallerBody+" support="+transitionVanillaBodyCallerSupport
                    +" managed body="+transitionManagedBodyCallerBody+" support="+transitionManagedBodyCallerSupport);
        } finally {
            transitionBody.discard();transitionSupport.discard();AnatomyRuntime.stop(server);
        }
        h.succeed();
    }
}
