package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMountTemptation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class TinyMountGoalsMixin {
    @Shadow @Final protected GoalSelector goalSelector;
    @Unique private boolean scalebrews$temptationInitialized;

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void scalebrews$temptation(CallbackInfo ci) {
        if (scalebrews$temptationInitialized) return;
        scalebrews$temptationInitialized = true;
        // Registry data and subclass goals are ready here, unlike in the Mob constructor.
        TinyMountTemptation.install((Mob)(Object)this, goalSelector);
    }
}
