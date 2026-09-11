package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.SurfaceContact;
import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** FR-061: passive certified transport changes position/accounting, not voluntary velocity or fall state. */
public final class S08PassiveTransportStateTests {
    @GameTest
    public void certifiedPassiveCarryDoesNotRewriteVelocityOrFallDistance(GameTestHelper h) {
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
            double fall=body.fallDistance;Vec3 applied=new Vec3(.25,0,0);
            body.setPos(body.position().add(applied));
            var root=new AnatomyMovement.RootFrame(1,tick,support.position(),0,1,AnatomyMovement.gravity(support));
            h.assertTrue(AnatomyMovement.recordCertifiedTransport(body,support,surface,root,applied,material,material.move(applied)),
                "Certified passive transport accounting must accept the retained relation");
            h.assertTrue(body.getDeltaMovement().equals(voluntary),
                "Passive carry must not be converted into voluntary body velocity: "+body.getDeltaMovement());
            h.assertTrue(body.fallDistance==fall,
                "Passive carry must not rewrite accumulated fall distance: before="+fall+" after="+body.fallDistance);
        } finally {
            AnatomyMovement.clear(body);AnatomyMovement.deactivate(level);support.discard();body.discard();
        }
        h.succeed();
    }
}
