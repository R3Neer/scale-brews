package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Prepared-lane regression for the legacy endpoint-carry fallback.
 *
 * <p>The material anchor is the local face point published by the acquired
 * {@link AnatomyMovement.SurfaceContact}; an entity origin is not generally that point (notably
 * for boats). Rotation must therefore transport the body by the material anchor delta rather than
 * treating {@code Entity.position()} as if it were itself the support-space anchor.</p>
 */
public final class S08PreparedLegacyEndpointCarryProof {
    private S08PreparedLegacyEndpointCarryProof() {}

    static void run(GameTestHelper h) {
        var support = h.spawn(net.minecraft.world.entity.EntityTypes.COW, 2, 20, 2);
        support.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(4);
        support.refreshDimensions();
        support.setNoAi(true);
        support.setNoGravity(true);

        GeometryProvider provider = entity -> java.util.Optional.of(new GeometryProvider.Snapshot(1, java.util.Map.of(
            "body", ConvexBox.of(new AABB(-2, 0, -2, 2, 2, 2),
                new Matrix4f().rotateY((float) Math.toRadians(entity.yBodyRot))).move(entity.position()))));
        AnatomyMovement.register(support, provider);

        var boat = h.spawn(net.minecraft.world.entity.EntityTypes.OAK_BOAT, 2, 23, 2);
        var rider = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        rider.startRiding(boat, true, true);
        try {
            boat.setPos(support.position().add(.7, 3, .4));
            boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -2, 0));
            h.assertTrue(AnatomyMovement.contact(boat) != null,
                "Occupied boat lands on convex geometry through Entity.move");
            h.assertTrue(io.github.r3neer.scalebrews.platform.PlatformPhysics.touching(boat),
                "Network grounding query sees anatomical contact without legacy surface state");

            var surface = AnatomyMovement.surface(boat);
            h.assertTrue(surface != null,
                "Material acquisition publishes the exact local face anchor used by passive carry");

            boat.positionRider(rider);
            var riderBefore = rider.position();
            var before = boat.position();
            var oldBox = provider.sample(support).orElseThrow().pieces().get("body");
            var oldAnchor = oldBox.point(surface.localPoint());

            support.setPos(support.position().add(.2, .1, .3));
            support.yBodyRot += 20;
            var newBox = provider.sample(support).orElseThrow().pieces().get("body");
            var newAnchor = newBox.point(surface.localPoint());
            var expectedDelta = newAnchor.subtract(oldAnchor);
            var expected = before.add(expectedDelta);

            AnatomyMovement.carry(boat);
            h.assertTrue(boat.position().distanceToSqr(expected) < 1e-9,
                "Material contact follows the published face anchor through translation and rotation");
            h.assertTrue(rider.position().subtract(riderBefore).distanceToSqr(expectedDelta) < 1e-9,
                "Native passenger seat follows the transported root immediately");

            var riderAfter = rider.position();
            AnatomyMovement.carry(boat);
            h.assertTrue(boat.position().distanceToSqr(expected) < 1e-9,
                "Repeated carry cannot duplicate transport");
            h.assertTrue(rider.position().equals(riderAfter) && AnatomyMovement.transport(rider) == null,
                "Passenger placement neither duplicates displacement nor creates independent transport");
            h.assertTrue(AnatomyMovement.transport(boat).displacement().distanceToSqr(expectedDelta) < 1e-9,
                "Passive displacement is accounted separately from the material anchor delta");
            h.assertTrue(rider.getVehicle() == boat && AnatomyMovement.contact(rider) == null,
                "Vehicle root carries without independent passenger support");
        } finally {
            boat.discard();
            rider.discard();
            support.discard();
        }
    }
}
