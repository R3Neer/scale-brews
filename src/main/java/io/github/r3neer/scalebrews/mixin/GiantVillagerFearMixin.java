package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.config.ScaleRules;
import io.github.r3neer.scalebrews.physics.ScalePhysics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the villager's local nearest-visible-threat sensor; no attacker or reputation writes. */
@Mixin(VillagerHostilesSensor.class)
public abstract class GiantVillagerFearMixin {
    @Inject(method = "isMatchingEntity", at = @At("HEAD"), cancellable = true)
    private void scalebrews$giant(ServerLevel level, LivingEntity villager, LivingEntity candidate, CallbackInfoReturnable<Boolean> cir) {
        if (ScaleRules.get(level).villagerFear() && !candidate.isSpectator()
                && ScalePhysics.growth(candidate) >= 2 && candidate.distanceToSqr(villager) <= 64)
            cir.setReturnValue(true);
    }
}
