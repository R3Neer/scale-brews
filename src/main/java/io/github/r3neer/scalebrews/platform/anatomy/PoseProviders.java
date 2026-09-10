package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.*;
import net.minecraft.resources.Identifier;

/** Common-only extension point. Register deterministic providers during mod initialization. */
public final class PoseProviders {
    private PoseProviders() {}
    private static final Map<Identifier,PoseProvider> PROVIDERS=new HashMap<>();
    static {
        register(Identifier.parse("scalebrews:player_walking"),new PlayerWalkingPose());
        register(Identifier.parse("scalebrews:quadruped"),new QuadrupedPose());
        register(Identifier.parse("scalebrews:chicken"),new VanillaFamilyPose(VanillaFamilyPose.Family.CHICKEN));
        register(Identifier.parse("scalebrews:villager"),new VanillaFamilyPose(VanillaFamilyPose.Family.VILLAGER));
        register(Identifier.parse("scalebrews:iron_golem"),new VanillaFamilyPose(VanillaFamilyPose.Family.IRON_GOLEM));
        register(Identifier.parse("scalebrews:ghast"),new VanillaFamilyPose(VanillaFamilyPose.Family.GHAST));
        register(Identifier.parse("scalebrews:feline"),new VanillaFamilyPose(VanillaFamilyPose.Family.FELINE));
        register(Identifier.parse("scalebrews:equine"),new VanillaFamilyPose(VanillaFamilyPose.Family.EQUINE));
        register(Identifier.parse("scalebrews:bee"),new VanillaFamilyPose(VanillaFamilyPose.Family.BEE));
        register(Identifier.parse("scalebrews:grizzly"),new GrizzlyPose());
        // Static is explicit in the world's JSON. Never use it as a missing-provider fallback.
        register(Identifier.parse("scalebrews:static"),(geometry,inputs)->inputs.ordinary()?Optional.of(Map.of()):Optional.empty());
    }
    public static synchronized void register(Identifier id,PoseProvider provider) {
        Objects.requireNonNull(id);Objects.requireNonNull(provider);
        if(PROVIDERS.putIfAbsent(id,provider)!=null)throw new IllegalArgumentException("Duplicate anatomical pose provider: "+id);
    }
    public static synchronized Optional<PoseProvider> find(Identifier id){return Optional.ofNullable(PROVIDERS.get(id));}
}
