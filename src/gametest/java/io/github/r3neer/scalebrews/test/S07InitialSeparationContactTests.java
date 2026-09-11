package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S07 holdout for the shared own-movement path after bounded initial separation. */
public final class S07InitialSeparationContactTests {
    @GameTest
    public void initialSeparationOntoSupportingFaceMustCreateAnchor(GameTestHelper h) {
        var level=h.getLevel();
        var support=h.spawn(EntityTypes.COW,2,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        var body=h.makeMockServerPlayerInLevel();
        body.setNoGravity(true);
        body.getAttribute(Attributes.SCALE).setBaseValue(.2);body.refreshDimensions();
        var center=support.position();
        var finalFloor=ConvexBox.of(new AABB(-1,-1,-1,1,0,1),new Matrix4f()).move(center.add(0,1,0));
        GeometryProvider provider=entity->Optional.of(new GeometryProvider.Snapshot(92,Map.of("back",finalFloor)));
        AnatomyMovement.activate(level);
        AnatomyMovement.register(support,provider);
        body.setPos(center.x,center.y+.5,center.z);body.setOnGround(false);
        try {
            h.assertTrue(io.github.r3neer.scalebrews.platform.Platforms.eligible(body,support),
                "Fixture body must be eligible for the anatomical support");
            Vec3 moved=AnatomyMovement.collide(body,new Vec3(.2,0,0));
            body.setPos(body.position().add(moved));
            AnatomyMovement.afterMove(body);
            h.assertTrue(Math.abs(body.getY()-center.y-1)<1e-6 && Math.abs(body.getX()-center.x-.2)<1e-6,
                "Initial separation must place the body on the final supporting face while preserving tangent motion");
            var contact=AnatomyMovement.contact(body);
            h.assertTrue(contact!=null && contact.support()==support && body.onGround(),
                "A successful initial separation onto a supporting face must establish the material anchor even when the later tangential sweep has no contact event");
        } finally {
            AnatomyMovement.clear(body);
            AnatomyMovement.deactivate(level);
            support.discard();body.discard();
        }
        h.succeed();
    }
}
