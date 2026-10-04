package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.test.S40NativeJumpAudit;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=LivingEntity.class,priority=900)
public abstract class S40NativeJumpAuditMixin {
    @Inject(method="jumpFromGround",at=@At("HEAD"))
    private void scalebrewsTest$jumpStart(CallbackInfo ci) {
        S40NativeJumpAudit.start((LivingEntity)(Object)this);
    }
    @Inject(method="jumpFromGround",at=@At("RETURN"))
    private void scalebrewsTest$jumpEnd(CallbackInfo ci) {
        S40NativeJumpAudit.end((LivingEntity)(Object)this);
    }
}
