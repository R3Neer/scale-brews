package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime;
import io.github.r3neer.scalebrews.collision.internal.S24TrackingAuthorityTestSeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GameTest-only opt-in tracking authority; inactive unless a fixture explicitly scopes one action. */
@Mixin(value=AnatomyRuntime.class,remap=false)
public abstract class TestTrackingAuthorityMixin {
    @Inject(method="trackingGeneration",at=@At("HEAD"),cancellable=true,remap=false)
    private static void scalebrewsTest$trackingAuthority(ServerPlayer recipient,Entity body,CallbackInfoReturnable<Long> cir) {
        long generation=S24TrackingAuthorityTestSeam.generation(recipient,body);
        if(generation>0)cir.setReturnValue(generation);
    }
}
