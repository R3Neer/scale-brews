package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.scale.ScaleGait;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Entity.class)
public abstract class ScaleStepSoundMixin {
    @ModifyArg(
            method = {"playStepSound", "playCombinationStepSounds", "playMuffledStepSound"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
            ),
            index = 2
    )
    private float scalebrews$scaleStepPitch(float vanillaPitch) {
        if (!((Object) this instanceof LivingEntity living)) return vanillaPitch;
        return ScaleGait.stepPitch(living, vanillaPitch);
    }
}
