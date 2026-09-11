package io.github.r3neer.scalebrews.platform;

import io.github.r3neer.scalebrews.collision.integration.CollisionRules;
import io.github.r3neer.scalebrews.collision.migration.LegacyCollisionData;

/**
 * @deprecated Legacy compatibility facade. Eligibility policy is owned by
 * {@link CollisionRules}; this class preserves old regression callers only.
 */
@Deprecated
public final class PlatformEligibility {
    private PlatformEligibility() {}

    public static boolean allows(PlatformPolicy policy, PlatformDefinition profile, String category, double widthRatio) {
        if (profile == null) return false;
        return CollisionRules.allows(LegacyCollisionData.policy(policy), LegacyCollisionData.profilePolicy(profile),
            category, profile.entity(), widthRatio);
    }
}
