package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.WolfMount;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Wolf.class)
public abstract class WolfMountMixin extends LivingEntity implements PlayerRideableJumping {
    protected WolfMountMixin(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void scalebrews$actions(CallbackInfo ci) {
        // Wolves do not advance LivingEntity's swing clock themselves. Tick on both sides
        // so native animation packets produce a finite, interpolated pack animation.
        updateSwingTime();
        WolfMount.tick((Wolf)(Object)this);
    }
    public boolean canJump() {
        var wolf = (Wolf)(Object)this;
        return WolfMount.enabled(wolf) && TinyMounts.controller(wolf) != null && wolf.onGround();
    }
    // Vanilla supplies the charge HUD. Actions use authoritative input duration, never packet power.
    public void onPlayerJump(int amount) {}
    public void handleStartJump(int amount) {}
    public void handleStopJump() {}
}
