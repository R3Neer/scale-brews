package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.scale.ScaleSprintHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(
            method = "setSprinting",
            at = @At("HEAD"),
            cancellable = true
    )
    private void scalebrews$blockGrowthThreeSprint(
            boolean isSprinting,
            CallbackInfo ci
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (isSprinting && ScaleSprintHandler.blocksSprinting(entity)) {
            entity.setSprinting(false);
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "setSprinting",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addTransientModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"
            ),
            index = 0
    )
    private AttributeModifier scalebrews$replaceSprintModifier(
            AttributeModifier vanillaModifier
    ) {
        return ScaleSprintHandler.sprintModifierFor(
                (LivingEntity) (Object) this,
                vanillaModifier
        );
    }
}