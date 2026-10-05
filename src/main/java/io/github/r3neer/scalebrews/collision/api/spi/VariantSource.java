package io.github.r3neer.scalebrews.collision.api.spi;

import net.minecraft.world.entity.LivingEntity;

/**
 * Reusable authority-side structural trait reader. Null means unavailable.
 * Reads must be pure, local, bounded and world-thread confined: no renderer,
 * geometry evaluation, catalog selection, reflection or global entity queries.
 * Owners of non-synchronized traits notify AnatomyApi.variantChanged after mutation.
 */
@FunctionalInterface
public interface VariantSource {
    String value(LivingEntity entity);
}
