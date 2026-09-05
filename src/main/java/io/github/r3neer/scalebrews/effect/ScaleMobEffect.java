package io.github.r3neer.scalebrews.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import io.github.r3neer.scalebrews.ScaleBrews;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ScaleMobEffect extends MobEffect {
    private final double scalePerLevel;

    public ScaleMobEffect(MobEffectCategory category, int color, double scalePerLevel) {
        super(category, color);
        this.scalePerLevel = scalePerLevel;
    }

    @Override
    public void createModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        super.createModifiers(amplifier, consumer);
        // Tooltip describes the final size; ScaleTransition owns the physical modifier.
        consumer.accept(Attributes.SCALE, new AttributeModifier(ScaleBrews.id("scale_tooltip"),
                scalePerLevel * Math.min(3, amplifier + 1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
