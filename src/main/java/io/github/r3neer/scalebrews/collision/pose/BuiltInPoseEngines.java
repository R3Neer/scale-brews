package io.github.r3neer.scalebrews.collision.pose;

import io.github.r3neer.scalebrews.collision.api.CollisionEngines;
import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Registers Scale's reusable built-in pose families once during common initialization. */
public final class BuiltInPoseEngines {
    private BuiltInPoseEngines() {}

    private static final Map<Identifier, PoseEngine> BUILT_INS = builtIns();

    private static Map<Identifier, PoseEngine> builtIns() {
        var result = new LinkedHashMap<Identifier, PoseEngine>();
        result.put(id("player_walking"), new PlayerWalkingPoseEngine());
        result.put(id("quadruped"), new QuadrupedPoseEngine());
        result.put(id("chicken"), new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.CHICKEN));
        result.put(id("villager"), new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.VILLAGER));
        result.put(id("iron_golem"), new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.IRON_GOLEM));
        result.put(id("ghast"), new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.GHAST));
        result.put(id("feline"), new FelinePoseEngine());
        result.put(id("equine"), new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.EQUINE));
        result.put(id("bee"), new VanillaFamilyPoseEngine(VanillaFamilyPoseEngine.Family.BEE));
        result.put(id("static"), (geometry, inputs, parameters) ->
            inputs.ordinary() && parameters.isEmpty() ? Optional.of(Map.of()) : Optional.empty());
        result.put(id("mojang_keyframes"), new MojangKeyframePoseEngine());
        return Map.copyOf(result);
    }

    public static synchronized void initialize() {
        BUILT_INS.forEach(BuiltInPoseEngines::registerReserved);
    }

    private static void registerReserved(Identifier id, PoseEngine engine) {
        var existing = CollisionEngines.pose(id);
        if (existing.isEmpty()) {
            CollisionEngines.registerPose(id, engine);
            return;
        }
        if (existing.get() != engine)
            throw new IllegalStateException("Scale built-in pose id was pre-claimed by another owner: " + id);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("scalebrews", path);
    }
}
