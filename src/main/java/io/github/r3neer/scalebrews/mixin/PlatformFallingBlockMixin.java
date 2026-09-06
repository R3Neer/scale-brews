package io.github.r3neer.scalebrews.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.r3neer.scalebrews.platform.*;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class PlatformFallingBlockMixin {
    @Shadow public int time;
    @Inject(method="tick",at=@At("HEAD"))
    private void scalebrews$pauseAge(CallbackInfo ci) {
        if(Platforms.supported((FallingBlockEntity)(Object)this) && time>0) time--;
    }
    // Only placement's grounded test changes. The normal move/impact damage still runs.
    @ModifyExpressionValue(method="tick",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/item/FallingBlockEntity;onGround()Z"))
    private boolean scalebrews$deferPlacement(boolean ground) {
        return ground && !Platforms.supported((FallingBlockEntity)(Object)this);
    }
    @ModifyExpressionValue(method="causeFallDamage",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private java.util.List<net.minecraft.world.entity.Entity> scalebrews$impactSupport(java.util.List<net.minecraft.world.entity.Entity> original) {
        var body=(FallingBlockEntity)(Object)this;
        if(!Platforms.supported(body)) return original;
        var support=Platforms.state(body).support;
        if(original.contains(support) || support.isSpectator()
            || support instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) return original;
        var targets=new java.util.ArrayList<>(original);
        targets.add(support);
        return targets;
    }
}
