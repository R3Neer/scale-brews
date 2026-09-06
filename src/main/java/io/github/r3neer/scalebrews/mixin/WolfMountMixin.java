package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.WolfMount;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.mount.WolfTaming;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Wolf.class)
public abstract class WolfMountMixin extends LivingEntity implements PlayerRideableJumping, WolfTaming.Trust {
    @Unique private int scalebrews$trust;
    public int scalebrews$getTrust() { return scalebrews$trust; }
    public void scalebrews$setTrust(int trust) { scalebrews$trust = Math.clamp(trust, 0, WolfTaming.MAX_TRUST); }
    protected WolfMountMixin(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void scalebrews$saveTrust(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        output.putInt("scalebrews:trust", scalebrews$trust);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void scalebrews$loadTrust(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        scalebrews$setTrust(input.getIntOr("scalebrews:trust", 0));
    }
    @Inject(method = "tryToTame", at = @At("TAIL"))
    private void scalebrews$boneTrust(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        WolfTaming.boneAccepted((Wolf)(Object)this);
    }
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
