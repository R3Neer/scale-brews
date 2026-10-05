package io.github.r3neer.scalebrews.collision.internal;

import io.github.r3neer.scalebrews.collision.api.CollisionVariantSources;
import io.github.r3neer.scalebrews.collision.api.spi.VariantSource;
import java.util.Set;

/** Common/server-safe structural families, independent of concrete entity types. */
public final class BuiltInVariantSources {
    private static final VariantSource AGE = entity -> entity.isBaby() ? "baby" : "adult";
    private BuiltInVariantSources() {}

    public static synchronized void initialize() {
        var existing = CollisionVariantSources.snapshot().get("age");
        if (existing == null) CollisionVariantSources.register("age", Set.of("adult", "baby"), AGE);
        else if (existing.reader() != AGE) throw new IllegalStateException("Built-in age source was claimed by another owner");
    }
}
