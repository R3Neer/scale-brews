package io.github.r3neer.scalebrews.mixin;

import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface BeaconEffectsAccessor {
    @Mutable
    @Accessor("BEACON_EFFECTS")
    static void scalebrews$setEffects(List<List<Holder<MobEffect>>> effects) {
        throw new AssertionError("Mixin was not applied");
    }

    @Accessor("VALID_EFFECTS")
    static Set<Holder<MobEffect>> scalebrews$validEffects() {
        throw new AssertionError("Mixin was not applied");
    }
}
