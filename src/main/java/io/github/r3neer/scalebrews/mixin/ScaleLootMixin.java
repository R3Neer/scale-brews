package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.loot.ScaleLoot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class ScaleLootMixin {
    @ModifyVariable(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"), argsOnly = true)
    private Consumer<ItemStack> scalebrews$materials(Consumer<ItemStack> output) {
        return stack -> ScaleLoot.emit((LivingEntity)(Object)this, stack, output);
    }
}
