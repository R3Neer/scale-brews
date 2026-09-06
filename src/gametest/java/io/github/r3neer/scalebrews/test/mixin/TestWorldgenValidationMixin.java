package io.github.r3neer.scalebrews.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.scalebrews.test.TestWorldgenValidation;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class TestWorldgenValidationMixin {
    @Inject(method="validate",at=@At("HEAD"),cancellable=true)
    private static void scalebrews$deferUntilDatapacks(CallbackInfo ci) {
        if(TestWorldgenValidation.defer()) ci.cancel();
    }
    @WrapOperation(method="validate",at=@At(value="INVOKE",target="Lnet/minecraft/data/registries/VanillaRegistries;createLookup()Lnet/minecraft/core/HolderLookup$Provider;"))
    private static HolderLookup.Provider scalebrews$loadedRegistries(Operation<HolderLookup.Provider> original) {
        var loaded=TestWorldgenValidation.context();
        return loaded!=null ? loaded : original.call();
    }
}
