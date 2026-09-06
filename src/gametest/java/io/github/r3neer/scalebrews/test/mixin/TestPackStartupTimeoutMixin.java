package io.github.r3neer.scalebrews.test.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Large worldgen packs need more than Fabric's hard-coded ten-second server bootstrap budget. */
@Mixin(targets="net.fabricmc.fabric.impl.client.gametest.util.DedicatedServerImplUtil", remap=false)
public class TestPackStartupTimeoutMixin {
    @ModifyConstant(method="start", constant=@Constant(longValue=10L))
    private static long scalebrews$bootstrapBudget(long original) {
        return FabricLoader.getInstance().isModLoaded("wilderwild") ? 120L : original;
    }
}
