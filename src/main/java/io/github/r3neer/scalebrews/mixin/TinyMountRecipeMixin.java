package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.config.ScaleRules;
import io.github.r3neer.scalebrews.item.ScaleItems;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the item registered for old inventories, but gate crafting in this world's rules. */
@Mixin(ShapelessRecipe.class)
public abstract class TinyMountRecipeMixin {
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void scalebrews$enabled(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!ScaleRules.get(level).tinyMounts()
                && ((ShapelessRecipe)(Object)this).assemble(input).is(ScaleItems.FLOWER_ON_A_STICK))
            cir.setReturnValue(false);
    }
}
