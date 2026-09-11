package io.github.r3neer.scalebrews.collision.integration;

import io.github.r3neer.scalebrews.collision.data.CollisionPolicy;
import net.minecraft.resources.Identifier;

/** Pure canonical policy evaluation. No legacy surface or solver type is accepted here. */
public final class CollisionRules {
    private CollisionRules() {}

    public static CollisionPolicy.Rule resolve(CollisionPolicy policy, CollisionPolicy.Patch profile,
                                               String category, Identifier supportType) {
        if (policy == null) throw new IllegalArgumentException("Missing collision policy");
        return policy.resolve(category, supportType, profile);
    }

    public static boolean allows(CollisionPolicy policy, CollisionPolicy.Patch profile,
                                 String category, Identifier supportType, double widthRatio) {
        if (category == null || supportType == null || !Double.isFinite(widthRatio) || widthRatio <= 0) return false;
        var resolved = resolve(policy, profile, category, supportType);
        return resolved.enabled() && widthRatio <= resolved.maxWidthRatio() + 1e-7;
    }
}
