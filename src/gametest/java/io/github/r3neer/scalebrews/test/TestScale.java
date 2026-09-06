package io.github.r3neer.scalebrews.test;

import io.github.r3neer.scalebrews.scale.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Advance only the size subsystem; avoid aging effects or running unrelated world/food ticks. */
final class TestScale {
    private TestScale() {}
    static void settle(LivingEntity entity) {
        var access=(io.github.r3neer.scalebrews.test.mixin.TestScaleTransitionAccess)entity;
        var transition = access.test$transition();
        if(transition==null) { transition=new ScaleTransition(); access.test$transition(transition); }
        for(int i=0;i<ScaleTransition.DURATION;i++) transition.tick(entity);
        ScaleSize.tick(entity);
        entity.refreshDimensions();
        if(entity instanceof Player player) {
            player.setSprinting(player.isSprinting());
            io.github.r3neer.scalebrews.physics.ScaleSneaking.tick(player);
        }
    }
}
