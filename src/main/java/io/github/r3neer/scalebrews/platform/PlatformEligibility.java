package io.github.r3neer.scalebrews.platform;

/** Pure policy decision, shared by live eligibility and decoded-datapack tests. */
public final class PlatformEligibility {
    private PlatformEligibility() {}
    public static boolean allows(PlatformPolicy policy,PlatformDefinition profile,String category,double widthRatio) {
        return policy.enabled() && profile!=null && profile.enabled() && category!=null
            && policy.bodies().getOrDefault(category,true)
            && policy.supports().getOrDefault(profile.entity(),true)
            && Double.isFinite(widthRatio) && widthRatio>0
            && widthRatio<=profile.maxRatio().orElse(policy.maxWidthRatio())+1e-7;
    }
}
