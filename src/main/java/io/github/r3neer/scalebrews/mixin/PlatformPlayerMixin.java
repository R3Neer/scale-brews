package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.platform.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlatformPlayerMixin {
    @Inject(method="interactOn",at=@At("HEAD"),cancellable=true)
    private void scalebrews$place(Entity target, net.minecraft.world.InteractionHand hand, Vec3 location,
            CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        var result=PlatformPlacement.interact((Player)(Object)this,target,hand,location);
        if(result != net.minecraft.world.InteractionResult.PASS) cir.setReturnValue(result);
    }
    @Inject(method="maybeBackOffFromEdge",at=@At("HEAD"),cancellable=true)
    private void scalebrews$edge(Vec3 delta, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        Player self=(Player)(Object)this;
        if (Platforms.supported(self) && (type==MoverType.SELF || type==MoverType.PLAYER))
            cir.setReturnValue(PlatformPhysics.edge(self,delta));
    }
}
