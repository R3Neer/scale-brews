package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMountTemptation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public abstract class TinyMountTemptGoalMixin {
    @Shadow @Final protected Mob mob;

    @Inject(method = "shouldFollow", at = @At("RETURN"), cancellable = true)
    private void scalebrews$steeringItem(LivingEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && TinyMountTemptation.follows(mob, player)) cir.setReturnValue(true);
    }
}
