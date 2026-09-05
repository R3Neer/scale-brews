package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.scale.ScaleHealthHandler;
import java.util.Collection;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class ScaleHealthMixin {
    @WrapMethod(method = "onEffectAdded")
    private void scalebrews$added(MobEffectInstance effect, Entity source, Operation<Void> original) {
        var entity = (LivingEntity) (Object) this;
        var snapshot = ScaleHealthHandler.capture(entity, ScaleHealthHandler.isScaleEffect(effect));
        original.call(effect, source);
        snapshot.restore(entity);
    }

    @WrapMethod(method = "onEffectUpdated")
    private void scalebrews$updated(MobEffectInstance effect, boolean refresh, Entity source, Operation<Void> original) {
        var entity = (LivingEntity) (Object) this;
        var snapshot = ScaleHealthHandler.capture(entity, refresh && ScaleHealthHandler.isScaleEffect(effect));
        original.call(effect, refresh, source);
        snapshot.restore(entity);
    }

    @WrapMethod(method = "onEffectsRemoved")
    private void scalebrews$removed(Collection<MobEffectInstance> effects, Operation<Void> original) {
        var entity = (LivingEntity) (Object) this;
        var snapshot = ScaleHealthHandler.capture(entity, effects.stream().anyMatch(ScaleHealthHandler::isScaleEffect));
        original.call(effects);
        snapshot.restore(entity);
    }
}
