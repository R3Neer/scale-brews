package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.internal.AnatomyMovement;
import io.github.r3neer.scalebrews.collision.internal.GeometryProvider;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Deterministic prepared-lane proof for pair-local own-movement suspension and reacquisition. */
public final class S07PreparedPairSuspensionProof {
    private S07PreparedPairSuspensionProof() {}

    static void run(GameTestHelper h) {
        var support=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,20,2);
        var other=h.spawn(net.minecraft.world.entity.EntityTypes.COW,3,20,2);
        support.setNoAi(true);support.setNoGravity(true);
        other.setNoAi(true);other.setNoGravity(true);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAttribute(Attributes.SCALE).setBaseValue(.2);player.refreshDimensions();
        player.setPos(support.position());

        // Own-movement recovery is hard-capped at four blocks.  This convex requires
        // more than five blocks of translation on every escape axis, so failure is
        // geometric and independent of GameTest block coordinates or clip ordering.
        var enclosing=ConvexBox.of(new AABB(-5,-5,-5,5,5,5),new Matrix4f()).move(support.position());
        AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(1,Map.of("body",enclosing))));
        try {
            var before=player.position();
            Vec3 blocked=AnatomyMovement.collide(player,new Vec3(.01,0,0));
            player.setPos(player.position().add(blocked));
            h.assertTrue(AnatomyMovement.suspended(player,support) && !AnatomyMovement.suspended(player,other),
                "Prepared own-movement recovery must suspend only the geometrically unresolvable body/support pair");
            h.assertTrue(player.position().distanceTo(before)<.1,
                "Prepared recovery must not teleport the body beyond the bounded separation policy");

            AnatomyMovement.register(support,e->java.util.Optional.of(new GeometryProvider.Snapshot(2,
                Map.of("body",enclosing.move(new Vec3(30,0,0))))));
            h.assertTrue(!AnatomyMovement.suspended(player,support),
                "Prepared pair suspension must reacquire automatically after the actual overlap ends");
        } finally {
            support.discard();other.discard();player.discard();
        }
    }
}
