package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.collision.geometry.ConvexBox;
import io.github.r3neer.scalebrews.collision.physics.ConservativeSweep;
import io.github.r3neer.scalebrews.collision.physics.TemporalResponse;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** S09 A9/A11: screening may reduce CCD work, never turn a within-budget clear sweep into exhaustion. */
public final class S09TemporalScreeningBudgetTests {
    @GameTest
    public void screeningCannotCostMoreThanTheClearSweepItReplaces(GameTestHelper h) {
        var body=new AABB(-.05,-.05,-.05,.05,.05,.05);
        // Keep the actual geometry static and clear by ~0.064 blocks, but use a valid loose
        // deformation bound near the prepared cow's real 13-16 block/tick bounds. This reproduces
        // the real A9 ratio without relying on a theatrical speed=200 certificate.
        var distant=ConvexBox.of(new AABB(.114,-.05,-.05,.214,.05,.05),new Matrix4f());
        var motion=new ConservativeSweep.Motion(t->distant,16.0,Vec3.ZERO);

        var direct=ConservativeSweep.query(body,Vec3.ZERO,motion,256);
        h.assertTrue(direct.status()==ConservativeSweep.Status.CLEAR && direct.evaluations()>128 && direct.evaluations()<256,
            "Fixture requires a valid clear CCD that is expensive but still inside the contractual 256 budget: "+direct);

        var response=TemporalResponse.resolve(body,Vec3.ZERO,Map.of("clear-but-loose",motion),32,256);
        h.assertTrue(response.status()==TemporalResponse.Status.COMPLETE,
            "A screening optimization may not make a single-piece CLEAR query exhaust when the unscreened CCD already fits the same 256 budget: direct="
                +direct+" response="+response);
        h.assertTrue(response.displacement().lengthSqr()<1e-20 && response.contacts().isEmpty(),
            "The clear loose-bound piece must remain physically inert: "+response);
        h.succeed();
    }
}
