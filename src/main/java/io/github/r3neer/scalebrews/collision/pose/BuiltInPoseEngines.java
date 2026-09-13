package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Registers Scale's reusable built-in pose families once during common initialization. */
public final class BuiltInPoseEngines {
    private BuiltInPoseEngines() {}

    public static synchronized void initialize() {
        register("player_walking", new PlayerWalkingPoseEngine());
        register("quadruped", new QuadrupedPoseEngine());
        register("chicken", new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.CHICKEN));
        register("villager", new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.VILLAGER));
        register("iron_golem", new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.IRON_GOLEM));
        register("ghast", new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.GHAST));
        register("feline", new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.FELINE));
        register("equine", new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.EQUINE));
        register("bee", new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.BEE));
        register("static", (geometry, inputs, parameters) ->
            inputs.ordinary() && parameters.isEmpty() ? Optional.of(Map.of()) : Optional.empty());
        register("mojang_keyframes", new MojangKeyframePoseEngine());
    }

    private static void register(String path, PoseEngine engine) {
        var id = Identifier.fromNamespaceAndPath("scalebrews", path);
        if (CollisionEngines.pose(id).isEmpty()) CollisionEngines.registerPose(id, engine);
    }
}
