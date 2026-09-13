package io.github.r3neer.scalebrews.mixin;

import io.github.r3neer.scalebrews.mount.TinyMounts;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends vanilla dispenser equipping to the slots granted by Tiny Mount family data. */
@Mixin(LivingEntity.class)
public abstract class TinyMountDispenserMixin {
    @Inject(method = "canEquipWithDispenser", at = @At("HEAD"), cancellable = true)
    private void scalebrews$canEquipTinyMount(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof Mob mob)) return;
        var definition = TinyMounts.definition(mob);
        if (!TinyMounts.equipmentAvailable(mob, definition)) return;

        if (stack.is(Items.SADDLE) && mob.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
            cir.setReturnValue(true);
            return;
        }
        if (definition.bodyEquipment().isPresent()
                && TinyMounts.matchesBodyEquipment(definition, stack)
                && mob.getItemBySlot(EquipmentSlot.BODY).isEmpty()
                && mob.isEquippableInSlot(stack, EquipmentSlot.BODY)) {
            cir.setReturnValue(true);
        }
    }
}
