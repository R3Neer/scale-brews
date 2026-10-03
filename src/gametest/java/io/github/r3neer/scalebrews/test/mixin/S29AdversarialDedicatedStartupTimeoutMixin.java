package io.github.r3neer.scalebrews.test.mixin;

import net.fabricmc.fabric.impl.client.gametest.util.DedicatedServerImplUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Test infrastructure only: Fabric's fixed ten-second server constructor wait. */
@Mixin(value=DedicatedServerImplUtil.class,remap=false)
public abstract class S29AdversarialDedicatedStartupTimeoutMixin {
    @ModifyConstant(method="start",constant=@Constant(longValue=10L),remap=false,require=1)
    private static long s29$startupTimeout(long original){return 60L;}
}
