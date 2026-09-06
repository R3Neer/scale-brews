package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.test.LandingObservation;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class TestLandingObservationMixin {
    @Inject(method = "gameEvent", at = @At("HEAD"))
    private void test$event(Holder<GameEvent> event, Vec3 position, GameEvent.Context context, CallbackInfo ci) {
        var observation = LandingObservation.ACTIVE.get();
        if (observation != null && event == GameEvent.HIT_GROUND && context.sourceEntity() == observation.entity) observation.landings++;
    }
    @Inject(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At("HEAD"))
    private void test$particles(ParticleOptions particle, double x, double y, double z, int count,
            double dx, double dy, double dz, double speed, CallbackInfoReturnable<Integer> ci) {
        var observation = LandingObservation.ACTIVE.get();
        if (observation == null) return;
        var type = particle.getType();
        if (type == ParticleTypes.SMALL_GUST || type == ParticleTypes.GUST_EMITTER_SMALL || type == ParticleTypes.GUST_EMITTER_LARGE) observation.gusts++;
        if (type == ParticleTypes.BLOCK) observation.terrain++;
    }
}
