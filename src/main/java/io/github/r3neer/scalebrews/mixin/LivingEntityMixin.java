package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.scale.ScaleSprintHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import io.github.r3neer.scalebrews.scale.ScaleTransition;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique private int scalebrews$sprintState;
    @Unique private ScaleTransition scalebrews$transition;

    @Inject(method = "tick", at = @At("HEAD"))
    private void scalebrews$tick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player) {
            io.github.r3neer.scalebrews.mount.TinyMounts.enforceRider((Player) entity);
            io.github.r3neer.scalebrews.physics.ScaleSneaking.tick((Player) entity);
            int state = ScaleSprintHandler.state(entity);
            if (state != scalebrews$sprintState) {
                scalebrews$sprintState = state;
                entity.setSprinting(entity.isSprinting());
            }
        }
        if (!entity.level().isClientSide()) {
            if (scalebrews$transition == null) scalebrews$transition = new ScaleTransition();
            scalebrews$transition.tick(entity);
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
