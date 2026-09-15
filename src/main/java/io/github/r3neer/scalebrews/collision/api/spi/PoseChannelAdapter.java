package io.github.r3neer.scalebrews.collision.api.spi;

import net.minecraft.world.entity.LivingEntity;

/**
 * Init-time extension point for server-authoritative pose channels owned by an external model family.
 *
 * <p>Adapters publish scalar state only. They must never upload or derive client renderer transforms.
 * Scale validates names, finiteness, cardinality and duplicate ownership at the sampling boundary.</p>
 */
@FunctionalInterface
public interface PoseChannelAdapter {
    void sample(LivingEntity entity, ChannelSink sink);

    @FunctionalInterface
    interface ChannelSink {
        void put(String name, float value);
    }
}
