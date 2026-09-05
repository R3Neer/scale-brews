package io.github.r3neer.scalebrews.physics;

import io.github.r3neer.scalebrews.ScaleBrews;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class ScaleSneaking {
    public static final Identifier MODIFIER = ScaleBrews.id("swift_sneak_synergy");
    private ScaleSneaking() {}

    public static double bonus(int shrinking, int swiftSneak) {
        if (shrinking < 2 || swiftSneak < 1) return 0;
        int enchantment = Math.min(3, swiftSneak);
        return shrinking == 2 ? Math.min(0.10, enchantment * 0.05) : Math.min(0.25, enchantment * 0.10);
    }

    public static void tick(Player player) {
        if (player.level().isClientSide()) return;
        var attribute = player.getAttribute(Attributes.SNEAKING_SPEED);
        if (attribute == null) return;
        int shrinking = ScalePhysics.shrinking(player);
        int swiftSneak = shrinking < 2 ? 0 : EnchantmentHelper.getItemEnchantmentLevel(
                player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWIFT_SNEAK),
                player.getItemBySlot(EquipmentSlot.LEGS));
        double amount = bonus(shrinking, swiftSneak);
        if (amount == 0) {
            attribute.removeModifier(MODIFIER);
        } else {
            var existing = attribute.getModifier(MODIFIER);
            if (existing == null || existing.amount() != amount) {
                attribute.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }
}
