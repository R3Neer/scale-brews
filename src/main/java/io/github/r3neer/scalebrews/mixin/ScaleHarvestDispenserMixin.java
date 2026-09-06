package io.github.r3neer.scalebrews.mixin;
import io.github.r3neer.scalebrews.loot.ScaleLoot;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(ShearsDispenseItemBehavior.class)
public abstract class ScaleHarvestDispenserMixin {
    @WrapOperation(method = "tryShearEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Shearable;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void harvest(Shearable entity, ServerLevel level, SoundSource sound, ItemStack tool, Operation<Void> original) {
        if (entity instanceof LivingEntity living) ScaleLoot.harvesting(living, () -> original.call(entity, level, sound, tool));
        else original.call(entity, level, sound, tool);
    }
}
