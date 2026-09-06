package io.github.r3neer.scalebrews.test.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import io.github.r3neer.scalebrews.mount.TinyMountDefinition;
import io.github.r3neer.scalebrews.test.TinyDefinitionTestScope;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TinyMounts.class)
public class TestTinyDefinitionMixin {
    @Inject(method = "configuredDefinition", at = @At("HEAD"), cancellable = true)
    private static void definition(Entity entity, CallbackInfoReturnable<TinyMountDefinition> cir) {
        var value = TinyDefinitionTestScope.CURRENT.get();
        if (value != null && value.entity().equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))
            cir.setReturnValue(value);
    }
}
