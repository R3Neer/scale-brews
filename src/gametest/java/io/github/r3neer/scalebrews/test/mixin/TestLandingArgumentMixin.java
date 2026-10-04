package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.platform.Platforms;
import io.github.r3neer.scalebrews.test.LandingObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only observation of the actual native landing arguments. */
@Mixin(LivingEntity.class)
public abstract class TestLandingArgumentMixin {
    @Inject(method="checkFallDamage",at=@At("HEAD"))
    private void test$landingArguments(double movementY,boolean onGround,BlockState ground,
            BlockPos pos,CallbackInfo ci) {
        var observation=LandingObservation.ACTIVE.get();
        var entity=(LivingEntity)(Object)this;
        if(observation==null || observation.entity!=entity || !onGround)return;
        observation.groundedChecks++;
        if(!ground.isAir())observation.nonAirGroundedChecks++;
        observation.materialAtGroundedCheck=Platforms.supported(entity);
    }
}
