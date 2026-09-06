package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.physics.ScalePhysics;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VibrationSystem.Listener.class)
public abstract class ScaleVibrationMixin {
    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    private void scalebrews$quietMovement(ServerLevel level, Holder<GameEvent> event,
            GameEvent.Context context, Vec3 origin, CallbackInfoReturnable<Boolean> cir) {
        Entity source = context.sourceEntity();
        if (!io.github.r3neer.scalebrews.config.ScaleRules.get(level).quietensMovement()) return;
        double shrinking = source instanceof net.minecraft.world.entity.player.Player p
                ? io.github.r3neer.scalebrews.scale.ScaleSize.shrinking(p) : 0;
        if (shrinking == 0 || source.isSteppingCarefully() || !event.is(ScalePhysics.QUIET_MOVEMENT)) return;
        var listener = (VibrationSystem.Listener) (Object) this;
        double radius = listener.getListenerRadius() * ScalePhysics.vibrationRange(shrinking, source.isSprinting());
        listener.getListenerSource().getPosition(level).ifPresent(destination -> {
            if (origin.distanceToSqr(destination) > radius * radius) cir.setReturnValue(false);
        });
    }
}
