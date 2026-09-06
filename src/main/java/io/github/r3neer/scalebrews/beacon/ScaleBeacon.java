package io.github.r3neer.scalebrews.beacon;

import io.github.r3neer.scalebrews.effect.ScaleEffects;
import io.github.r3neer.scalebrews.mixin.BeaconEffectsAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

public final class ScaleBeacon {
    private ScaleBeacon() {}

    public static void initialize() {
        var tiers = new ArrayList<>(BeaconBlockEntity.BEACON_EFFECTS);
        List<Holder<MobEffect>> thirdTier = new ArrayList<>(tiers.get(2));
        for (var effect : List.of(ScaleEffects.GROWTH, ScaleEffects.SHRINKING)) {
            if (!thirdTier.contains(effect)) thirdTier.add(effect);
            BeaconEffectsAccessor.scalebrews$validEffects().add(effect);
        }
        // FrozenLib and other beacon extensions append their powers after our initializer.
        tiers.set(2, thirdTier);
        BeaconEffectsAccessor.scalebrews$setEffects(tiers);
    }
}
