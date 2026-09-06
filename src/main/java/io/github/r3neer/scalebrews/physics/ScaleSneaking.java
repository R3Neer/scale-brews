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

    public static double bonus(double shrinking, int swiftSneak) {
        if (shrinking <= 1 || swiftSneak < 1) return 0;
        int enchantment = Math.min(3, swiftSneak);
        return io.github.r3neer.scalebrews.scale.ScaleSize.interpolate(shrinking,0,0,
                Math.min(.10,enchantment*.05),Math.min(.25,enchantment*.10));
    }

    public static void tick(Player player) {
        if (player.level().isClientSide()) return;
        var attribute = player.getAttribute(Attributes.SNEAKING_SPEED);
        if (attribute == null) return;
        double shrinking = io.github.r3neer.scalebrews.scale.ScaleSize.shrinking(player);
        int swiftSneak = shrinking <= 1 ? 0 : EnchantmentHelper.getItemEnchantmentLevel(
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
